package com.xiongdwm.future_backend.resource;

import java.util.Date;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.StudioManageDto;
import com.xiongdwm.future_backend.platform.entity.PlatformAuditLog;
import com.xiongdwm.future_backend.platform.entity.PlatformUser;
import com.xiongdwm.future_backend.service.PlatformService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/platform")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @PostMapping("/login")
    public Mono<ApiResponse<String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        System.out.println("platform login attempt: " + username);
        return Mono.fromCallable(() -> {
            String token = platformService.login(username.trim(), password);
            return ApiResponse.success(token);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/studios")
    public Mono<ApiResponse<List<StudioManageDto>>> getStudios() {
        return Mono.fromCallable(() -> ApiResponse.success(platformService.getStudios()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/studio/update")
    public Mono<ApiResponse<String>> updateStudio(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Long id = ((Number) body.get("id")).longValue();
            platformService.updateStudio(id, body);
            return ApiResponse.success();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/admins")
    public Mono<ApiResponse<List<PlatformUser>>> getAdmins() {
        return Mono.fromCallable(() -> ApiResponse.success(platformService.getAdmins()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/admin/create")
    public Mono<ApiResponse<String>> createAdmin(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            PlatformUser.Role role = PlatformUser.Role.valueOf((String) body.get("role"));
            Long phone = body.get("phone") != null ? ((Number) body.get("phone")).longValue() : null;
            platformService.createAdmin(username, password, role, phone);
            return ApiResponse.success();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/admin/update")
    public Mono<ApiResponse<String>> updateAdmin(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Long id = ((Number) body.get("id")).longValue();
            platformService.updateAdmin(id, body);
            return ApiResponse.success();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/admin/delete")
    public Mono<ApiResponse<String>> deleteAdmin(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Long id = ((Number) body.get("id")).longValue();
            platformService.deleteAdmin(id);
            return ApiResponse.success();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/audit-logs")
    public Mono<ApiResponse<List<PlatformAuditLog>>> getAuditLogs(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            long fromTs = ((Number) body.get("from")).longValue();
            long toTs = ((Number) body.get("to")).longValue();
            String action = (String) body.get("action");
            return ApiResponse.success(platformService.getAuditLogs(new Date(fromTs), new Date(toTs), action));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
