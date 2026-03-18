package com.xiongdwm.future_backend.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.FindingRequestFillDto;
import com.xiongdwm.future_backend.bo.FindingRequestParam;
import com.xiongdwm.future_backend.bo.OrderCloseDto;
import com.xiongdwm.future_backend.bo.OrderDetailDto;
import com.xiongdwm.future_backend.bo.OrderListItemDto;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.bo.WorkWorkParam;
import com.xiongdwm.future_backend.entity.FindingRequest;
import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.service.FindingRequestService;
import com.xiongdwm.future_backend.service.OrderSectionService;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.utils.JacksonUtil;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final FindingRequestService findingRequestService;
    private final OrderSectionService orderSectionService;
    private final JwtTokenProvider tokenProvider;

    public OrderController(OrderService orderService, FindingRequestService findingRequestService,
                           OrderSectionService orderSectionService, JwtTokenProvider tokenProvider) {
        this.orderService = orderService;
        this.findingRequestService = findingRequestService;
        this.orderSectionService = orderSectionService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/order/list")
    public Mono<ApiResponse<Page<OrderListItemDto>>> listOrders(@RequestBody PageableParam param,@RequestHeader(name="Authorization", required=false) String token) {
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
            return ApiResponse.success(page.map(OrderListItemDto::fromEntity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/order/cancel")
    public Mono<ApiResponse<String>> cancelOrder(@RequestBody Map<String, String> orderId) {
        return Mono.fromCallable(() -> {
            var id = orderId.get("orderId");
            System.out.println("取消订单: " + id);
            var success = orderService.cancelOrder(id);
            return success ? ApiResponse.success("订单已取消") : ApiResponse.<String>error("订单取消失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }
    @PostMapping("/order/settlement")
    public Mono<ApiResponse<String>> settleOrder(@RequestBody Map<String, String> orderId) {
        return Mono.fromCallable(() -> {
            var id = orderId.get("orderId");
            var success = orderService.orderSettle(id);
            return success ? ApiResponse.success("订单已结算") : ApiResponse.<String>error("订单结算失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }
    @PostMapping("/section/audit")
    public Mono<ApiResponse<String>> auditSection(@RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            String subId = body.get("subId");
            boolean confirm = Boolean.parseBoolean(body.get("confirm"));
            String rejectReason = body.get("rejectReason");
            var success = orderSectionService.audit(subId, confirm, rejectReason);
            return success ? ApiResponse.success("审核成功") : ApiResponse.<String>error("审核失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/order/upload-settlement")
    public Mono<ApiResponse<String>> uploadSettlementPic(@RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            String orderId = body.get("orderId");
            String picString = body.get("picString");
            var success = orderService.uploadSettlementPic(orderId, picString);
            return success ? ApiResponse.success("上传成功") : ApiResponse.<String>error("上传失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/order/detail")
    public Mono<ApiResponse<OrderDetailDto>> orderDetail(@RequestParam("orderId") String orderId) {
        return Mono.fromCallable(() -> {
            var dto = orderService.getOrderDetailDto(orderId);
            if (dto == null) return ApiResponse.<OrderDetailDto>error(null);
            return ApiResponse.success(dto);
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
            String continuePic = body.get("continuePic");
            var success = orderService.continueOrder(orderId, price, amount, unitType, additionalPic, continuePic);
            return success ? ApiResponse.success("续单成功") : ApiResponse.<String>error("续单失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 为工单添加协作打手（创建子单） */
    @PostMapping("/order/collaborator")
    public Mono<ApiResponse<String>> addCollaborator(@RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            String orderId = body.get("orderId");
            Long palId = Long.valueOf(body.get("palId"));
            var subOrder = orderService.addCollaborator(orderId, palId);
            return subOrder != null ? ApiResponse.success(subOrder.getOrderId()) : ApiResponse.<String>error("添加协作失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
