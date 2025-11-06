package com.campus.book.repository;

import com.campus.book.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByFromUserIdAndToUserIdOrFromUserIdAndToUserIdOrderByCreateTime(
        String fromUser1, String toUser1, String fromUser2, String toUser2);
    List<Message> findByToUserIdAndReadFalse(String userId);
    Long countByToUserIdAndReadFalse(String userId);
}