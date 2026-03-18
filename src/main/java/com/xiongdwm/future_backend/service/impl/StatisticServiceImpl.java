package com.xiongdwm.future_backend.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiongdwm.future_backend.bo.OnlineCountStatistic;
import com.xiongdwm.future_backend.bo.OnlineCountStatistic.SnapshotPoint;
import com.xiongdwm.future_backend.entity.PalOnlineSnapshot;
import com.xiongdwm.future_backend.repository.OrderRepository;
import com.xiongdwm.future_backend.repository.PalOnlineSnapshotRepository;
import com.xiongdwm.future_backend.service.OrderSectionService;
import com.xiongdwm.future_backend.service.StatisticService;
import com.xiongdwm.future_backend.service.UserService;

@Service
public class StatisticServiceImpl implements StatisticService {

    private static final Logger log = LoggerFactory.getLogger(StatisticServiceImpl.class);

    private final UserService userService;
    private final PalOnlineSnapshotRepository snapshotRepository;
    private final OrderRepository orderRepository;
    private final OrderSectionService sectionService;

    public StatisticServiceImpl(UserService userService, PalOnlineSnapshotRepository snapshotRepository,
                                OrderRepository orderRepository, OrderSectionService sectionService) {
        this.userService = userService;
        this.snapshotRepository = snapshotRepository;
        this.orderRepository = orderRepository;
        this.sectionService = sectionService;
    }

    @Override
    public OnlineCountStatistic getOnlineCountStatistic() {
        var stat = new OnlineCountStatistic();

        // 当前实时在线人数
        int currentCount = userService.listOnlinePalworld().size();
        stat.setCurrentCount(currentCount);

        Calendar cal = Calendar.getInstance();

        // --- 今天 00:00 ~ 明天 00:00 ---
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrowStart = cal.getTime();

        // --- 昨天 00:00 ---
        cal.setTime(todayStart);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterdayStart = cal.getTime();

        // --- 本周一 00:00 ---
        cal.setTime(todayStart);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (cal.getTime().after(todayStart)) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        Date thisWeekStart = cal.getTime();

        // --- 上周一 / 上周日 ---
        cal.setTime(thisWeekStart);
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        Date lastWeekStart = cal.getTime();
        Date lastWeekEnd = thisWeekStart;

        // 计算各时段平均
        int dayCount = avgCount(todayStart, tomorrowStart);
        int lastDayCount = avgCount(yesterdayStart, todayStart);
        int weekCount = avgCount(thisWeekStart, tomorrowStart);
        int lastWeekCount = avgCount(lastWeekStart, lastWeekEnd);

        stat.setDayCount(dayCount);
        stat.setLastDayCount(lastDayCount);
        stat.setWeekCount(weekCount);
        stat.setLastWeekCount(lastWeekCount);

        // 时环比：当前在线 vs 上一小时快照
        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, -1);
        Date oneHourAgo = cal.getTime();
        cal.add(Calendar.HOUR_OF_DAY, -1);
        Date twoHoursAgo = cal.getTime();
        int thisHourCount = currentCount;
        int lastHourCount = avgCount(twoHoursAgo, oneHourAgo);
        stat.setHourChangePercent(calcChangePercent(thisHourCount, lastHourCount));

        // 环比百分比
        stat.setDayChangePercent(calcChangePercent(dayCount, lastDayCount));
        stat.setWeekChangePercent(calcChangePercent(weekCount, lastWeekCount));

        // 7天每小时明细
        cal.setTime(todayStart);
        cal.add(Calendar.DAY_OF_MONTH, -6); // 往前7天（含今天）
        Date sevenDaysAgo = cal.getTime();

        List<PalOnlineSnapshot> snapshots =
                snapshotRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(sevenDaysAgo, tomorrowStart);
        List<SnapshotPoint> hourlyData = snapshots.stream()
                .map(s -> new SnapshotPoint(s.getRecordedAt(), s.getOnlineCount()))
                .collect(Collectors.toList());
        stat.setHourlyData(hourlyData);

        return stat;
    }

    @Override
    @Transactional
    public void recordOnlineSnapshot() {
        int count = userService.listOnlinePalworld().size();
        var snapshot = new PalOnlineSnapshot(new Date(), count);
        snapshotRepository.save(snapshot);
        log.info("Recorded online snapshot: count={}", count);
    }

    @Override
    @Transactional
    public void cleanOldSnapshots() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        int deleted = snapshotRepository.deleteByRecordedAtBefore(cal.getTime());
        if (deleted > 0) {
            log.info("Cleaned {} old snapshots", deleted);
        }
    }

    /** 计算时间段内快照的平均在线数 */
    private int avgCount(Date start, Date end) {
        var list = snapshotRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(start, end);
        if (list.isEmpty()) return 0;
        return (int) Math.round(list.stream().mapToInt(PalOnlineSnapshot::getOnlineCount).average().orElse(0));
    }

    /** 计算环比百分比 */
    private double calcChangePercent(int current, int previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((current - previous) * 1000.0 / previous) / 10.0;
    }

    @Override
    public java.util.Map<String, Object> getUserOrderSummary(Long userId) {
        var orders = orderRepository.findAll(
            (root, query, cb) -> cb.equal(root.get("userId"), userId)
        );
        int totalOrders = orders.size();
        double totalIncome = 0;
        for (var order : orders) {
            var sections = sectionService.findByOrderId(order.getOrderId());
            totalIncome += sections.stream()
                .mapToDouble(com.xiongdwm.future_backend.entity.OrderSection::getPrice)
                .sum();
        }
        return java.util.Map.of("totalOrders", totalOrders, "totalIncome", totalIncome);
    }
}
