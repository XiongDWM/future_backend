package com.xiongdwm.future_backend.utils.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiongdwm.future_backend.entity.User;
import com.xiongdwm.future_backend.service.UserService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具：生成 / 解析 / 校验。
 * <p>
 * token 载荷包含：sub = userId, role = 角色名, username = 用户名
 */
@Component
public class JwtTokenProvider {
    @Resource
    UserService userService;

    private final SecretKey key;
    private final long expirationMs;
    private final boolean devMode;
    private static final Long DEV_USER_ID = -1L;
    private static final String DEV_USERNAME = "dev";
    private static final String DEV_ROLE = User.Role.ADMIN.name();

    public JwtTokenProvider(
            @Value("${jwt.secret:FutureBackendDefaultSecret2026!@#$%^&*()ABCDE}") String secret,
            @Value("${jwt.expiration-ms:1800000}") long expirationMs,
            @Value("${project.dev:false}") boolean devMode) {
        // 确保 secret 至少 32 字节 (256 位)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.devMode = devMode;
    }

    public String generateToken(Long userId, String username, String role) {
        if (devMode) {
            return "DEV_TOKEN";
        }
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 解析 token，返回 Claims；token 无效或过期则返回 null
    public Claims parseToken(String token) {
        if (devMode) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    
    // 校验 token 是否有效（签名正确 + 未过期） 
    public boolean isValid(String token) {
        if (devMode) {
            return true;
        }
        return parseToken(token) != null;
    }

    public Long getUserIdFromRawToken(String rawAuthorization) {
        if (devMode) {
            return DEV_USER_ID;
        }
        if (rawAuthorization == null || rawAuthorization.isBlank()) {
            return null;
        }
        String token = rawAuthorization.startsWith("Bearer ")
                ? rawAuthorization.substring(7)
                : rawAuthorization;
        Claims claims = parseToken(token);
        return claims != null ? Long.parseLong(claims.getSubject()) : null;
    }

    public User getUserFromRawToken(String token){
        if (devMode) {
            User dev = new User();
            dev.setId(DEV_USER_ID);
            dev.setUsername(DEV_USERNAME);
            dev.setRole(User.Role.ADMIN);
            return dev;
        }
        var userId = getUserIdFromRawToken(token);
        return userId != null ? userService.getUserById(userId) : null;
    }

    public String getUserId(String token) {
        if (devMode) {
            return String.valueOf(DEV_USER_ID);
        }
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Long getUserIdFromClaims(Claims claims) {
        if (claims == null) return null;
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public String getRole(String token) {
        if (devMode) {
            return DEV_ROLE;
        }
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    // 判断 token 是否需要续期：如果 token 已过半生命周期，则建议续期
    public boolean shouldRenew(Claims claims) {
        if (claims == null) return false;
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        if (issuedAt == null || expiration == null) return false;
        long lifetime = expiration.getTime() - issuedAt.getTime();
        long elapsed = System.currentTimeMillis() - issuedAt.getTime();
        return elapsed > lifetime / 2;
    }

    // 用旧 claims 里的信息签发一个新 token（续期）
    public String renewToken(Claims claims) {
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        return generateToken(Long.parseLong(userId), username, role);
    }
}
