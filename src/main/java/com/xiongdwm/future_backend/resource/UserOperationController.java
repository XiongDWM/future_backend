package com.xiongdwm.future_backend.resource;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.bo.RegisterRequest;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.service.StudioService;
import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class UserOperationController {

    private final UserService userService;
    private final StudioService studioService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserOperationController(UserService userService, StudioService studioService,
                                   JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.studioService = studioService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 分页查询用户列表，支持按名称模糊搜索 + 按状态筛选 */
    @PostMapping("/user/list")
    public Mono<ApiResponse<Page<User>>> listUsers(@RequestBody PageableParam param) {
        return Mono.fromCallable(() -> {
            String username = param.getFilters() != null ? param.getFilters().get("username") : null;
            User.Status status = null;
            if (param.getFilters() != null && param.getFilters().containsKey("status")) {
                String statusStr = param.getFilters().get("status");
                if (statusStr != null && !statusStr.isBlank()) {
                    status = User.Status.valueOf(statusStr);
                }
            }
            var page = userService.listUsers(param.getPageNumber() + 1, param.getPageSize(), username, status);
            return ApiResponse.success(page);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询当前在线打手（role=PALWORLD，status 非 OFFLINE/INACTIVE） */
    @GetMapping("/user/online-palworld")
    public Mono<ApiResponse<List<User>>> onlinePalworld() {
        return Mono.fromCallable(() -> {
            var list = userService.listOnlinePalworld();
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询所有打手（用于下拉选择） */
    @GetMapping("/user/all-palworld")
    public Mono<ApiResponse<List<User>>> allPalworld() {
        return Mono.fromCallable(() -> {
            var list = userService.listAllPalworld();
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/user/register")
    public Mono<ApiResponse<String>> register(@RequestBody RegisterRequest user,
                                              @RequestHeader(name = "Authorization") String token) {
        var userEntity =new User();
        userEntity.setUsername(user.getUsername());
        userEntity.setPassword(user.getPassword());
        userEntity.setRole(user.getRole());
        userEntity.setIdentity(user.getIdentity());
        userEntity.setRealName(user.getRealName());
        Long studioId = jwtTokenProvider.getStudioIdFromRawToken(token);
        return Mono.fromCallable(() -> {
            if (studioService.isUsernameTaken(userEntity.getUsername())) {
                return ApiResponse.<String>error("用户名已被占用");
            }
            var success = userService.regist(userEntity);
            if (success && studioId != null) {
                studioService.addUserToStudio(userEntity.getUsername(), studioId);
            }
            return success ? ApiResponse.success("注册成功") : ApiResponse.<String>error("注册失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }


    @PostMapping("/user/status")
    public Mono<ApiResponse<String>> changeStatus(@RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            Long userId = Long.valueOf(body.get("userId"));
            String statusStr = body.get("status");
            var user = userService.getUserById(userId);
            if (user == null) return ApiResponse.<String>error("用户不存在");
            var status = User.Status.valueOf(statusStr);
            if (status == User.Status.OFFLINE) {
                user.setLastLogout(new java.util.Date());
            }
            user.setStatus(status);
            var success = userService.updateUser(user);
            return success ? ApiResponse.success("状态已更新") : ApiResponse.<String>error("更新失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // 结算收入
    @PostMapping("/user/income-settlement")
    public Mono<ApiResponse<String>> settleIncome(@RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            Long userId = Long.valueOf(body.get("userId"));
            var success = userService.settleIncome(userId);
            return success ? ApiResponse.success("收入已结算") : ApiResponse.<String>error("结算失败");
        }).subscribeOn(Schedulers.boundedElastic());

    }

    @PostMapping("/user/delete")
    public Mono<ApiResponse<String>> deleteUser(@RequestBody Map<String, String> body,
                                                @RequestHeader(name = "Authorization") String token) {
        return Mono.fromCallable(() -> {
            var operator =jwtTokenProvider.getUserIdFromRawToken(token);
            var operatorUser = userService.getUserById(Long.valueOf(operator));
            if(operatorUser.getRole() != User.Role.ADMIN) return ApiResponse.<String>error("无权限操作");
            Long userId = Long.valueOf(body.get("userId"));
            var success = userService.deleteUser(userId);
            return success ? ApiResponse.success("用户已删除") : ApiResponse.<String>error("删除失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

}
