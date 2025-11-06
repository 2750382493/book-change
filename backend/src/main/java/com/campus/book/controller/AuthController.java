package com.campus.book.controller;

import com.campus.book.model.User;
import com.campus.book.service.JwtService;
import com.campus.book.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User savedUser = userService.register(user);
            String token = jwtService.generateToken(savedUser.getUsername()); // 这里使用getUsername()

            Map<String, Object> response = new HashMap<>();
            response.put("user", savedUser);
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> user = userService.login(username, password);
        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get().getUsername()); // 这里使用getUsername()

            Map<String, Object> response = new HashMap<>();
            response.put("user", user.get());
            response.put("token", token);

            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body("用户名或密码错误");
    }
}