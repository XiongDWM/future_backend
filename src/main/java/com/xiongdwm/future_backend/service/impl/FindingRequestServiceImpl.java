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
	public boolean submit(FindingRequest findingRequest) {
        // 确保 palworld 是受管实体（客户端可能只传了 id）
        // if (findingRequest.getPalworld() != null && findingRequest.getPalworld().getId() != null) {
        //     var user = userService.getUserById(findingRequest.getPalworld().getId());
        //     findingRequest.setPalworld(user);
        // }
        findingRequest.setRequestedAt(new Date());
        findingRequest.setFulfilled(false);
        var saved=requestRepository.saveAndFlush(findingRequest);
        var userId=findingRequest.getPalworld()!=null?findingRequest.getPalworld().getId():null;
        if(null==userId)throw new RuntimeException("找单请求必须关联一个打手");
        var user=userService.getUserById(userId);
        user.setStatus(User.Status.ACTIVE);
        userService.updateUser(user);
        eventBus.emit(domain, GlobalEventSpec.Action.CREATE, true,saved.getId());
        return saved!=null;
	}

	@Override
        @Transactional
	public boolean confirm(FindingRequestFillDto findingRequestDto) {
        var request=requestRepository.findById(findingRequestDto.getRequestId()).orElse(null);
        if(request==null)throw new ServiceException("找单请求不存在");
        if(request.isFulfilled()==null)throw new ServiceException("找单请求已被取消");
        if(request.isFulfilled())throw new ServiceException("找单请求已完成");
        var order=orderService.createOrderFromFindingRequest(findingRequestDto);
        if(null==order)throw new ServiceException("创建订单失败");

        // 为协作打手创建子单
        if (findingRequestDto.getCollaboratorPalIds() != null) {
            for (Long collabPalId : findingRequestDto.getCollaboratorPalIds()) {
                orderService.addCollaborator(order.getOrderId(), collabPalId);
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
	public boolean cancel(Long requestId) {
		var request = requestRepository.findById(requestId).orElse(null);
		if (request == null) throw new ServiceException("找单请求不存在");
		request.setFulfilled(null);
		requestRepository.saveAndFlush(request);
		eventBus.emit(domain, GlobalEventSpec.Action.CANCEL, true, request.getId());
		return true;
	}

        @Override
        public List<FindingRequest> getRequests() {
        var since = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        return requestRepository.findByRequestedAtAfterOrderByRequestedAtDesc(since);
        }
    
}
