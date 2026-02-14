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
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

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
        if (findingRequest.getPalworld() != null && findingRequest.getPalworld().getId() != null) {
            var user = userService.getUserById(findingRequest.getPalworld().getId());
            findingRequest.setPalworld(user);
        }
        findingRequest.setRequestedAt(new Date());
        var saved=requestRepository.saveAndFlush(findingRequest);
        eventBus.emit(domain, GlobalEventSpec.Action.CREATE, true,saved.getId());
        return saved!=null;
	}

	@Override
	public boolean confirm(FindingRequestFillDto findingRequestDto) {
        var order=orderService.createOrderFromFindingRequest(findingRequestDto);
        if(!order)throw new RuntimeException("创建订单失败");
        var request=requestRepository.findById(findingRequestDto.getRequestId()).orElse(null);
        if(request==null)throw new RuntimeException("找单请求不存在");
        request.setFulfilled(true);
        request.setFulfilledAt(new Date());
        requestRepository.saveAndFlush(request);
        eventBus.emit(domain, GlobalEventSpec.Action.UPDATE, true,request.getId());
        return true;
	}

	@Override
	public List<FindingRequest> getRequests() {
        return requestRepository.findAll(Sort.by(Sort.Direction.DESC, "requestedAt"));
	}
    
}
