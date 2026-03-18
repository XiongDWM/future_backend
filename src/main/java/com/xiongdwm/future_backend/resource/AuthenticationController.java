package com.xiongdwm.future_backend.resource;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;
import com.xiongdwm.future_backend.utils.security.UserActivityTracker;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class AuthenticationController {
    private final UserService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserActivityTracker activityTracker;

    public AuthenticationController(UserService authenticationService, JwtTokenProvider jwtTokenProvider,
                                    UserActivityTracker activityTracker) {
        this.authenticationService = authenticationService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.activityTracker = activityTracker;
    }

    @PostMapping("/user/login")
    public Mono<ApiResponse<String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username").trim();
        String password = body.get("password");
        return Mono.fromCallable(() -> {
            var user = authenticationService.authenticate(username, password);
            String token = jwtTokenProvider.generateToken(
                    user.getId(), user.getUsername(), user.getRole().name());
            return ApiResponse.success(token);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/user/pal/login")
    public Mono<ApiResponse<String>> palLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username").trim();
        String password = body.get("password");
        String softwareCode = body.get("softwareCode");
        return Mono.fromCallable(() -> {
            var user = authenticationService.authenticate(username, password, softwareCode);
            String token = jwtTokenProvider.generateToken(
                    user.getId(), user.getUsername(), user.getRole().name());
            return ApiResponse.success(token);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/user/logout")
    public Mono<ApiResponse<String>> logout(@RequestHeader(name="Authorization", required=true) String token) {
        var userId = jwtTokenProvider.getUserId(token);
        return Mono.fromCallable(() -> {
            boolean success = authenticationService.logout(Long.parseLong(userId));
            if(!success) return ApiResponse.error("Logout failed");
            activityTracker.remove(Long.parseLong(userId));
            return ApiResponse.success("Logged out successfully");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/user/heartbeat")
    public Mono<ApiResponse<String>> heartbeat() {
        System.out.println("===============heartbeat===============>>");
        return Mono.just(ApiResponse.success("boomboom"));
    }

    @GetMapping("/user/me")
    public Mono<ApiResponse<String>> me(@RequestHeader(name = "Authorization", required = true) String token) {
        var userId = jwtTokenProvider.getUserId(token);
        return Mono.fromCallable(() -> {
            var user = authenticationService.getUserById(Long.parseLong(userId));
            if (user == null) return ApiResponse.<String>error("用户不存在");
            return ApiResponse.success(user.getStatus().name());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
