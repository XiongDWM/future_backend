package com.xiongdwm.future_backend.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.bo.LeaveRecordParam;
import com.xiongdwm.future_backend.entity.LeaveRecord;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.repository.LeaveRecordRepository;
import com.xiongdwm.future_backend.repository.UserRepository;
import com.xiongdwm.future_backend.service.LeaveRecordService;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;

@Service
public class LeaveRecordServiceImpl implements LeaveRecordService {

    @Autowired
    private LeaveRecordRepository leaveRecordRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GlobalEventBus eventBus;
    @Autowired
    private TaskScheduler taskScheduler;
    private final GlobalEventSpec.Domain domain = GlobalEventSpec.Domain.LEAVE_RECORD;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void restoreScheduledTasks() {
        var pendingRecords = leaveRecordRepository.findByCancelDateIsNullAndStartDateAfter(new Date());
        for (var record : pendingRecords) {
            scheduleLeaveStart(record);
        }
    }

    private void scheduleLeaveStart(LeaveRecord record) {
        var future = taskScheduler.schedule(() -> {
            var user = userRepository.findById(record.getUser().getId()).orElse(null);
            if (user != null && user.getStatus() != User.Status.ON_LEAVE && user.getStatus() != User.Status.INACTIVE) {
                user.setStatus(User.Status.ON_LEAVE);
                userRepository.saveAndFlush(user);
            }
            scheduledTasks.remove(record.getId());
        }, record.getStartDate().toInstant());
        scheduledTasks.put(record.getId(), future);
    }

    private void cancelScheduledTask(Long recordId) {
        var future = scheduledTasks.remove(recordId);
        if (future != null) future.cancel(false);
    }

    @Override
    public LeaveRecord createLeaveRecord(LeaveRecordParam record, User user) {
        var entity = new LeaveRecord();
        entity.setType(record.type());
        entity.setReason(record.reason());
        entity.setStartDate(record.startDate());
        entity.setEndDate(record.endDate());
        entity.setUser(user);
        var saved = leaveRecordRepository.save(entity);
        var action = GlobalEventSpec.Action.CREATE;
        eventBus.emit(domain, action, action.isFetchable(), saved.getId());
        if (saved.getStartDate() != null && saved.getStartDate().after(new Date())) {
            scheduleLeaveStart(saved);
        } else if (saved.getStartDate() != null) {
            // startDate已过，立即生效
            if (user.getStatus() != User.Status.ON_LEAVE && user.getStatus() != User.Status.INACTIVE) {
                user.setStatus(User.Status.ON_LEAVE);
                userRepository.saveAndFlush(user);
            }
        }
        return saved;
    }

    @Override
    public LeaveRecord updateLeaveRecord(Long id, LeaveRecord record) {
        var existing = leaveRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("休假记录不存在"));
        existing.setType(record.getType());
        existing.setReason(record.getReason());
        existing.setStartDate(record.getStartDate());
        existing.setEndDate(record.getEndDate());
        var updated = leaveRecordRepository.saveAndFlush(existing);
        var action = GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), updated.getId());
        // 重新调度定时任务
        cancelScheduledTask(id);
        if (updated.getStartDate() != null && updated.getStartDate().after(new Date())) {
            scheduleLeaveStart(updated);
        }
        return updated;
    }

    @Override
    public void deleteLeaveRecord(Long id) {
        if (!leaveRecordRepository.existsById(id)) {
            throw new RuntimeException("休假记录不存在");
        }
        cancelScheduledTask(id);
        leaveRecordRepository.deleteById(id);
        var action = GlobalEventSpec.Action.DELETE;
        eventBus.emit(domain, action, action.isFetchable(), id);
    }

    @Override
    public void cancelLeave(Long id) {
        var existing = leaveRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("休假记录不存在"));
        cancelScheduledTask(id);
        existing.setCancelDate(new Date());
        leaveRecordRepository.saveAndFlush(existing);
        var action = GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), id);
    }

    @Override
    public void cancelLeaveByUser(User user) {
        var activeLeaves = leaveRecordRepository.findByUserAndCancelDateIsNullOrderByIdDesc(user);
        if (activeLeaves.isEmpty()) return;
        var now = new Date();
        for (var leave : activeLeaves) {
            cancelScheduledTask(leave.getId());
            leave.setCancelDate(now);
        }
        leaveRecordRepository.saveAllAndFlush(activeLeaves);
        var action = GlobalEventSpec.Action.UPDATE;
        eventBus.emit(domain, action, action.isFetchable(), user.getId());
    }

    @Override
    public LeaveRecord getLeaveRecord(Long id) {
        return leaveRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("休假记录不存在"));
    }

    @Override
    public Page<LeaveRecord> listLeaveRecords(int page, int size, Long userId, String type) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<LeaveRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return leaveRecordRepository.findAll(spec, pageable);
    }
}
