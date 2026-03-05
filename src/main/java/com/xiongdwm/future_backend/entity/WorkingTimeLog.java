package com.xiongdwm.future_backend.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "working_time_log")
public class WorkingTimeLog {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Column
    private Date date; // 工作日期 如果是0-16 回退一天的日期 如果是16-23则就是当天日期
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column
    private Date thisTermBeginTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column
    private Date thisTermEndTime;
    @Column
    private double timeLasted; // 本次工作时长，单位小时
    @ManyToOne
    @JoinColumn(name = "user_id_working_log", referencedColumnName = "id")
    @JsonManagedReference("user-working-logs")
    private User user; // 关联用户
}
