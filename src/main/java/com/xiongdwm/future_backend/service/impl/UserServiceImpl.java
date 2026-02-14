package com.xiongdwm.future_backend.service.impl;



import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.repository.UserRepository;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.exception.AuthenticationFailException;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;


@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.USER; // 定义编译单元内主要操作的领域

    @Override
    public User authenticate(String username, String password) {
        var action=GlobalEventSpec.Action.UPDATE;
        var user=userRepository.findByUsernameAndPassword(username, password).orElseThrow(()->new AuthenticationFailException("用户名或密码错误"));
        user.setStatus(User.Status.ONLINE);
        user.setLastLogin(new Date());
        userRepository.saveAndFlush(user);
        eventBus.emit(domain, action, action.isFetchable(),user.getId());
        return user;
    }


    @Override
    public boolean regist(User user) {
        var action=GlobalEventSpec.Action.CREATE;
        user.setEnterDate(new Date());
        user.setStatus(User.Status.HANGING);
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
        return userRepository.save(user)!=null;
    }


    @Override
    public User authenticate(String username, String password, String softwareCode) {
        var action=GlobalEventSpec.Action.UPDATE;
        var user=userRepository.findByUsernameAndPassword(username, password).orElseThrow(()->new AuthenticationFailException("用户名或密码错误"));
        user.setStatus(User.Status.ACTIVE);
        user.setSoftwareCode(softwareCode);
        userRepository.saveAndFlush(user);
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
        return user;
    }


    @Override
    public Page<User> listUsers(int page, int size) {
        Page<User> users=userRepository.findAll(PageRequest.of(page-1, size));
        return users;
    }

    @Override
    public Page<User> listUsers(int page, int size, String username, User.Status status) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
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
            cb.equal(root.get("role"), User.Role.PALWORLD);
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


        
}
