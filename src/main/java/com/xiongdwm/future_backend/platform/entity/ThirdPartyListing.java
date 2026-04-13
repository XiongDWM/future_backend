package com.xiongdwm.future_backend.platform.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 甩单大厅 - 挂单记录（存储于平台库）
 */
@Entity
@Table(name = "third_party_listing")
public class ThirdPartyListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studioId;       // 发布方工作室id

    @Column(nullable = false)
    private String studioName;   // 冗余，快速展示用

    @Column(nullable = false)
    private String gameType;     // 游戏类型

    @Column
    private String rankInfo;     // 段位要求描述

    @Column(length = 500)
    private String description;  // 补充说明

    @Column
    private String customerId; // 客户游戏ID，或者房间号

    @Column(nullable = false)
    private double originalPrice; // 原单价

    @Column(nullable = false)
    private boolean customerTran; // 是否送老板，默认送老板，前端单选checkbox，选了就是false，不选是true

    @Column(nullable = false)
    private double price;        // 挂单报价 -- 结算价格

    @Column
    @Enumerated(EnumType.STRING)
    private OrderType orderType;  // 单子类型：男单/女单/AI女

    @Column(columnDefinition = "LONGTEXT")
    private String picStart;     // 开工截图（从Order同步）

    @Column(columnDefinition = "LONGTEXT")
    private String picEnd;       // 完成截图（从Order同步）

    @Column(nullable = false)
    private Date postedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    private String failureReason; // 炸单原因，在status=FAILURE时填写

    public enum Status {
        OPEN,       // 挂单中
        TAKEN,      // 已被接单（确认了某个申请）
        DONE,       // 已完成
        FAILURE, // 炸单 在玩成时去选 需要输入炸单原因
        CANCELLED   // 已撤回
    }

    public enum OrderType {
        MALE("男单"),
        FEMALE("女单"),
        AI_FEMALE("AI女");

        private final String label;
        OrderType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }
    public String getStudioName() { return studioName; }
    public void setStudioName(String studioName) { this.studioName = studioName; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public String getRankInfo() { return rankInfo; }
    public void setRankInfo(String rankInfo) { this.rankInfo = rankInfo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public Date getPostedAt() { return postedAt; }
    public void setPostedAt(Date postedAt) { this.postedAt = postedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    public double getOriginalPrice() {
        return originalPrice;
    }
    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }
    public boolean isCustomerTran() {
        return customerTran;
    }
    public void setCustomerTran(boolean customerTran) {
        this.customerTran = customerTran;
    }
    public String getFailureReason() {
        return failureReason;
    }
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    public OrderType getOrderType() {
        return orderType;
    }
    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }
    public String getPicStart() {
        return picStart;
    }
    public void setPicStart(String picStart) {
        this.picStart = picStart;
    }
    public String getPicEnd() {
        return picEnd;
    }
    public void setPicEnd(String picEnd) {
        this.picEnd = picEnd;
    }
}
