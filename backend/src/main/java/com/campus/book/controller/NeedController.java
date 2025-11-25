package com.campus.book.controller;

import com.campus.book.model.Need;
import com.campus.book.model.User;
import com.campus.book.service.NeedService;
import com.campus.book.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/needs")
@CrossOrigin(origins = "*")
public class NeedController {
    @Autowired
    private NeedService needService;
    
    @Autowired
    private UserService userService;

    @GetMapping
    public List<Need> getAllNeeds() {
        return needService.getAllActiveNeeds();
    }
    
    @GetMapping("/search")
    public List<Need> searchNeeds(@RequestParam String keyword) {
        return needService.searchNeeds(keyword);
    }
    
    @GetMapping("/user/{userId}")
    public List<Need> getUserNeeds(@PathVariable String userId) {
        return needService.getUserNeeds(userId);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Need> getNeedById(@PathVariable String id) {
        Optional<Need> need = needService.getNeedById(id);
        return need.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createNeed(@RequestBody NeedRequest request) {
        try {
            System.out.println("接收到创建需求请求: " + request);
            
            if (request.getTitle() == null || request.getUserId() == null) {
                return ResponseEntity.badRequest().body("缺少必要参数: title, userId");
            }
            
            Optional<User> user = userService.findById(request.getUserId());
            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body("用户不存在");
            }
            
            Need need = new Need();
            need.setTitle(request.getTitle());
            need.setAuthor(request.getAuthor());
            need.setIsbn(request.getIsbn());
            need.setDescription(request.getDescription());
            need.setMaxPrice(request.getMaxPrice());
            need.setCategory(request.getCategory());
            need.setUser(user.get());
            
            Need savedNeed = needService.saveNeed(need);
            System.out.println("需求创建成功, ID: " + savedNeed.getId());
            return ResponseEntity.ok(savedNeed);
            
        } catch (Exception e) {
            System.err.println("创建需求异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("服务器内部错误: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/fulfilled")
    public ResponseEntity<Need> markAsFulfilled(@PathVariable String id) {
        try {
            Need need = needService.markAsFulfilled(id);
            return ResponseEntity.ok(need);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNeed(@PathVariable String id) {
        try {
            needService.deleteNeed(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public static class NeedRequest {
        private String title;
        private String author;
        private String isbn;
        private String description;
        private Double maxPrice;
        private String category;
        private String userId;
        
        // Getter和Setter
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        @Override
        public String toString() {
            return "NeedRequest{" +
                    "title='" + title + '\'' +
                    ", author='" + author + '\'' +
                    ", maxPrice=" + maxPrice +
                    ", category='" + category + '\'' +
                    ", userId='" + userId + '\'' +
                    '}';
        }
    }
}