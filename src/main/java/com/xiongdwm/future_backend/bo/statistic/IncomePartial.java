package com.xiongdwm.future_backend.bo.statistic;

public record IncomePartial(
    double totalIncome,
    double authorizedIncome, // 审核通过section收入
    double unauthorizedIncome, // 被拒绝section收入
    double pendingIncome, // 待审核section收入
    double count // section的amount*unitType.multiplier的和,代表工作了多少小时
) {
}
