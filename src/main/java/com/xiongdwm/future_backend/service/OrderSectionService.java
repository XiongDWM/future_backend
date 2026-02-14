package com.xiongdwm.future_backend.service;

import java.util.List;

import com.xiongdwm.future_backend.entity.OrderSection;

public interface OrderSectionService {
    public boolean createOrderSection(OrderSection orderSection);
    public void finishAllSections(String orderId);
    public List<OrderSection> findByOrderId(String orderId);
}
