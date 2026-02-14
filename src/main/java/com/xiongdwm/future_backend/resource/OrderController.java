package com.xiongdwm.future_backend.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.bo.OrderCloseDto;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.entity.FindingRequest;
import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.service.FindingRequestService;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.utils.JacksonUtil;

import jakarta.annotation.Resource;

import java.util.List;

import org.springframework.data.domain.Page;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class OrderController {

    @Resource
    private OrderService orderService;
    @Resource
    private FindingRequestService findingRequestService;

    @PostMapping("/order/list")
    public Mono<ApiResponse<Page<Order>>> listOrders(@RequestBody PageableParam param) {
        return Mono.fromCallable(() -> {
            var filters = param.getFilters();
            var type = filters != null && filters.containsKey("type") && !filters.get("type").isBlank()
                    ? Order.Type.valueOf(filters.get("type"))
                    : null;
            var userId = filters != null && filters.containsKey("userId") && !filters.get("userId").isBlank()
                    ? Long.valueOf(filters.get("userId"))
                    : null;
            var todayOnly = filters != null && "true".equals(filters.get("todayOnly"));
            var page = orderService.listOrders(param.getPageNumber() + 1, param.getPageSize(), type, userId, todayOnly);
            return ApiResponse.success(page);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/order/create")
    public Mono<ApiResponse<String>> createOrder(@RequestBody Order order) {
        return Mono.fromCallable(() -> {
            var success = orderService.createOrder(order);
            return success ? ApiResponse.success("创建成功") : ApiResponse.<String>error("创建失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/finding/list")
    public Mono<ApiResponse<List<FindingRequest>>> listFindingRequests() {
        return Mono.fromCallable(() -> {
            var list = findingRequestService.getRequests();
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/finding/submit")
    public Mono<ApiResponse<String>> submitFindingRequest(@RequestBody FindingRequest findingRequest) {
        return Mono.fromCallable(() -> {
            var success = findingRequestService.submit(findingRequest);
            return success ? ApiResponse.success("提交成功") : ApiResponse.<String>error("提交失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping(value = "/finding/confirm")
    public Mono<ApiResponse<String>> confirmFindingRequest(@RequestBody String stringfyDto) {
        return Mono.fromCallable(() -> {
            var dto = JacksonUtil.fromJsonString(stringfyDto, FindingRequestFillDto.class).orElse(null);
            if (null == dto) return ApiResponse.<String>error("反序列化失败");
            var success = findingRequestService.confirm(dto);
            return success ? ApiResponse.success("找单完成") : ApiResponse.<String>error("找单失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/order/close")
    public Mono<ApiResponse<String>> closeOrder(@RequestBody OrderCloseDto dto) {
        return Mono.fromCallable(() -> {
            var success = orderService.closeOrder(dto);
            return success ? ApiResponse.success("订单已完成") : ApiResponse.<String>error("订单关闭失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
}
