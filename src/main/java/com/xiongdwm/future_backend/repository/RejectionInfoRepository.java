package com.xiongdwm.future_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.RejectionInfo;

@Repository
public interface RejectionInfoRepository extends JpaRepository<RejectionInfo, Long>, JpaSpecificationExecutor<RejectionInfo> {
    
}
