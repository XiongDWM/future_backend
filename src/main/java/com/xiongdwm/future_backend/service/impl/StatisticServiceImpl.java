package com.xiongdwm.future_backend.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiongdwm.future_backend.bo.OnlineCountStatistic;
import com.xiongdwm.future_backend.bo.OnlineCountStatistic.SnapshotPoint;
import com.xiongdwm.future_backend.bo.statistic.DailyOrderStatistic;
import com.xiongdwm.future_backend.bo.statistic.IncomePartial;
import com.xiongdwm.future_backend.bo.statistic.IncomeStatisticBo;
import com.xiongdwm.future_backend.bo.statistic.UserRankingItem;
import com.xiongdwm.future_backend.bo.statistic.UserWeeklyIncomeTrend;
import com.xiongdwm.future_backend.entity.OrderSection;
import com.xiongdwm.future_backend.entity.PalOnlineSnapshot;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.repository.OrderSectionRepository;
import com.xiongdwm.future_backend.repository.PalOnlineSnapshotRepository;
import com.xiongdwm.future_backend.service.StatisticService;
import com.xiongdwm.future_backend.service.UserService;

@Service
public class StatisticServiceImpl implements StatisticService {

    private static final Logger log = LoggerFactory.getLogger(StatisticServiceImpl.class);

    private final UserService userService;
    private final PalOnlineSnapshotRepository snapshotRepository;
    private final OrderSectionRepository sectionRepository;

