package com.xiongdwm.future_backend.resource;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private UserService authenticationService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserActivityTracker activityTracker;

    @PostMapping("/user/login")
    public Mono<ApiResponse<String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username").trim();
        String password = body.get("password");
        System.out.println("========login api called=========");
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
        boolean s=authenticationService.logout(Long.parseLong(userId));
        activityTracker.remove(Long.parseLong(userId));
        return Mono.just(ApiResponse.success("Logged out successfully"));
    }
}
