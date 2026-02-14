package com.xiongdwm.future_backend.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.entity.RejectionInfo;
import com.xiongdwm.future_backend.repository.RejectionInfoRepository;
import com.xiongdwm.future_backend.service.RejectionInfoService;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

@Service
public class RejectionInfoServiceImpl implements RejectionInfoService {
    @Autowired
    private RejectionInfoRepository rejectionInfoRepository;
    @Autowired
    private GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.REJECTION_INFO;

    @Override
    public boolean saveRejectionInfo(RejectionInfo rejectionInfo) {
        var info=rejectionInfoRepository.saveAndFlush(rejectionInfo);
        eventBus.emit(domain, GlobalEventSpec.Action.CREATE, true, info.getId());
        return info!=null;
    }
    
}
