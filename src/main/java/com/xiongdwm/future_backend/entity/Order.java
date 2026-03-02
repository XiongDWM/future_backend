package com.xiongdwm.future_backend.entity;

import java.util.Date;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private String orderId;
    @Column
    private Date issueDate; // 创建时间
    @Column
    private Date endAt; // 订单完成时间
    @Column
    private String customer; 
    @Column
    private String customerEvaluate; 
    @Column
    private String resource; // 来源 二手单填写对方工作室或者微信号，一手单填写平台
    @Column
    @Enumerated(EnumType.STRING)
    private Type type; 
    @Column
    private String picStart;
    @Column
    private String picEnd;
    @Column(name="redundant_user_id")
    private Long userId; // 打手id 冗余
    @Column
    private String toWhom="N/A"; // 甩单有值 复制一个微信号
    @Column 
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column
    private double lowIncome; // 首单结算价格 (也就是首单结算价格 同步过来)
    @Column
    private double amount; // 数量
    @Column
    @Enumerated(EnumType.STRING)
    private UnitType unitType;
    @Column
    @Nullable
    @Enumerated(EnumType.STRING)
    private SecondHandStatus secondHandStatus; // 二手单状态
    @Column
    @Nullable
    private String additionalPic; // 二手单的附加截图
    @Column
    private String gameType; // 订单相关的游戏类型，由前端传入，后端不做解析, 条件查询用
    @Column
    private String rankInfo;  // 订单相关的游戏段位等信息，纯文本，由前端传入，后端不做解析, 条件查询用


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palworld_id", referencedColumnName = "id")
    @JsonManagedReference("user-orders")
    private User palworld; // 打手

    @OneToMany(mappedBy = "order", targetEntity = OrderSection.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<OrderSection> orderSections;

    public enum Type {
        SELF_B("自接男单", true),
        SELF_G("自接女单/Ai", true),
        BOOKED("存单", true), // 打手自己的预约单，从打手端发起
        SECOND_HAND("二手男单", true),
        SECOND_HAND_G("二手女单/Ai", true),
        THIRD_PARTY("甩单", false),
        LONG_TERM("长时间订单", true); 

        private final String targetText;
        private final boolean isSelf;
        private Type(String targetText, boolean isSelf) {
            this.targetText = targetText;
            this.isSelf = isSelf;
        }
        public String getTargetText() {
            return targetText;
        }
        public boolean isSelf() {
            return isSelf;
        }
    }

    public enum SecondHandStatus{
        THIRD_PARTY_TAKEN_PROCESS_DONE("二手单接单完成（图片已经上传）"), 
        THIRD_PARTY_SETTLEMENT_PULL("二手单等待结算"),
        THIRD_PARTY_SETTLED("二手单完成（上传结算截图）");
        private final String statusText;
        private SecondHandStatus(String statusText) {
            this.statusText = statusText;
        }
        public String getStatusText() {
            return statusText;
        }
        
    }
    public enum Status {
        PENDING("待接单"),
        IN_PROGRESS("进行中"),  
        THIRD_PARTY_WAITING("甩单待接单"),
        THIRD_PARTY_TAKEN("甩单被接单"),
        COMPLETED("已完成"),
        CANCELLED("已取消");

        private final String statusText;
        private Status(String statusText) {
            this.statusText = statusText;
        }
        public String getStatusText() {
            return statusText;
        }
    }

    public enum UnitType {
        HOUR("小时",1.0d),
        BATTLE("局",0.5d),
        DAY("天",10.0d);

        private final String text;
        private final double multiplier;
        private UnitType(String text, double multiplier) {
            this.text = text;
            this.multiplier = multiplier;
        }
        public String getText() {
            return text;
        }
        public double getMultiplier() {
            return multiplier;
        }

    }

    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public Date getIssueDate() {
        return issueDate;
    }
    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }
    public String getCustomer() {
        return customer;
    }
    public void setCustomer(String customer) {
        this.customer = customer;
    }
    public String getCustomerEvaluate() {
        return customerEvaluate;
    }
    public void setCustomerEvaluate(String customerEvaluate) {
        this.customerEvaluate = customerEvaluate;
    }
    public User getPalworld() {
        return palworld;
    }
    public void setPalworld(User palworld) {
        this.palworld = palworld;
    }
    public List<OrderSection> getOrderSections() {
        return orderSections;
    }
    public void setOrderSections(List<OrderSection> orderSections) {
        this.orderSections = orderSections;
    }
    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
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
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getToWhom() {
        return toWhom;
    }
    public void setToWhom(String toWhom) {
        this.toWhom = toWhom;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public double getLowIncome() {
        return lowIncome;
    }
    public void setLowIncome(double lowIncome) {
        this.lowIncome = lowIncome;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public UnitType getUnitType() {
        return unitType;
    }
    public void setUnitType(UnitType unitType) {
        this.unitType = unitType;
    }
	public String getAdditionalPic() {
		return additionalPic;
	}
	public void setAdditionalPic(String additionalPic) {
		this.additionalPic = additionalPic;
	}
    public Date getEndAt() {
        return endAt;
    }
    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }
    public SecondHandStatus getSecondHandStatus() {
        return secondHandStatus;
    }
    public void setSecondHandStatus(SecondHandStatus secondHandStatus) {
        this.secondHandStatus = secondHandStatus;
    }
    public String getGameType() {
        return gameType;
    }
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    public String getRankInfo() {
        return rankInfo;
    }
    public void setRankInfo(String rankInfo) {
        this.rankInfo = rankInfo;
    }
        
    @Override
    public String toString() {
        return "Order [orderId=" + orderId + ", issueDate=" + issueDate + ", status=" + status + ", lowIncome="
                + lowIncome + ", amount=" + amount + ", secondHandStatus=" + secondHandStatus + "]";
    }
    
}
