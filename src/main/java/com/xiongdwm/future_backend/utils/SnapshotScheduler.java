package com.xiongdwm.future_backend.utils;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.xiongdwm.future_backend.service.StatisticService;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.security.UserActivityTracker;

import jakarta.annotation.Resource;

@EnableScheduling
@Component
public class SnapshotScheduler {

    @Resource
    private StatisticService statisticService;
    @Resource
    private UserService userService;
    @Resource
    private UserActivityTracker activityTracker;

    /** 空闲超时（毫秒）— 与前端 30 分钟一致 */
    private static final long IDLE_TIMEOUT_MS = 30 * 60 * 1000L;

    /** 每整点记录一次在线打手快照 */
    @Scheduled(cron = "0 0 * * * *")
    public void recordSnapshot() {
        statisticService.recordOnlineSnapshot();
    }

    /** 每天凌晨3点清理超过7天的快照 */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldSnapshots() {
        statisticService.cleanOldSnapshots();
    }

    /** 每 5 分钟扫描一次：将超过 30 分钟无活跃的用户自动登出（兜底） */
    @Scheduled(fixedRate = 300_000)
    public void autoLogoutIdleUsers() {
        var idleUserIds = activityTracker.getIdleUserIds(IDLE_TIMEOUT_MS);
        for (Long userId : idleUserIds) {
            try {
                userService.logout(userId);
                activityTracker.remove(userId);
            } catch (Exception ignored) { 
                // 忽略异常
                activityTracker.remove(userId);
            }
        }
    }
}
