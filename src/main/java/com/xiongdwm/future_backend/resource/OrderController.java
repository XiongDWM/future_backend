package com.xiongdwm.future_backend.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.bo.FindingRequestParam;
import com.xiongdwm.future_backend.bo.OrderCloseDto;
import com.xiongdwm.future_backend.bo.OrderDetailDto;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.bo.WorkWorkParam;
import com.xiongdwm.future_backend.entity.FindingRequest;
import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.service.FindingRequestService;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.utils.JacksonUtil;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class OrderController {

    @Resource
    private OrderService orderService;
    @Resource
    private FindingRequestService findingRequestService;
    @Resource
    private JwtTokenProvider tokenProvider;

    @PostMapping("/order/list")
    public Mono<ApiResponse<Page<Order>>> listOrders(@RequestBody PageableParam param,@RequestHeader(name="Authorization", required=false) String token) {
        return Mono.fromCallable(() -> {
            var filters = param.getFilters();
            var type = filters != null && filters.containsKey("type") && !filters.get("type").isBlank()
                    ? Order.Type.valueOf(filters.get("type"))
                    : null;
            var userId = filters != null && filters.containsKey("userId") && !filters.get("userId").isBlank()
                    ? Long.valueOf(filters.get("userId"))
                    : null;
            var todayOnly = filters != null && "true".equals(filters.get("todayOnly"));
            var orderId = filters != null && filters.containsKey("orderId") && !filters.get("orderId").isBlank()
                    ? filters.get("orderId")
                    : null;
            var page = orderService.listOrders(param.getPageNumber() + 1, param.getPageSize(), type, userId, todayOnly, orderId);
            return ApiResponse.success(page);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 订单详情（含 sections） */
    @GetMapping("/order/detail")
    public Mono<ApiResponse<OrderDetailDto>> orderDetail(@org.springframework.web.bind.annotation.RequestParam("orderId") String orderId) {
        return Mono.fromCallable(() -> {
            var order = orderService.getOrderDetail(orderId);
            if (order == null) return ApiResponse.<OrderDetailDto>error(null);
            return ApiResponse.success(OrderDetailDto.fromEntity(order));
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
    public Mono<ApiResponse<List<FindingRequest>>> listFindingRequests(@RequestHeader(name = "Authorization", required = false) String token) {
        var user = token != null ? tokenProvider.getUserFromRawToken(token) : null;
        return Mono.fromCallable(() -> {
            var list = findingRequestService.getRequests(user);
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/web/finding/list")
    public Mono<ApiResponse<List<FindingRequest>>> listFindingRequestsWeb(@RequestHeader(name = "Authorization", required = false) String token) {
        var user = token != null ? tokenProvider.getUserFromRawToken(token) : null;
        return Mono.fromCallable(() -> {
            var list = findingRequestService.getRequests();
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/finding/submit")
    public Mono<ApiResponse<String>> submitFindingRequest(@RequestBody FindingRequestParam param,@RequestHeader("Authorization") String token) {
        return Mono.fromCallable(() -> {
            var findingRequest = new FindingRequest();
            // 从token获取用户信息，关联打手
            var user = tokenProvider.getUserFromRawToken(token);
            findingRequest.setPalworld(user); // 后端关联打手
            findingRequest.setDescription(param.getGameType()+"|"+param.getRank()); // 描述里存游戏类型和段位，方便查询
            findingRequest.setMan(param.isMan());
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

    @PostMapping("/finding/cancel")
    public Mono<ApiResponse<String>> cancelFindingRequest(@RequestBody Long requestId, @RequestHeader(name = "Authorization", required = false) String token) {
        return Mono.fromCallable(() -> {
            var success = findingRequestService.cancel(requestId);
            return success ? ApiResponse.success("已取消") : ApiResponse.<String>error("取消失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/order/close")
    public Mono<ApiResponse<String>> closeOrder(@RequestBody OrderCloseDto dto) {
        return Mono.fromCallable(() -> {
            var success = orderService.closeOrder(dto);
            return success ? ApiResponse.success("订单已完成") : ApiResponse.<String>error("订单关闭失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 打手接单开工 */
    @PostMapping("/order/work")
    public Mono<ApiResponse<String>> workOrder(@RequestBody WorkWorkParam body) {
        return Mono.fromCallable(() -> {
            long palId = body.getPalId();
            String orderId = body.getOrderId();
            String picStart = body.getPicStart();
            var success = orderService.workWork(palId, orderId, picStart);
            return success ? ApiResponse.success("接单成功") : ApiResponse.<String>error("接单失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 续单 */
    @PostMapping("/order/continue")
    public Mono<ApiResponse<String>> continueOrder(@RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            String orderId = body.get("orderId");
            double price = Double.parseDouble(body.getOrDefault("price", "0"));
            double amount = Double.parseDouble(body.getOrDefault("amount", "0"));
            var unitType = Order.UnitType.valueOf(
                    body.getOrDefault("unitType", "HOUR"));
            String additionalPic = body.get("additionalPic");
            var success = orderService.continueOrder(orderId, price, amount, unitType, additionalPic);
            return success ? ApiResponse.success("续单成功") : ApiResponse.<String>error("续单失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
