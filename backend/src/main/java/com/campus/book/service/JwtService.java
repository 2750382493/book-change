package com.campus.book.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String TOKEN_TYPE = "JWT";
    private static final String ISSUER_CLAIM = "iss";
    private static final String AUDIENCE_CLAIM = "aud";
    private static final String JTI_CLAIM = "jti";
    private static final String ROLES_CLAIM = "roles";
    private static final String PERMISSIONS_CLAIM = "perms";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400}") // 默认24小时（秒）
    private long jwtExpirationSeconds;

    @Value("${app.jwt.refresh-expiration:2592000}") // 默认30天（秒）
    private long refreshTokenExpirationSeconds;

    @Value("${app.jwt.issuer:campus-book-service}")
    private String issuer;

    @Value("${app.jwt.audience:campus-book-client}")
    private String audience;

    // 令牌黑名单（简易内存实现，生产环境用Redis）
    private final Set<String> tokenBlacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 刷新令牌存储（简易内存实现）
    private final Map<String, RefreshTokenInfo> refreshTokenStore = new ConcurrentHashMap<>();

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        try {
            // 支持Base64编码的密钥
            if (isBase64Encoded(jwtSecret)) {
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
                return Keys.hmacShaKeyFor(keyBytes);
            }

            // 普通字符串密钥，确保长度足够
            byte[] keyBytes = jwtSecret.getBytes();
            if (keyBytes.length < 32) {
                log.warn("JWT密钥长度不足256位，建议使用更长的密钥或Base64编码的密钥");
                // 填充到32字节
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                return Keys.hmacShaKeyFor(paddedKey);
            }

            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("JWT密钥初始化失败", e);
            throw new JwtConfigurationException("JWT密钥配置错误", e);
        }
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String username, List<String> roles, List<String> permissions) {
        return generateToken(username, roles, permissions, jwtExpirationSeconds, TokenType.ACCESS);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String username, List<String> roles) {
        String refreshToken = generateToken(username, roles, Collections.emptyList(),
                refreshTokenExpirationSeconds, TokenType.REFRESH);

        // 存储刷新令牌信息
        RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
                .username(username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirationSeconds))
                .roles(roles)
                .build();

        refreshTokenStore.put(refreshToken, tokenInfo);
        return refreshToken;
    }

    /**
     * 通用令牌生成方法
     */
    private String generateToken(String subject, List<String> roles, List<String> permissions,
                                 long expirationSeconds, TokenType tokenType) {
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("令牌主题（用户名）不能为空");
        }

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        String tokenId = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .setId(tokenId)
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim(TOKEN_TYPE_CLAIM, tokenType.getValue())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);

        // 添加自定义声明
        if (roles != null && !roles.isEmpty()) {
            builder.claim(ROLES_CLAIM, roles);
        }

        if (permissions != null && !permissions.isEmpty()) {
            builder.claim(PERMISSIONS_CLAIM, permissions);
        }

        String token = builder.compact();
        log.debug("生成{}令牌，用户: {}, 有效期: {}秒", tokenType, subject, expirationSeconds);

        return token;
    }

    /**
     * 刷新访问令牌
     */
    public TokenPair refreshTokens(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new InvalidTokenException("刷新令牌无效");
        }

        // 验证是否为刷新令牌
        if (!isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("提供的令牌不是刷新令牌");
        }

        // 检查刷新令牌是否在存储中
        RefreshTokenInfo tokenInfo = refreshTokenStore.get(refreshToken);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            refreshTokenStore.remove(refreshToken);
            throw new InvalidTokenException("刷新令牌已过期或无效");
        }

        String username = getUsernameFromToken(refreshToken);
        List<String> roles = getRolesFromToken(refreshToken);
        List<String> permissions = getPermissionsFromToken(refreshToken);

        // 生成新的令牌对
        String newAccessToken = generateAccessToken(username, roles, permissions);
        String newRefreshToken = generateRefreshToken(username, roles);

        // 使旧刷新令牌失效
        revokeRefreshToken(refreshToken);

        return TokenPair.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationSeconds)
                .build();
    }

    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从令牌中提取角色
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object rolesObj = claims.get(ROLES_CLAIM);

        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return Collections.emptyList();
    }

    /**
     * 从令牌中提取权限
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object permsObj = claims.get(PERMISSIONS_CLAIM);

        if (permsObj instanceof List) {
            return (List<String>) permsObj;
        }
        return Collections.emptyList();
    }

    /**
     * 获取令牌过期时间
     */
    public Instant getExpirationFromToken(String token) {
        return getClaimsFromToken(token).getExpiration().toInstant();
    }

    /**
     * 获取令牌签发时间
     */
    public Instant getIssuedAtFromToken(String token) {
        return getClaimsFromToken(token).getIssuedAt().toInstant();
    }

    /**
     * 获取令牌剩余有效期（秒）
     */
    public long getRemainingValiditySeconds(String token) {
        try {
            Instant expiration = getExpirationFromToken(token);
            return Duration.between(Instant.now(), expiration).getSeconds();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token) || isTokenRevoked(token)) {
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("令牌已过期: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT令牌: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT令牌格式错误: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            log.warn("JWT签名验证失败: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT令牌参数错误: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证令牌并返回声明（不抛出异常）
     */
    public Optional<Claims> validateTokenQuietly(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return Optional.of(claims);
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    /**
     * 检查是否为访问令牌
     */
    public boolean isAccessToken(String token) {
        try {
            String tokenType = getClaimsFromToken(token).get(TOKEN_TYPE_CLAIM, String.class);
            return TokenType.ACCESS.getValue().equals(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 检查是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = getClaimsFromToken(token).get(TOKEN_TYPE_CLAIM, String.class);
            return TokenType.REFRESH.getValue().equals(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 撤销令牌（加入黑名单）
     */
    public void revokeToken(String token) {
        if (StringUtils.hasText(token)) {
            tokenBlacklist.add(token);
            log.debug("令牌已撤销: {}", getTokenPreview(token));
        }
    }

    /**
     * 撤销刷新令牌
     */
    public void revokeRefreshToken(String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenStore.remove(refreshToken);
            revokeToken(refreshToken);
            log.debug("刷新令牌已撤销: {}", getTokenPreview(refreshToken));
        }
    }

    /**
     * 批量撤销令牌
     */
    public void revokeTokens(List<String> tokens) {
        tokens.forEach(this::revokeToken);
    }

    /**
     * 清理过期令牌黑名单
     */
    public void cleanExpiredBlacklist() {
        int initialSize = tokenBlacklist.size();
        tokenBlacklist.removeIf(this::isTokenExpired);
        log.debug("清理令牌黑名单，移除{}个过期令牌", initialSize - tokenBlacklist.size());
    }

    /**
     * 清理过期刷新令牌
     */
    public void cleanExpiredRefreshTokens() {
        int initialSize = refreshTokenStore.size();
        refreshTokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("清理刷新令牌，移除{}个过期令牌", initialSize - refreshTokenStore.size());
    }

    /**
     * 获取令牌统计信息
     */
    public TokenStatistics getTokenStatistics() {
        return TokenStatistics.builder()
                .activeRefreshTokens(refreshTokenStore.size())
                .blacklistedTokens(tokenBlacklist.size())
                .build();
    }

    /**
     * 生成密钥对（开发工具方法）
     */
    public static SecretKey generateSecretKey() {
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    /**
     * 生成Base64编码的密钥（开发工具方法）
     */
    public static String generateBase64SecretKey() {
        SecretKey key = generateSecretKey();
        return Encoders.BASE64.encode(key.getEncoded());
    }

    // ============ 私有方法 ============

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("令牌已过期", e);
        } catch (JwtException e) {
            throw new InvalidTokenException("令牌无效: " + e.getMessage(), e);
        }
    }

    private boolean isTokenRevoked(String token) {
        return tokenBlacklist.contains(token);
    }

    private boolean isTokenExpired(String token) {
        try {
            Instant expiration = getExpirationFromToken(token);
            return Instant.now().isAfter(expiration);
        } catch (JwtException e) {
            return true;
        }
    }

    private boolean isBase64Encoded(String str) {
        try {
            Decoders.BASE64.decode(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getTokenPreview(String token) {
        if (token.length() <= 20) {
            return token;
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }

    // ============ 内部类和枚举 ============

    public enum TokenType {
        ACCESS("access"),
        REFRESH("refresh");

        private final String value;

        TokenType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class RefreshTokenInfo {
        private String username;
        private Instant issuedAt;
        private Instant expiresAt;
        private List<String> roles;

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
    }

    @lombok.Data
    @lombok.Builder
    public static class TokenStatistics {
        private int activeRefreshTokens;
        private int blacklistedTokens;
    }

    // ============ 自定义异常 ============

    public static class JwtConfigurationException extends RuntimeException {
        public JwtConfigurationException(String message) {
            super(message);
        }

        public JwtConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TokenExpiredException extends InvalidTokenException {
        public TokenExpiredException(String message) {
            super(message);
        }

        public TokenExpiredException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}