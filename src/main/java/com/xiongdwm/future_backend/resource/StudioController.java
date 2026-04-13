package com.xiongdwm.future_backend.resource;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.StudioListItemDto;
import com.xiongdwm.future_backend.bo.StudioRegisterParam;
import com.xiongdwm.future_backend.service.StudioService;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class StudioController {

    private final StudioService studioService;
    private final JwtTokenProvider jwtTokenProvider;

    public StudioController(StudioService studioService, JwtTokenProvider jwtTokenProvider) {
        this.studioService = studioService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/studio/register")
    public Mono<ApiResponse<String>> register(@RequestBody StudioRegisterParam param) {
        return Mono.fromCallable(() -> {
            studioService.registerStudio(param);
            return ApiResponse.success("工作室注册成功");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/studio/list")
    public Mono<ApiResponse<List<StudioListItemDto>>> listStudios(
            @RequestHeader(name = "Authorization") String token) {
        String username = jwtTokenProvider.getUsernameFromRawToken(token);
        return Mono.fromCallable(() -> {
            if (!studioService.isPlatformAdmin(username)) {
                return ApiResponse.<List<StudioListItemDto>>bussiness_error(null);
            }
            return ApiResponse.success(studioService.listAllStudios());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