    public StatisticServiceImpl(UserService userService, PalOnlineSnapshotRepository snapshotRepository,
                                OrderSectionRepository sectionRepository) {
        this.userService = userService;
        this.snapshotRepository = snapshotRepository;
        this.sectionRepository = sectionRepository;
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
        // log.info("Recorded online snapshot: count={}", count);
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
    public IncomeStatisticBo getUserOrderSummary(Long userId) {
        var user = userService.getUserById(userId);
        Date from = user.getLastPaidDate();
        if (from == null) from = user.getEnterDate();
        Date to = new Date();

        var sections = sectionRepository.findByOrderUserIdAndEndAtGreaterThanAndEndAtLessThan(userId, from, to);

        // 按 repeated 分组
        var unrepeatedSections = sections.stream().filter(s -> !s.isRepeated()).toList();
        var repeatedSections = sections.stream().filter(OrderSection::isRepeated).toList();

        IncomePartial unrepeatedIncome = buildPartial(unrepeatedSections);
        IncomePartial repeatedIncome = buildPartial(repeatedSections);
        IncomePartial othersIncome = new IncomePartial(0, 0, 0, 0, 0);

        double totalIncome = unrepeatedIncome.totalIncome() + repeatedIncome.totalIncome();
        double totalCount = unrepeatedIncome.count() + repeatedIncome.count();

        return new IncomeStatisticBo(from, to, unrepeatedIncome, repeatedIncome, othersIncome, totalIncome, totalCount);
    }

    @Override
    public List<UserRankingItem> getAllUsersRanking() {
        var users = userService.listAllPalworld();
        List<User> activeUsers = users.stream().filter(u -> !u.isDeleted()).toList();
        if (activeUsers.isEmpty()) return List.of();

        Date now = new Date();
        // 找最早的起始日期，只做一次批量查询
        Date earliestFrom = activeUsers.stream()
                .map(u -> u.getLastPaidDate() != null ? u.getLastPaidDate() : u.getEnterDate())
                .filter(d -> d != null)
                .min(Date::compareTo)
                .orElse(now);

        List<Long> userIds = activeUsers.stream().map(User::getId).toList();
        var allSections = sectionRepository.findByUserIdsAndEndAtBetween(userIds, earliestFrom, now);

        // 按 order.userId 分组
        Map<Long, List<OrderSection>> sectionsByUser = allSections.stream()
                .collect(Collectors.groupingBy(s -> s.getOrder().getUserId()));

        List<UserRankingItem> items = new ArrayList<>();
        for (User user : activeUsers) {
            Date from = user.getLastPaidDate() != null ? user.getLastPaidDate() : user.getEnterDate();
            if (from == null) from = now;

            List<OrderSection> userSections = sectionsByUser.getOrDefault(user.getId(), List.of());
            // 过滤到该用户自身的结算周期内
            final Date userFrom = from;
            var filtered = userSections.stream()
                    .filter(s -> s.getEndAt() != null && s.getEndAt().after(userFrom) && s.getEndAt().before(now))
                    .toList();

            IncomePartial unrepeated = buildPartial(filtered.stream().filter(s -> !s.isRepeated()).toList());
            IncomePartial repeated = buildPartial(filtered.stream().filter(OrderSection::isRepeated).toList());

            double totalIncome = unrepeated.totalIncome() + repeated.totalIncome();
            double totalCount = unrepeated.count() + repeated.count();

            items.add(new UserRankingItem(
                    user.getId(),
                    user.getRealName(),
                    user.getUsername(),
                    totalIncome,
                    totalCount
            ));
        }
        return items;
    }

    @Override
    public DailyOrderStatistic getDailyOrderStatistic() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        // 计算当前周期的起点（最近的中午12点）
        Calendar noonToday = Calendar.getInstance();
        noonToday.set(Calendar.HOUR_OF_DAY, 12);
        noonToday.set(Calendar.MINUTE, 0);
        noonToday.set(Calendar.SECOND, 0);
        noonToday.set(Calendar.MILLISECOND, 0);

        Date periodStart;
        if (now.after(noonToday.getTime())) {
            // 当前时间在12点之后 → 从今天中午12点开始
            periodStart = noonToday.getTime();
        } else {
            // 当前时间在12点之前 → 从昨天中午12点开始
            noonToday.add(Calendar.DAY_OF_MONTH, -1);
            periodStart = noonToday.getTime();
        }

        // 前一天同周期：(periodStart - 24h) ~ periodStart
        Calendar prevCal = Calendar.getInstance();
        prevCal.setTime(periodStart);
        prevCal.add(Calendar.DAY_OF_MONTH, -1);
        Date prevPeriodStart = prevCal.getTime();

        // 查询当前周期和上一周期的sections
        var currentSections = sectionRepository.findByEndAtGreaterThanAndEndAtLessThan(periodStart, now);
        var prevSections = sectionRepository.findByEndAtGreaterThanAndEndAtLessThan(prevPeriodStart, periodStart);

        int curFirst = (int) currentSections.stream().filter(s -> !s.isRepeated()).count();
        int curRenewal = (int) currentSections.stream().filter(OrderSection::isRepeated).count();
        int curTotal = curFirst + curRenewal;
        double curRatio = curFirst > 0 ? (double) curRenewal / curFirst : 0;

        double curFirstIncome = currentSections.stream().filter(s -> !s.isRepeated()).mapToDouble(s -> s.getPrice() * s.getAmount()).sum();
        double curRenewalIncome = currentSections.stream().filter(OrderSection::isRepeated).mapToDouble(s -> s.getPrice() * s.getAmount()).sum();
        double curTotalIncome = curFirstIncome + curRenewalIncome;
        double curIncomeRatio = curFirstIncome > 0 ? curRenewalIncome / curFirstIncome : 0;

        int prevFirst = (int) prevSections.stream().filter(s -> !s.isRepeated()).count();
        int prevRenewal = (int) prevSections.stream().filter(OrderSection::isRepeated).count();
        int prevTotal = prevFirst + prevRenewal;
        double prevRatio = prevFirst > 0 ? (double) prevRenewal / prevFirst : 0;

        double prevFirstIncome = prevSections.stream().filter(s -> !s.isRepeated()).mapToDouble(s -> s.getPrice() * s.getAmount()).sum();
        double prevRenewalIncome = prevSections.stream().filter(OrderSection::isRepeated).mapToDouble(s -> s.getPrice() * s.getAmount()).sum();
        double prevTotalIncome = prevFirstIncome + prevRenewalIncome;
        double prevIncomeRatio = prevFirstIncome > 0 ? prevRenewalIncome / prevFirstIncome : 0;

        return new DailyOrderStatistic(
                curTotal, calcChangePercent(curTotal, prevTotal),
                curFirst, calcChangePercent(curFirst, prevFirst),
                curRenewal, calcChangePercent(curRenewal, prevRenewal),
                Math.round(curRatio * 1000.0) / 1000.0,
                calcChangePercent(curRatio, prevRatio),
                Math.round(curTotalIncome * 100.0) / 100.0,
                calcChangePercent(curTotalIncome, prevTotalIncome),
                Math.round(curFirstIncome * 100.0) / 100.0,
                calcChangePercent(curFirstIncome, prevFirstIncome),
                Math.round(curRenewalIncome * 100.0) / 100.0,
                calcChangePercent(curRenewalIncome, prevRenewalIncome),
                Math.round(curIncomeRatio * 1000.0) / 1000.0,
                calcChangePercent(curIncomeRatio, prevIncomeRatio)
        );
    }

    
    private double calcChangePercent(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((current - previous) * 1000.0 / previous) / 10.0;
    }

