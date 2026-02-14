package com.xiongdwm.future_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.future_backend.entity.FileLog;

@Repository
public interface FileLogRepository extends JpaRepository<FileLog, String>, JpaSpecificationExecutor<FileLog> {

}
