package com.campus.book.service;

import com.campus.book.model.User;
import com.campus.book.repository.UserRepository;
import com.campus.book.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 使用工具类加密密码
        user.setPassword(PasswordUtil.encodePassword(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && PasswordUtil.matches(password, user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
}