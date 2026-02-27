package com.xiongdwm.future_backend.service;



import java.util.List;

import org.springframework.data.domain.Page;

import com.xiongdwm.future_backend.entity.User;


public interface UserService {
    public User authenticate(String username, String password);
    public User authenticate(String username,String password,String softwareCode);
    public boolean regist(User user);
    public Page<User> listUsers(int page,int size);
    public Page<User> listUsers(int page, int size, String username, User.Status status);
    public List<User> listOnlinePalworld();
    public List<User> listAllPalworld();
    public User getUserById(Long id);
    public boolean hangging(Long userId);
    public  boolean updateUser(User user);
    public boolean logout(long userId);
}
    