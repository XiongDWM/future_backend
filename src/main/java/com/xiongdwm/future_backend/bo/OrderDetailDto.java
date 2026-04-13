package com.xiongdwm.future_backend.bo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.entity.OrderSection;

public class OrderDetailDto {
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
    private String additionalPic;
    private boolean platformSecondHand;
    private String gameType;
    private String rankInfo;

    // 打手信息（精简）
    private Long palworldId;
    private String palworldUsername;

    // 协作子单列表
    private List<SubOrderDto> subOrders;

    // sections 列表
    private List<SectionDto> sections;

    public static OrderDetailDto from(Order order) {
        return from(order, null);
    }

    public static OrderDetailDto from(Order order, List<Order> subOrdersOverride) {
        var dto = new OrderDetailDto();
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
        dto.additionalPic = order.getAdditionalPic();
        dto.platformSecondHand = order.isPlatformSecondHand();
        dto.gameType = order.getGameType();
        dto.rankInfo = order.getRankInfo();
        if (order.getPalworld() != null) {
            dto.palworldId = order.getPalworld().getId();
            dto.palworldUsername = order.getPalworld().getUsername();
        }
        List<Order> sourceSubOrders = subOrdersOverride != null ? subOrdersOverride : order.getSubOrders();
        if (sourceSubOrders != null && !sourceSubOrders.isEmpty()) {
            dto.subOrders = sourceSubOrders.stream()
                    .map(SubOrderDto::fromEntity)
                    .collect(Collectors.toList());
        } else {
            dto.subOrders = new ArrayList<>();
        }
        if (order.getOrderSections() != null) {
            dto.sections = order.getOrderSections().stream()
                    .map(SectionDto::fromEntity)
                    .collect(Collectors.toList());
        }
        return dto;
    }

    public static class SectionDto {
        private String orderSubId;
        private double amount;
        private double price;
        private Date startDate;
        private Date willEndAt;
        private Date endAt;
        private boolean repeated;
        private boolean finished;
        private Order.UnitType unitType;
        private String continuePic;
        private String rejectReason; // 续单审核拒绝理由
        private String rejectForDisplay; // 前端展示用的是否审核以及审核结果文本

        public static SectionDto fromEntity(OrderSection s) {
            var dto = new SectionDto();
            dto.orderSubId = s.getOrderSubId();
            dto.amount = s.getAmount();
            dto.price = s.getPrice();
            dto.startDate = s.getStartDate();
            dto.willEndAt = s.getWillEndAt();
            dto.endAt = s.getEndAt();
            dto.repeated = s.isRepeated();
            dto.finished = s.isFinished();
            dto.unitType = s.getUnitType();
            dto.continuePic = s.getContinuePic();
            dto.rejectReason = s.getRejectReason();
            if(s.getConfirmed()==null){
                dto.rejectForDisplay="待审核";
            }else if(s.getConfirmed()){
                dto.rejectForDisplay="通过";
            }else{
                dto.rejectForDisplay="未通过";
            }
            return dto;
        }

        public String getOrderSubId() { return orderSubId; }
        public double getAmount() { return amount; }
        public double getPrice() { return price; }
        public Date getStartDate() { return startDate; }
        public Date getWillEndAt() { return willEndAt; }
        public Date getEndAt() { return endAt; }
        public boolean isRepeated() { return repeated; }
        public boolean isFinished() { return finished; }
        public Order.UnitType getUnitType() { return unitType; }
        public String getContinuePic() { return continuePic; }
        public String getRejectReason() { return rejectReason; }
        public String getRejectForDisplay() { return rejectForDisplay; }
    }
    // getters
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
    public String getAdditionalPic() { return additionalPic; }
    public boolean isPlatformSecondHand() { return platformSecondHand; }
    public String getGameType() { return gameType; }
    public String getRankInfo() { return rankInfo; }
    public Long getPalworldId() { return palworldId; }
    public String getPalworldUsername() { return palworldUsername; }
    public List<SubOrderDto> getSubOrders() { return subOrders; }
    public List<SectionDto> getSections() { return sections; }

    public static class SubOrderDto {
        private String orderId;
        private Long palworldId;
        private String palworldUsername;
        private Order.Status status;
        private Date issueDate;
        private Date endAt;
        private List<SectionDto> sections;

        public static SubOrderDto fromEntity(Order subOrder) {
            var dto = new SubOrderDto();
            dto.orderId = subOrder.getOrderId();
            dto.status = subOrder.getStatus();
            dto.issueDate = subOrder.getIssueDate();
            dto.endAt = subOrder.getEndAt();
            if (subOrder.getPalworld() != null) {
                dto.palworldId = subOrder.getPalworld().getId();
                dto.palworldUsername = subOrder.getPalworld().getUsername();
            }
            if (subOrder.getOrderSections() != null) {
                dto.sections = subOrder.getOrderSections().stream()
                        .map(SectionDto::fromEntity)
                        .collect(Collectors.toList());
            } else {
                dto.sections = new ArrayList<>();
            }
            return dto;
        }

        public String getOrderId() { return orderId; }
        public Long getPalworldId() { return palworldId; }
        public String getPalworldUsername() { return palworldUsername; }
        public Order.Status getStatus() { return status; }
        public Date getIssueDate() { return issueDate; }
        public Date getEndAt() { return endAt; }
        public List<SectionDto> getSections() { return sections; }
    }
}
