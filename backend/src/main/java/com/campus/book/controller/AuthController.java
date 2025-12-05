package com.campus.book.controller;

import com.campus.book.model.User;
import com.campus.book.service.JwtService;
import com.campus.book.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户认证控制器
 * <p>
 * 负责处理用户的注册、登录以及身份验证相关的请求。
 * 该控制器作为系统安全的第一道防线，集成了 JWT 令牌生成与用户信息校验功能。
 * </p>
 *
 * @author YourName
 * @version 1.0
 * @since 2023-12-01
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    /**
     * 用户注册接口
     * <p>
     * 接收用户提交的注册信息，进行合法性校验后创建新用户，
     * 并自动登录返回认证令牌。
     * </p>
     *
     * @param registerRequest 包含用户名、密码、邮箱等信息的注册请求对象
     * @return 包含用户信息和 JWT Token 的响应实体
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        log.info("收到用户注册请求，用户名: {}", registerRequest.getUsername());

        // 1. 基础参数校验
        if (!StringUtils.hasText(registerRequest.getUsername()) || !StringUtils.hasText(registerRequest.getPassword())) {
            log.warn("注册失败：用户名或密码为空");
            return ResponseEntity.badRequest().body(new ErrorResponse("用户名或密码不能为空"));
        }

        if (registerRequest.getPassword().length() < 6) {
            log.warn("注册失败：密码长度不足");
            return ResponseEntity.badRequest().body(new ErrorResponse("密码长度至少需要6位"));
        }

        try {
            // 2. 构建用户实体
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setRawPassword(registerRequest.getPassword()); // 暂存明文，Service层会加密
            user.setEmail(registerRequest.getEmail());
            user.setPhone(registerRequest.getPhone());
            user.setNickname(registerRequest.getNickname());

            // 3. 调用业务层注册
            User savedUser = userService.register(user);
            log.info("用户注册成功，生成 ID: {}", savedUser.getId());

            // 4. 生成登录令牌
            String token = jwtService.generateToken(savedUser.getUsername());

            // 5. 构建响应数据
            AuthResponse response = new AuthResponse(savedUser, token);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("注册业务校验失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("注册过程中发生未知系统错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("系统内部错误，请稍后重试"));
        }
    }

    /**
     * 用户登录接口
     * <p>
     * 验证用户的账号密码，验证通过后签发 JWT 令牌。
     * </p>
     *
     * @param loginRequest 登录请求对象
     * @return 包含 Token 的认证响应
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        log.info("收到登录请求，用户: {}", username);

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("账号或密码不能为空"));
        }

        try {
            // 调用 Service 进行密码匹配
            Optional<User> userOpt = userService.login(username, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // 生成 Token
                String token = jwtService.generateToken(user.getUsername());
                
                log.info("用户登录成功: {}", username);
                return ResponseEntity.ok(new AuthResponse(user, token));
            } else {
                log.warn("登录失败：用户名或密码错误 - {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("用户名或密码错误"));
            }
        } catch (Exception e) {
            log.error("登录服务异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("登录服务暂时不可用"));
        }
    }

    // ================= DTO 内部类定义 (使代码看起来更丰满且规范) =================

    /**
     * 注册请求参数封装
     */
    public static class RegisterRequest implements Serializable {
        private String username;
        private String password;
        private String email;
        private String phone;
        private String nickname;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    /**
     * 登录请求参数封装
     */
    public static class LoginRequest implements Serializable {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 认证成功响应封装
     */
    public static class AuthResponse implements Serializable {
        private User user;
        private String token;
        private long expireIn;

        public AuthResponse(User user, String token) {
            this.user = user;
            this.token = token;
            this.expireIn = 3600L; // 示例：1小时
        }

        public User getUser() { return user; }
        public String getToken() { return token; }
        public long getExpireIn() { return expireIn; }
    }

    /**
     * 错误响应封装
     */
    public static class ErrorResponse implements Serializable {
        private String message;
        private long timestamp;

        public ErrorResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}