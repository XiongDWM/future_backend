package com.xiongdwm.future_backend.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.bo.BookOrderParam;
import com.xiongdwm.future_backend.entity.BookOrder;
import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.repository.BookOrderRepository;
import com.xiongdwm.future_backend.service.BookOrderService;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;

@Service
public class BookOrderServiceImpl implements BookOrderService {
    @Autowired
    private BookOrderRepository bookOrderRepository;
    @Resource
    private OrderService orderService;
    @Autowired
    private GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.BOOKING;
    

    @Override
    public boolean createBookOrder(BookOrderParam param,User user) {
        var bookOrder=new BookOrder();
        BeanUtils.copyProperties(param, bookOrder);
        bookOrder.setCreateTime(new Date());
        bookOrder.setRemaining(param.amount());
        bookOrder.setPalworld(user);
        bookOrder.setPid(user.getId());
        bookOrderRepository.save(bookOrder);
        var action=GlobalEventSpec.Action.CREATE;
        eventBus.emit(domain, action, action.isFetchable(), bookOrder.getId());
        return true; 
    }

    @Override
    public Page<BookOrder> listBookOrders(int page, int size, String customer, String customerId, Long pid) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Specification<BookOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customer != null && !customer.isBlank()) {
                predicates.add(cb.like(root.get("customer"), "%" + customer + "%"));
            }
            if (customerId != null && !customerId.isBlank()) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (pid != null) {
                predicates.add(cb.equal(root.get("pid"), pid));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return bookOrderRepository.findAll(spec, pageable);
    }

    @Override
    public void startBookOrder(Long orderId) {
        var bookOrder = bookOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("存单不存在"));
        if(bookOrder.getRemaining()<=0) {
            throw new RuntimeException("请先存单");
        }
        var order=new Order();
        order.setCustomer(bookOrder.getCustomer());
        order.setIssueDate(new Date());
        order.setGameType("N/A");
        order.setAmount(bookOrder.getRemaining());
        order.setLowIncome(0d);
        order.setRankInfo("N/A");
        order.setStatus(Order.Status.PENDING);
        order.setType(Order.Type.BOOKED);
        order.setPalworld(bookOrder.getPalworld());
        orderService.createOrder(order);
        orderService.workWork(bookOrder.getPid(), order.getOrderId(),null);
    }

    @Override
    public void updateBookOrder(BookOrderParam param) {
            var bookOrder=bookOrderRepository.findById(param.id()).orElseThrow(()->new RuntimeException("存单不存在"));
            BeanUtils.copyProperties(param, bookOrder);
            bookOrderRepository.saveAndFlush(bookOrder);
            var action=GlobalEventSpec.Action.UPDATE;
            eventBus.emit(domain, action, action.isFetchable(), bookOrder.getId());
    }

    @Override
    public boolean confirmBookOrder(Long orderId) {
        var bookOrder = bookOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("存单不存在"));
        bookOrder.setConfirmed(true);
        bookOrderRepository.saveAndFlush(bookOrder);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), bookOrder.getId());
        return true;
    }

    @Override
    public boolean rechargeBookOrder(Long orderId, int amount, double price) {
        var bookOrder = bookOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("存单不存在"));
        bookOrder.setRemaining(bookOrder.getRemaining() + amount);
        bookOrder.setLastRechargeTime(System.currentTimeMillis());
        bookOrder.setLastRechargeValue(amount*price);
        bookOrder.setPrice(price);
        bookOrderRepository.saveAndFlush(bookOrder);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), bookOrder.getId());
        return true;
    }

    @Override
    public boolean rejectBookOrder(Long orderId, String rejectReason) {
        var bookOrder = bookOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("存单不存在"));
        bookOrder.setConfirmed(false);
        bookOrder.setRejectReason(rejectReason);
        bookOrderRepository.saveAndFlush(bookOrder);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), bookOrder.getId());
        return true;
    }
}
