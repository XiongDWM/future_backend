package com.xiongdwm.future_backend.resource;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.platform.entity.Studio;
import com.xiongdwm.future_backend.platform.repository.StudioRepository;
import com.xiongdwm.future_backend.platform.repository.UserStudioMappingRepository;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.cache.CacheHandler;
import com.xiongdwm.future_backend.utils.ecc.CryptoSessionConfig;
import com.xiongdwm.future_backend.utils.exception.AuthenticationFailException;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;
import com.xiongdwm.future_backend.utils.security.UserActivityTracker;
import com.xiongdwm.future_backend.utils.tenant.TenantContext;
import com.xiongdwm.future_backend.utils.tenant.TenantRoutingDataSource;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class AuthenticationController {
    private final UserService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserActivityTracker activityTracker;
    private final UserStudioMappingRepository mappingRepository;
    private final TenantRoutingDataSource tenantRoutingDataSource;
    private final StudioRepository studioRepository;
    private final CacheHandler cacheHandler;
    // private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationController.class);

    public AuthenticationController(UserService authenticationService, JwtTokenProvider jwtTokenProvider,
                                    UserActivityTracker activityTracker,
                                    UserStudioMappingRepository mappingRepository,
                                    TenantRoutingDataSource tenantRoutingDataSource,
                                    StudioRepository studioRepository,
                                    CacheHandler cacheHandler) {
        this.authenticationService = authenticationService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.activityTracker = activityTracker;
        this.mappingRepository = mappingRepository;
        this.tenantRoutingDataSource = tenantRoutingDataSource;
        this.studioRepository = studioRepository;
        this.cacheHandler = cacheHandler;
    }

    @PostMapping("/user/login")
    public Mono<ApiResponse<Map<String, String>>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username").trim();
        String password = body.get("password");
        return Mono.fromCallable(() -> {
            // 1. 通过平台库查找用户所属工作室
            var mapping = mappingRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationFailException("用户名或密码错误"));
            Long studioId = mapping.getStudioId();
            String dbName = tenantRoutingDataSource.getDbName(studioId);
            if (dbName == null) throw new AuthenticationFailException("工作室不可用");

            // 检查工作室是否到期
            Studio studio = studioRepository.findById(studioId).orElse(null);
            if (studio != null && studio.getWillChargeAt() != null && studio.getWillChargeAt().before(new java.util.Date())) {
                throw new AuthenticationFailException("到期需续费");
            }

            // 2. 切换到租户数据库进行认证
            TenantContext.setCurrentTenant(dbName);
            TenantContext.setCurrentStudioId(studioId);
            try {
                var user = authenticationService.authenticate(username, password);
                String token = jwtTokenProvider.generateToken(
                        user.getId(), user.getUsername(), user.getRole().name(), studioId);
                String studioName = studio != null ? studio.getName() : "";
                return ApiResponse.success(Map.of(
                        "token", token,
                        "studioName", studioName,
                        "studioId", String.valueOf(studioId)
                ));
            } finally {
                TenantContext.clear();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/user/pal/login")
    public Mono<ApiResponse<String>> palLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username").trim();
        String password = body.get("password");
        String softwareCode = body.get("softwareCode");
        return Mono.fromCallable(() -> {
            var mapping = mappingRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationFailException("用户名或密码错误"));
            Long studioId = mapping.getStudioId();
            String dbName = tenantRoutingDataSource.getDbName(studioId);
            if (dbName == null) throw new AuthenticationFailException("工作室不可用");

            // 检查工作室是否到期
            Studio studio = studioRepository.findById(studioId).orElse(null);
            if (studio != null && studio.getWillChargeAt() != null && studio.getWillChargeAt().before(new java.util.Date())) {
                throw new AuthenticationFailException("到期需续费");
            }

            TenantContext.setCurrentTenant(dbName);
            TenantContext.setCurrentStudioId(studioId);
            try {
                var user = authenticationService.authenticate(username, password, softwareCode);
                String token = jwtTokenProvider.generateToken(
                        user.getId(), user.getUsername(), user.getRole().name(), studioId);
                return ApiResponse.success(token);
            } finally {
                TenantContext.clear();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/user/logout")
    public Mono<ApiResponse<String>> logout(
            @RequestHeader(name="Authorization", required=true) String token,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionId) {
        var userId = jwtTokenProvider.getUserId(token);
        return Mono.fromCallable(() -> {
            boolean success = authenticationService.logout(Long.parseLong(userId));
            if(!success) return ApiResponse.error("Logout failed");
            activityTracker.remove(Long.parseLong(userId));

            // 登出时主动失效当前加密会话，防止旧 session 被继续用于签名请求
            if (sessionId != null && !sessionId.isBlank()) {
                var km = cacheHandler.<String, javax.crypto.SecretKey>getCache(CryptoSessionConfig.CACHE_KM);
                km.remove(sessionId);
            }
            return ApiResponse.success("Logged out successfully");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/user/heartbeat")
    public Mono<ApiResponse<String>> heartbeat() {
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
