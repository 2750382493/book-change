package com.campus.book.controller;

import com.campus.book.model.Message;
import com.campus.book.service.MessageService;
import com.campus.book.service.UserService;
import com.campus.book.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/messages")
@CrossOrigin(origins = "*")
public class MessageController {
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @PostMapping
    public ResponseEntity<Message> sendMessage(@RequestBody Message message) {
        Message savedMessage = messageService.sendMessage(message);
        return ResponseEntity.ok(savedMessage);
    }
    
    @GetMapping("/conversation")
    public List<Message> getConversation(@RequestParam String user1Id,
                                       @RequestParam String user2Id,
                                       @RequestParam String bookId) {
        return messageService.getConversation(user1Id, user2Id, bookId);
    }
    
    @GetMapping("/unread/{userId}")
    public List<Message> getUnreadMessages(@PathVariable String userId) {
        return messageService.getUnreadMessages(userId);
    }
    
    @GetMapping("/unread/count/{userId}")
    public Long getUnreadCount(@PathVariable String userId) {
        return messageService.getUnreadCount(userId);
    }
    
    @PostMapping("/mark-read")
    public ResponseEntity<?> markAsRead(@RequestBody List<String> messageIds) {
        // 实现标记已读逻辑
        return ResponseEntity.ok().build();
    }
}