package com.xiongdwm.future_backend.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="rejection_info")
public class RejectionInfo {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY) 
    private Long id;
    @Column
    private String reason;
    @Column
    private String orderId;
    @Column
    private Date occurAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="palworld_id", referencedColumnName = "id")
    @JsonManagedReference
    User palworld;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public Date getOccurAt() {
        return occurAt;
    }
    public void setOccurAt(Date occurAt) {
        this.occurAt = occurAt;
    }
    public User getPalworld() {
        return palworld;
    }
    public void setPalworld(User palworld) {
        this.palworld = palworld;
    }

    
    
}
