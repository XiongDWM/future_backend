package com.xiongdwm.future_backend.entity;



import java.util.Date;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_section")
public class OrderSection {
    @Id
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
    @Enumerated(EnumType.STRING)
    private Order.UnitType unitType;


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
    

    
    
}
