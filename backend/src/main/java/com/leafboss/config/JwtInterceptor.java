package com.leafboss.config;

import com.leafboss.entity.Admin;
import com.leafboss.entity.User;
import com.leafboss.service.AdminService;
import com.leafboss.service.UserService;
import com.leafboss.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 公开路径带 token 时也解析身份，供可选认证的接口（如 /user/me）使用
        parseTokenIfPresent(request, response);

        // 白名单：不需要认证的接口
        if (isPublicPath(uri, method)) {
            return true;
        }

        if (request.getAttribute("currentUserId") == null) {
            return reject(response, "未登录或登录已过期，请重新登录");
        }

        // 允许任何登录用户访问（不限于 admin）
        if (isUserAccessiblePath(uri)) {
            return true;
        }

        // 管理接口要求 admin 角色
        if (isAdminOnlyPath(uri, method) && !"admin".equals(request.getAttribute("currentUserRole"))) {
            return reject(response, "无权限执行此操作");
        }

        return true;
    }

    private void parseTokenIfPresent(HttpServletRequest request, HttpServletResponse response) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return;
        }
        try {
            String token = authorization.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return;
            }
            String userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            String sid = jwtUtil.getSessionTokenFromToken(token);

            // 单设备登录校验：比对 session_token
            if (sid != null && !isSessionValid(userId, role, sid)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"账号已在其他设备登录，请重新登录\"}");
                return;
            }

            // 滑动续期
            if (jwtUtil.shouldRenew(token)) {
                response.setHeader("New-Token", jwtUtil.renewToken(token));
            }
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUserRole", role);
        } catch (Exception ignored) {
            // 无效 token 按未登录处理，由主流程拒绝
        }
    }

    /**
     * 校验 session_token 是否匹配当前数据库中的值
     */
    private boolean isSessionValid(String userId, String role, String sid) {
        if ("admin".equals(role)) {
            Admin admin = adminService.getById(userId);
            return admin != null && sid.equals(admin.getSessionToken());
        } else {
            User user = userService.getById(userId);
            return user != null && sid.equals(user.getSessionToken());
        }
    }

    private boolean isPublicPath(String uri, String method) {
        // 登录和注册
        if ("POST".equals(method)) {
            if (uri.endsWith("/api/auth/login") ||
                uri.endsWith("/api/auth/register") ||
                uri.endsWith("/api/user-auth/login") ||
                uri.endsWith("/api/user-auth/register") ||
                // 登录页自助重置密码（安全由一次性验证码保证）
                uri.endsWith("/api/user-auth/send-reset-code") ||
                uri.endsWith("/api/user-auth/send-register-code") ||
                uri.endsWith("/api/user-auth/reset-password") ||
                uri.endsWith("/api/admins/send-reset-code") ||
                uri.endsWith("/api/admins/reset-password")) {
                return true;
            }
        }
        // 公开查询接口（无需任何认证）
        if ("GET".equals(method)) {
            if (uri.startsWith("/api/specifications") ||
                uri.startsWith("/api/companies") ||
                uri.startsWith("/api/notices/public") ||
                uri.startsWith("/api/products") ||
                uri.startsWith("/api/card-keys/verify") ||
                uri.startsWith("/api/boss-reviews") ||
                uri.startsWith("/api/review-votes")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 仅管理员可访问的接口（写操作的管理后台接口）
     */
    private boolean isAdminOnlyPath(String uri, String method) {
        // 管理员、用户、授权、日志、仪表盘等管理接口
        if (uri.startsWith("/api/admins") ||
            uri.startsWith("/api/users") ||
            uri.startsWith("/api/user-products") ||
            (uri.startsWith("/api/agent-authorizations") && !uri.startsWith("/api/agent-authorizations/products")) ||
            uri.startsWith("/api/operation-logs") ||
            uri.startsWith("/api/admin")) {
            return true;
        }
        // 卡密接口特殊规则：精确控制每个端点的权限
        if (uri.startsWith("/api/card-keys")) {
            if ("GET".equals(method)) {
                // 以下 GET 端点允许已登录用户访问（控制器内做数据隔离）
                if (uri.startsWith("/api/card-keys/verify") ||
                    uri.startsWith("/api/card-keys/with-details") ||
                    uri.startsWith("/api/card-keys/remaining-generate-count")) {
                    return false;
                }
            }
            if ("POST".equals(method) && uri.equals("/api/card-keys/batch")) {
                return false; // 用户/代理商导入卡密
            }
            // 其余 card-keys 端点均限 admin
            return true;
        }
        // 商品/规格/公司的写操作（GET 为公开查询）
        if ("GET".equals(method) || "OPTIONS".equals(method)) {
            return false;
        }
        if (uri.startsWith("/api/products") ||
            uri.startsWith("/api/specifications") ||
            uri.startsWith("/api/companies") ||
            uri.startsWith("/api/boss-reviews")) {
            return true;
        }
        // 通知管理接口仅管理员可操作
        if (uri.startsWith("/api/notices")) {
            return true;
        }
        return false;
    }

    /**
     * 任何登录用户均可访问（无需 admin 角色）
     */
    private boolean isUserAccessiblePath(String uri) {
        // 用户查看自己的商品授权
        if (uri.startsWith("/api/user-products/user/me")) {
            return true;
        }
        // 卡密验证（用户/代理商均可）
        if (uri.equals("/api/card-keys/verify")) {
            return true;
        }
        // 卡密兑换（登录用户可兑换）
        if (uri.equals("/api/card-keys/redeem")) {
            return true;
        }
        return false;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
