package com.xiongdwm.future_backend.resource;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.ConversationSummaryDto;
import com.xiongdwm.future_backend.platform.entity.ChatMessage;
import com.xiongdwm.future_backend.service.ChatService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final JwtTokenProvider tokenProvider;

    public ChatController(ChatService chatService, JwtTokenProvider tokenProvider) {
        this.chatService = chatService;
        this.tokenProvider = tokenProvider;
    }

    private Long requireStudioId(String token) {
        Long studioId = tokenProvider.getStudioIdFromRawToken(token);
        if (studioId == null) throw new ServiceException("无法识别工作室，请重新登录");
        return studioId;
    }

    /** 发送消息 */
    @PostMapping("/send")
    public Mono<ApiResponse<ChatMessage>> send(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        String username = tokenProvider.getUsernameFromRawToken(token);
        Long applicationId = ((Number) body.get("applicationId")).longValue();
        String content = (String) body.get("content");
        return Mono.fromCallable(() -> {
            var msg = chatService.send(applicationId, studioId, username, content);
            return ApiResponse.success(msg);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 获取某对话的全部消息（同时标记已读） */
    @PostMapping("/messages")
    public Mono<ApiResponse<List<ChatMessage>>> getMessages(
            @RequestBody Map<String, Long> body,
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        Long applicationId = body.get("applicationId");
        return Mono.fromCallable(() -> {
            var messages = chatService.getMessages(applicationId, studioId);
            return ApiResponse.success(messages);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 获取我的所有对话的未读数 */
    @PostMapping("/unread")
    public Mono<ApiResponse<Map<Long, Integer>>> getUnread(
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        return Mono.fromCallable(() -> {
            var counts = chatService.getUnreadCounts(studioId);
            return ApiResponse.success(counts);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 获取我参与的所有对话摘要 */
    @PostMapping("/conversations")
    public Mono<ApiResponse<List<ConversationSummaryDto>>> getConversations(
            @RequestHeader("Authorization") String token) {
        Long studioId = requireStudioId(token);
        return Mono.fromCallable(() -> {
            var convs = chatService.getMyConversations(studioId);
            return ApiResponse.success(convs);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
