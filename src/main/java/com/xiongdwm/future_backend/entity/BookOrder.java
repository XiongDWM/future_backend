package com.xiongdwm.future_backend.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// 存单 计算当月
@Entity
@Table(name="book_order")
public class BookOrder {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column
    private String customer; // 客户备注
    @Column
    private String customerId; // 客户id 微信号
    @Column
    private Date createTime;
    @Column
    private String details; // 客户描述
    @Column
    private double amount; // 存单数量
    @Column
    private double price; // 存单 单价格
    @Column
    private String picProvence; // 订单相关的图片证明来源，纯文本（oss上传获取的文件ID），由前端传入，后端不做解析
    @Column
    private Long pid; // 打手id 冗余 repository查询用
    @Column
    private double remaining=0d; // 剩余数量，初始值等于amount，订单完成后会变成0，订单部分完成会小于amount但大于0
    @Column
    private Boolean confirmed; // 是否确认（客服或管理确认后才会结算), null:未确认，true:确认，false:拒绝
    @Column 
    private String rejectReason="N/A"; // 拒绝理由，仅当confirmed=false时有值 需要客服或管理人员填写，比如图片对不上转账等
    @Column
    private Long lastRechargeTime=0L; // 上次充值时间，单位毫秒
    @Column
    private Long lastSettleTime=0L; // 上次结算时间，单位毫秒
    @Column
    private Double lastRechargeValue=0d; // 上次充值数量
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palworld_id", referencedColumnName = "id")
    @JsonManagedReference("user-book-orders")
    private User palworld; // 关联打手
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCustomer() {
        return customer;
    }
    public void setCustomer(String customer) {
        this.customer = customer;
    }
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    public User getPalworld() {
        return palworld;
    }
    public void setPalworld(User palworld) {
        this.palworld = palworld;
    }
    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    public Long getPid() {
        return pid;
    }
    public void setPid(Long pid) {
        this.pid = pid;
    }
    public String getPicProvence() {
        return picProvence;
    }
    public void setPicProvence(String picProvence) {
        this.picProvence = picProvence;
    }
    public double getRemaining() {
        return remaining;
    }
    public void setRemaining(double remaining) {
        this.remaining = remaining;
    }
    public Boolean isConfirmed() {
        return confirmed;
    }
    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
    public long getLastRechargeTime() {
        return lastRechargeTime;
    }
    public void setLastRechargeTime(long lastRechargeTime) {
        this.lastRechargeTime = lastRechargeTime;
    }
    public long getLastSettleTime() {
        return lastSettleTime;
    }
    public void setLastSettleTime(long lastSettleTime) {
        this.lastSettleTime = lastSettleTime;
    }
    public double getLastRechargeValue() {
        return lastRechargeValue;
    }
    public void setLastRechargeValue(double lastRechargeValue) {
        this.lastRechargeValue = lastRechargeValue;
    }
    public String getRejectReason() {
        return rejectReason;
    }
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
    public Boolean getConfirmed() {
        return confirmed;
    }
    
    
}
