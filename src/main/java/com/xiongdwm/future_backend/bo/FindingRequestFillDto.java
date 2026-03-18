package com.xiongdwm.future_backend.bo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.entity.Order.UnitType;

public class FindingRequestFillDto implements Serializable {
    private Long requestId; // 请求单信息带出的找单请求id
	private Long palId; // 请求单信息带出打手id
    private Order.Type type; // 订单类型
    private Date issueDate; // 创建时间
    private String customer; // 玩家账号
    private String resource; // 来源
    private double lowIncome; // 最低收入
    private double amount; // 数量
    private UnitType unitType;
	private String description; // 描述，存游戏类型和段位，格式是 "gameType|rank"
	private List<Long> collaboratorPalIds; // 协作打手id列表（可选）

	public Long getRequestId() {
		return requestId;
	}
	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}
	public Order.Type getType() {
		return type;
	}
	public void setType(Order.Type type) {
		this.type = type;
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
	public String getResource() {
		return resource;
	}
	public void setResource(String resource) {
		this.resource = resource;
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
	public Long getPalId() {
		return palId;
	}
	public void setPalId(Long palId) {
		this.palId = palId;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Long> getCollaboratorPalIds() {
		return collaboratorPalIds;
	}
	public void setCollaboratorPalIds(List<Long> collaboratorPalIds) {
		this.collaboratorPalIds = collaboratorPalIds;
	}
}
