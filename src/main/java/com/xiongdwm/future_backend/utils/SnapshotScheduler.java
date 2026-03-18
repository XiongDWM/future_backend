package com.xiongdwm.future_backend.utils;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.xiongdwm.future_backend.service.StatisticService;

import jakarta.annotation.Resource;

@EnableScheduling
@Component
public class SnapshotScheduler {

    @Resource
    private StatisticService statisticService;

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
}
