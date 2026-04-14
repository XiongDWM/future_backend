package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiongdwm.future_backend.bo.StudioManageDto;
import com.xiongdwm.future_backend.platform.entity.PlatformAuditLog;
import com.xiongdwm.future_backend.platform.entity.PlatformUser;
import com.xiongdwm.future_backend.platform.entity.Studio;
import com.xiongdwm.future_backend.platform.repository.PlatformAuditLogRepository;
import com.xiongdwm.future_backend.platform.repository.PlatformUserRepository;
import com.xiongdwm.future_backend.platform.repository.StudioRepository;
import com.xiongdwm.future_backend.platform.repository.UserStudioMappingRepository;
import com.xiongdwm.future_backend.service.PlatformService;
import com.xiongdwm.future_backend.utils.exception.AuthenticationFailException;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

@Service
@Transactional("platformTransactionManager")
public class PlatformServiceImpl implements PlatformService {

    private final PlatformUserRepository platformUserRepository;
    private final StudioRepository studioRepository;
    private final UserStudioMappingRepository mappingRepository;
    private final PlatformAuditLogRepository auditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlatformServiceImpl(PlatformUserRepository platformUserRepository,
                               StudioRepository studioRepository,
                               UserStudioMappingRepository mappingRepository,
                               PlatformAuditLogRepository auditLogRepository,
                               JwtTokenProvider jwtTokenProvider) {
        this.platformUserRepository = platformUserRepository;
        this.studioRepository = studioRepository;
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String login(String username, String password) {
        var rawPassword = "XiongDWM01231996.";
        var p=passwordEncoder.encode(rawPassword); // 预热 BCrypt 加密器，避免首次登录时的性能问题
        System.out.println("!!!"+p);
        PlatformUser user = platformUserRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AuthenticationFailException("用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailException("用户名或密码错误");
        }
        return jwtTokenProvider.generatePlatformToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    @Override
    public List<StudioManageDto> getStudios() {
        List<Studio> studios = studioRepository.findAll();
        return studios.stream().map(s -> new StudioManageDto(
                s.getId(), s.getName(), s.getDbName(), s.getPlace(),
                s.getAssignDate(), s.getLastChargeAt(), s.getWillChargeAt(),
                s.getChargeAmount(), mappingRepository.countByStudioId(s.getId())
        )).toList();
    }

    @Override
    public void updateStudio(Long id, Map<String, Object> updates) {
        Studio studio = studioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作室不存在"));
        if (updates.containsKey("name")) studio.setName((String) updates.get("name"));
        if (updates.containsKey("place")) studio.setPlace((String) updates.get("place"));
        if (updates.containsKey("chargeAmount")) studio.setChargeAmount(((Number) updates.get("chargeAmount")).doubleValue());
        if (updates.containsKey("willChargeAt")) {
            Object val = updates.get("willChargeAt");
            if (val instanceof Number n) {
                studio.setWillChargeAt(new Date(n.longValue()));
            }
        }
        studioRepository.save(studio);
    }

    @Override
    public List<PlatformUser> getAdmins() {
        return platformUserRepository.findAllByDeletedFalse();
    }

    @Override
    public void createAdmin(String username, String password, PlatformUser.Role role, Long phone) {
        if (platformUserRepository.findByUsernameAndDeletedFalse(username).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        PlatformUser user = new PlatformUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setPhone(phone);
        user.setDeleted(false);
        user.setCreatedAt(new Date());
        platformUserRepository.save(user);
    }

    @Override
    public void updateAdmin(Long id, Map<String, Object> updates) {
        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        if (updates.containsKey("role")) {
            user.setRole(PlatformUser.Role.valueOf((String) updates.get("role")));
        }
        if (updates.containsKey("phone")) {
            user.setPhone(((Number) updates.get("phone")).longValue());
        }
        if (updates.containsKey("password")) {
            user.setPasswordHash(passwordEncoder.encode((String) updates.get("password")));
        }
        platformUserRepository.save(user);
    }

    @Override
    public void deleteAdmin(Long id) {
        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        user.setDeleted(true);
        platformUserRepository.save(user);
    }

    @Override
    public List<PlatformAuditLog> getAuditLogs(Date from, Date to, String action) {
        if (action != null && !action.isBlank()) {
            return auditLogRepository.findAllByActionAndCreatedAtBetweenOrderByCreatedAtDesc(action, from, to);
        }
        return auditLogRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
    }
}
