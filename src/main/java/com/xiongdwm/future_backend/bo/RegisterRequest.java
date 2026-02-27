package com.xiongdwm.future_backend.bo;

import java.io.Serializable;

import com.xiongdwm.future_backend.entity.User;

public class RegisterRequest implements Serializable {
    private String username;
    private String password;
    private String identity;
    private String realName;
    private User.Role role;

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public User.Role getRole() {
        return role;
    }
    public void setRole(User.Role role) {
        this.role = role;
    }
    public String getIdentity() {
        return identity;
    }
    public void setIdentity(String identity) {
        this.identity = identity;
    }
    public String getRealName() {
        return realName;
    }
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    
}
