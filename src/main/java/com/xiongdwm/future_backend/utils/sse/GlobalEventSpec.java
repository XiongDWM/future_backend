package com.xiongdwm.future_backend.utils.sse;

import java.io.Serializable;

public record GlobalEventSpec(
    String uid,
    boolean update, 
    Domain domain, 
    Action action,
    Serializable resourceId
) {
    public enum Domain{
        USER("用户"),
        ORDER("订单"),
        ORDER_SECTION("订单段"),
        SECOND_HAND("二手单"),
        BOOKING("存单"),
        REJECTION_INFO("拒绝信息"),
        FINDING_REQUEST("找单"),
        ACCOUNT_RENTING("租账号");

        private final String label;
        private Domain(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
    }

    public enum Action{
        SNAPSHOT("快照",false),
        CANCEL("撤销", true),
        CREATE("创建", true),
        UPDATE("更新", true),
        DELETE("删除", true);

        private final String label;
        private final boolean fetchable;
        private Action(String label, boolean fetchable) {
            this.label = label;
            this.fetchable = fetchable;
        }
        public String getLabel() {
            return label;
        }
        public boolean isFetchable() {
            return fetchable;
        }   
    }
}
