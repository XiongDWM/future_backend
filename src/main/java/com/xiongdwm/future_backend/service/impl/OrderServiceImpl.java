package com.xiongdwm.future_backend.service.impl;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;

import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.bo.OrderCloseDto;
import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.entity.OrderSection;
import com.xiongdwm.future_backend.entity.RejectionInfo;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.entity.Order.Type;
import com.xiongdwm.future_backend.repository.OrderRepository;
import com.xiongdwm.future_backend.service.OrderSectionService;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.service.RejectionInfoService;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

import jakarta.annotation.Resource;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Resource
    private UserService userService;
    @Resource
    private OrderSectionService sectionService;
    @Resource
    private RejectionInfoService rejectionInfoService;
    @Autowired
    private GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.ORDER;

    @Override
    public boolean assignedOrderToUser(Long userId, String orderId) {
        var user = userService.getUserById(userId);
        if(user==null)throw new ServiceException("用户不存在");
        if(user.getStatus()==User.Status.INACTIVE)throw new ServiceException("打手离职");
        if(user.getStatus()==User.Status.BUSY)throw new ServiceException("打手忙碌");
        if(user.getStatus()==User.Status.HANGING)throw new ServiceException("打手休息");
        if(user.getStatus()==User.Status.OFFLINE)throw new ServiceException("打手离线");
        if(user.getStatus()==User.Status.PREPARE)throw new ServiceException("打手已有订单，无法派单");
        
        user.setStatus(User.Status.PREPARE);
        var success =userService.updateUser(user);
        if (!success) throw new ServiceException("派单失败");

        var order = orderRepository.findById(orderId).orElse(null);
        if(order==null)throw new ServiceException("订单不存在");
        order.setUserId(userId);
        order.setPalworld(user);
        order.setStatus(Order.Status.PENDING);
        orderRepository.saveAndFlush(order);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), orderId);    
        return true;
    }

    @Override
    public Page<Order> listOrders(int page, int size, Type type, Long userId, boolean todayOnly) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "issueDate"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (todayOnly) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfDay = cal.getTime();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                Date startOfNextDay = cal.getTime();
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), startOfDay));
                predicates.add(cb.lessThan(root.get("issueDate"), startOfNextDay));
                // 排除已完成和已取消
                predicates.add(cb.notEqual(root.get("status"), Order.Status.COMPLETED));
                predicates.add(cb.notEqual(root.get("status"), Order.Status.CANCELLED));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable);
    }

    @Override
    public boolean workWork(long palId, String orderId,String picStart) {
        var pal = userService.getUserById(palId);
        if(pal==null||pal.getStatus()!=User.Status.ACTIVE)throw new ServiceException("异常");
        var order = orderRepository.findById(orderId).orElse(null);
        if(order==null||!order.getType().isSelf())throw new ServiceException("订单不存在");
        order.setStatus(Order.Status.IN_PROGRESS);
        order.setPicStart(picStart);
        var orderSuccess=orderRepository.saveAndFlush(order)!=null;
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), orderId);
        // set pal status to busy
        pal.setStatus(User.Status.BUSY);
        var userSuccess=userService.updateUser(pal);
        // create first order section
        var section = new OrderSection();
        var uuid=UUID.randomUUID().toString();
        section.setOrderSubId(uuid);
        section.setOrder(order);
        section.setPrice(order.getLowIncome());
        section.setAmount(order.getAmount());
        var sectionSuccess=sectionService.createOrderSection(section);
        return orderSuccess&&userSuccess&&sectionSuccess;
    }

    @Override
    public void reject(Long palId, boolean isConfirmed, String info, String orderId) {
        var user= userService.getUserById(palId);
        if(isConfirmed)return;
        if(null==info||info.isBlank())throw new ServiceException("拒绝原因不能为空");
        var rejection=new RejectionInfo();
        rejection.setOccurAt(new Date());            
        rejection.setReason(info);
        rejection.setOrderId(orderId);
        rejection.setPalworld(user);
        rejectionInfoService.saveRejectionInfo(rejection);
        
    }

    @Override
    public boolean createOrder(Order order) {
        var action=GlobalEventSpec.Action.CREATE;
        order.setIssueDate(new Date());
        var saved=orderRepository.saveAndFlush(order);
        eventBus.emit(domain, action, action.isFetchable(),saved.getOrderId());
        return saved!=null;
    }

    @Override
    public boolean continueOrder(String orderId, double price, double amount, Order.UnitType unitType,String additionalPic) {
        var order = orderRepository.findById(orderId).orElse(null);
        if (order == null) throw new ServiceException("订单不存在");

        // 二手单必须上传截图，否则不允许续单
        boolean isSecondHand = order.getType() == Order.Type.SECOND_HAND;
        if (isSecondHand && (additionalPic == null || additionalPic.isBlank())) {
            throw new ServiceException("二手单续单需要上传附加截图");
        }

        Date now = new Date();

        // 状态校验
        if (order.getStatus() != Order.Status.IN_PROGRESS) {
            // 非进行中（如手滑点了完成），校验最近section是否在12小时内
            var sections = sectionService.findByOrderId(orderId);
            if (!sections.isEmpty()) {
                var lastSection = sections.stream()
                        .filter(s -> s.getEndAt() != null)
                        .max((a, b) -> a.getEndAt().compareTo(b.getEndAt()))
                        .orElse(null);
                if (lastSection != null) {
                    long twelveHours = 12 * 60 * 60 * 1000L;
                    if (now.getTime() - lastSection.getEndAt().getTime() > twelveHours) {
                        throw new ServiceException("订单已超过12小时，无法续单");
                    }
                }
            }
            order.setStatus(Order.Status.IN_PROGRESS);
            if (isSecondHand) {
                order.setPicEnd(additionalPic);
                order.setSecondHandStatus(Order.SecondHandStatus.THIRD_PARTY_TAKEN_PROCESS_DONE);
            } 
            orderRepository.saveAndFlush(order);
            // 二手单推送两个事件：SECOND_HAND（消息盒子）+ ORDER（订单列表）
            if (isSecondHand) {
                eventBus.emit(GlobalEventSpec.Domain.SECOND_HAND, GlobalEventSpec.Action.UPDATE, true, orderId);
            }
            eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true, orderId);
        }

        // 将该订单下所有未完成的section标记为已完成
        sectionService.finishAllSections(orderId);

        // 创建新的续单section
        var section = new OrderSection();
        section.setOrderSubId(UUID.randomUUID().toString());
        section.setOrder(order);
        section.setPrice(price);
        section.setAmount(amount);
        section.setUnitType(unitType);
        section.setStartDate(now);
        section.setRepeated(true);
        section.setFinished(false);

        // 计算预计结束时间: amount * unitType.multiplier 折算成小时
        long durationMs = (long) (amount * unitType.getMultiplier() * 60 * 60 * 1000);
        section.setWillEndAt(new Date(now.getTime() + durationMs));

        return sectionService.createOrderSection(section);
    }

	@Override
	public boolean createOrderFromFindingRequest(FindingRequestFillDto findingRequestDto) {
        var order=new Order();
        var id=UUID.randomUUID().toString();
        order.setOrderId(id);
        order.setType(findingRequestDto.getType());
        order.setAmount(findingRequestDto.getAmount());
        order.setUnitType(findingRequestDto.getUnitType());
        order.setLowIncome(findingRequestDto.getLowIncome());
        order.setIssueDate(findingRequestDto.getIssueDate());
        order.setCustomer(findingRequestDto.getCustomer());
        order.setResource(findingRequestDto.getResource());
        createOrder(order);
        assignedOrderToUser(findingRequestDto.getPalId(), order.getOrderId());
        return order!=null;
	}

    @Override
    public boolean closeOrder(OrderCloseDto dto) {
        // 1. 查询订单，校验订单用户和登录用户是否一致，订单状态是否进行中
        // 2。更新订单状态为已完成，记录结束时间和结算截图
        // 3. 更新订单下的所有section为已完成，记录结束时间
        // 4. 更新用户状态为ACTIVE
        var order = orderRepository.findById(dto.getOrderId()).orElse(null);
        if (order == null) throw new ServiceException("订单不存在");
        var user = userService.getUserById(order.getUserId());
        // token中的用户信息获取
        // var tokenUserId = ; 
        if (user == null) throw new ServiceException("用户不存在");
        if (order.getStatus() != Order.Status.IN_PROGRESS && order.getStatus() != Order.Status.THIRD_PARTY_TAKEN_PROCESS_DONE) {
            throw new ServiceException("订单状态不允许关闭");
        }

        order.setStatus(Order.Status.COMPLETED);
        order.setEndAt(new Date());;
        order.setPicEnd(dto.getPicString());
        orderRepository.saveAndFlush(order);

        sectionService.finishAllSections(order.getOrderId());

        user.setStatus(User.Status.ACTIVE);
        userService.updateUser(user);

        return true;
    }

    
    
}
