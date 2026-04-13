package com.xiongdwm.future_backend.service;

import java.util.List;


import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.entity.FindingRequest;
import com.xiongdwm.future_backend.entity.User;

public interface FindingRequestService {

    public boolean submit(FindingRequest findingRequestDto, Long studioId);
    public boolean confirm(FindingRequestFillDto findingRequestDto); // 相当于要填写一个order，然后把这个request的fulfilled字段改为true
    public boolean cancel(Long requestId); // 取消找单请求
    public List<FindingRequest> getRequests(User user); // 获取找单请求列表，参数是当前用户（如果有的话），用来标记哪些请求是这个用户提交的
    public List<FindingRequest> getRequests();
    
}
