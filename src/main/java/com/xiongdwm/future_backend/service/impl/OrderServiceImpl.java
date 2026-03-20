package com.xiongdwm.future_backend.service.impl;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.bo.OrderCloseDto;
import com.xiongdwm.future_backend.bo.OrderDetailDto;
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

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final OrderSectionService sectionService;
    private final RejectionInfoService rejectionInfoService;
    private final GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.ORDER;

    public OrderServiceImpl(OrderRepository orderRepository, UserService userService,
                            OrderSectionService sectionService, RejectionInfoService rejectionInfoService,
                            GlobalEventBus eventBus) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.sectionService = sectionService;
        this.rejectionInfoService = rejectionInfoService;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional
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
        if(order.getStatus()==Order.Status.CANCELLED)throw new ServiceException("订单已取消");
        order.setUserId(userId);
        order.setPalworld(user);
        order.setStatus(Order.Status.PENDING);
        orderRepository.saveAndFlush(order);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), orderId);    
        return true;
    }

    @Override
    public Page<Order> listOrders(int page, int size, Type type, Long userId, boolean todayOnly, String orderId) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "issueDate"));
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 排除协作子单，子单只跟随主单展示，但是当userId不为空时，协作子单也要参与查询条件过滤
            if (userId == null) {
                predicates.add(cb.notEqual(root.get("type"), Type.SUB_ORDER));
            }

            if (orderId != null) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (todayOnly) {
                Date now = new Date();
                Date twentyFourHoursAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000L);
                
                Predicate withinTwentyFourHours = cb.greaterThanOrEqualTo(root.get("issueDate"), twentyFourHoursAgo);
                
                if (userId == null) {
                    Predicate secondHandStatusMatch = cb.or(
                        cb.equal(root.get("secondHandStatus"), Order.SecondHandStatus.THIRD_PARTY_TAKEN_PROCESS_DONE),
                        cb.equal(root.get("secondHandStatus"), Order.SecondHandStatus.THIRD_PARTY_SETTLEMENT_PULL)
                    );
                    predicates.add(cb.or(withinTwentyFourHours, secondHandStatusMatch));
                } else {
                    predicates.add(withinTwentyFourHours);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        var result = orderRepository.findAll(spec, pageable);
        return result;
    }

    @Override
    @Transactional
    public boolean workWork(long palId, String orderId,String picStart) {
        var pal = userService.getUserById(palId);
        if(pal==null||(pal.getStatus()!=User.Status.ONLINE&&pal.getStatus()!=User.Status.ACTIVE&&pal.getStatus()!=User.Status.PREPARE))throw new ServiceException("状态异常");
        var order = orderRepository.findById(orderId).orElse(null);
        if(order==null||!order.getType().isSelf())throw new ServiceException("订单不存在");
        if(order.getStatus()==Order.Status.CANCELLED)throw new ServiceException("订单已取消");
        order.setStatus(Order.Status.IN_PROGRESS);
        order.setPicStart(picStart);
        var orderSuccess=orderRepository.saveAndFlush(order)!=null;
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), orderId);
        // set pal status to busy
        pal.setStatus(User.Status.BUSY);
        var userSuccess=userService.updateUser(pal);
        // create first order section
        var section = new OrderSection();
        section.setOrder(order);
        section.setPrice(order.getLowIncome());
        section.setConfirmed(true);
        section.setAmount(order.getAmount());
        section.setUnitType(order.getUnitType());
        section.setStartDate(new Date());
        // 预计结束时间=当前时间+amount*unitType折算成的小时
        long durationMs = (long) (order.getAmount() * order.getUnitType().getMultiplier() * 60 * 60 * 1000);
        section.setWillEndAt(new Date(System.currentTimeMillis() + durationMs));
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
        eventBus.emitAfterCommit(domain, action, action.isFetchable(),saved.getOrderId());
        return saved!=null;
    }

    @Override
    @Transactional
    public boolean continueOrder(String orderId, double price, double amount, Order.UnitType unitType,String additionalPic,String continuePic) {
        var order = orderRepository.findById(orderId).orElse(null);
        
        if (order == null) throw new ServiceException("订单不存在");
        if(order.getStatus()==Order.Status.CANCELLED)throw new ServiceException("订单已取消");
        // 二手单第一次必须上传截图，否则不允许续单
        boolean isSecondHand = (order.getType() == Order.Type.SECOND_HAND || order.getType() == Order.Type.SECOND_HAND_G)&&order.getSecondHandStatus()==null;
        if (isSecondHand && (additionalPic == null || additionalPic.isBlank())) throw new ServiceException("二手单续单需要上传附加截图");
        

        Date now = new Date();

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
        }
        if (isSecondHand) {
            order.setPicEnd(additionalPic);
            order.setSecondHandStatus(Order.SecondHandStatus.THIRD_PARTY_TAKEN_PROCESS_DONE);
        } 
        orderRepository.saveAndFlush(order);
        if (isSecondHand) {
            eventBus.emitAfterCommit(GlobalEventSpec.Domain.SECOND_HAND, GlobalEventSpec.Action.UPDATE, true, orderId);
        }
        eventBus.emitAfterCommit(domain, GlobalEventSpec.Action.UPDATE, true, orderId);

        // 将该订单下所有未完成的section标记为已完成
        var lastEndTime = sectionService.finishAllSectionsAndGetLastEndTime(orderId);

        // 创建新的续单section
        var section = new OrderSection();
        section.setOrder(order);
        section.setPrice(price);
        section.setAmount(amount);
        section.setUnitType(unitType);
        section.setStartDate(lastEndTime);
        section.setRepeated(true);
        section.setFinished(false);
        if (continuePic != null && !continuePic.isBlank()) {
            section.setContinuePic(continuePic);
        }

        // 计算预计结束时间: amount * unitType.multiplier 折算成小时
        long durationMs = (long) (amount * unitType.getMultiplier() * 60 * 60 * 1000);
        section.setWillEndAt(new Date(lastEndTime.getTime() + durationMs));
        return sectionService.createOrderSection(section);
    }

	@Override
	@Transactional
	public Order createOrderFromFindingRequest(FindingRequestFillDto findingRequestDto) {
        var order=new Order();
        var gameTypeAndRank=findingRequestDto.getDescription().split("\\|");
        order.setType(findingRequestDto.getType());
        order.setGameType(gameTypeAndRank[0]);
        order.setRankInfo(gameTypeAndRank.length>1?gameTypeAndRank[1]:null);
        order.setAmount(findingRequestDto.getAmount());
        order.setUnitType(findingRequestDto.getUnitType());
        order.setLowIncome(findingRequestDto.getLowIncome());
        order.setIssueDate(findingRequestDto.getIssueDate());
        order.setCustomer(findingRequestDto.getCustomer());
        order.setResource(findingRequestDto.getResource());
        createOrder(order);
        assignedOrderToUser(findingRequestDto.getPalId(), order.getOrderId());
        return order;
	}

    @Override
    @Transactional
    public Order addCollaborator(String parentOrderId, Long palId) {
        var parentOrder = orderRepository.findById(parentOrderId).orElse(null);
        if (parentOrder == null) throw new ServiceException("主工单不存在");
        if (parentOrder.getType() == Order.Type.SUB_ORDER) throw new ServiceException("子单不能再添加协作者");
        if (parentOrder.getStatus() == Order.Status.CANCELLED) throw new ServiceException("工单已取消");

        var pal = userService.getUserById(palId);
        if (pal == null) throw new ServiceException("打手不存在");
        if (pal.getStatus() == User.Status.INACTIVE) throw new ServiceException("打手离职");
        if (pal.getStatus() == User.Status.OFFLINE) throw new ServiceException("打手离线");

        // 检查该打手是否已经是主单的打手
        if (parentOrder.getUserId() != null && parentOrder.getUserId().equals(palId)) {
            throw new ServiceException("该打手已是主单打手");
        }
        // 检查是否已有此打手的子单
        var existingSub = parentOrder.getSubOrders().stream()
                .filter(sub -> sub.getUserId() != null && sub.getUserId().equals(palId)
                        && sub.getStatus() != Order.Status.CANCELLED)
                .findAny();
        if (existingSub.isPresent()) throw new ServiceException("该打手已是协作打手");

        // 从父单复制数据创建子单
        var subOrder = new Order();
        subOrder.setType(Order.Type.SUB_ORDER);
        subOrder.setParentOrder(parentOrder);
        subOrder.setCustomer(parentOrder.getCustomer());
        subOrder.setResource(parentOrder.getResource());
        subOrder.setGameType(parentOrder.getGameType());
        subOrder.setRankInfo(parentOrder.getRankInfo());
        subOrder.setAmount(parentOrder.getAmount());
        subOrder.setUnitType(parentOrder.getUnitType());
        subOrder.setLowIncome(parentOrder.getLowIncome());
        subOrder.setIssueDate(new Date());
        subOrder.setUserId(palId);
        subOrder.setPalworld(pal);
        subOrder.setStatus(Order.Status.PENDING);

        pal.setStatus(User.Status.PREPARE);
        userService.updateUser(pal);

        var saved = orderRepository.saveAndFlush(subOrder);
        var action = GlobalEventSpec.Action.CREATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), saved.getOrderId());
        return saved;
    }

    @Override
    @Transactional
    public boolean closeOrder(OrderCloseDto dto) {
        var order = orderRepository.findById(dto.getOrderId()).orElse(null);
        if (order == null) throw new ServiceException("订单不存在");
        var user = userService.getUserById(order.getUserId());
        if (user == null) throw new ServiceException("用户不存在");
        if (order.getStatus() != Order.Status.IN_PROGRESS) {
            throw new ServiceException("订单状态不允许关闭");
        }
        var isSecondHand=order.getSecondHandStatus()==null&&(order.getType()==Order.Type.SECOND_HAND||order.getType()==Order.Type.SECOND_HAND_G);
        order.setStatus(Order.Status.COMPLETED);
        if(isSecondHand){
            order.setSecondHandStatus(Order.SecondHandStatus.THIRD_PARTY_TAKEN_PROCESS_DONE);
        }
        order.setEndAt(new Date());
        if(order.getPicEnd()==null)order.setPicEnd(dto.getPicString());
        orderRepository.saveAndFlush(order);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), order.getOrderId());
        if (isSecondHand) {
            eventBus.emitAfterCommit(GlobalEventSpec.Domain.SECOND_HAND, GlobalEventSpec.Action.UPDATE, true, order.getOrderId());
        }

        sectionService.finishAllSections(order.getOrderId());

        user.setStatus(User.Status.ONLINE);
        userService.updateUser(user);

        return true;
    }

    @Override
    public Order getOrderDetail(String orderId) {
        return orderRepository.findDetailRootByOrderId(orderId).orElse(null);
    }

    @Override
    @Transactional
    public OrderDetailDto getOrderDetailDto(String orderId) {
        var order = orderRepository.findDetailRootByOrderId(orderId).orElse(null);
        if (order == null) return null;

        // 对于主单，分步加载子单（含 sections）后仅用于 DTO 组装，避免修改受管集合触发 orphanRemoval 异常
        if (order.getType() != Order.Type.SUB_ORDER) {
            var subOrders = orderRepository.findByParentOrderOrderId(orderId);
            return OrderDetailDto.from(order, subOrders);
        }

        return OrderDetailDto.from(order);
    }

    @Override
    public boolean cancelOrder(String orderId) {
        var order = orderRepository.findById(orderId).orElse(null);
        if (order == null) throw new ServiceException("订单不存在");
        if(order.getStatus()==Order.Status.CANCELLED) throw new ServiceException("订单已取消");
        order.setStatus(Order.Status.CANCELLED);    
        orderRepository.saveAndFlush(order);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), orderId);
        return true;
    }

    @Override
    public boolean orderSettle(String orderId) {
        var order = orderRepository.findById(orderId).orElse(null);
        if (order == null) throw new ServiceException("订单不存在");
        if(order.getStatus()==Order.Status.CANCELLED) throw new ServiceException("订单已取消");
        order.setSecondHandStatus(Order.SecondHandStatus.THIRD_PARTY_SETTLEMENT_PULL);
        orderRepository.saveAndFlush(order);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), orderId);
        return true;
    }

    @Override
    public boolean uploadSettlementPic(String orderId, String picString) {
        var order = orderRepository.findById(orderId).orElse(null);
        if (order == null) throw new ServiceException("订单不存在");
        if(order.getStatus()==Order.Status.CANCELLED) throw new ServiceException("订单已取消");
        order.setSecondHandStatus(Order.SecondHandStatus.THIRD_PARTY_SETTLED);
        order.setAdditionalPic(picString);
        orderRepository.saveAndFlush(order);
        var action=GlobalEventSpec.Action.UPDATE;
        eventBus.emitAfterCommit(domain, action, action.isFetchable(), orderId);
        return true;
    }

    
    
}
