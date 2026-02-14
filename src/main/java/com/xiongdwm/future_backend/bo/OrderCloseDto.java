package com.xiongdwm.future_backend.bo;

import jakarta.annotation.Nonnull;

public class OrderCloseDto {
    @Nonnull private String orderId;
    @Nonnull private String picString;
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getPicString() {
        return picString;
    }
    public void setPicString(String picString) {
        this.picString = picString;
    }

    
}
