package com.xiongdwm.future_backend.service;


import org.springframework.data.domain.Page;

import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.bo.OrderCloseDto;
import com.xiongdwm.future_backend.bo.OrderDetailDto;
import com.xiongdwm.future_backend.entity.Order;

import jakarta.annotation.Nullable;

public interface OrderService {
    public boolean createOrder(Order order);
    public boolean assignedOrderToUser(Long userId, String orderId); 
    public Page<Order> listOrders(int page, int size, Order.Type type, Long userId, boolean todayOnly, String orderId);
    // 首单派发指定，其实是系列工单开始，关联多个section, 开始一个计时？计时完成未接new一个RejectionInfo,原因是超时未接
    public Order workWork(long palId, String orderId,String picStart); 
    // 接单确认，new 一个section，如果拒绝，输入规则，消息总线推送消息，要求重派
    public void reject(Long palId, boolean isConfirmed, String info,String orderId);
    public Order continueOrder(String orderId,double price,double amount,Order.UnitType unitType,@Nullable String additionalPic,@Nullable String continuePic);
    public Order createOrderFromFindingRequest(FindingRequestFillDto findingRequestDto);
    public Order closeOrder(OrderCloseDto dto);
    public Order getOrderDetail(String orderId);
    public OrderDetailDto getOrderDetailDto(String orderId);
    public boolean cancelOrder(String orderId);
    public boolean orderSettle(String orderId);
    public boolean uploadSettlementPic(String orderId, String picString);
    public Order addCollaborator(String parentOrderId, Long palId,double slice); // 为工单添加协作打手（创建子单）
    public boolean deleteOrder(String orderId);
}
