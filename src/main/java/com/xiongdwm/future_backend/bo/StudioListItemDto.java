package com.xiongdwm.future_backend.bo;

import java.util.Date;

public record StudioListItemDto(
    Long id,
    String name,
    String place,
    Date assignDate,
    Date lastChargeAt,
    Date willChargeAt,
    long userCount
) {}
