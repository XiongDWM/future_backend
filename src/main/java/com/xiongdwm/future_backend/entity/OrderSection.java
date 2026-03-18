package com.xiongdwm.future_backend.entity;



import java.util.Date;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "order_section")
public class OrderSection {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private String orderSubId; //
    @Column
    private double amount;
    @Column
    private double price;
    @Column
    private Date startDate;
    @Column
    private Date willEndAt;
    @Column
    private Date endAt;
    @Column
    private boolean repeated=false; // 是否是续单
    @Column
    private boolean finished=false; // 是否完成
    @Column
    private String continuePic; // 续单截图
    @Column
    @Enumerated(EnumType.STRING)
    private Order.UnitType unitType;
    @Column
    private Boolean confirmed; // 是否确认（仅用于续单，客服或管理确认后才会结算）
    @Column
    private String rejectReason="-"; // 拒绝理由，仅当confirmed=false         需要客服或管理人员填写，比如图片对不上转账等
    


    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Order.class)
    @JoinColumn(name = "parent_id", referencedColumnName = "orderId")
    @JsonManagedReference("order-sections")
    private Order order;


    public String getOrderSubId() {
        return orderSubId;
    }


    public void setOrderSubId(String orderSubId) {
        this.orderSubId = orderSubId;
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


    public Date getStartDate() {
        return startDate;
    }


    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }


    public Date getEndAt() {
        return endAt;
    }


    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }


    public boolean isRepeated() {
        return repeated;
    }


    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }


    public Order getOrder() {
        return order;
    }


    public void setOrder(Order order) {
        this.order = order;
    }


    public Order.UnitType getUnitType() {
        return unitType;
    }


    public void setUnitType(Order.UnitType unitType) {
        this.unitType = unitType;
    }


	public Date getWillEndAt() {
		return willEndAt;
	}


	public void setWillEndAt(Date willEndAt) {
		this.willEndAt = willEndAt;
	}


	public boolean isFinished() {
		return finished;
	}


	public void setFinished(boolean finished) {
		this.finished = finished;
	}


	public String getContinuePic() {
		return continuePic;
	}


	public void setContinuePic(String continuePic) {
		this.continuePic = continuePic;
	}


    public Boolean getConfirmed() {
        return confirmed;
    }


    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }


    public String getRejectReason() {
        return rejectReason;
    }


    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    
    

    
    
}
