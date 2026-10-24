package com.leafboss.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.dto.BatchImportRequest;
import com.leafboss.dto.CardKeyActionRequest;
import com.leafboss.dto.CardKeyDTO;
import com.leafboss.dto.CardKeyStatusRequest;
import com.leafboss.entity.*;
import com.leafboss.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/card-keys")
public class CardKeyController {

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private AgentAuthorizationService agentAuthorizationService;

    @Autowired
    private SpecificationService specificationService;

    @GetMapping("/with-details")
    public Result getCardKeysWithDetails(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) Integer specId,
            HttpServletRequest request) {

        // 数据隔离：非 admin 用户只能查看自己的卡密
        String currentUserId = (String) request.getAttribute("currentUserId");
        String currentRole = (String) request.getAttribute("currentUserRole");
        User currentUser = null;
        if (currentUserId != null) {
            currentUser = userService.getById(currentUserId);
        }
        // admin 不在 users 表中，不需要数据隔离
        boolean isAdmin = "admin".equals(currentRole);
        if (!isAdmin && currentUser == null) {
            return Result.error("用户不存在");
        }
        if (!isAdmin) {
            agentId = currentUserId;
        }

        IPage<CardKeyDTO> pageData = cardKeyService.getCardKeyListWithDetails(
                new Page<>(page, size), keyword, specId != null ? specId.longValue() : null, status, agentId);
        return Result.success(pageData);
    }

    @GetMapping("/agents")
    public Result getAgents() {
        List<User> agents = userService.lambdaQuery()
                .eq(User::getRole, "agent")
                .list();
        return Result.success(agents);
    }

    @GetMapping("/remaining-generate-count")
    public Result getRemainingGenerateCount(HttpServletRequest request,
            @RequestParam(required = false) Integer productId) {
        String userId = (String) request.getAttribute("currentUserId");
        String currentRole = (String) request.getAttribute("currentUserRole");
        // admin 不在 users 表中，无需查询
        if ("admin".equals(currentRole)) {
            return Result.success(Map.of("remaining", 0));
        }
        User currentUser = userId != null ? userService.getById(userId) : null;
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        // 按商品授权余额返回
        if (productId != null) {
            AgentAuthorization authorization = agentAuthorizationService.findActiveAuthorization(currentUser.getId(), productId);
            return Result.success(Map.of("remaining", authorization != null ? authorization.getRemainingCount() : 0));
        }

        return Result.success(Map.of("remaining", 0));
    }

    /** 批量导入卡密：一次请求导入整批 */
    @PostMapping("/batch")
    public Result batchCreateCardKeys(@RequestBody BatchImportRequest req, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String currentRole = (String) request.getAttribute("currentUserRole");
        boolean isAdmin = "admin".equals(currentRole);
        return Result.success(cardKeyService.batchImport(req, userId, isAdmin));
    }

    @PostMapping("/status")
    public Result toggleCardKeyStatus(@RequestBody CardKeyStatusRequest params) {
        cardKeyService.updateStatus(params.getCardKey(), params.getStatus());
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/by-card-key/{cardKey}")
    public Result deleteCardKeyByCardKey(@PathVariable String cardKey) {
        cardKeyService.deleteByCardKey(cardKey);
        return Result.success("删除成功");
    }

    // ---- 代理商操作自己的卡密 ----

    @PostMapping("/agent/revoke")
    public Result agentRevokeCardKey(@RequestBody CardKeyActionRequest params, HttpServletRequest request) {
        cardKeyService.agentRevoke(params.getCardKey(), (String) request.getAttribute("currentUserId"));
        return Result.success("已吊销");
    }

    @PostMapping("/agent/restore")
    public Result agentRestoreCardKey(@RequestBody CardKeyActionRequest params, HttpServletRequest request) {
        cardKeyService.agentRestore(params.getCardKey(), (String) request.getAttribute("currentUserId"));
        return Result.success("已恢复");
    }

    @DeleteMapping("/agent/by-card-key/{cardKey}")
    public Result agentDeleteCardKey(@PathVariable String cardKey, HttpServletRequest request) {
        cardKeyService.agentDelete(cardKey, (String) request.getAttribute("currentUserId"));
        return Result.success("删除成功");
    }

    @PostMapping("/verify")
    public Result verifyCardKey(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String cardKey = params.get("cardKey");
        if (cardKey == null || cardKey.isEmpty()) {
            return Result.error("卡密不能为空");
        }

        CardKey key = cardKeyService.lambdaQuery()
                .eq(CardKey::getCardKey, cardKey)
                .one();

        if (key == null) {
            return Result.error("卡密不存在");
        }

        // 敏感信息脱敏：仅 admin、卡密所属代理商、买家本人可见邮箱
        String currentUserId = (String) request.getAttribute("currentUserId");
        String currentRole = (String) request.getAttribute("currentUserRole");
        boolean isAdmin = "admin".equals(currentRole);
        boolean isOwnerAgent = currentUserId != null && currentUserId.equals(key.getAgentId());
        User currentUser = (!isAdmin && currentUserId != null) ? userService.getById(currentUserId) : null;
        boolean isBuyer = currentUser != null && key.getUserEmail() != null
                && key.getUserEmail().equals(currentUser.getEmail());
        boolean maskContact = !isAdmin && !isOwnerAgent && !isBuyer;

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("cardKey", key.getCardKey());
        result.put("status", key.getStatus());
        result.put("specificationId", key.getSpecificationId());
        result.put("specificationName", null);
        result.put("productName", null);
        result.put("productSpec", null);
        result.put("userEmail", maskContact ? null : key.getUserEmail());
        result.put("activateTime", key.getActivateTime());
        result.put("expireTime", key.getExpireTime() != null ? key.getExpireTime().toString() : null);
        result.put("createdAt", key.getCreatedAt() != null ? key.getCreatedAt().toString().replace("T", " ") : null);
        result.put("price", null);

        Specification spec = specificationService.getById(key.getSpecificationId());
        if (spec != null) {
            result.put("specName", spec.getName());
            result.put("specificationName", spec.getName());
            result.put("productSpec", spec.getName());
            result.put("price", spec.getPrice());
            Product product = productService.getById(spec.getProductId());
            if (product != null) {
                result.put("productName", product.getName());
            }
        }

        // Agent info (agents are stored in users table with role=agent)
        if (key.getAgentId() != null && !key.getAgentId().isEmpty()) {
            result.put("agentId", key.getAgentId());
            User agent = userService.getById(key.getAgentId());
            if (agent != null && !maskContact) {
                result.put("agentUsername", agent.getUsername());
                result.put("agentEmail", agent.getEmail());
            }
        }

        return Result.success(result);
    }

    /**
     * 兑换卡密：激活卡密并创建用户商品授权
     */
    @PostMapping("/redeem")
    public Result redeemCardKey(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String cardKey = params.get("cardKey");
        if (cardKey == null || cardKey.isEmpty()) {
            return Result.error("卡密不能为空");
        }

        String userId = (String) request.getAttribute("currentUserId");
        if (userId == null) {
            return Result.error("未登录");
        }

        Map<String, Object> result = cardKeyService.redeemCardKey(cardKey, userId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }
}
