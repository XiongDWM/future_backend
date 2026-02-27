package com.xiongdwm.future_backend.entity;

import java.util.Date;


import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "finding_request")
public class FindingRequest {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = true)
    private Boolean man; // 男单true 女单false null不限
    private String description;
    private Date requestedAt;
    private Date fulfilledAt;
    @Column(nullable = true)
    private Boolean fulfilled;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palworld_id", referencedColumnName = "id")
    @JsonManagedReference("user-finding-requests")
    private User palworld; // 打手
    @OneToOne(fetch = FetchType.LAZY)
    private Order order;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Boolean isMan() {
        return man;
    }
    public void setMan(Boolean man) {
        this.man = man;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Date getRequestedAt() {
        return requestedAt;
    }
    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }
    public Date getFulfilledAt() {
        return fulfilledAt;
    }
    public void setFulfilledAt(Date fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }
    public Boolean isFulfilled() {
        return fulfilled;
    }
    public void setFulfilled(Boolean fulfilled) {
        this.fulfilled = fulfilled;
    }
    public User getPalworld() {
        return palworld;
    }
    public void setPalworld(User palworld) {
        this.palworld = palworld;
    }
    public Order getOrder() {
        return order;
    }
    public void setOrder(Order order) {
        this.order = order;
    }

    
    
}
