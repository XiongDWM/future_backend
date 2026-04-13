package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.bo.StudioListItemDto;
import com.xiongdwm.future_backend.bo.StudioRegisterParam;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.platform.entity.Studio;
import com.xiongdwm.future_backend.platform.entity.UserStudioMapping;
import com.xiongdwm.future_backend.platform.repository.StudioRepository;
import com.xiongdwm.future_backend.platform.repository.UserStudioMappingRepository;
import com.xiongdwm.future_backend.repository.UserRepository;
import com.xiongdwm.future_backend.service.StudioService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.tenant.TenantContext;
import com.xiongdwm.future_backend.utils.tenant.TenantRoutingDataSource;

@Service
public class StudioServiceImpl implements StudioService {

    private final StudioRepository studioRepository;
    private final UserStudioMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final TenantRoutingDataSource tenantRoutingDataSource;

    public StudioServiceImpl(StudioRepository studioRepository,
                             UserStudioMappingRepository mappingRepository,
                             UserRepository userRepository,
                             TenantRoutingDataSource tenantRoutingDataSource) {
        this.studioRepository = studioRepository;
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
        this.tenantRoutingDataSource = tenantRoutingDataSource;
    }

    @Override
    public Studio registerStudio(StudioRegisterParam param) {
        // 1. 校验用户名全局唯一
        if (mappingRepository.existsByUsername(param.adminUsername())) {
            throw new ServiceException("用户名已存在");
        }

        var studio = new Studio();
        studio.setName(param.studioName());
        studio.setPlace(param.place());
        studio.setAssignDate(new Date());
        studio.setLastChargeAt(new Date());
        studio.setWillChargeAt(new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000)); // 默认7天后续费, 默认创建后免费7天
        studio = studioRepository.saveAndFlush(studio);

        // 设置数据库名 = tenant_ + id
        String dbName = "tenant_" + studio.getId();
        studio.setDbName(dbName);
        studio = studioRepository.saveAndFlush(studio);

        // 3. 创建 MySQL 数据库
        tenantRoutingDataSource.createDatabase(dbName);

        // 4. 自动建表（DDL）
        tenantRoutingDataSource.initializeSchema(dbName);

        // 5. 注册租户到路由数据源
        tenantRoutingDataSource.registerTenant(studio.getId(), dbName);

        // 6. 在租户库中创建管理员用户
        TenantContext.setCurrentTenant(dbName);
        try {
            var admin = new User();
            admin.setUsername(param.adminUsername());
            admin.setPassword(param.adminPassword());
            admin.setRole(User.Role.ADMIN);
            admin.setStatus(User.Status.OFFLINE);
            admin.setEnterDate(new Date());
            userRepository.saveAndFlush(admin);
        } finally {
            TenantContext.clear();
        }

        // 7. 创建用户-工作室映射（平台库）
        var mapping = new UserStudioMapping();
        mapping.setUsername(param.adminUsername());
        mapping.setStudioId(studio.getId());
        mappingRepository.saveAndFlush(mapping);

        return studio;
    }

    @Override
    public void addUserToStudio(String username, Long studioId) {
        if (mappingRepository.existsByUsername(username)) {
            return; // 已存在映射
        }
        var mapping = new UserStudioMapping();
        mapping.setUsername(username);
        mapping.setStudioId(studioId);
        mappingRepository.saveAndFlush(mapping);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return mappingRepository.existsByUsername(username);
    }

    @Override
    public boolean isPlatformAdmin(String username) {
        return mappingRepository.findByUsername(username)
                .map(m -> m.isPlatformAdmin())
                .orElse(false);
    }

    @Override
    public List<StudioListItemDto> listAllStudios() {
        return studioRepository.findAll().stream()
                .map(s -> new StudioListItemDto(
                        s.getId(),
                        s.getName(),
                        s.getPlace(),
                        s.getAssignDate(),
                        s.getLastChargeAt(),
                        s.getWillChargeAt(),
                        mappingRepository.countByStudioId(s.getId())
                ))
                .toList();
    }
}
