package com.xiongdwm.future_backend.resource;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.BookOrderParam;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.entity.BookOrder;
import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.service.BookOrderService;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class BookOrderController {

    private final BookOrderService bookOrderService;
    private final JwtTokenProvider tokenProvider;

    public BookOrderController(BookOrderService bookOrderService, JwtTokenProvider tokenProvider) {
        this.bookOrderService = bookOrderService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 
     * @param booking 填写的存单信息
     * @param token 用户token
     * @return 是否成功
     */
    @PostMapping("/bookOrder/create")
    public Mono<ApiResponse<String>> createBookOrder(@RequestBody BookOrderParam booking,@RequestHeader("Authorization") String token) {
        var user=tokenProvider.getUserFromRawToken(token);
        return Mono.fromCallable(() -> {
            boolean success = bookOrderService.createBookOrder(booking, user);
            return success ? ApiResponse.success("存单成功") : ApiResponse.<String>error("存单失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 分页查询存单列表
     * 打手角色只能查看自己的存单，其他角色可按条件自由查询
     * filters支持: customer(模糊), customerId(精确), pid(打手id)
     */
    @PostMapping("/bookOrder/list")
    public Mono<ApiResponse<Page<BookOrder>>> listBookOrders(@RequestBody PageableParam param, @RequestHeader(name = "Authorization", required = false) String token) {
        return Mono.fromCallable(() -> {
            var filters = param.getFilters();
            var customer = filters != null ? filters.get("customer") : null;
            var customerId = filters != null ? filters.get("customerId") : null;
            Long pid = filters != null && filters.containsKey("pid") && !filters.get("pid").isBlank()
                    ? Long.valueOf(filters.get("pid"))
                    : null;
            var user = tokenProvider.getUserFromRawToken(token);
            if (user != null && user.getRole() == User.Role.PALWORLD) {
                pid = user.getId();
            }

            var page = bookOrderService.listBookOrders(param.getPageNumber() + 1, param.getPageSize(), customer, customerId, pid);
            return ApiResponse.success(page);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从存单创建order
     */
    @PostMapping("/bookOrder/starting")
    public Mono<ApiResponse<String>> startBookOrder(@RequestBody Map<String,Long> body) {
        return Mono.fromCallable(() -> {
            var orderId = body.get("orderId");
            bookOrderService.startBookOrder(orderId);
            return ApiResponse.success("接单成功");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/bookOrder/audit")
    public Mono<ApiResponse<String>> auditBookOrder(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Long id = Long.valueOf(body.get("id").toString());
            Boolean confirm = (Boolean) body.get("confirm");
            String rejectReason = body.containsKey("rejectReason") ? (String) body.get("rejectReason") : "";
            boolean success = bookOrderService.auditBookOrder(id, confirm, rejectReason);
            return success ? ApiResponse.success("操作成功") : ApiResponse.<String>error("操作失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/bookOrder/recharge")
    public Mono<ApiResponse<String>> rechargeBookOrder(@RequestBody Map<String, Object> body) {
         Long orderId = Long.valueOf(body.get("orderId").toString());
         Integer amount = Integer.valueOf(body.get("amount").toString());
         Double price = Double.valueOf(body.get("price").toString());
        return Mono.fromCallable(() -> {
            boolean success = bookOrderService.rechargeBookOrder(orderId, amount, price);
            return success ? ApiResponse.success("充值成功") : ApiResponse.<String>error("充值失败");
        }).subscribeOn(Schedulers.boundedElastic());
    }

}
