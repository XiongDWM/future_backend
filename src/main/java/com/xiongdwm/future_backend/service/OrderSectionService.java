package com.xiongdwm.future_backend.service;

import java.util.Date;
import java.util.List;

import com.xiongdwm.future_backend.entity.OrderSection;

public interface OrderSectionService {
    public boolean createOrderSection(OrderSection orderSection);
    public void finishAllSections(String orderId);
    public Date finishAllSectionsAndGetLastEndTime(String orderId);
    public List<OrderSection> findByOrderId(String orderId);
    public boolean audit(String subId, boolean confirm, String rejectReason); // 续单审核，confirm=true表示确认，false表示拒绝
}
