package com.xiongdwm.future_backend.bo.statistic;

public record UserRankingItem(
    Long userId,
    String realName,
    String username,
    double totalIncome,
    double totalCount
) {
}
