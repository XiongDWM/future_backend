package com.xiongdwm.future_backend.utils.sse;

import java.io.Serializable;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec.Action;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec.Domain;
import com.xiongdwm.future_backend.utils.tenant.TenantContext;

import reactor.core.publisher.Sinks;

@Component
public class GlobalEventBus {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(GlobalEventBus.class);
    private final Sinks.Many<GlobalEventSpec> eventSink;

    public GlobalEventBus(@Qualifier("globalEventSink") Sinks.Many<GlobalEventSpec> eventSink) {
        this.eventSink = eventSink;
    }

    public void emit(Domain domain, Action action, boolean update, Serializable resourceId) {
        emit(domain, action, update, resourceId, false);
    }

    public void emit(Domain domain, Action action, boolean update, Serializable resourceId, boolean broadcast) {
        Long studioId = broadcast ? null : TenantContext.getCurrentStudioId();
        var event = new GlobalEventSpec(UUID.randomUUID().toString(), update, domain, action, resourceId, studioId, broadcast);
        var result = eventSink.tryEmitNext(event);
        if(result.isFailure()) {
            logger.error("Failed to emit event: domain={}, action={}, update={}, reason={}", domain, action, update, result);
        }
    }

    /**
     * 在事务提交后才发送事件；若无活跃事务则立即发送。
     */
    public void emitAfterCommit(Domain domain, Action action, boolean update, Serializable resourceId) {
        emitAfterCommit(domain, action, update, resourceId, false);
    }

    public void emitAfterCommit(Domain domain, Action action, boolean update, Serializable resourceId, boolean broadcast) {
        // 捕获当前 studioId，避免 afterCommit 时 ThreadLocal 丢失
        Long studioId = broadcast ? null : TenantContext.getCurrentStudioId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emitWithStudioId(domain, action, update, resourceId, studioId, broadcast);
                }
            });
        } else {
            emitWithStudioId(domain, action, update, resourceId, studioId, broadcast);
        }
    }

    /**
     * 在事务提交后发送定向事件；若无活跃事务则立即发送。
     * studioId=null 时等同于广播。
     */
    public void emitAfterCommitTo(Domain domain, Action action, boolean update, Serializable resourceId, Long studioId) {
        boolean broadcast = (studioId == null);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emitWithStudioId(domain, action, update, resourceId, studioId, broadcast);
                }
            });
        } else {
            emitWithStudioId(domain, action, update, resourceId, studioId, broadcast);
        }
    }

    // 允许显式传递 studioId
    private void emitWithStudioId(Domain domain, Action action, boolean update, Serializable resourceId, Long studioId, boolean broadcast) {
        var event = new GlobalEventSpec(UUID.randomUUID().toString(), update, domain, action, resourceId, studioId, broadcast);
        var result = eventSink.tryEmitNext(event);
        if(result.isFailure()) {
            logger.error("Failed to emit event: domain={}, action={}, update={}, reason={}", domain, action, update, result);
        }
    }

}
