package com.leafboss.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.dto.CardKeyDTO;
import com.leafboss.entity.*;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/card-keys")
public class CardKeyController {

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private SpecificationService specificationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private AgentAuthorizationService agentAuthorizationService;

    @Autowired
    private SpecificationMapper specificationMapper;

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

    /** 批量导入卡密：一次请求导入整批，替代前端逐条调用 POST /api/card-keys */
    @PostMapping("/batch")
    public Result batchCreateCardKeys(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Object specIdObj = params.get("specId");
        Object keysObj = params.get("keys");
        String agentId = (String) params.get("agentId");

        if (!(specIdObj instanceof Number) || ((Number) specIdObj).intValue() <= 0) {
            return Result.error("规格ID不能为空");
        }
        if (!(keysObj instanceof List) || ((List<?>) keysObj).isEmpty()) {
            return Result.error("卡密列表不能为空");
        }
        List<?> keys = (List<?>) keysObj;
        if (keys.size() > 10000) {
            return Result.error("单次最多导入10000个卡密");
        }

        String userId = (String) request.getAttribute("currentUserId");
        String currentRole = (String) request.getAttribute("currentUserRole");
        User currentUser = null;
        if (userId != null) {
            currentUser = userService.getById(userId);
        }

        // 数据隔离：非 admin 用户导入的卡密 agentId 强制设为自己
        boolean isAdmin = "admin".equals(currentRole);
        if (!isAdmin && currentUser != null) {
            agentId = userId;
        }

        int specificationId = ((Number) specIdObj).intValue();

        // 余额制：按授权余额校验并原子扣减
        int importCount = keys.size();
        boolean deducted = false;
        Integer deductedProductId = null;
        if (!isAdmin && currentUser != null && ("user".equals(currentUser.getRole()) || "agent".equals(currentUser.getRole()))) {
            Specification spec = specificationMapper.selectById(specificationId);
            if (spec == null) {
                return Result.error("规格不存在");
            }
            AgentAuthorization authorization = agentAuthorizationService.findActiveAuthorization(userId, spec.getProductId());
            if (authorization == null) {
                return Result.error("未获得该商品的销售授权，请联系管理员");
            }
            int remaining = authorization.getRemainingCount();
            if (remaining <= 0) {
                return Result.error("该商品余额已用完");
            }
            if (importCount > remaining) {
                importCount = remaining;
            }
            if (!agentAuthorizationService.deductRemaining(userId, spec.getProductId(), importCount)) {
                return Result.error("余额不足");
            }
            deducted = true;
            deductedProductId = spec.getProductId();
        }
        LocalDateTime now = LocalDateTime.now();
        List<CardKey> cardKeys = new ArrayList<>();

        for (int i = 0; i < importCount; i++) {
            String key = ((List<?>) keys).get(i).toString();
            if (key.isBlank()) {
                return Result.error("卡密列表包含非法项");
            }
            CardKey cardKey = new CardKey();
            cardKey.setCardKey(key.trim());
            cardKey.setSpecificationId(specificationId);
            cardKey.setAgentId(agentId);
            cardKey.setStatus("未使用");
            cardKey.setCreatedAt(now);
            cardKey.setUpdatedAt(now);
            cardKeys.add(cardKey);
        }

        boolean saved = cardKeyService.saveBatch(cardKeys);
        if (!saved) {
            if (deducted) {
                agentAuthorizationService.refundRemaining(userId, deductedProductId, importCount);
            }
            return Result.error("批量导入失败");
        }

        if (importCount < keys.size()) {
            return Result.success(Map.of(
                    "message", "部分导入成功：额度不足，已导入 " + importCount + " 个，" + (keys.size() - importCount) + " 个被跳过",
                    "imported", importCount,
                    "skipped", keys.size() - importCount
            ));
        }

        return Result.success("批量导入成功，共导入 " + importCount + " 个卡密");
    }

    @PostMapping("/status")
    public Result toggleCardKeyStatus(@RequestBody Map<String, String> params) {
        String cardKey = params.get("cardKey");
        String status = params.get("status");
        if (cardKey == null || cardKey.isEmpty()) {
            return Result.error("卡密不能为空");
        }
        if (status == null || status.isEmpty()) {
            return Result.error("状态不能为空");
        }

        CardKey card = cardKeyService.findByCardKey(cardKey);
        if (card == null) {
            return Result.error("卡密不存在");
        }

        card.setStatus(status);
        boolean updated = cardKeyService.updateById(card);
        return updated ? Result.success("状态更新成功") : Result.error("状态更新失败");
    }

    @DeleteMapping("/by-card-key/{cardKey}")
    public Result deleteCardKeyByCardKey(@PathVariable String cardKey) {
        if (cardKey == null || cardKey.isEmpty()) {
            return Result.error("卡密不能为空");
        }

        CardKey card = cardKeyService.findByCardKey(cardKey);
        if (card == null) {
            return Result.error("卡密不存在");
        }

        boolean deleted = cardKeyService.removeById(card.getId());
        return deleted ? Result.success("删除成功") : Result.error("删除失败");
    }

    @DeleteMapping("/batch-delete-used")
    public Result batchDeleteUsedCardKeys() {
        boolean deleted = cardKeyService.batchDeleteUsedCardKeys();
        return deleted ? Result.success("已使用卡密清空成功") : Result.error("清空已使用卡密失败");
    }

    @PostMapping("/verify")
    public Result verifyCardKey(@RequestBody Map<String, String> params) {
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

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("cardKey", key.getCardKey());
        result.put("status", key.getStatus());
        result.put("specificationId", key.getSpecificationId());
        result.put("specificationName", null);
        result.put("productName", null);
        result.put("productSpec", null);
        result.put("userEmail", key.getUserEmail());
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
            if (agent != null) {
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
