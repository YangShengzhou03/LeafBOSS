package com.leafboss.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存验证码服务：随机6位码，5分钟过期，一次性使用。
 * ponytail: 无邮件服务时验证码仅记入操作日志/控制台，上线需接入 SMTP 后替换投递方式。
 */
@Service
public class VerificationCodeService {

    private static final long EXPIRY_MINUTES = 5;
    private final Map<String, LocalDateTime> codeStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generateCode(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        codeStore.put(email + ":" + code, LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        return code;
    }

    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        String key = email + ":" + code.trim();
        LocalDateTime expiry = codeStore.remove(key); // 一次性使用
        return expiry != null && expiry.isAfter(LocalDateTime.now());
    }

    /** 每5分钟清理过期条目，防止内存泄漏 */
    @Scheduled(fixedRate = 300_000)
    public void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        codeStore.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
