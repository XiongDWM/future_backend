package com.xiongdwm.future_backend.service.impl;



import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.repository.UserRepository;
import com.xiongdwm.future_backend.service.LeaveRecordService;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.exception.AuthenticationFailException;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.security.UserActivityTracker;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

import jakarta.annotation.PostConstruct;


@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final LeaveRecordService leaveRecordService;
    private final UserActivityTracker activityTracker;
    private final GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.USER; // 定义编译单元内主要操作的领域
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository, LeaveRecordService leaveRecordService,
                           UserActivityTracker activityTracker, GlobalEventBus eventBus) {
        this.userRepository = userRepository;
        this.leaveRecordService = leaveRecordService;
        this.activityTracker = activityTracker;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void restoreOnlineUsers() {
        var onlineStatuses = List.of(User.Status.ONLINE, User.Status.ACTIVE, User.Status.PREPARE, User.Status.BUSY, User.Status.HANGING);
        for (var status : onlineStatuses) {
            var users = listUsers(1, 10000, null, status);
            for (var user : users.getContent()) {
                activityTracker.touch(user.getId());
            }
        }
    }

    @Override
    public User authenticate(String username, String password) {
        var action=GlobalEventSpec.Action.UPDATE;
        var user=userRepository.findByUsername(username).orElseThrow(()->new AuthenticationFailException("用户名或密码错误"));
        if(!user.getPassword().equals(password))throw new AuthenticationFailException("用户名或密码错误");
        log.info("authenticate user: "+user.getUsername());
        if(user.getRole()==User.Role.PALWORLD)throw new AuthenticationFailException("打手账号无权限登录平台");
        if(user.getStatus()==User.Status.ON_LEAVE) {
            leaveRecordService.cancelLeaveByUser(user);
        }
        if(user.getStatus()==User.Status.OFFLINE||user.getStatus()==User.Status.ON_LEAVE)user.setStatus(User.Status.ONLINE);
        user.setLastLogin(new Date());
        userRepository.saveAndFlush(user);
        eventBus.emit(domain, action, action.isFetchable(),user.getId());
        activityTracker.touch(user.getId());
        return user;
    }


    @Override
    public boolean regist(User user) {
        if(user.getRole()!=User.Role.ADMIN&&user.getIdentity()==null)throw new ServiceException("注册必须提供身份证号");
        if(user.getRole()!=User.Role.ADMIN&&user.getRealName()==null)throw new ServiceException("注册必须提供真实姓名");
        var action=GlobalEventSpec.Action.CREATE;
        user.setEnterDate(new Date());
        user.setStatus(User.Status.OFFLINE);
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
        return userRepository.save(user)!=null;
    }


    @Override
    public User authenticate(String username, String password, String softwareCode) {
        var action=GlobalEventSpec.Action.UPDATE;
        var user=userRepository.findByUsername(username).orElseThrow(()->new AuthenticationFailException("用户名或密码错误"));
        if(!user.getPassword().equals(password))throw new AuthenticationFailException("用户名或密码错误");
        if(user.getStatus()==User.Status.ON_LEAVE) {
            leaveRecordService.cancelLeaveByUser(user);
        }
        if(user.getStatus()==User.Status.OFFLINE||user.getStatus()==User.Status.ON_LEAVE)user.setStatus(User.Status.ONLINE);
        user.setSoftwareCode(softwareCode);
        user.setLastLogin(new Date());
        userRepository.saveAndFlush(user);
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
        activityTracker.touch(user.getId());
        return user;
    }


    @Override
    public Page<User> listUsers(int page, int size) {
        Specification<User> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));
        return userRepository.findAll(spec, PageRequest.of(page - 1, size));
    }

    @Override
    public Page<User> listUsers(int page, int size, String username, User.Status status) {
        Specification<User> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(root.get("username"), "%" + username.trim() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return userRepository.findAll(spec, PageRequest.of(page - 1, size));
    }

    @Override
    public List<User> listOnlinePalworld() {
        Specification<User> spec = (Specification<User>) (root, query, cb) -> cb.and(
            cb.equal(root.get("role"), User.Role.PALWORLD),
            cb.not(root.get("status").in(User.Status.OFFLINE, User.Status.INACTIVE))
        );
        return userRepository.findAll(spec);
    }

    @Override
    public List<User> listAllPalworld() {
        Specification<User> spec = (Specification<User>) (root, query, cb) ->
            cb.and(
                cb.equal(root.get("role"), User.Role.PALWORLD),
                cb.isFalse(root.get("deleted")),
                root.get("status").in(User.Status.ACTIVE, User.Status.ONLINE)
            );
        return userRepository.findAll(spec);
    }


    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }


    @Override
    public boolean hangging(Long userId) {
        var action=GlobalEventSpec.Action.UPDATE;
        var user=userRepository.findById(userId).orElse(null);
        assert user!=null;
        user.setStatus(User.Status.HANGING);
        var t=userRepository.save(user);
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
        return t!=null; 
    }


    @Override
    public boolean updateUser(User user) {
        var action=GlobalEventSpec.Action.UPDATE;
        var t=userRepository.saveAndFlush(user);
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
        return t!=null;
    }


    @Override
    public boolean logout(long userId) {
        var user=userRepository.findById(userId).orElse(null);
        if(user==null)return false;
        if(user.getStatus()==User.Status.OFFLINE)throw new ServiceException("用户已离线"); 
        user.setStatus(User.Status.OFFLINE);
        user.setLastLogoutAuto(false);
        user.setLastLogout(new Date());
        boolean updated = updateUser(user);
        activityTracker.remove(userId);
        return updated;
    }

    @Override
    public boolean autoLogout(long userId) {
        var user=userRepository.findById(userId).orElse(null);
        if(user==null)return false;
        if(user.getStatus()==User.Status.BUSY)return true;
        if(user.getStatus()==User.Status.OFFLINE)return true;
        user.setStatus(User.Status.OFFLINE);
        user.setLastLogoutAuto(true);
        user.setLastLogout(new Date());
        var saved = userRepository.saveAndFlush(user);
        // autoLogout 由 LRU 缓存驱逐线程触发，无 HTTP 请求上下文，
        // TenantContext.getCurrentStudioId() 为 null，必须用 broadcast=true
        eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true, user.getId(), true);
        activityTracker.remove(userId);
        return saved!=null;
    }

    @Override
    public boolean settleIncome(Long userId) {
        var user=userRepository.findById(userId).orElse(null);
        if(user==null)return false;
        user.setLastPaidDate(new Date());
        return updateUser(user);
    }

    @Override
    public boolean deleteUser(Long userId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        user.setStatus(User.Status.INACTIVE); // 软删除：标记为不可用但不从数据库删除
        user.setDeleted(true); // 软删除标志
        return updateUser(user);
        
    }


        
}
