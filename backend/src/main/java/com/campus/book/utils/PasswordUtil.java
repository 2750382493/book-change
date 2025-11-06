package com.campus.book.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtil {

    private static final String SALT = "campus_book_salt_2024";

    public static String encodePassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 添加盐值增加安全性
            String saltedPassword = password + SALT;
            byte[] hash = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        String encodedRaw = encodePassword(rawPassword);
        return encodedRaw.equals(encodedPassword);
    }
}