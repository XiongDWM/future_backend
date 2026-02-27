package com.xiongdwm.future_backend.utils.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中的用户最后活跃时间追踪器。
 * <p>
 * JWT filter 每次认证成功后调用 {@link #touch(long)} 更新时间戳，
 * 定时任务读取 {@link #getIdleUserIds(long)} 获取超时用户列表。
 * <p>
 * 不持久化、不写数据库，重启后自然清空。
 */
@Component
public class UserActivityTracker {

    /** userId → 最后活跃时间（System.currentTimeMillis） */
    private final ConcurrentHashMap<Long, Long> lastActiveMap = new ConcurrentHashMap<>();

    /** 记录用户活跃 */
    public void touch(long userId) {
        lastActiveMap.put(userId, System.currentTimeMillis());
    }

    /** 移除用户（登出后不再追踪） */
    public void remove(long userId) {
        lastActiveMap.remove(userId);
    }

    /** 获取所有空闲超过 timeoutMs 的用户 ID */
    public Set<Long> getIdleUserIds(long timeoutMs) {
        long cutoff = System.currentTimeMillis() - timeoutMs;
        Set<Long> idle = ConcurrentHashMap.newKeySet();
        for (Map.Entry<Long, Long> entry : lastActiveMap.entrySet()) {
            if (entry.getValue() < cutoff) {
                idle.add(entry.getKey());
            }
        }
        return idle;
    }
}
