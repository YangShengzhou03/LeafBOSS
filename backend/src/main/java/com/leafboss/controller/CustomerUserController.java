package com.leafboss.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.User;
import com.leafboss.service.UserService;
import com.leafboss.utils.IpRegionUtil;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class CustomerUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private IpRegionUtil ipRegionUtil;

    @GetMapping
    public Result<Object> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        
        Page<User> pageInfo = new Page<>(page, size);
        Page<User> result = userService.page(pageInfo, keyword, status, role);
        
        return Result.success("用户列表查询成功", java.util.Map.of(
            "page", result.getCurrent(),
            "size", result.getSize(),
            "total", result.getTotal(),
            "records", result.getRecords()
        ));
    }

    @PostMapping
    public Result<Boolean> createUser(@RequestBody User user, HttpServletRequest request) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 20) {
            return Result.error("密码长度必须在6-20个字符之间");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            String emailPrefix = user.getEmail() != null ? user.getEmail().split("@")[0] : "user";
            user.setUsername(emailPrefix + "_" + (System.currentTimeMillis() % 10000));
        }
        if (userService.findByUsername(user.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        if (user.getEmail() != null && userService.findByEmail(user.getEmail()) != null) {
            return Result.error("邮箱已存在");
        }
        
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("active");
        }

        // 禁止通过用户接口创建管理员角色
        if ("admin".equals(user.getRole())) {
            return Result.error("无权创建管理员角色");
        }
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("user");
        }
        
        user.setRegisteredAt(java.time.LocalDateTime.now());
        String regIp = LogUtil.getClientIpAddress(request);
        user.setRegisterIp(regIp);
        user.setRegisterRegion(ipRegionUtil.getCityInfo(regIp));
        boolean saved = userService.save(user);
        if (saved) {
            logUtil.logOperation("管理", "创建用户: " + user.getUsername(), request);
            return Result.success("用户创建成功", true);
        } else {
            return Result.error("用户创建失败");
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateUser(@PathVariable String id, @RequestBody User user, HttpServletRequest request) {
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            return Result.error("用户不存在");
        }
        
        // 检查用户名是否已存在
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            if (userService.findByUsername(user.getUsername()) != null) {
                return Result.error("用户名已存在");
            }
        }
        
        // 检查邮箱是否已存在
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            if (userService.findByEmail(user.getEmail()) != null) {
                return Result.error("邮箱已存在");
            }
        }
        
        // 禁止通过用户接口提升为管理员角色
        if ("admin".equals(user.getRole())) {
            return Result.error("无权设置为管理员角色");
        }

        // 只更新提供的字段，避免覆盖其他字段
        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (user.getPassword().length() < 6 || user.getPassword().length() > 20) {
                return Result.error("密码长度必须在6-20个字符之间");
            }
            existingUser.setPassword(user.getPassword());
        }
        
        boolean updated = userService.updateById(existingUser);
        if (updated) {
            logUtil.logOperation("管理", "更新用户: " + existingUser.getUsername(), request);
            return Result.success("用户更新成功", true);
        } else {
            return Result.error("用户更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(@PathVariable String id, HttpServletRequest request) {
        User user = userService.getById(id);
        boolean deleted = userService.removeById(id);
        if (deleted) {
            logUtil.logOperation("管理", "删除用户: " + (user != null ? user.getUsername() : id), request);
            return Result.success("用户删除成功", true);
        } else {
            return Result.error("用户删除失败");
        }
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody java.util.Map<String, String> resetRequest, HttpServletRequest request) {
        String email = resetRequest.get("email");
        String newPassword = resetRequest.get("newPassword");
        
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error("新密码不能为空");
        }

        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("密码长度必须在6-20个字符之间");
        }
        
        User user = userService.findByEmail(email);
        if (user == null) {
            return Result.error("该邮箱对应的用户不存在");
        }
        
        user.setPassword(newPassword);
        boolean updated = userService.updateById(user);
        
        if (updated) {
            logUtil.logOperation("管理", "重置用户密码 - 邮箱: " + email, request);
            return Result.success("密码重置成功", true);
        } else {
            return Result.error("密码重置失败");
        }
    }
}
