package com.school.management.security;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token服务
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtTokenService {

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshExpiration;

    private final RedisTemplate<String, Object> redisTemplate;
    private final Environment environment;

    // 默认密钥标识（用于检测是否使用了默认值）
    private static final String DEFAULT_SECRET_PREFIX = "student-management-system-jwt-secret";

    public JwtTokenService(RedisTemplate<String, Object> redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    /**
     * 启动时验证JWT密钥配置
     * 生产环境必须使用自定义密钥
     */
    @PostConstruct
    public void validateJwtSecret() {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod")
                || Arrays.asList(environment.getActiveProfiles()).contains("production");

        // 检查是否使用了默认密钥
        boolean isUsingDefaultSecret = jwtSecret != null && jwtSecret.startsWith(DEFAULT_SECRET_PREFIX);

        if (isProduction && isUsingDefaultSecret) {
            log.error("【安全错误】生产环境检测到使用默认JWT密钥！请通过环境变量JWT_SECRET设置安全密钥");
            throw new IllegalStateException("生产环境必须配置自定义JWT密钥，请设置环境变量JWT_SECRET");
        }

        if (isUsingDefaultSecret) {
            log.warn("【安全警告】正在使用默认JWT密钥，仅适用于开发环境。生产部署前请设置环境变量JWT_SECRET");
        }

        // 验证密钥长度（HS512需要至少64字节）
        if (jwtSecret == null || jwtSecret.getBytes().length < 64) {
            log.error("【安全错误】JWT密钥长度不足，HS512算法需要至少64字节");
            throw new IllegalStateException("JWT密钥长度不足，请使用至少64字节的密钥");
        }

        // 初始化Token加密密钥（使用JWT密钥派生）
        com.school.management.common.util.TokenEncryptionUtil.initKey(jwtSecret + ":token-encryption");
        log.info("JWT密钥配置验证通过，Token加密已启用");
    }

    /**
     * 生成访问令牌
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("username", username)
                .claim("roles", roles)
                .claim("tokenType", "access_token")
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId) {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshExpiration);

        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("tokenType", "refresh_token")
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // 存储到Redis（加密存储）
        try {
            String key = "refresh_token:" + userId;
            // 使用AES-GCM加密Token后存储
            String encryptedToken = com.school.management.common.util.TokenEncryptionUtil.encrypt(refreshToken);
            redisTemplate.opsForValue().set(key, encryptedToken, refreshExpiration, TimeUnit.MILLISECONDS);
            log.debug("刷新令牌已加密存储: userId={}", userId);
        } catch (Exception e) {
            // Redis不可用时仅记录警告,不影响token生成
            log.warn("Redis连接失败,跳过refresh token存储: {}", e.getMessage());
        }

        return refreshToken;
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            if (StrUtil.isBlank(token)) {
                return false;
            }

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            // 检查是否在黑名单中
            return !isTokenBlacklisted(token);

        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("username", String.class);
    }

    /**
     * 从令牌中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("roles", List.class);
    }

    /**
     * 验证刷新令牌
     */
    public boolean validateRefreshToken(String refreshToken, Long userId) {
        try {
            if (StrUtil.isBlank(refreshToken)) {
                return false;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            // 验证用户ID
            Long tokenUserId = Long.valueOf(claims.getSubject());
            if (!userId.equals(tokenUserId)) {
                return false;
            }

            // 验证令牌类型
            String tokenType = claims.get("tokenType", String.class);
            if (!"refresh_token".equals(tokenType)) {
                return false;
            }

            // 验证刷新令牌是否有效（从Redis获取并解密）
            String encryptedStoredToken = (String) redisTemplate.opsForValue().get("refresh_token:" + userId);
            if (encryptedStoredToken == null) {
                return false;
            }
            // 解密存储的Token后比较
            String storedToken = com.school.management.common.util.TokenEncryptionUtil.decrypt(encryptedStoredToken);
            return refreshToken.equals(storedToken);

        } catch (JwtException | IllegalArgumentException e) {
            log.error("刷新令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 令牌加入黑名单
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            long expiry = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (expiry > 0) {
                String key = "blacklist_token:" + token;
                redisTemplate.opsForValue().set(key, "true", expiry, TimeUnit.MILLISECONDS);
            }
        } catch (JwtException e) {
            log.error("令牌加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String key = "blacklist_token:" + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // Redis不可用时,安全优先：假设token在黑名单中，拒绝访问
            log.error("Redis连接失败,安全优先拒绝token: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 获取当前登录用户ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        throw new IllegalStateException("当前用户未认证");
    }

    /**
     * 获取当前登录用户名
     */
    public String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        throw new IllegalStateException("当前用户未认证");
    }
}