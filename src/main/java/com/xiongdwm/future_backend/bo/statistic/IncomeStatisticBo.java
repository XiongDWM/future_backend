package com.xiongdwm.future_backend.bo.statistic;

import java.util.Date;

public record IncomeStatisticBo(
    Date from,
    Date to,
    IncomePartial unrepeatedIncome, // 首单收入
    IncomePartial repeatedIncome, // 续单收入
    IncomePartial othersIncome, // 其他收入（存单等
    double totalIncome, // 上面三个收入的和
    double totalCount // 上面三个收入的count之和,代表工作了多少小时
) {
}
