package com.xiongdwm.future_backend.utils.tenant;

public class TenantContext {
    private static final ThreadLocal<String> currentTenantDb = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentStudioId = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantDb) {
        currentTenantDb.set(tenantDb);
    }

    public static String getCurrentTenant() {
        return currentTenantDb.get();
    }

    public static void setCurrentStudioId(Long studioId) {
        currentStudioId.set(studioId);
    }

    public static Long getCurrentStudioId() {
        return currentStudioId.get();
    }

    public static void clear() {
        currentTenantDb.remove();
        currentStudioId.remove();
    }
}
