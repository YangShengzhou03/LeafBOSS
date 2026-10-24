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

        // token 中的 session_token 与数据库不匹配（已在 parseTokenIfPresent 中写响应）
        if (Boolean.TRUE.equals(request.getAttribute("sessionRejected"))) {
            return false;
        }

        if (request.getAttribute("currentUserId") == null) {
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期，请重新登录");
        }

        // fail-closed：管理员全放行；非管理员仅允许显式白名单，其余一律拒绝
        if ("admin".equals(request.getAttribute("currentUserRole"))) {
            return true;
        }
        if (isUserAllowedPath(uri, method)) {
            return true;
        }
        return reject(response, HttpServletResponse.SC_FORBIDDEN, "无权限执行此操作");
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
                request.setAttribute("sessionRejected", Boolean.TRUE);
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
        return false;
    }

    /**
     * 非管理员用户可访问的路径白名单（fail-closed：未列出的路径一律拒绝）
     */
    private boolean isUserAllowedPath(String uri, String method) {
        // 登录用户的个人中心（/me、/logout、换邮箱验证码等）
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/user-auth/")) {
            return true;
        }
        // 公开只读资源（登录后可访问）
        // 商品/规格/公司/反馈的只读查询
        if ("GET".equals(method) && (uri.startsWith("/api/products") ||
                uri.startsWith("/api/specifications") ||
                uri.startsWith("/api/companies") ||
                uri.startsWith("/api/feedbacks"))) {
            return true;
        }
        // 公司评论：浏览/发表/点赞/删除自己的评论（控制器内做归属校验）
        if (uri.startsWith("/api/v1/company-reviews")) {
            return true;
        }
        // 代理商可查询自己的授权商品
        if (uri.startsWith("/api/agent-authorizations/products")) {
            return true;
        }
        // 用户查看自己的商品授权
        if (uri.equals("/api/user-products/user/me")) {
            return true;
        }
        // 卡密精确规则：白名单之外全部拒绝
        if (uri.startsWith("/api/card-keys")) {
            if ("GET".equals(method) && (uri.startsWith("/api/card-keys/verify") ||
                    uri.startsWith("/api/card-keys/with-details") ||
                    uri.startsWith("/api/card-keys/remaining-generate-count"))) {
                return true;
            }
            if ("POST".equals(method) && (uri.equals("/api/card-keys/verify") ||
                    uri.equals("/api/card-keys/batch") ||
                    uri.equals("/api/card-keys/redeem") ||
                    uri.equals("/api/card-keys/agent/revoke") ||
                    uri.equals("/api/card-keys/agent/restore"))) {
                return true;
            }
            if ("DELETE".equals(method) && uri.startsWith("/api/card-keys/agent/by-card-key/")) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
        return false;
    }
}