    @Override
    public UserWeeklyIncomeTrend getUserWeeklyIncomeTrend(Long userId) {
        Date now = new Date();
        // 算出当前周期起点（最近的中午12点）
        Calendar noon = Calendar.getInstance();
        noon.set(Calendar.HOUR_OF_DAY, 12);
        noon.set(Calendar.MINUTE, 0);
        noon.set(Calendar.SECOND, 0);
        noon.set(Calendar.MILLISECOND, 0);
        Date periodEnd;
        if (now.after(noon.getTime())) {
            periodEnd = now;
        } else {
            periodEnd = now;
            noon.add(Calendar.DAY_OF_MONTH, -1);
        }
        // noon 现在是当前周期起点
        // 往前推7天，共7个周期
        List<UserWeeklyIncomeTrend.DayPoint> days = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");

        for (int i = 6; i >= 0; i--) {
            Calendar dayStart = (Calendar) noon.clone();
            dayStart.add(Calendar.DAY_OF_MONTH, -i);
            Calendar dayEnd = (Calendar) dayStart.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);

            Date from = dayStart.getTime();
            Date to = (i == 0) ? periodEnd : dayEnd.getTime();

            var sections = sectionRepository.findByOrderUserIdAndEndAtGreaterThanAndEndAtLessThan(userId, from, to);

            double firstIncome = sections.stream().filter(s -> !s.isRepeated())
                    .mapToDouble(s -> s.getPrice() * s.getAmount()).sum();
            double renewalIncome = sections.stream().filter(OrderSection::isRepeated)
                    .mapToDouble(s -> s.getPrice() * s.getAmount()).sum();
            // 其他收入暂为0，后续可扩展
            double otherIncome = 0;

            days.add(new UserWeeklyIncomeTrend.DayPoint(
                    sdf.format(from),
                    Math.round(firstIncome * 100.0) / 100.0,
                    Math.round(renewalIncome * 100.0) / 100.0,
                    Math.round(otherIncome * 100.0) / 100.0
            ));
        }
        return new UserWeeklyIncomeTrend(days);
    }

    private IncomePartial buildPartial(List<OrderSection> sections) {
        double authorized = 0, unauthorized = 0, pending = 0, count = 0;
        for (var s : sections) {
            double income = s.getAmount() * s.getPrice();
            double workHours = s.getUnitType() != null ? s.getAmount() * s.getUnitType().getMultiplier() : 0;
            if (s.getConfirmed() == null) {
                pending += income;
            } else if (s.getConfirmed()) {
                authorized += income;
            } else {
                unauthorized += income;
            }
            count += workHours;
        }
        double total = authorized + unauthorized + pending;
        return new IncomePartial(total, authorized, unauthorized, pending, count);
    }
}
