package com.xiongdwm.future_backend.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="account_renting")
public class AccountRenting {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id; 
    @Column
    private String gameType;
    @Column 
    private String rankRequirement;
    @Column
    private String content; //账号密码那些 令牌那些
    @Column
    @Enumerated(EnumType.STRING)
    private RentingStatus status;
    @Column
    private String orderId; // 关联的工单ID
    @Column
    private double amount; // 小时数
    @Column
    private double price; // 单价
    @Column
    private Date startAt; // 开始时间
    @Column
    private Date willEndAt; // 预计结束时间

    public enum RentingStatus{
        USED("已用"),
        ASKING("请求"),
        CONFIRMED("已租");

        private final String text;
        private RentingStatus(String text){
            this.text = text;
        }
        public String getText(){
            return text;
        }
        
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getRankRequirement() {
        return rankRequirement;
    }

    public void setRankRequirement(String rankRequirement) {
        this.rankRequirement = rankRequirement;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RentingStatus getStatus() {
        return status;
    }

    public void setStatus(RentingStatus status) {
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getWillEndAt() {
        return willEndAt;
    }

    public void setWillEndAt(Date willEndAt) {
        this.willEndAt = willEndAt;
    }

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

    



}
