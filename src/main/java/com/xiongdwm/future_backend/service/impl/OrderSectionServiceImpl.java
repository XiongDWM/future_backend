package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.xiongdwm.future_backend.entity.OrderSection;
import com.xiongdwm.future_backend.repository.OrderSectionRepository;
import com.xiongdwm.future_backend.service.OrderSectionService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

@Service
public class OrderSectionServiceImpl implements OrderSectionService {
    private final OrderSectionRepository orderSectionRepository;
    private final GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.ORDER_SECTION;

    public OrderSectionServiceImpl(OrderSectionRepository orderSectionRepository, GlobalEventBus eventBus) {
        this.orderSectionRepository = orderSectionRepository;
        this.eventBus = eventBus;
    }

    @Override
    public boolean createOrderSection(OrderSection orderSection) {
        var section=orderSectionRepository.saveAndFlush(orderSection);
        eventBus.emitAfterCommit(domain, GlobalEventSpec.Action.CREATE, true, section.getOrderSubId());
        return section!=null;
    }

    @Override
    public void finishAllSections(String orderId) {
        var sections=orderSectionRepository.findByOrderOrderIdAndFinishedFalse(orderId);
        if(sections.isEmpty()) return;
        loopingFinishSections(sections);
    }

    @Override
    public List<OrderSection> findByOrderId(String orderId) {
        return orderSectionRepository.findByOrderOrderId(orderId);
    }

    @Override
    public Date finishAllSectionsAndGetLastEndTime(String orderId) {
        var sections=orderSectionRepository.findByOrderOrderIdAndFinishedFalse(orderId);
        if(sections.isEmpty()) return new Date();
        loopingFinishSections(sections);
        Date lastEndTime = sections.getLast().getWillEndAt();
        return lastEndTime;
    }

    private void loopingFinishSections(List<OrderSection> sections){
        if(sections.isEmpty()) return;
        Date now=new Date();
        sections.stream().forEach(section->{
            section.setFinished(true);
            section.setEndAt(now);
        });
        orderSectionRepository.saveAllAndFlush(sections).forEach(s -> eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true, s.getOrderSubId()));
    }

    @Override
    public boolean audit(String subId, boolean confirm, String rejectReason) {
        if(confirm==false && !StringUtils.hasText(rejectReason))throw new IllegalArgumentException("拒绝续单必须提供拒绝理由");
        var section=orderSectionRepository.findById(subId).orElse(null);
        if(section==null)return false;
        if(section.getConfirmed()!=null)throw new ServiceException("该续单已被审核"); // 已审核过了
        section.setConfirmed(confirm);
        if(!confirm) {
            section.setRejectReason(rejectReason);
        }           
        orderSectionRepository.saveAndFlush(section);
        eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true, section.getOrderSubId());
        return true;
    }
}
