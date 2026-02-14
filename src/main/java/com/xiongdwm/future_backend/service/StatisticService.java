package com.xiongdwm.future_backend.service;

import com.xiongdwm.future_backend.bo.OnlineCountStatistic;

public interface StatisticService {
    /** 获取在线打手统计（含7天每小时明细） */
    OnlineCountStatistic getOnlineCountStatistic();

    /** 记录当前时刻快照（定时任务调用） */
    void recordOnlineSnapshot();

    /** 清理超过7天的快照数据 */
    void cleanOldSnapshots();
}
