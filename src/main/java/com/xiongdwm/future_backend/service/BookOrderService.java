package com.xiongdwm.future_backend.service;


import org.springframework.data.domain.Page;

import com.xiongdwm.future_backend.bo.BookOrderParam;
import com.xiongdwm.future_backend.entity.BookOrder;
import com.xiongdwm.future_backend.entity.User;

public interface BookOrderService {
    boolean createBookOrder(BookOrderParam param,User user);
    Page<BookOrder> listBookOrders(int page, int size, String customer, String customerId, Long pid);
    void startBookOrder(Long orderId);
    void updateBookOrder(BookOrderParam param);
    boolean auditBookOrder(Long id, Boolean confirm, String rejectReason);
    boolean rechargeBookOrder(Long orderId, int amount, double price);


}
