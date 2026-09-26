package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.Admin;
import com.leafboss.service.AdminService;
import com.leafboss.service.EmailService;
import com.leafboss.service.VerificationCodeService;
import com.leafboss.utils.IpRegionUtil;
import com.leafboss.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private IpRegionUtil ipRegionUtil;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private EmailService emailService;

    @PostMapping
    public Result<Boolean> createAdmin(@RequestBody Admin admin, HttpServletRequest request) {
        if (adminService.findByEmail(admin.getEmail()) != null) {
            return Result.error("邮箱已存在");
        }

        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
            admin.setUsername("leafAdmin");
        }

        if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
            admin.setPassword("123456");
        }

        if (admin.getStatus() == null || admin.getStatus().trim().isEmpty()) {
            admin.setStatus("active");
        }

        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        String regIp = LogUtil.getClientIpAddress(request);
        admin.setRegisterIp(regIp);
        admin.setRegisterRegion(ipRegionUtil.getCityInfo(regIp));

        boolean saved = adminService.save(admin);
        if (saved) {
            logUtil.logUserOperation("管理", "创建管理员账户 - 邮箱: " + admin.getEmail(), request);

            return Result.success("管理员创建成功", true);
        } else {
            return Result.error("管理员创建失败");
        }
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody Map<String, String> resetRequest, HttpServletRequest request) {
        String email = resetRequest.get("email");
        String verificationCode = resetRequest.get("verificationCode");
        String newPassword = resetRequest.get("newPassword");

        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return Result.error("请输入验证码");
        }

        if (!verificationCodeService.verifyCode(email, verificationCode)) {
            return Result.error("验证码错误或已过期");
        }

        return performPasswordReset(email, newPassword, "通过邮箱验证重置密码", request);
    }

    @PostMapping("/admin-reset-password")
    public Result<Boolean> adminResetPassword(@RequestBody Map<String, String> resetRequest, HttpServletRequest request) {
        String email = resetRequest.get("email");
        String newPassword = resetRequest.get("newPassword");

        // 需要操作者自己的密码验证，防止任意管理员重置他人密码
        String operatorPassword = resetRequest.get("operatorPassword");
        if (operatorPassword == null || operatorPassword.trim().isEmpty()) {
            return Result.error("请输入您的密码以确认操作");
        }

        String operatorId = (String) request.getAttribute("currentUserId");
        Admin operator = operatorId != null ? adminService.getById(operatorId) : null;
        if (operator == null || !operatorPassword.equals(operator.getPassword())) {
            return Result.error("操作者密码验证失败");
        }

        return performPasswordReset(email, newPassword, "管理员重置其他管理员密码（已验证）", request);
    }

    private Result<Boolean> performPasswordReset(String email, String newPassword, String logAction, HttpServletRequest request) {
        Admin admin = adminService.findByEmail(email);
        if (admin == null) {
            return Result.error("该邮箱对应的管理员不存在");
        }

        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("密码长度必须在6-20个字符之间");
        }
        admin.setPassword(newPassword);
        boolean updated = adminService.updateById(admin);

        if (updated) {
            logUtil.logUserOperation("管理", logAction + " - 邮箱: " + email, request);
            return Result.success("密码重置成功", true);
        } else {
            return Result.error("密码重置失败");
        }
    }

    @PostMapping("/send-reset-code")
    public Result<Boolean> sendResetCode(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }

        Admin admin = adminService.findByEmail(email);
        if (admin == null) {
            return Result.error("该邮箱对应的管理员不存在");
        }

        String code = verificationCodeService.generateCode(email);
        boolean sent = emailService.sendVerificationCode(email, code, "管理员密码重置");
        // 邮件失败时降级为控制台日志，保证流程可用
        if (!sent) {
            log.warn("[管理员密码重置] SMTP 发送失败，验证码降级输出 - 邮箱: {} 验证码: {}", email, code);
        }
        logUtil.logUserOperation("管理", "发送管理员密码重置验证码 - 邮箱: " + email, httpRequest);

        return Result.success("验证码已发送，请查收邮箱", true);
    }

    @GetMapping
    public Result<Object> getAdmins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Admin> pageInfo =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Admin> result = adminService.page(pageInfo, keyword, status);

        return Result.success("管理员列表查询成功", Map.of(
            "page", result.getCurrent(),
            "size", result.getSize(),
            "total", result.getTotal(),
            "records", result.getRecords()
        ));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAdmin(@PathVariable String id, HttpServletRequest request) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }

        boolean deleted = adminService.removeById(id);

        if (deleted) {
            logUtil.logUserOperation("管理", "删除管理员 - " + admin.getEmail(), request);
            return Result.success("管理员删除成功", true);
        } else {
            return Result.error("管理员删除失败");
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateAdmin(@PathVariable String id, @RequestBody Admin adminData, HttpServletRequest request) {
        try {
            Admin existingAdmin = adminService.getById(id);
            if (existingAdmin == null) {
                return Result.error("管理员不存在");
            }

            // 只更新提供的字段，避免覆盖其他字段
            if (adminData.getUsername() != null) {
                existingAdmin.setUsername(adminData.getUsername());
            }
            if (adminData.getEmail() != null) {
                existingAdmin.setEmail(adminData.getEmail());
            }
            if (adminData.getStatus() != null) {
                existingAdmin.setStatus(adminData.getStatus());
            }
            if (adminData.getPassword() != null && !adminData.getPassword().isEmpty()) {
                if (adminData.getPassword().length() < 6 || adminData.getPassword().length() > 20) {
                    return Result.error("密码长度必须在6-20个字符之间");
                }
                existingAdmin.setPassword(adminData.getPassword());
            }

            boolean updated = adminService.updateById(existingAdmin);

            if (updated) {
                logUtil.logUserOperation("管理", "更新管理员信息 - " + existingAdmin.getEmail(), request);
                return Result.success("管理员更新成功", true);
            } else {
                return Result.error("管理员更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新管理员信息时发生错误: " + e.getMessage());
        }
    }
}