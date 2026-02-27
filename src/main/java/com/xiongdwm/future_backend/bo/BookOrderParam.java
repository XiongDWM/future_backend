package com.xiongdwm.future_backend.bo;

public record BookOrderParam(
    String customer,  // 客户称呼
    String customerId, // 微信号 
    String details, // 客户偏好，订单描述等
    String picProvence, // 订单相关的图片证明来源，纯文本（oss上传获取的文件ID），由前端传入，后端不做解析
    double amount,  // 存单数量
    double price // 存单单价价格
) {

}
