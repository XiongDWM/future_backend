package com.xiongdwm.future_backend.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xiongdwm.future_backend.bo.StudioManageDto;
import com.xiongdwm.future_backend.platform.entity.PlatformAuditLog;
import com.xiongdwm.future_backend.platform.entity.PlatformUser;

public interface PlatformService {
    String login(String username, String password);
    List<StudioManageDto> getStudios();
    void updateStudio(Long id, Map<String, Object> updates);
    List<PlatformUser> getAdmins();
    void createAdmin(String username, String password, PlatformUser.Role role, Long phone);
    void updateAdmin(Long id, Map<String, Object> updates);
    void deleteAdmin(Long id);
    List<PlatformAuditLog> getAuditLogs(Date from, Date to, String action);
}
