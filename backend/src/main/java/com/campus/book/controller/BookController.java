package com.campus.book.controller;

import com.campus.book.model.Book;
import com.campus.book.model.User;
import com.campus.book.service.BookService;
import com.campus.book.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 图书管理控制器
 * 提供图书的增删改查及图片上传接口
 */
@Slf4j
@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有未售出的图书
     */
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllAvailableBooks();
    }

    /**
     * 根据关键词搜索图书
     */
    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String keyword) {
        return bookService.searchBooks(keyword);
    }

    /**
     * 按分类获取图书
     */
    @GetMapping("/category/{category}")
    public List<Book> getBooksByCategory(@PathVariable String category) {
        return bookService.getBooksByCategory(category);
    }

    /**
     * 获取指定用户发布的图书
     */
    @GetMapping("/user/{userId}")
    public List<Book> getUserBooks(@PathVariable String userId) {
        return bookService.getUserBooks(userId);
    }

    /**
     * 获取图书详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        Optional<Book> book = bookService.getBookById(id);
        return book.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 发布图书
     */
    @PostMapping("/add")
    public ResponseEntity<?> createBook(@RequestBody BookRequest request) {
        try {
            log.info("接收到创建书籍请求: {}", request.getTitle());

            if (request.getTitle() == null || request.getPrice() == null || request.getSellerId() == null) {
                return ResponseEntity.badRequest().body("缺少必要参数: title, price, sellerId");
            }

            Optional<User> seller = userService.findById(request.getSellerId());
            if (seller.isEmpty()) {
                return ResponseEntity.badRequest().body("卖家用户不存在");
            }

            Book book = new Book();
            book.setTitle(request.getTitle());
            book.setAuthor(request.getAuthor() != null ? request.getAuthor() : "");
            book.setPrice(request.getPrice());
            book.setCategory(request.getCategory() != null ? request.getCategory() : "其他");
            book.setDescription(request.getDescription() != null ? request.getDescription() : "");
            book.setCondition(request.getCondition() != null ? request.getCondition() : "良好");
            book.setSeller(seller.get());

            Book savedBook = bookService.saveBook(book);
            return ResponseEntity.ok(savedBook);

        } catch (Exception e) {
            log.error("创建书籍异常", e);
            return ResponseEntity.status(500).body("服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 上传图书封面图片
     */
    @PostMapping("/upload-image/{bookId}")
    public ResponseEntity<?> uploadImage(@PathVariable String bookId,
                                         @RequestParam("file") MultipartFile file) {
        try {
            Optional<Book> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("书籍不存在");
            }

            // 确保上传目录存在
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return ResponseEntity.status(500).body("无法创建上传目录");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            String filename = System.currentTimeMillis() + fileExtension;
            String filepath = uploadDir + filename;

            // 保存文件
            file.transferTo(new File(filepath));

            // 更新数据库记录
            Book book = bookOpt.get();
            String imageUrl = "/uploads/" + filename;
            if (book.getImages() == null) {
                book.setImages(new ArrayList<>());
            }
            book.getImages().add(imageUrl);
            bookService.saveBook(book);

            log.info("图片上传成功: {}", imageUrl);
            return ResponseEntity.ok(imageUrl);

        } catch (IOException e) {
            log.error("文件IO异常", e);
            return ResponseEntity.status(500).body("文件上传失败");
        } catch (Exception e) {
            log.error("上传未知异常", e);
            return ResponseEntity.status(500).body("服务器错误");
        }
    }

    /**
     * 标记图书为已售出
     */
    @PutMapping("/{id}/sold")
    public ResponseEntity<Book> markAsSold(@PathVariable String id) {
        try {
            Book book = bookService.markAsSold(id);
            return ResponseEntity.ok(book);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTO 内部类
    public static class BookRequest {
        private String title;
        private String author;
        private Double price;
        private String category;
        private String description;
        private String condition;
        private String sellerId;
        
        // Getters and Setters ...
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getSellerId() { return sellerId; }
        public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    }
}