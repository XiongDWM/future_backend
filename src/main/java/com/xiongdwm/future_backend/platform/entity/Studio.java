package com.xiongdwm.future_backend.platform.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "studio")
public class Studio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String dbName;

    @Column(nullable = false)
    private String name;

    @Column
    private String place;

    @Column
    private Date assignDate;

    @Column
    private Date lastChargeAt;

    @Column
    private Date willChargeAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public Date getAssignDate() { return assignDate; }
    public void setAssignDate(Date assignDate) { this.assignDate = assignDate; }
    public Date getLastChargeAt() { return lastChargeAt; }
    public void setLastChargeAt(Date lastChargeAt) { this.lastChargeAt = lastChargeAt; }
    public Date getWillChargeAt() { return willChargeAt; }
    public void setWillChargeAt(Date willChargeAt) { this.willChargeAt = willChargeAt; }
}
