package com.xiongdwm.future_backend.bo;

import java.util.Date;

public record StudioManageDto(
    Long id,
    String name,
    String dbName,
    String place,
    Date assignDate,
    Date lastChargeAt,
    Date willChargeAt,
    double chargeAmount,
    long accountCount
) {}
