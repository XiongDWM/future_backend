package com.xiongdwm.future_backend.bo;

import java.util.Date;

public record LeaveRecordParam(
    String type,
    String reason,
    Date startDate,
    Date endDate
) {
}   

    
