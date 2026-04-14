package com.xiongdwm.future_backend.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiongdwm.future_backend.bo.ConversationSummaryDto;
import com.xiongdwm.future_backend.platform.entity.ChatMessage;
import com.xiongdwm.future_backend.platform.entity.ThirdPartyApplication;
import com.xiongdwm.future_backend.platform.repository.ChatMessageRepository;
import com.xiongdwm.future_backend.platform.repository.ThirdPartyApplicationRepository;
import com.xiongdwm.future_backend.platform.repository.ThirdPartyListingRepository;
import com.xiongdwm.future_backend.service.ChatService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatRepo;
    private final ThirdPartyApplicationRepository appRepo;
    private final ThirdPartyListingRepository listingRepo;
    private final GlobalEventBus eventBus;

    private static final GlobalEventSpec.Domain DOMAIN = GlobalEventSpec.Domain.CHAT;

    public ChatServiceImpl(ChatMessageRepository chatRepo,
                           ThirdPartyApplicationRepository appRepo,
                           ThirdPartyListingRepository listingRepo,
                           GlobalEventBus eventBus) {
        this.chatRepo = chatRepo;
        this.appRepo = appRepo;
        this.listingRepo = listingRepo;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional("platformTransactionManager")
    public ChatMessage send(Long applicationId, Long senderStudioId, String senderName, String content, String messageType) {
        var app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ServiceException("申请记录不存在"));

        // 确认发送方是对话的参与者（挂单方或申请方）
        Long listingStudioId = listingRepo.findById(app.getListingId())
                .orElseThrow(() -> new ServiceException("挂单不存在"))
                .getStudioId();
        Long appStudioId = app.getStudioId();

        if (!senderStudioId.equals(listingStudioId) && !senderStudioId.equals(appStudioId)) {
            throw new ServiceException("无权发送消息");
        }

        // 确定接收方
        Long receiverStudioId = senderStudioId.equals(listingStudioId) ? appStudioId : listingStudioId;

        var msg = new ChatMessage();
        msg.setApplicationId(applicationId);
        msg.setSenderStudioId(senderStudioId);
        msg.setSenderName(senderName);
        msg.setContent(content);
        msg.setMessageType(messageType != null ? messageType : "TEXT");
        msg.setSentAt(new Date());
        msg.setReadByReceiver(false);
        chatRepo.saveAndFlush(msg);

        // SSE 推送给对方工作室
        eventBus.emitAfterCommitTo(DOMAIN, GlobalEventSpec.Action.CREATE, true, applicationId, receiverStudioId);

        return msg;
    }

    @Override
    @Transactional("platformTransactionManager")
    public List<ChatMessage> getMessages(Long applicationId, Long myStudioId) {
        // 标记对方发来的消息为已读
        chatRepo.markReadByReceiver(applicationId, myStudioId);
        return chatRepo.findByApplicationIdOrderBySentAtAsc(applicationId);
    }

    @Override
    @Transactional(value = "platformTransactionManager", readOnly = true)
    public Map<Long, Integer> getUnreadCounts(Long myStudioId) {
        var rows = chatRepo.countAllUnread(myStudioId);
        var result = new HashMap<Long, Integer>();
        for (Object[] row : rows) {
            Long appId = (Long) row[0];
            int count = ((Number) row[1]).intValue();
            result.put(appId, count);
        }
        return result;
    }

    @Override
    @Transactional(value = "platformTransactionManager", readOnly = true)
    public List<ConversationSummaryDto> getMyConversations(Long myStudioId) {
        // 收集我参与的所有 applicationId（作为申请方 + 作为挂单方）
        var myApps = new ArrayList<>(appRepo.findByStudioIdOrderByAppliedAtDesc(myStudioId));
        var myListings = listingRepo.findByStudioIdOrderByPostedAtDesc(myStudioId);
        for (var listing : myListings) {
            myApps.addAll(appRepo.findByListingIdOrderByAppliedAtDesc(listing.getId()));
        }
        // 去重并过滤出有消息记录的，组装摘要
        var seen = new java.util.HashSet<Long>();
        var result = new ArrayList<ConversationSummaryDto>();
        for (var app : myApps) {
            if (!seen.add(app.getId())) continue;
            if (chatRepo.findByApplicationIdOrderBySentAtAsc(app.getId()).isEmpty()) continue;
            var listing = listingRepo.findById(app.getListingId()).orElse(null);
            result.add(new ConversationSummaryDto(
                app.getId(),
                listing != null ? listing.getGameType() : null,
                listing != null && listing.getOrderType() != null ? listing.getOrderType().name() : null,
                listing != null ? listing.getStudioName() : "未知",
                app.getStudioName()
            ));
        }
        return result;
    }
}
