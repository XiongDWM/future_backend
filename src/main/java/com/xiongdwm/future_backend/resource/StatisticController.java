package com.xiongdwm.future_backend.resource;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.OnlineCountStatistic;
import com.xiongdwm.future_backend.bo.statistic.DailyOrderStatistic;
import com.xiongdwm.future_backend.bo.statistic.IncomeStatisticBo;
import com.xiongdwm.future_backend.bo.statistic.UserRankingItem;
import com.xiongdwm.future_backend.bo.statistic.UserWeeklyIncomeTrend;
import com.xiongdwm.future_backend.service.StatisticService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class StatisticController {

    private final StatisticService statisticService;

    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    /** 在线打手统计：当前人数、日/周环比、7天每小时曲线数据 */
    @GetMapping("/statistic/online-count")
    public Mono<ApiResponse<OnlineCountStatistic>> onlineCountStatistic() {
        return Mono.fromCallable(() -> {
            var stat = statisticService.getOnlineCountStatistic();
            return ApiResponse.success(stat);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 打手个人当期收入统计 */
    @PostMapping("/statistic/user-summary")
    public Mono<ApiResponse<IncomeStatisticBo>> userOrderSummary(@RequestBody Map<String, Long> body) {
        return Mono.fromCallable(() -> {
            var userId = body.get("userId");
            var summary = statisticService.getUserOrderSummary(userId);
            return ApiResponse.success(summary);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 所有打手当期排行 */
    @GetMapping("/statistic/ranking")
    public Mono<ApiResponse<List<UserRankingItem>>> ranking() {
        return Mono.fromCallable(() -> {
            var items = statisticService.getAllUsersRanking();
            return ApiResponse.success(items);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 今日订单统计（以中午12点为分界） */
    @GetMapping("/statistic/daily-order")
    public Mono<ApiResponse<DailyOrderStatistic>> dailyOrderStatistic() {
        return Mono.fromCallable(() -> {
            var stat = statisticService.getDailyOrderStatistic();
            return ApiResponse.success(stat);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 指定打手近7天收入趋势 */
    @GetMapping("/statistic/weekly-trend")
    public Mono<ApiResponse<UserWeeklyIncomeTrend>> weeklyTrend(@RequestParam("userId") Long userId) {
        return Mono.fromCallable(() -> {
            var trend = statisticService.getUserWeeklyIncomeTrend(userId);
            return ApiResponse.success(trend);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
