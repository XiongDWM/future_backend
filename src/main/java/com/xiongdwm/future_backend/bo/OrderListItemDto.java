package com.xiongdwm.future_backend.bo;

import java.util.Date;

import com.xiongdwm.future_backend.entity.Order;

public class OrderListItemDto {
    private String orderId;
    private Date issueDate;
    private Date endAt;
    private String customer;
    private String customerEvaluate;
    private String resource;
    private Order.Type type;
    private String picStart;
    private String picEnd;
    private Long userId;
    private String toWhom;
    private Order.Status status;
    private double lowIncome;
    private double amount;
    private Order.UnitType unitType;
    private Order.SecondHandStatus secondHandStatus;
    private String additinalPic; // 保持前端历史字段名兼容
    private String gameType;
    private String rankInfo;
    private PalDto palworld;

    public static OrderListItemDto fromEntity(Order order) {
        var dto = new OrderListItemDto();
        dto.orderId = order.getOrderId();
        dto.issueDate = order.getIssueDate();
        dto.endAt = order.getEndAt();
        dto.customer = order.getCustomer();
        dto.customerEvaluate = order.getCustomerEvaluate();
        dto.resource = order.getResource();
        dto.type = order.getType();
        dto.picStart = order.getPicStart();
        dto.picEnd = order.getPicEnd();
        dto.userId = order.getUserId();
        dto.toWhom = order.getToWhom();
        dto.status = order.getStatus();
        dto.lowIncome = order.getLowIncome();
        dto.amount = order.getAmount();
        dto.unitType = order.getUnitType();
        dto.secondHandStatus = order.getSecondHandStatus();
        dto.additinalPic = order.getAdditionalPic();
        dto.gameType = order.getGameType();
        dto.rankInfo = order.getRankInfo();
        if (order.getPalworld() != null) {
            dto.palworld = PalDto.from(order.getPalworld().getId(), order.getPalworld().getUsername());
        }
        return dto;
    }

    public static class PalDto {
        private Long id;
        private String username;

        public static PalDto from(Long id, String username) {
            var dto = new PalDto();
            dto.id = id;
            dto.username = username;
            return dto;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
    }

    public String getOrderId() { return orderId; }
    public Date getIssueDate() { return issueDate; }
    public Date getEndAt() { return endAt; }
    public String getCustomer() { return customer; }
    public String getCustomerEvaluate() { return customerEvaluate; }
    public String getResource() { return resource; }
    public Order.Type getType() { return type; }
    public String getPicStart() { return picStart; }
    public String getPicEnd() { return picEnd; }
    public Long getUserId() { return userId; }
    public String getToWhom() { return toWhom; }
    public Order.Status getStatus() { return status; }
    public double getLowIncome() { return lowIncome; }
    public double getAmount() { return amount; }
    public Order.UnitType getUnitType() { return unitType; }
    public Order.SecondHandStatus getSecondHandStatus() { return secondHandStatus; }
    public String getAdditinalPic() { return additinalPic; }
    public String getGameType() { return gameType; }
    public String getRankInfo() { return rankInfo; }
    public PalDto getPalworld() { return palworld; }
}
