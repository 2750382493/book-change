package com.campus.book.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码安全工具类
 * <p>
 * 提供基于 BCrypt 强哈希算法的密码加密与验证功能。
 * 封装了 Spring Security 的 PasswordEncoder，确保用户密码在数据库中以密文形式存储。
 * </p>
 *
 * @author YourName
 * @version 1.0
 */
@Slf4j
public class PasswordUtil {

    // 使用 BCrypt 算法，强度参数设置为 10
    private static final PasswordEncoder encoder = new BCryptPasswordEncoder(10);
    
    // 安全随机数生成器，用于生成盐值（如有需要）
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 私有构造函数，防止工具类被实例化
     */
    private PasswordUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 对原始密码进行加密
     * <p>
     * 使用 BCrypt 算法，该算法会自动生成随机盐并包含在哈希字符串中。
     * 每次加密相同的密码，生成的哈希值都会不同，这是正常的。
     * </p>
     *
     * @param rawPassword 用户输入的明文密码
     * @return 加密后的哈希字符串 (长度通常为 60 字符)
     */
    public static String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            log.error("尝试加密空密码");
            throw new IllegalArgumentException("Password cannot be empty");
        }
        String encoded = encoder.encode(rawPassword);
        log.debug("密码加密完成");
        return encoded;
    }

    /**
     * 验证密码是否匹配
     * <p>
     * 将明文密码与数据库中存储的加密哈希进行比对。
     * </p>
     *
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return true 如果密码匹配，否则 false
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            log.warn("密码比对时参数为空");
            return false;
        }
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            log.debug("密码验证失败");
        }
        return matches;
    }

    /**
     * 生成随机盐值字符串 (辅助功能)
     *
     * @param length 盐值长度
     * @return Base64 编码的随机字符串
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}