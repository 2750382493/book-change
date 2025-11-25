package com.campus.book.controller;

import com.campus.book.model.Book;
import com.campus.book.model.User;
import com.campus.book.service.BookService;
import com.campus.book.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 添加缺失的导入
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
@CrossOrigin(origins = "*")
public class BookController {
    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllAvailableBooks();
    }

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String keyword) {
        return bookService.searchBooks(keyword);
    }

    @GetMapping("/category/{category}")
    public List<Book> getBooksByCategory(@PathVariable String category) {
        return bookService.getBooksByCategory(category);
    }

    @GetMapping("/user/{userId}")
    public List<Book> getUserBooks(@PathVariable String userId) {
        return bookService.getUserBooks(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        Optional<Book> book = bookService.getBookById(id);
        return book.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // 简化版创建书籍接口
    @PostMapping("/simple")
    public ResponseEntity<?> createBookSimple(@RequestBody BookRequest request) {
        try {
            System.out.println("接收到创建书籍请求: " + request);

            if (request.getTitle() == null || request.getPrice() == null || request.getSellerId() == null) {
                return ResponseEntity.badRequest().body("缺少必要参数: title, price, sellerId");
            }

            Optional<User> seller = userService.findById(request.getSellerId());
            if (seller.isEmpty()) {
                return ResponseEntity.badRequest().body("用户不存在");
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
            System.out.println("书籍创建成功, ID: " + savedBook.getId());
            return ResponseEntity.ok(savedBook);

        } catch (Exception e) {
            System.err.println("创建书籍异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("服务器内部错误: " + e.getMessage());
        }
    }

    // 测试图片上传功能
    @GetMapping("/test-upload")
    public ResponseEntity<String> testUpload() {
        try {
            // 创建测试目录
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return ResponseEntity.status(500).body("无法创建上传目录");
                }
            }

            // 检查目录权限
            if (!dir.canWrite()) {
                return ResponseEntity.status(500).body("上传目录没有写入权限");
            }

            return ResponseEntity.ok("上传目录正常: " + dir.getAbsolutePath());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("上传目录检查失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload-image/{bookId}")
    public ResponseEntity<?> uploadImage(@PathVariable String bookId,
                                         @RequestParam("file") MultipartFile file) {
        try {
            Optional<Book> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("书籍不存在");
            }

            // 创建上传目录 - 使用相对路径
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return ResponseEntity.status(500).body("无法创建上传目录");
                }
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = System.currentTimeMillis() + fileExtension;
            String filepath = uploadDir + filename;

            // 保存文件
            File dest = new File(filepath);
            file.transferTo(dest);

            // 更新书籍图片信息 - 使用相对路径
            Book book = bookOpt.get();
            String imageUrl = "/uploads/" + filename;
            if (book.getImages() == null) {
                book.setImages(new ArrayList<>());
            }
            book.getImages().add(imageUrl);
            Book savedBook = bookService.saveBook(book);

            System.out.println("图片保存成功: " + filepath);
            System.out.println("图片访问URL: " + imageUrl);
            System.out.println("书籍图片列表: " + savedBook.getImages());

            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("服务器内部错误: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/sold")
    public ResponseEntity<Book> markAsSold(@PathVariable String id) {
        try {
            Book book = bookService.markAsSold(id);
            return ResponseEntity.ok(book);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 请求包装类
    public static class BookRequest {
        private String title;
        private String author;
        private Double price;
        private String category;
        private String description;
        private String condition;
        private String sellerId;

        // Getter和Setter
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

        @Override
        public String toString() {
            return "BookRequest{" +
                    "title='" + title + '\'' +
                    ", author='" + author + '\'' +
                    ", price=" + price +
                    ", category='" + category + '\'' +
                    ", sellerId='" + sellerId + '\'' +
                    '}';
        }
    }
}