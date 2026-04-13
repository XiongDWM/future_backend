package com.xiongdwm.future_backend.service;

import java.util.List;

import com.xiongdwm.future_backend.bo.OnlineCountStatistic;
import com.xiongdwm.future_backend.bo.statistic.DailyOrderStatistic;
import com.xiongdwm.future_backend.bo.statistic.IncomeStatisticBo;
import com.xiongdwm.future_backend.bo.statistic.UserRankingItem;
import com.xiongdwm.future_backend.bo.statistic.UserWeeklyIncomeTrend;

public interface StatisticService {
    /** 获取在线打手统计（含7天每小时明细） */
    OnlineCountStatistic getOnlineCountStatistic();

    /** 记录当前时刻快照（定时任务调用） */
    void recordOnlineSnapshot();

    /** 清理超过7天的快照数据 */
    void cleanOldSnapshots();

    /** 获取指定打手的当期收入统计 */
    IncomeStatisticBo getUserOrderSummary(Long userId);

    /** 获取所有打手的当期排行 */
    List<UserRankingItem> getAllUsersRanking();

    /** 今日订单统计（以中午12点为分界） */
    DailyOrderStatistic getDailyOrderStatistic();

    /** 指定打手近7天收入趋势（以中午12点为分界） */
    UserWeeklyIncomeTrend getUserWeeklyIncomeTrend(Long userId);
}
