package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.entity.FindingRequest;
import com.xiongdwm.future_backend.repository.FindingRequestRepository;
import com.xiongdwm.future_backend.service.FindingRequestService;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;
import com.xiongdwm.future_backend.entity.User;

import jakarta.transaction.Transactional;

@Service
public class FindingRequestServiceImpl implements FindingRequestService {
    private final FindingRequestRepository requestRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.FINDING_REQUEST;

    public FindingRequestServiceImpl(FindingRequestRepository requestRepository, OrderService orderService,
                                     UserService userService, GlobalEventBus eventBus) {
        this.requestRepository = requestRepository;
        this.orderService = orderService;
        this.userService = userService;
        this.eventBus = eventBus;
    }

	@Override
        @Transactional
	public boolean submit(FindingRequest findingRequest, Long studioId) {
        findingRequest.setRequestedAt(new Date());
        findingRequest.setFulfilled(false);
        var saved=requestRepository.saveAndFlush(findingRequest);
        var userId=findingRequest.getPalworld()!=null?findingRequest.getPalworld().getId():null;
        if(null==userId)throw new RuntimeException("找单请求必须关联一个打手");
        var user=userService.getUserById(userId);
        user.setStatus(User.Status.ACTIVE);
        userService.updateUser(user);
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.CREATE, true, saved.getId(), studioId);
        return saved!=null;
	}

	@Override
        @Transactional
	public boolean confirm(FindingRequestFillDto findingRequestDto) {
        var request=requestRepository.findById(findingRequestDto.getRequestId()).orElse(null);
        if(request==null)throw new ServiceException("找单请求不存在");
        if(request.isFulfilled()==null)throw new ServiceException("找单请求已被取消");
        if(request.isFulfilled())throw new ServiceException("找单请求已完成");
        var collaborator=findingRequestDto.getCollaboratorPalIds();
        if(collaborator!=null&&collaborator.contains(request.getPalworld().getId()))throw new ServiceException("打手不能同时是订单的发布者和协作者");
        var income=findingRequestDto.getLowIncome(); 
        if(collaborator!=null&&collaborator.size()>0)findingRequestDto.setLowIncome(income/(collaborator.size()+1)); // 协作打手分摊底价
        var order=orderService.createOrderFromFindingRequest(findingRequestDto);
        if(null==order)throw new ServiceException("创建订单失败");

        // 为协作打手创建子单
        if (collaborator != null) {
            var slice = income/(collaborator.size()+1);
            for (Long collabPalId : findingRequestDto.getCollaboratorPalIds()) {
                orderService.addCollaborator(order.getOrderId(), collabPalId,slice);
            }
        }

        request.setFulfilled(true);
        request.setFulfilledAt(new Date());
        request.setOrder(order);
        requestRepository.saveAndFlush(request);
        eventBus.emitAfterCommit(domain, GlobalEventSpec.Action.UPDATE, true,request.getId());
        return true;
	}

	@Override
	public List<FindingRequest> getRequests(User user) {
        var since = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        if (user == null) {
            return requestRepository.findByRequestedAtAfterOrderByRequestedAtDesc(since);
        }
        return requestRepository.findByPalworldAndRequestedAtAfterOrderByRequestedAtDesc(user,since);
	}

	@Override
        @Transactional
	public boolean cancel(Long requestId) {
		var request = requestRepository.findById(requestId).orElse(null);
		if (request == null) throw new ServiceException("找单请求不存在");
		request.setFulfilled(null);
		requestRepository.saveAndFlush(request);
		eventBus.emitAfterCommit(domain, GlobalEventSpec.Action.CANCEL, true, request.getId());
		return true;
	}

        @Override
        public List<FindingRequest> getRequests() {
        var since = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        return requestRepository.findByRequestedAtAfterOrderByRequestedAtDesc(since);
        }
    
}
