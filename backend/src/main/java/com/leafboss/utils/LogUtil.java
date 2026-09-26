package com.leafboss.utils;

import com.leafboss.service.OperationLogService;
import com.leafboss.service.AdminService;
import com.leafboss.entity.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class LogUtil {
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public void logOperation(String operationType, String description, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        
        String adminInfo = getCurrentAdminInfo(request);
        
        String finalDescription = description;
        if (!description.contains("管理员") && adminInfo != null) {
            finalDescription = adminInfo + " - " + description;
        }
        
        operationLogService.logOperation(operationType, finalDescription, ipAddress);
    }
    
    public void logLogin(boolean success, String description, HttpServletRequest request) {
        logOperation("登录", description, request);
    }

    public void logProductOperation(String operationType, String description, HttpServletRequest request) {
        logOperation(operationType, description, request);
    }
    
    public void logSpecificationOperation(String operationType, String description, HttpServletRequest request) {
        logOperation(operationType, description, request);
    }
    
    public void logUserOperation(String operationType, String description, HttpServletRequest request) {
        logOperation(operationType, description, request);
    }

    private String getCurrentAdminInfo(HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return null;
            }
            
            String token = authorization.substring(7);
            String adminId = jwtUtil.getUserIdFromToken(token);
            
            if (adminId != null) {
                Admin admin = adminService.getById(adminId);
                if (admin != null) {
                    return admin.getUsername() + " (" + admin.getEmail() + ")";
                }
            }
        } catch (Exception e) {
        }
        
        return null;
    }
    
    public static String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    

}