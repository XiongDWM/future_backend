package com.xiongdwm.future_backend.bo.statistic;

public record DailyOrderStatistic(
    // 数量统计
    int total,
    double totalChangePercent,
    int firstOrder,
    double firstOrderChangePercent,
    int renewalOrder,
    double renewalOrderChangePercent,
    double renewalRatio,
    double renewalRatioChangePercent,
    // 收入统计
    double totalIncome,
    double totalIncomeChangePercent,
    double firstIncome,
    double firstIncomeChangePercent,
    double renewalIncome,
    double renewalIncomeChangePercent,
    double incomeRenewRatio,
    double incomeRenewRatioChangePercent
) {}
