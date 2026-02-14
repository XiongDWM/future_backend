package com.xiongdwm.future_backend.resource;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.service.UserService;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class UserOperationController {

    @Resource
    private UserService userService;

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

    /** 注册新用户 */
    @PostMapping("/user/register")
    public Mono<ApiResponse<String>> register(@RequestBody User user) {
        return Mono.fromCallable(() -> {
            var success = userService.regist(user);
            return success ? ApiResponse.success("注册成功") : ApiResponse.<String>error("注册失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 打手状态变更（ACTIVE / HANGING / OFFLINE） */
    @PostMapping("/user/status")
    public Mono<ApiResponse<String>> changeStatus(@RequestBody java.util.Map<String, String> body) {
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
}
