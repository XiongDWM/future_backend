package com.xiongdwm.future_backend.bo;

public record ThirdPartyListingParam(
    String gameType,    // 游戏类型
    String rankInfo,    // 段位描述
    String description, // 补充说明（可选）
    double originalPrice, // 原单价
    double price,       // 结算价
    boolean customerTran,  // 是否送老板（true=送，false=不送）
    String orderType    // 单子类型：MALE/FEMALE/AI_FEMALE
) {}
