package com.xiongdwm.future_backend.service;

import java.util.List;
import java.util.Map;

import com.xiongdwm.future_backend.bo.ConversationSummaryDto;
import com.xiongdwm.future_backend.platform.entity.ChatMessage;

public interface ChatService {

    /** 发送消息，返回持久化后的消息实体 */
    ChatMessage send(Long applicationId, Long senderStudioId, String senderName, String content);

    /** 获取某对话的全部消息，同时将对方发来的标记已读 */
    List<ChatMessage> getMessages(Long applicationId, Long myStudioId);

    /** 获取我的所有对话的未读数 Map<applicationId, unreadCount> */
    Map<Long, Integer> getUnreadCounts(Long myStudioId);

    /** 获取我参与过的所有对话摘要列表（有消息记录的） */
    List<ConversationSummaryDto> getMyConversations(Long myStudioId);
}
