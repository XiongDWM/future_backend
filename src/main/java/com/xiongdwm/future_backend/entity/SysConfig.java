package com.xiongdwm.future_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="sys_config")
public class SysConfig {
    @Id
    @Column(name = "`key`")
    private String key;
    @Column(name = "`value`")
    private String value;

    

    public SysConfig(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public SysConfig() {
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    
    
}
