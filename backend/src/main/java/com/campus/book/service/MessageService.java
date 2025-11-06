package com.campus.book.service;

import com.campus.book.model.Message;
import com.campus.book.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;
    
    public Message sendMessage(Message message) {
        return messageRepository.save(message);
    }
    
    public List<Message> getConversation(String user1Id, String user2Id, String bookId) {
        return messageRepository.findByFromUserIdAndToUserIdOrFromUserIdAndToUserIdOrderByCreateTime(
            user1Id, user2Id, user2Id, user1Id);
    }
    
    public List<Message> getUnreadMessages(String userId) {
        return messageRepository.findByToUserIdAndReadFalse(userId);
    }
    
    public Long getUnreadCount(String userId) {
        return messageRepository.countByToUserIdAndReadFalse(userId);
    }
    
    public void markAsRead(List<Message> messages) {
        for (Message message : messages) {
            message.setRead(true);
        }
        messageRepository.saveAll(messages);
    }
}