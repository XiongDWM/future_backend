package com.xiongdwm.future_backend.bo;

import java.util.Date;

public record ThirdPartyListingDto(
    Long id,
    Long studioId,
    String studioName,
    String gameType,
    String rankInfo,
    String description,
    double originalPrice, // 原单价
    double price,         // 结算价
    boolean customerTran, // 是否送老板
    String orderType,     // 单子类型
    Date postedAt,
    String status,
    int applicationCount, // 申请人数
    boolean applied,      // 当前工作室是否已申请
    String failureReason, // 炸单原因（status=FAILURE时有值）
    String customerId,    // 客户ID（仅我的挂单可见）
    String picStart,      // 开工截图
    String picEnd         // 完成截图
) {}
