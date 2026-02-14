package com.xiongdwm.future_backend.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.OnlineCountStatistic;
import com.xiongdwm.future_backend.service.StatisticService;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class StatisticController {

    @Resource
    private StatisticService statisticService;

    /** 在线打手统计：当前人数、日/周环比、7天每小时曲线数据 */
    @GetMapping("/statistic/online-count")
    public Mono<ApiResponse<OnlineCountStatistic>> onlineCountStatistic() {
        return Mono.fromCallable(() -> {
            var stat = statisticService.getOnlineCountStatistic();
            return ApiResponse.success(stat);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
