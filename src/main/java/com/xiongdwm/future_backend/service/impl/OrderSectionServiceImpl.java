package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.entity.OrderSection;
import com.xiongdwm.future_backend.repository.OrderSectionRepository;
import com.xiongdwm.future_backend.service.OrderSectionService;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

@Service
public class OrderSectionServiceImpl implements OrderSectionService {
    @Autowired
    private OrderSectionRepository orderSectionRepository;
    @Autowired
    private GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.ORDER_SECTION;

    @Override
    public boolean createOrderSection(OrderSection orderSection) {
        var section=orderSectionRepository.saveAndFlush(orderSection);
        eventBus.emit(domain, GlobalEventSpec.Action.CREATE, true, section.getOrderSubId());
        return section!=null;
    }

    @Override
    public void finishAllSections(String orderId) {
        var sections=orderSectionRepository.findByOrderOrderIdAndFinishedFalse(orderId);
        Date now=new Date();
        for(var section:sections){
            section.setFinished(true);
            section.setEndAt(now);
        }
        orderSectionRepository.saveAllAndFlush(sections);
        sections.forEach(s->eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true, s.getOrderSubId()));
    }

    @Override
    public List<OrderSection> findByOrderId(String orderId) {
        return orderSectionRepository.findByOrderOrderId(orderId);
    }
}
