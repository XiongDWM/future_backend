package com.xiongdwm.future_backend.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column
    private Date enterDate;
    @Column
    private Date leaveDate;
    @Column
    private Date lastLogin;
    @Column
    private Date lastLogout;
    @JsonIgnore
    @Column(nullable = false)
    private String password;
    @Column
    private String softwareCode;
    @Column
    @Enumerated(EnumType.STRING)
    private Role role;
    @Column
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column
    @JsonIgnore
    private String identity; // 身份证号
    @Column 
    private String realName; // 真实姓名
    @Column
    private boolean lastLogoutAuto=false; // 上次登出是否为自动登出（可能是掉线或者直接关闭程序,默认是正常登出）


    @OneToMany(mappedBy = "palworld",targetEntity = Order.class, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonBackReference
    private List<Order> orders = new ArrayList<>();
    @OneToMany(mappedBy = "palworld", targetEntity = RejectionInfo.class, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonBackReference
    private List<RejectionInfo> rejectionInfos = new ArrayList<>();
    @OneToMany(mappedBy = "palworld", targetEntity = BookOrder.class,orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonBackReference
    private List<BookOrder> bookOrders = new ArrayList<>();
    @OneToMany(mappedBy = "palworld", targetEntity = FindingRequest.class, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonBackReference
    private List<FindingRequest> findingRequests = new ArrayList<>();
    @OneToMany(mappedBy = "user", targetEntity = WorkingTimeLog.class, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonBackReference
    private List<WorkingTimeLog> workingTimeLogs = new ArrayList<>();
    @OneToMany(mappedBy = "user", targetEntity = LeaveRecord.class, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonBackReference
    private List<LeaveRecord> leaveRecords = new ArrayList<>();

    public enum Role {
        ADMIN("管理员"),
        SEC_LEVEL_ADMIN("管理"),
        CUSTOMER_SERVICE("客服"),
        PALWORLD("打手");
        private final String roleName;
        private Role(String roleName) {
            this.roleName = this.name();
        }
        public String getRoleName() {
            return roleName;
        }
    }

    public enum Status {
        ACTIVE("等单"),
        PREPARE("有单"),
        HANGING("休息"),
        ONLINE("上线"),
        OFFLINE("下线"),
        BUSY("忙碌"),
        INACTIVE("离职"),
        ON_LEAVE("休假");
        private final String statusName;
        private Status(String statusName) {
            this.statusName = this.name();
        }
        public String getStatusName() {
            return statusName;
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Date getEnterDate() {
        return enterDate;
    }

    public Date getLeaveDate() {
        return leaveDate;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEnterDate(Date enterDate) {
        this.enterDate = enterDate;
    }

    public void setLeaveDate(Date leaveDate) {
        this.leaveDate = leaveDate;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public String getSoftwareCode() {
        return softwareCode;
    }

    public void setSoftwareCode(String softwareCode) {
        this.softwareCode = softwareCode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<RejectionInfo> getRejectionInfos() {
        return rejectionInfos;
    }

    public void setRejectionInfos(List<RejectionInfo> rejectionInfos) {
        this.rejectionInfos = rejectionInfos;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Date getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(Date lastLogout) {
        this.lastLogout = lastLogout;
    }

    public List<BookOrder> getBookOrders() {
        return bookOrders;
    }

    public void setBookOrders(List<BookOrder> bookOrders) {
        this.bookOrders = bookOrders;
    }

    public List<FindingRequest> getFindingRequests() {
        return findingRequests;
    }

    public void setFindingRequests(List<FindingRequest> findingRequests) {
        this.findingRequests = findingRequests;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public boolean isLastLogoutAuto() {
        return lastLogoutAuto;
    }

    public void setLastLogoutAuto(boolean lastLogoutAuto) {
        this.lastLogoutAuto = lastLogoutAuto;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", enterDate=" + enterDate + ", leaveDate=" + leaveDate
                + ", lastLogin=" + lastLogin + ", lastLogout=" + lastLogout + ", password=" + password
                + ", softwareCode=" + softwareCode + ", role=" + role + ", status=" + status + "]";
    }
    

}
