package com.xiongdwm.future_backend.bo;

import java.util.Date;

public record ConversationSummaryDto(
    Long applicationId,
    String gameType,          // 游戏
    String orderType,         // 类型（MALE/FEMALE/AI_FEMALE）
    String listingStudioName, // 挂单工作室
    String appStudioName,     // 接单工作室
    Date lastMessageAt       // 最后消息时间
) {}
