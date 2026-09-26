package com.leafboss.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String smtpUser;

    @Value("${spring.mail.from-name:LEAF-BOSS}")
    private String fromName;

    /**
     * 发送验证码邮件
     * @param to 收件人邮箱
     * @param code 6位验证码
     * @param scene 场景：注册、密码重置、邮箱变更
     */
    public boolean sendVerificationCode(String to, String code, String scene) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置发件人：显示名 <邮箱地址>
            helper.setFrom(new InternetAddress(smtpUser, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(buildSubject(scene));

            String html = buildEmailTemplate(code);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("[邮件发送成功] 收件人: {} 场景: {}", to, scene);
            return true;
        } catch (Exception e) {
            log.error("[邮件发送失败] 收件人: {} 场景: {} 原因: {}", to, scene, e.getMessage());
            return false;
        }
    }

    private String buildSubject(String scene) {
        return switch (scene) {
            case "用户注册验证" -> "【" + fromName + "】您的注册验证码";
            case "用户密码重置" -> "【" + fromName + "】您的重置密码验证码";
            case "邮箱变更验证" -> "【" + fromName + "】您的邮箱变更验证码";
            case "管理员密码重置" -> "【" + fromName + "】您的重置密码验证码";
            default -> "【" + fromName + "】您的验证码";
        };
    }

    private String buildEmailTemplate(String code) {
        return "<!DOCTYPE html>"
                + "<html lang=\"zh-CN\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>验证码</title>"
                + "</head>"
                + "<body style=\"margin:0; padding:0; background-color:#f5f7fa; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\">"
                + "<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f5f7fa;\">"
                + "<tr><td align=\"center\" style=\"padding:40px 20px;\">"
                + "<table width=\"480\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff; border-radius:16px; box-shadow:0 2px 12px rgba(0,0,0,0.06);\">"
                + "<tr><td style=\"padding:36px 32px;\">"
                + "<p style=\"margin:0 0 24px; font-size:22px; color:#1f2937; font-weight:600;\">您的验证码</p>"
                + "<p style=\"margin:0 0 20px; font-size:15px; color:#4b5563; line-height:1.6;\">您好，请使用以下验证码完成操作。</p>"
                + "<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"background:#f0f4ff; border-radius:12px; padding:20px;\">"
                + "<span style=\"font-size:36px; font-weight:700; color:#2563eb; letter-spacing:8px;\">" + code + "</span>"
                + "</td></tr></table>"
                + "<p style=\"margin:20px 0 0; font-size:13px; color:#6b7280; line-height:1.6;\">验证码 <strong>5 分钟</strong> 后过期，过期请重新获取。</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:20px 32px; background:#f9fafb; border-radius:0 0 16px 16px;\">"
                + "<p style=\"margin:0 0 8px; font-size:13px; color:#374151; font-weight:600;\">安全提示</p>"
                + "<ul style=\"margin:0; padding:0 0 0 18px; font-size:12px; color:#6b7280; line-height:1.8;\">"
                + "<li>请勿将验证码透露给他人或转发给第三方</li>"
                + "<li>如非本人操作，请放心忽略，您的账号安全不受影响</li>"
                + "<li>频繁收到此类邮件？建议及时检查账号安全</li>"
                + "</ul>"
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 32px; border-top:1px solid #eee;\">"
                + "<p style=\"margin:0; font-size:12px; color:#9ca3af; text-align:center;\">© Yangshengzhou " + Year.now().getValue() + ". All rights reserved.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }
}
