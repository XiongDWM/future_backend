package com.xiongdwm.future_backend.resource;

import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec.Domain;

import reactor.core.publisher.Flux;

@RestController
public class EventStreamController {
    
    private final Flux<GlobalEventSpec> globalEventFlux;

    public EventStreamController(@Qualifier("globalEventFlux") Flux<GlobalEventSpec> globalEventFlux) {
        this.globalEventFlux = globalEventFlux;
    }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GlobalEventSpec> streamEvents(
            @RequestParam(name = "domain", required = false) List<Domain> domains) {

        if (domains == null || domains.isEmpty()) {
            return globalEventFlux; // 不传参数则下发全部事件
        }
        var domainSet = EnumSet.copyOf(domains);
        return globalEventFlux.filter(ev -> domainSet.contains(ev.domain()));
    }
}
