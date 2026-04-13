package com.xiongdwm.future_backend.resource;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec.Domain;

import reactor.core.publisher.Flux;

@RestController
public class EventStreamController {
    
    private final Flux<GlobalEventSpec> globalEventFlux;
    private final JwtTokenProvider jwtTokenProvider;

    public EventStreamController(@Qualifier("globalEventFlux") Flux<GlobalEventSpec> globalEventFlux,
                                 JwtTokenProvider jwtTokenProvider) {
        this.globalEventFlux = globalEventFlux;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<GlobalEventSpec>> streamEvents(
            @RequestParam(name = "domain", required = false) List<Domain> domains,
            @RequestParam(name = "token", required = false) String token) {

        // 从 token 提取 studioId 实现租户隔离
        Long studioId = (token != null && !token.isBlank())
                ? jwtTokenProvider.getStudioIdFromRawToken(token)
                : null;

        Flux<GlobalEventSpec> stream = globalEventFlux;

        if (studioId != null) {
            final Long sid = studioId;
            stream = stream.filter(ev -> ev.broadcast() || sid.equals(ev.studioId()));
        } else {
            // 无法识别租户时只允许广播事件，避免跨工作室事件串台
            stream = stream.filter(GlobalEventSpec::broadcast);
        }

        if (domains != null && !domains.isEmpty()) {
            var domainSet = EnumSet.copyOf(domains);
            stream = stream.filter(ev -> domainSet.contains(ev.domain()));
        }

        // 业务事件 → 包装为 SSE data 帧
        Flux<ServerSentEvent<GlobalEventSpec>> dataFrames = stream
                .map(ev -> ServerSentEvent.<GlobalEventSpec>builder().data(ev).build());

        // 每 25 秒发送一次 SSE 注释帧 ": keepalive"，防止空闲断连
        Flux<ServerSentEvent<GlobalEventSpec>> keepalive = Flux.interval(Duration.ofSeconds(25))
                .map(i -> ServerSentEvent.<GlobalEventSpec>builder().comment("keepalive").build());

        return Flux.merge(dataFrames, keepalive);
    }
}
