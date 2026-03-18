package com.xiongdwm.future_backend.service.impl;


import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.entity.RejectionInfo;
import com.xiongdwm.future_backend.repository.RejectionInfoRepository;
import com.xiongdwm.future_backend.service.RejectionInfoService;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

@Service
public class RejectionInfoServiceImpl implements RejectionInfoService {
    private final RejectionInfoRepository rejectionInfoRepository;
    private final GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain=GlobalEventSpec.Domain.REJECTION_INFO;

    public RejectionInfoServiceImpl(RejectionInfoRepository rejectionInfoRepository, GlobalEventBus eventBus) {
        this.rejectionInfoRepository = rejectionInfoRepository;
        this.eventBus = eventBus;
    }

    @Override
    public boolean saveRejectionInfo(RejectionInfo rejectionInfo) {
        var info=rejectionInfoRepository.saveAndFlush(rejectionInfo);
        eventBus.emit(domain, GlobalEventSpec.Action.CREATE, true, info.getId());
        return info!=null;
    }
    
}
