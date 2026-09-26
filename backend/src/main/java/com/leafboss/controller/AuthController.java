package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.Admin;
import com.leafboss.service.AdminService;
import com.leafboss.utils.IpRegionUtil;
import com.leafboss.utils.JwtUtil;
import com.leafboss.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private IpRegionUtil ipRegionUtil;

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

        Admin admin = adminService.findByEmail(email);
        if (admin != null) {
            if ("inactive".equals(admin.getStatus())) {
                logUtil.logLogin(false, "管理员登录失败 - 邮箱: " + email + " (账号已被禁用)", request);
                return Result.error("账号已被禁用，请联系管理员");
            }

            if (password.equals(admin.getPassword())) {
                admin.setLastLoginTime(LocalDateTime.now());
                String loginIp = LogUtil.getClientIpAddress(request);
                admin.setLastLoginIp(loginIp);
                admin.setLastLoginRegion(ipRegionUtil.getCityInfo(loginIp));
                // 生成新的 session_token，踢掉旧登录
                admin.setSessionToken(UUID.randomUUID().toString().replace("-", ""));
                adminService.updateById(admin);

                String token = jwtUtil.generateToken(admin.getId(), admin.getEmail(), "admin", admin.getSessionToken());

                logUtil.logLogin(true, "管理员登录成功 - 邮箱: " + email, request);

                admin.setPassword(null);
                Map<String, Object> response = Map.of(
                    "token", token,
                    "user", admin
                );
                return Result.success("登录成功", response);
            }
        }

        logUtil.logLogin(false, "管理员登录失败 - 邮箱: " + email + " (密码错误或用户不存在)", request);
        return Result.error("邮箱或密码错误");
    }

    @GetMapping("/me")
    public Result<Admin> getCurrentUser(HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        Admin admin = adminService.getById(userId);

        if (admin != null) {
            admin.setPassword(null);
            return Result.success(admin);
        } else {
            return Result.error("用户不存在");
        }
    }

    @PutMapping("/me")
    public Result<Boolean> updateCurrentUser(HttpServletRequest request, @RequestBody Map<String, String> profile) {
        String userId = (String) request.getAttribute("currentUserId");
        Admin admin = adminService.getById(userId);
        if (admin == null) {
            return Result.error("用户不存在");
        }

        // 字段白名单：仅允许更新 username 和 email
        String username = profile.get("username");
        String email = profile.get("email");

        if (username != null && !username.trim().isEmpty()) {
            Admin existing = adminService.findByUsername(username.trim());
            if (existing != null && !existing.getId().equals(userId)) {
                return Result.error("用户名已存在");
            }
            admin.setUsername(username.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            Admin existing = adminService.findByEmail(email.trim());
            if (existing != null && !existing.getId().equals(userId)) {
                return Result.error("邮箱已存在");
            }
            admin.setEmail(email.trim());
        }

        boolean updated = adminService.updateById(admin);
        if (updated) {
            return Result.success("用户信息更新成功", true);
        } else {
            return Result.error("用户信息更新失败");
        }
    }

    @GetMapping("/storage")
    public Result<Map<String, Object>> getStorageInfo(HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        Admin admin = adminService.getById(userId);

        if (admin != null) {
            Map<String, Object> storageInfo = Map.of(
                "storageQuota", 1073741824L,
                "usedStorage", 104857600L,
                "availableStorage", 1073741824L - 104857600L,
                "usagePercentage", 10
            );
            return Result.success(storageInfo);
        } else {
            return Result.error("用户不存在");
        }
    }

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody Admin admin, HttpServletRequest request) {
        if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (adminService.findByEmail(admin.getEmail()) != null) {
            return Result.error("邮箱已存在");
        }
        if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (admin.getPassword().length() < 6 || admin.getPassword().length() > 20) {
            return Result.error("密码长度必须在6-20个字符之间");
        }

        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
            String emailPrefix = admin.getEmail().split("@")[0];
            admin.setUsername(emailPrefix);
        }

        // 新注册管理员强制为待审核状态，禁止客户端指定 status 直接激活
        admin.setStatus("inactive");

        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        String regIp = LogUtil.getClientIpAddress(request);
        admin.setRegisterIp(regIp);
        admin.setRegisterRegion(ipRegionUtil.getCityInfo(regIp));

        boolean saved = adminService.save(admin);

        if (saved) {
            return Result.success("注册成功", true);
        } else {
            return Result.error("注册失败");
        }
    }

}