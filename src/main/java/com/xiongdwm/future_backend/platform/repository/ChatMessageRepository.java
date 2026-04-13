package com.xiongdwm.future_backend.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xiongdwm.future_backend.platform.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 按时间升序获取某个对话的所有消息 */
    List<ChatMessage> findByApplicationIdOrderBySentAtAsc(Long applicationId);

    /** 将对方发来的消息标记为已读 */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByReceiver = true WHERE m.applicationId = :appId AND m.senderStudioId <> :myStudioId AND m.readByReceiver = false")
    int markReadByReceiver(@Param("appId") Long applicationId, @Param("myStudioId") Long myStudioId);

    /** 统计某个对话中对方发给我的未读消息数 */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.applicationId = :appId AND m.senderStudioId <> :myStudioId AND m.readByReceiver = false")
    int countUnread(@Param("appId") Long applicationId, @Param("myStudioId") Long myStudioId);

    /** 查询我参与的所有对话中，每个对话的未读数 — 返回 [applicationId, count] */
    @Query("SELECT m.applicationId, COUNT(m) FROM ChatMessage m WHERE m.senderStudioId <> :myStudioId AND m.readByReceiver = false GROUP BY m.applicationId")
    List<Object[]> countAllUnread(@Param("myStudioId") Long myStudioId);
}
