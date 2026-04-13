package com.xiongdwm.future_backend.resource;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.bo.ThirdPartyApplicationDto;
import com.xiongdwm.future_backend.bo.ThirdPartyListingDto;
import com.xiongdwm.future_backend.bo.ThirdPartyListingParam;
import com.xiongdwm.future_backend.platform.entity.ThirdPartyListing;
import com.xiongdwm.future_backend.platform.repository.StudioRepository;
import com.xiongdwm.future_backend.service.ThirdPartyListingService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/thirdParty")
public class ThirdPartyListingController {

    private final ThirdPartyListingService listingService;
    private final JwtTokenProvider tokenProvider;
    private final StudioRepository studioRepository;

    public ThirdPartyListingController(ThirdPartyListingService listingService,
                                       JwtTokenProvider tokenProvider,
                                       StudioRepository studioRepository) {
        this.listingService = listingService;
        this.tokenProvider = tokenProvider;
        this.studioRepository = studioRepository;
    }

    /** 根据 studioId 解析工作室名称 */
    private String resolveStudioName(Long studioId) {
        if (studioId == null) return "未知工作室";
        return studioRepository.findById(studioId).map(s -> s.getName()).orElse("未知工作室");
    }

    /** studioId 不可为 null，否则抛业务异常（GlobalExceptionHandler 统一处理） */
    private Long requireStudioId(String token) {
        Long studioId = tokenProvider.getStudioIdFromRawToken(token);
        if (studioId == null) throw new ServiceException("无法识别工作室，请重新登录");
        return studioId;
    }

    /** 发布甩单 */
    @PostMapping("/listing/post")
    public Mono<ApiResponse<ThirdPartyListing>> post(
            @RequestBody ThirdPartyListingParam param,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        return Mono.fromCallable(() -> {
            String studioName = resolveStudioName(studioId);
            var listing = listingService.post(param, studioId, studioName);
            return ApiResponse.success(listing);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 浏览大厅（分页，可按游戏类型过滤） */
    @PostMapping("/listing/list")
    public Mono<ApiResponse<Page<ThirdPartyListingDto>>> list(
            @RequestBody PageableParam param,
            @RequestHeader(name = "Authorization", required = false) String token) {
        Long studioId = tokenProvider.getStudioIdFromRawToken(token);
        return Mono.fromCallable(() -> {
            var filters = param.getFilters();
            String gameType = filters != null ? filters.get("gameType") : null;
            var page = listingService.listOpen(param.getPageNumber() + 1, param.getPageSize(), gameType, studioId);
            return ApiResponse.success(page);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 撤回自己的挂单 */
    @PostMapping("/listing/cancel")
    public Mono<ApiResponse<String>> cancel(
            @RequestBody Map<String, Long> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long listingId = body.get("listingId");
        return Mono.fromCallable(() -> {
            listingService.cancel(listingId, studioId);
            return ApiResponse.success("已撤回");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 挂单方审核（完成/炸单） */
    @PostMapping("/listing/done")
    public Mono<ApiResponse<String>> done(
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long listingId = Long.valueOf(body.get("listingId"));
        boolean passed = !"FAILURE".equals(body.get("result"));
        String failureReason = body.get("failureReason");
        return Mono.fromCallable(() -> {
            listingService.done(listingId, studioId, passed, failureReason);
            return ApiResponse.success(passed ? "已标记完成" : "已标记炸单");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 申请接单 */
    @PostMapping("/apply/submit")
    public Mono<ApiResponse<String>> applyListing(
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long listingId = Long.valueOf(body.get("listingId"));
        String note = body.get("note");
        return Mono.fromCallable(() -> {
            String studioName = resolveStudioName(studioId);
            listingService.apply(listingId, studioId, studioName, note);
            return ApiResponse.success("申请已提交");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 挂单方接受申请 */
    @PostMapping("/apply/confirm")
    public Mono<ApiResponse<String>> confirmApplicant(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long applicationId = Long.valueOf(body.get("applicationId").toString());
        String customerId = body.get("customerId").toString();
        return Mono.fromCallable(() -> {
            listingService.confirmApplicant(applicationId, studioId, customerId);
            return ApiResponse.success("已接受申请");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 挂单方拒绝申请 */
    @PostMapping("/apply/reject")
    public Mono<ApiResponse<String>> rejectApplicant(
            @RequestBody Map<String, Long> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long applicationId = body.get("applicationId");
        return Mono.fromCallable(() -> {
            listingService.rejectApplicant(applicationId, studioId);
            return ApiResponse.success("已拒绝申请");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查看某条挂单的所有申请（仅挂单方） */
    @PostMapping("/apply/byListing")
    public Mono<ApiResponse<List<ThirdPartyApplicationDto>>> applicationsByListing(
            @RequestBody Map<String, Long> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long listingId = body.get("listingId");
        return Mono.fromCallable(() -> {
            var list = listingService.getApplicationsByListing(listingId, studioId);
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /** 查看自己提交的所有申请 */
    @PostMapping("/apply/mine")
    public Mono<ApiResponse<List<ThirdPartyApplicationDto>>> myApplications(
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        return Mono.fromCallable(() -> {
            var list = listingService.getMyApplications(studioId);
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查看自己发布的所有挂单 */
    @PostMapping("/listing/mine")
    public Mono<ApiResponse<List<ThirdPartyListingDto>>> myListings(
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        return Mono.fromCallable(() -> {
            var list = listingService.getMyListings(studioId);
            return ApiResponse.success(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 申请方派单：根据已接受的申请创建二手工单 */
    @PostMapping("/apply/dispatch")
    public Mono<ApiResponse<String>> dispatch(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long applicationId = ((Number) body.get("applicationId")).longValue();
        Long palId = ((Number) body.get("palId")).longValue();
        @SuppressWarnings("unchecked")
        List<Long> collaboratorPalIds = body.get("collaboratorPalIds") != null
                ? ((List<Number>) body.get("collaboratorPalIds")).stream().map(Number::longValue).toList()
                : null;
        return Mono.fromCallable(() -> {
            var orderId = listingService.dispatch(applicationId, studioId, palId, collaboratorPalIds);
            return ApiResponse.success(orderId);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
