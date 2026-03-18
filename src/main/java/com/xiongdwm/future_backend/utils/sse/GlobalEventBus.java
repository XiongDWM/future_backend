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

import reactor.core.publisher.Sinks;

@Component
public class GlobalEventBus {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(GlobalEventBus.class);
    private final Sinks.Many<GlobalEventSpec> eventSink;

    public GlobalEventBus(@Qualifier("globalEventSink") Sinks.Many<GlobalEventSpec> eventSink) {
        this.eventSink = eventSink;
    }

    public void emit(Domain domain, Action action, boolean update, Serializable resourceId) {
        var event = new GlobalEventSpec(UUID.randomUUID().toString(), update, domain, action, resourceId);
        var result = eventSink.tryEmitNext(event);
        if(result.isFailure()) {
            logger.error("Failed to emit event: domain={}, action={}, update={}, reason={}", domain, action, update, result);
        }
    }

    /**
     * 在事务提交后才发送事件；若无活跃事务则立即发送。
     */
    public void emitAfterCommit(Domain domain, Action action, boolean update, Serializable resourceId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emit(domain, action, update, resourceId);
                }
            });
        } else {
            emit(domain, action, update, resourceId);
        }
    }

}
