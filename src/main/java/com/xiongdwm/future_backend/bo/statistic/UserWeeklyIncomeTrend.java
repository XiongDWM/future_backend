package com.xiongdwm.future_backend.bo.statistic;

import java.util.List;

public record UserWeeklyIncomeTrend(
    List<DayPoint> days
) {
    public record DayPoint(
        String date,
        double firstIncome,
        double renewalIncome,
        double otherIncome
    ) {}
}
