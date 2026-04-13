package com.xiongdwm.future_backend.platform.entity;

import java.util.Date;

import com.xiongdwm.future_backend.platform.entity.ThirdPartyListing.OrderType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 甩单大厅 - 申请记录（存储于平台库）
 */
@Entity
@Table(name = "third_party_application")
public class ThirdPartyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long listingId;      // 关联挂单id

    @Column(nullable = false)
    private Long studioId;       // 申请方工作室id

    @Column(nullable = false)
    private String studioName;   // 冗余，快速展示用

    @Column(nullable = false)
    private Date appliedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(length = 300)
    private String note;         // 申请备注（可选）

    @Column(length = 500)
    private String failureReason; // 炸单原因

    @Column
    private String customerId;    // 客户ID，确认时从挂单同步

    @Column
    private double originalPrice; // 原单价，确认时从挂单同步

    @Column
    private double price;         // 结算价，确认时从挂单同步

    @Column
    private boolean customerTran; // 是否送老板，确认时从挂单同步

    @Column
    @Enumerated(EnumType.STRING)
    private OrderType orderType;  // 单子类型，确认时从挂单同步

    @Column
    private String orderId;       // 绑定内部系统订单ID

    @Column(columnDefinition = "LONGTEXT")
    private String picStart;      // 开工截图（从Order同步）

    @Column(columnDefinition = "LONGTEXT")
    private String picEnd;        // 完成截图（从Order同步）

    public enum Status {
        PENDING,   // 待确认
        ACCEPTED,  // 已接受
        DONE,      // 已完成
        REJECTED,  // 已拒绝
        FAILURE    // 炸单
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }
    public String getStudioName() { return studioName; }
    public void setStudioName(String studioName) { this.studioName = studioName; }
    public Date getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Date appliedAt) { this.appliedAt = appliedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isCustomerTran() { return customerTran; }
    public void setCustomerTran(boolean customerTran) { this.customerTran = customerTran; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPicStart() { return picStart; }
    public void setPicStart(String picStart) { this.picStart = picStart; }
    public String getPicEnd() { return picEnd; }
    public void setPicEnd(String picEnd) { this.picEnd = picEnd; }
}
