package com.xiongdwm.future_backend.utils.sse;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Configuration
public class GlobalEventBusConfig {
    private static final Logger logger = LoggerFactory.getLogger(GlobalEventBusConfig.class);

    @Bean(name = "globalEventSink")
    public Sinks.Many<GlobalEventSpec> globalEventSink() {
        return Sinks.many().multicast().onBackpressureBuffer(256, false);
    }

    @Bean
    public Flux<GlobalEventSpec> globalEventFlux(Sinks.Many<GlobalEventSpec> globalEventSink) {
        return globalEventSink.asFlux()
                .bufferTimeout(20, Duration.ofMillis(300))
                .map(events -> {
                    var merged = new LinkedHashMap<String, GlobalEventSpec>();
                    for (var e : events) {
                        merged.put(e.domain() + ":" + e.resourceId(), e);
                    }
                    return merged.values();
                })
                .flatMapIterable(Function.identity())
                .onBackpressureLatest()
                .doOnSubscribe(sub -> logger.info("New SSE client connected"))
                .doOnCancel(() -> logger.info("SSE client disconnected"));
    }
}
