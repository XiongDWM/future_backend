package com.xiongdwm.future_backend.bo;

import java.util.Date;

public record ThirdPartyApplicationDto(
    Long id,
    Long listingId,
    Long studioId,
    String studioName,
    Date appliedAt,
    String status,
    String note,
    String failureReason, // 炸单原因（status=FAILURE时有值）
    String customerId,    // 客户ID
    double originalPrice, // 原单价
    double price,         // 结算价
    boolean customerTran, // 是否送老板
    String orderType,     // 单子类型
    String orderId,       // 绑定内部系统订单ID
    String picStart,      // 开工截图
    String picEnd,        // 完成截图
    String gameType,          // 挂单游戏类型
    String listingStudioName  // 挂单方工作室名
) {}
