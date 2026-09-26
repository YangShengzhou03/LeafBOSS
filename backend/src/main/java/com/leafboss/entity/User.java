package com.leafboss.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@TableName("users")
public class User {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    @TableField("username")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @TableField("email")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @TableField("password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @TableField(exist = false)
    private String verificationCode;

    @TableField("role")
    private String role;
    
    @TableField("status")
    private String status;

    @TableField("session_token")
    private String sessionToken;
    
    @TableField("registered_at")
    private LocalDateTime registeredAt;

    @TableField("register_ip")
    private String registerIp;

    @TableField("register_region")
    private String registerRegion;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("last_login_region")
    private String lastLoginRegion;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getRegisterIp() {
        return registerIp;
    }

    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getRegisterRegion() {
        return registerRegion;
    }

    public void setRegisterRegion(String registerRegion) {
        this.registerRegion = registerRegion;
    }

    public String getLastLoginRegion() {
        return lastLoginRegion;
    }

    public void setLastLoginRegion(String lastLoginRegion) {
        this.lastLoginRegion = lastLoginRegion;
    }
}
