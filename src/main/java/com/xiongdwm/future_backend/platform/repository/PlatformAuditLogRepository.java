package com.xiongdwm.future_backend.platform.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.future_backend.platform.entity.PlatformAuditLog;

public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLog, Long> {
    List<PlatformAuditLog> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Date from, Date to);
    List<PlatformAuditLog> findAllByActionAndCreatedAtBetweenOrderByCreatedAtDesc(String action, Date from, Date to);
}
