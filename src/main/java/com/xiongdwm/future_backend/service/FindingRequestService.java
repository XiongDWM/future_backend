package com.xiongdwm.future_backend.service;

import java.util.List;

import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.entity.FindingRequest;

public interface FindingRequestService {

    public boolean submit(FindingRequest findingRequestDto);
    public boolean confirm(FindingRequestFillDto findingRequestDto); // 相当于要填写一个order，然后把这个request的fulfilled字段改为true
    public List<FindingRequest> getRequests();
    
}
