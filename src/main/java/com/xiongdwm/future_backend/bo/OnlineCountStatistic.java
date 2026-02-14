package com.xiongdwm.future_backend.bo;

import java.util.Date;
import java.util.List;

public class OnlineCountStatistic {
    private int currentCount;        // 当前在线打手数
    private int dayCount;            // 今天平均在线
    private int lastDayCount;        // 昨天平均在线
    private int weekCount;           // 本周平均在线
    private int lastWeekCount;       // 上周平均在线
    private double hourChangePercent;// 时环比变化 %
    private double dayChangePercent; // 日环比变化 %
    private double weekChangePercent;// 周环比变化 %
    private List<SnapshotPoint> hourlyData; // 7天每小时明细

    /** 每小时快照数据点 */
    public static class SnapshotPoint {
        private Date recordedAt;
        private int onlineCount;

        public SnapshotPoint() {}
        public SnapshotPoint(Date recordedAt, int onlineCount) {
            this.recordedAt = recordedAt;
            this.onlineCount = onlineCount;
        }

        public Date getRecordedAt() { return recordedAt; }
        public void setRecordedAt(Date recordedAt) { this.recordedAt = recordedAt; }
        public int getOnlineCount() { return onlineCount; }
        public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }
    }

    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }
    public int getDayCount() { return dayCount; }
    public void setDayCount(int dayCount) { this.dayCount = dayCount; }
    public int getLastDayCount() { return lastDayCount; }
    public void setLastDayCount(int lastDayCount) { this.lastDayCount = lastDayCount; }
    public int getWeekCount() { return weekCount; }
    public void setWeekCount(int weekCount) { this.weekCount = weekCount; }
    public int getLastWeekCount() { return lastWeekCount; }
    public void setLastWeekCount(int lastWeekCount) { this.lastWeekCount = lastWeekCount; }
    public double getHourChangePercent() { return hourChangePercent; }
    public void setHourChangePercent(double hourChangePercent) { this.hourChangePercent = hourChangePercent; }
    public double getDayChangePercent() { return dayChangePercent; }
    public void setDayChangePercent(double dayChangePercent) { this.dayChangePercent = dayChangePercent; }
    public double getWeekChangePercent() { return weekChangePercent; }
    public void setWeekChangePercent(double weekChangePercent) { this.weekChangePercent = weekChangePercent; }
    public List<SnapshotPoint> getHourlyData() { return hourlyData; }
    public void setHourlyData(List<SnapshotPoint> hourlyData) { this.hourlyData = hourlyData; }
}
