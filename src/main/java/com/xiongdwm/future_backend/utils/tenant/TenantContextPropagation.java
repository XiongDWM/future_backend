package com.xiongdwm.future_backend.utils.tenant;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Hooks;

/**
 * 将 TenantContext（ThreadLocal）注册到 Reactor 上下文传播机制。
 * 当 Mono.fromCallable().subscribeOn(boundedElastic) 切换线程时，
 * TenantContext 会自动从 Reactor Context 传播到 ThreadLocal。
 */
@Configuration
public class TenantContextPropagation {

    public static final String KEY = "tenantDb";
    public static final String STUDIO_ID_KEY = "tenantStudioId";

    @PostConstruct
    void register() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                KEY,
                TenantContext::getCurrentTenant,
                TenantContext::setCurrentTenant,
                () -> { /* clear handled by STUDIO_ID_KEY accessor */ TenantContext.clear(); }
        );
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                STUDIO_ID_KEY,
                TenantContext::getCurrentStudioId,
                TenantContext::setCurrentStudioId,
                () -> { /* no-op, clear already done above */ }
        );
        Hooks.enableAutomaticContextPropagation();
    }
}
