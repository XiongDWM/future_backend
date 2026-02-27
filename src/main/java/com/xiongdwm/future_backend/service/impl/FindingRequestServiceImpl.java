package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

import jakarta.annotation.Resource;

@Service
public class FindingRequestServiceImpl implements FindingRequestService {
    @Autowired
    private FindingRequestRepository requestRepository;
    @Resource
    private OrderService orderService;
    @Resource 
    private UserService userService;
    @Autowired
    private GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.FINDING_REQUEST;

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
	public boolean confirm(FindingRequestFillDto findingRequestDto) {
        var request=requestRepository.findById(findingRequestDto.getRequestId()).orElse(null);
        if(request==null)throw new ServiceException("找单请求不存在");
        if(request.isFulfilled()==null)throw new ServiceException("找单请求已被取消");
        if(request.isFulfilled())throw new ServiceException("找单请求已完成");
        var order=orderService.createOrderFromFindingRequest(findingRequestDto);
        if(null==order)throw new ServiceException("创建订单失败");

        request.setFulfilled(true);
        request.setFulfilledAt(new Date());
        request.setOrder(order);
        requestRepository.saveAndFlush(request);
        eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true,request.getId());
        return true;
	}

	@Override
	public List<FindingRequest> getRequests(User user) {
        if (user != null) {
            return requestRepository.findByPalworldOrderByRequestedAtDesc(user);
        }
        return requestRepository.findAll(Sort.by(Sort.Direction.DESC, "requestedAt"));
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
    
}
