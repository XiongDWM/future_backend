package com.xiongdwm.future_backend.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pal_online_snapshot")
public class PalOnlineSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Date recordedAt;

    @Column(nullable = false)
    private int onlineCount;

    public PalOnlineSnapshot() {}

    public PalOnlineSnapshot(Date recordedAt, int onlineCount) {
        this.recordedAt = recordedAt;
        this.onlineCount = onlineCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Date recordedAt) { this.recordedAt = recordedAt; }
    public int getOnlineCount() { return onlineCount; }
    public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }
}
