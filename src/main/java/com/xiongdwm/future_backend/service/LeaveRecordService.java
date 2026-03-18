package com.xiongdwm.future_backend.service;

import org.springframework.data.domain.Page;

import com.xiongdwm.future_backend.bo.LeaveRecordParam;
import com.xiongdwm.future_backend.entity.LeaveRecord;
import com.xiongdwm.future_backend.entity.User;

public interface LeaveRecordService {
    LeaveRecord createLeaveRecord(LeaveRecordParam record, User user);
    LeaveRecord updateLeaveRecord(Long id, LeaveRecord record);
    void deleteLeaveRecord(Long id);
    LeaveRecord getLeaveRecord(Long id);
    Page<LeaveRecord> listLeaveRecords(int page, int size, Long userId, String type);
    void cancelLeave(Long id);
    void cancelLeaveByUser(User user);
}
