package com.xiongdwm.future_backend.platform.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 挂单大厅即时通讯消息（平台库）
 * 以 applicationId 为对话键，连接挂单方与申请方工作室
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;   // 对话上下文：申请记录id

    @Column(nullable = false)
    private Long senderStudioId;  // 发送方工作室id

    @Column(nullable = false, length = 50)
    private String senderName;    // 发送者显示名

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;       // 消息内容（IMAGE 类型时为逗号分隔的文件ID）

    @Column(nullable = false, length = 10)
    private String messageType = "TEXT"; // TEXT / IMAGE

    @Column(nullable = false)
    private Date sentAt;          // 发送时间

    @Column(nullable = false)
    private boolean readByReceiver; // 对方是否已读

    // — getters & setters —

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getSenderStudioId() { return senderStudioId; }
    public void setSenderStudioId(Long senderStudioId) { this.senderStudioId = senderStudioId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public boolean isReadByReceiver() { return readByReceiver; }
    public void setReadByReceiver(boolean readByReceiver) { this.readByReceiver = readByReceiver; }
}
