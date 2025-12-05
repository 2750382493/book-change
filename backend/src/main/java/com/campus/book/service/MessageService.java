package com.campus.book.service;

import com.campus.book.model.Message;
import com.campus.book.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 消息业务服务类
 * 处理即时通讯相关的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * 发送消息
     * @param message 消息实体
     * @return 保存后的消息
     */
    @Transactional
    public Message sendMessage(Message message) {
        // 设置初始状态
        message.setCreateTime(new Date());
        message.setStatus(0); // 0: 未读, 1: 已读
        
        Message savedMessage = messageRepository.save(message);
        log.info("发送消息成功, ID: {}, 发送者: {}, 接收者: {}", 
                savedMessage.getId(), message.getFromUserId(), message.getToUserId());
        return savedMessage;
    }

    /**
     * 获取两个用户关于某本书的对话记录
     * @param user1Id 用户1 ID
     * @param user2Id 用户2 ID
     * @param bookId 关联图书 ID
     * @return 消息列表（按时间正序）
     */
    public List<Message> getConversation(String user1Id, String user2Id, String bookId) {
        // 实际开发中通常需要在 Repository 自定义复杂的查询方法，这里假设 Repository 已支持
        // 或者是查找两个用户之间的所有交互
        log.debug("获取对话记录: {} <-> {}", user1Id, user2Id);
        return messageRepository.findByFromUserIdAndToUserIdOrFromUserIdAndToUserIdOrderByCreateTime(
                user1Id, user2Id, user2Id, user1Id);
    }

    /**
     * 获取用户的未读消息列表
     * @param userId 用户 ID
     * @return 未读消息列表
     */
    public List<Message> getUnreadMessages(String userId) {
        return messageRepository.findByToUserIdAndStatus(userId, 0);
    }

    /**
     * 统计未读消息数量
     * @param userId 用户 ID
     * @return 未读数
     */
    public Long getUnreadCount(String userId) {
        return messageRepository.countByToUserIdAndStatus(userId, 0);
    }

    /**
     * 批量标记消息为已读
     * @param messageIds 消息 ID 列表
     */
    @Transactional
    public void markAsRead(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return;
        
        List<Message> messages = messageRepository.findAllById(messageIds);
        for (Message msg : messages) {
            msg.setStatus(1); // 标记已读
        }
        messageRepository.saveAll(messages);
        log.info("批量标记已读, 数量: {}", messages.size());
    }
}