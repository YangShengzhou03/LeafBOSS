package com.leafboss.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey SECRET_KEY;

    @Value("${app.jwt.expiration}")
    private long expirationTime;

    /** 签发超过此时间则续期（默认30分钟） */
    @Value("${app.jwt.renew-threshold:1800000}")
    private long renewThreshold;

    /** 绝对最大存活时间，超过后拒绝续期（默认7天） */
    @Value("${app.jwt.max-age:604800000}")
    private long maxAge;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        // 使用配置的固定密钥，确保重启后 token 仍然有效
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        if (keyBytes.length < 32) {
            // 密钥过短，用 SHA-256 哈希到 32 字节
            try {
                keyBytes = java.security.MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException e) {
                // SHA-256 是 JVM 规范强制实现的算法，理论上不会到达
                throw new IllegalStateException("SHA-256 algorithm not available", e);
            }
        }
        this.SECRET_KEY = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(String adminId, String username, String role, String sessionToken) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);
        if (role != null) {
            claims.put("role", role);
        }
        if (sessionToken != null) {
            claims.put("sid", sessionToken);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SECRET_KEY)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getAdminIdFromToken(String token) {
        return parseClaims(token).get("adminId", String.class);
    }

    public String getUserIdFromToken(String token) {
        return getAdminIdFromToken(token);
    }

    /**
     * 判断是否需要续期：签发时间超过 renewThreshold 且未超过 maxAge
     */
    public boolean shouldRenew(String token) {
        try {
            Claims claims = parseClaims(token);
            long issuedAt = claims.getIssuedAt().getTime();
            long now = System.currentTimeMillis();
            return (now - issuedAt > renewThreshold) && (now - issuedAt < maxAge);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 从旧 token 签发新 token（滑动续期，保留 session_token）
     */
    public String renewToken(String oldToken) {
        Claims claims = parseClaims(oldToken);
        return generateToken(
                claims.get("adminId", String.class),
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("sid", String.class)
        );
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getSessionTokenFromToken(String token) {
        return parseClaims(token).get("sid", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}