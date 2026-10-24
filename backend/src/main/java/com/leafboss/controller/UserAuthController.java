package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.User;
import com.leafboss.service.EmailService;
import com.leafboss.service.UserService;
import com.leafboss.service.VerificationCodeService;
import com.leafboss.utils.IpRegionUtil;
import com.leafboss.utils.JwtUtil;
import com.leafboss.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-auth")
public class UserAuthController {

    private static final Logger log = LoggerFactory.getLogger(UserAuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private IpRegionUtil ipRegionUtil;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest, HttpServletRequest request) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        User user = userService.findByEmail(email.trim().toLowerCase());
        if (user != null) {
            // 拒绝管理员通过用户接口登录
            if ("admin".equals(user.getRole())) {
                logUtil.logLogin(false, "用户登录失败 - 邮箱: " + email + " (管理员账号请使用管理员入口)", request);
                return Result.error("请使用管理员入口登录");
            }

            if ("inactive".equals(user.getStatus())) {
                logUtil.logLogin(false, "用户登录失败 - 邮箱: " + email + " (账号已被禁用)", request);
                return Result.error("账号已被禁用，请联系管理员");
            }

            if (password.equals(user.getPassword())) {
                user.setLastLoginTime(LocalDateTime.now());
                String loginIp = LogUtil.getClientIpAddress(request);
                user.setLastLoginIp(loginIp);
                user.setLastLoginRegion(ipRegionUtil.getCityInfo(loginIp));
                // 生成新的 session_token，踢掉旧登录
                user.setSessionToken(UUID.randomUUID().toString().replace("-", ""));
                userService.updateById(user);

                String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getSessionToken());

                logUtil.logLogin(true, "用户登录成功 - 邮箱: " + email, request);

                user.setPassword(null);
                Map<String, Object> response = Map.of(
                    "token", token,
                    "user", user
                );
                return Result.success("登录成功", response);
            }
        }

        logUtil.logLogin(false, "用户登录失败 - 邮箱: " + email + " (密码错误或用户不存在)", request);
        return Result.error("邮箱或密码错误");
    }

    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        User user = userService.getById(userId);

        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        } else {
            return Result.error("用户不存在");
        }
    }

    /** 修改当前用户资料：用户名/邮箱/密码均可选，留空不修改 */
    @PutMapping("/me")
    public Result<Boolean> updateCurrentUser(HttpServletRequest request, @RequestBody Map<String, String> profile) {
        String userId = (String) request.getAttribute("currentUserId");
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        String username = profile.get("username");
        String email = profile.get("email");
        String password = profile.get("password");

        if (username != null && !username.trim().isEmpty()) {
            User existing = userService.findByUsername(username.trim());
            if (existing != null && !existing.getId().equals(userId)) {
                return Result.error("用户名已存在");
            }
            user.setUsername(username.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            String newEmail = email.trim().toLowerCase();
            // 邮箱未变更时跳过验证
            if (!newEmail.equals(user.getEmail())) {
                User existing = userService.findByEmail(newEmail);
                if (existing != null && !existing.getId().equals(userId)) {
                    return Result.error("邮箱已存在");
                }
                // 先查重再消耗验证码：邮箱不可用时不应烧掉用户一次验证码机会
                String verificationCode = profile.get("verificationCode");
                if (verificationCode == null || verificationCode.trim().isEmpty()) {
                    return Result.error("修改邮箱需要验证码，请先获取验证码");
                }
                if (!verificationCodeService.verifyCode(newEmail, verificationCode)) {
                    return Result.error("验证码错误或已过期");
                }
                user.setEmail(newEmail);
            }
        }
        if (password != null && !password.isEmpty()) {
            if (password.length() < 6 || password.length() > 20) {
                return Result.error("密码长度必须在6-20个字符之间");
            }
            user.setPassword(password);
        }

        boolean updated = userService.updateById(user);
        return updated ? Result.success("用户信息更新成功", true) : Result.error("用户信息更新失败");
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        if (userId != null) {
            // 清空会话令牌，使当前 token 立即失效
            userService.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                    .eq("id", userId)
                    .set("session_token", null));
        }
        return Result.success("登出成功", true);
    }

    @PostMapping("/send-email-change-code")
    public Result<Boolean> sendEmailChangeCode(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        email = email.trim().toLowerCase();

        // 新邮箱已被占用时立即拒绝，避免用户等邮件、填验证码后才发现不可用
        if (userService.findByEmail(email) != null) {
            return Result.error("该邮箱已被其他账号使用");
        }

        String code = verificationCodeService.generateCode(email);
        boolean sent = emailService.sendVerificationCode(email, code, "邮箱变更验证");
        if (!sent) {
            log.warn("[邮箱变更验证] SMTP 发送失败 - 邮箱: {}", email.trim());
            return Result.error("验证码发送失败，请稍后重试");
        }
        logUtil.logOperation("管理", "发送邮箱变更验证码 - 邮箱: " + email.trim(), httpRequest);

        return Result.success("验证码已发送，请查收邮箱", true);
    }

    @PostMapping("/send-register-code")
    public Result<Boolean> sendRegisterCode(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        email = email.trim().toLowerCase();

        // 邮箱已注册时立即拒绝，避免用户走完整个注册流程后才在提交时发现重复
        if (userService.findByEmail(email) != null) {
            return Result.error("该邮箱已被其他账号使用");
        }

        String code = verificationCodeService.generateCode(email);
        boolean sent = emailService.sendVerificationCode(email, code, "用户注册验证");
        if (!sent) {
            log.warn("[用户注册验证] SMTP 发送失败 - 邮箱: {}", email.trim());
            return Result.error("验证码发送失败，请稍后重试");
        }
        logUtil.logOperation("管理", "发送用户注册验证码 - 邮箱: " + email.trim(), httpRequest);

        return Result.success("验证码已发送，请查收邮箱", true);
    }

    @PostMapping("/send-reset-code")
    public Result<Boolean> sendResetCode(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        email = email.trim().toLowerCase();

        User user = userService.findByEmail(email);
        if (user == null) {
            return Result.error("该邮箱对应的用户不存在");
        }

        String code = verificationCodeService.generateCode(email);
        boolean sent = emailService.sendVerificationCode(email, code, "用户密码重置");
        if (!sent) {
            log.warn("[用户密码重置] SMTP 发送失败 - 邮箱: {}", email);
            return Result.error("验证码发送失败，请稍后重试");
        }
        logUtil.logOperation("管理", "发送用户密码重置验证码 - 邮箱: " + email, httpRequest);

        return Result.success("验证码已发送，请查收邮箱", true);
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody Map<String, String> resetRequest, HttpServletRequest request) {
        String email = resetRequest.get("email");
        String verificationCode = resetRequest.get("verificationCode");
        String newPassword = resetRequest.get("newPassword");

        if (email != null) {
            email = email.trim().toLowerCase();
        }

        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return Result.error("请输入验证码");
        }

        // 用户不存在时先拒绝，避免一次性验证码被 verifyCode 白白消费
        User user = userService.findByEmail(email);
        if (user == null) {
            return Result.error("该邮箱对应的用户不存在");
        }

        // 参数校验全部前置到 verifyCode 之前，避免一次性验证码被白白消费
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("密码长度必须在6-20个字符之间");
        }

        if (!verificationCodeService.verifyCode(email, verificationCode)) {
            return Result.error("验证码错误或已过期");
        }

        user.setPassword(newPassword);
        boolean updated = userService.updateById(user);

        if (updated) {
            logUtil.logOperation("管理", "用户通过验证码重置密码 - 邮箱: " + email, request);
            return Result.success("密码重置成功", true);
        } else {
            return Result.error("密码重置失败");
        }
    }

    // 只接收 email/password/verificationCode 三个字段，防止客户端塞入 role/registeredAt 等任意字段
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = body.get("email");
        String password = body.get("password");
        String verificationCode = body.get("verificationCode");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        // 邮箱统一小写存储，避免大小写重复注册
        email = email.trim().toLowerCase();
        // 邮箱已存在时先拒绝，避免一次性验证码被 verifyCode 白白消费
        if (userService.findByEmail(email) != null) {
            return Result.error("邮箱已存在");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 20) {
            return Result.error("密码长度必须在6-20个字符之间");
        }
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return Result.error("请输入验证码");
        }
        if (!verificationCodeService.verifyCode(email, verificationCode)) {
            return Result.error("验证码错误或已过期");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        // 用户名由服务端从邮箱前缀生成，不接受客户端指定
        String emailPrefix = email.split("@")[0];
        if (emailPrefix.length() > 6) {
            emailPrefix = emailPrefix.substring(0, 6);
        }
        user.setUsername(emailPrefix + (System.currentTimeMillis() % 10000));
        if (userService.findByUsername(user.getUsername()) != null) {
            return Result.error("用户名已存在");
        }

        // 强制新用户为普通用户角色，禁止客户端指定 role 提权
        user.setRole("user");
        // 强制新用户为激活状态
        user.setStatus("active");

        user.setRegisteredAt(LocalDateTime.now());
        String regIp = LogUtil.getClientIpAddress(request);
        user.setRegisterIp(regIp);
        user.setRegisterRegion(ipRegionUtil.getCityInfo(regIp));

        boolean saved = userService.save(user);

        if (saved) {
            return Result.success("注册成功", true);
        } else {
            return Result.error("注册失败");
        }
    }
}
