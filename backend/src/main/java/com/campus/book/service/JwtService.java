package com.campus.book.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT (JSON Web Token) 服务类
 * <p>
 * 提供 JWT 令牌的生成、解析、验证以及从令牌中提取用户信息的功能。
 * 本服务采用 HMAC SHA256 算法进行签名，确保令牌在传输过程中的不可篡改性。
 * </p>
 *
 * @author YourName
 */
@Slf4j
@Service
public class JwtService {

    // 从配置文件读取密钥，若不存在则使用默认值（仅供开发测试）
    @Value("${jwt.secret:MySuperSecretKeyForCampusBookSystem2023}")
    private String secretKey;

    // Token 有效期：24小时 (毫秒)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * 根据用户名生成 Token
     *
     * @param username 用户名
     * @return 加密后的 JWT 字符串
     */
    public String generateToken(String username) {
        log.debug("开始为用户生成 Token: {}", username);
        Map<String, Object> claims = new HashMap<>();
        // 可以在这里添加额外的 payload 信息，例如用户角色
        claims.put("role", "USER");
        claims.put("platform", "WEB");
        
        return createToken(claims, username);
    }

    /**
     * 创建 Token 的具体实现
     *
     * @param claims 数据声明
     * @param subject 主题（通常是用户名）
     * @return Token 字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date(System.currentTimeMillis());
        Date expiration = new Date(System.currentTimeMillis() + EXPIRATION_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * 从 Token 中验证并提取用户名
     *
     * @param token JWT 令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从 Token 中提取过期时间
     *
     * @param token JWT 令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 泛型方法：提取 Token 中的特定信息
     *
     * @param token 令牌
     * @param claimsResolver 处理 Claims 的函数
     * @param <T> 返回类型
     * @return 提取的数据
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 解析 Token 获取所有的 Claims
     * <p>
     * 此过程会验证签名，如果签名无效或 Token 格式错误，将抛出异常。
     * </p>
     *
     * @param token 令牌
     * @return Claims 对象
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Token 解析失败: {}", e.getMessage());
            throw new RuntimeException("无效的身份认证令牌");
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token 令牌
     * @param username 待验证的用户名
     * @return true 如果有效
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            boolean isUsernameMatch = extractedUsername.equals(username);
            boolean isTokenExpired = isTokenExpired(token);
            
            if (!isUsernameMatch) {
                log.warn("Token 验证失败：用户名不匹配 (Expected: {}, Actual: {})", username, extractedUsername);
            }
            if (isTokenExpired) {
                log.warn("Token 验证失败：令牌已过期");
            }
            
            return (isUsernameMatch && !isTokenExpired);
        } catch (Exception e) {
            log.error("Token 验证过程中发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token 令牌
     * @return true 如果已过期
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}