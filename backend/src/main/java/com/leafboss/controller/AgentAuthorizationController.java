package com.leafboss.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.AgentAuthorization;
import com.leafboss.service.AgentAuthorizationService;
import com.leafboss.service.ProductService;
import com.leafboss.service.UserService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent-authorizations")
public class AgentAuthorizationController {

    @Autowired
    private AgentAuthorizationService agentAuthorizationService;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    /**
     * 获取当前代理商已授权的商品列表（代理商端使用）
     */
    @GetMapping("/products")
    public Result<List<Map<String, Object>>> getAuthorizedProducts(HttpServletRequest request) {
        String agentId = (String) request.getAttribute("currentUserId");
        if (agentId == null) {
            return Result.error("未登录");
        }
        List<Map<String, Object>> products = agentAuthorizationService.findAuthorizedProducts(agentId);
        return Result.success("查询成功", products);
    }

    /**
     * 管理员：分页查询代理授权列表
     */
    @GetMapping
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Page<AgentAuthorization> pageParam = new Page<>(page, size);
        return Result.success(agentAuthorizationService.listWithDetails(pageParam, keyword, status));
    }

    /**
     * 管理员：创建代理授权
     */
    @PostMapping
    public Result<Boolean> create(@RequestBody AgentAuthorization auth, HttpServletRequest request) {
        if (auth.getAgentId() == null || auth.getAgentId().trim().isEmpty()) {
            return Result.error("代理商不能为空");
        }
        if (auth.getProductId() == null) {
            return Result.error("商品不能为空");
        }
        if (auth.getRemainingCount() == null || auth.getRemainingCount() <= 0) {
            return Result.error("余额必须大于0");
        }
        boolean saved = agentAuthorizationService.createAuthorization(auth);
        if (saved) {
            String agentName = userService.getById(auth.getAgentId()) != null ? userService.getById(auth.getAgentId()).getUsername() : auth.getAgentId();
            String productName = productService.getById(auth.getProductId()) != null ? productService.getById(auth.getProductId()).getName() : "#" + auth.getProductId();
            logUtil.logOperation("代理授权", "创建 - 代理:" + agentName + " 商品:" + productName + " 余额:" + auth.getRemainingCount(), request);
            return Result.success("代理授权创建成功", true);
        }
        return Result.error("代理授权创建失败");
    }

    /**
     * 管理员：更新代理授权
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Integer id, @RequestBody AgentAuthorization auth, HttpServletRequest request) {
        if (agentAuthorizationService.getById(id) == null) {
            return Result.error("代理授权不存在");
        }
        auth.setId(id);
        if (auth.getRemainingCount() != null && auth.getRemainingCount() < 0) {
            return Result.error("余额不能为负数");
        }
        boolean updated = agentAuthorizationService.updateAuthorization(auth);
        if (updated) {
            AgentAuthorization existing = agentAuthorizationService.getById(id);
            String agentName = existing != null && userService.getById(existing.getAgentId()) != null ? userService.getById(existing.getAgentId()).getUsername() : "#" + id;
            logUtil.logOperation("代理授权", "更新 - " + agentName + " 余额:" + auth.getRemainingCount(), request);
            return Result.success("代理授权更新成功", true);
        }
        return Result.error("代理授权更新失败");
    }

    /**
     * 管理员：吊销代理授权
     */
    @PutMapping("/{id}/revoke")
    public Result<Boolean> revoke(@PathVariable Integer id, HttpServletRequest request) {
        AgentAuthorization auth = agentAuthorizationService.getById(id);
        boolean ok = agentAuthorizationService.revoke(id);
        if (ok) {
            String agentName = auth != null && userService.getById(auth.getAgentId()) != null ? userService.getById(auth.getAgentId()).getUsername() : "#" + id;
            String productName = auth != null && productService.getById(auth.getProductId()) != null ? productService.getById(auth.getProductId()).getName() : "";
            logUtil.logOperation("代理授权", "吊销 - " + agentName + " " + productName, request);
            return Result.success("代理授权已吊销", true);
        }
        return Result.error("吊销失败，授权不存在");
    }

    /**
     * 管理员：恢复代理授权
     */
    @PutMapping("/{id}/restore")
    public Result<Boolean> restore(@PathVariable Integer id, HttpServletRequest request) {
        AgentAuthorization auth = agentAuthorizationService.getById(id);
        boolean ok = agentAuthorizationService.restore(id);
        if (ok) {
            String agentName = auth != null && userService.getById(auth.getAgentId()) != null ? userService.getById(auth.getAgentId()).getUsername() : "#" + id;
            String productName = auth != null && productService.getById(auth.getProductId()) != null ? productService.getById(auth.getProductId()).getName() : "";
            logUtil.logOperation("代理授权", "恢复 - " + agentName + " " + productName, request);
            return Result.success("代理授权已恢复", true);
        }
        return Result.error("恢复失败，授权不存在");
    }

    /**
     * 管理员：删除代理授权
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id, HttpServletRequest request) {
        AgentAuthorization auth = agentAuthorizationService.getById(id);
        boolean ok = agentAuthorizationService.delete(id);
        if (ok) {
            String agentName = auth != null && userService.getById(auth.getAgentId()) != null ? userService.getById(auth.getAgentId()).getUsername() : "#" + id;
            String productName = auth != null && productService.getById(auth.getProductId()) != null ? productService.getById(auth.getProductId()).getName() : "";
            logUtil.logOperation("代理授权", "删除 - " + agentName + " " + productName, request);
            return Result.success("代理授权删除成功", true);
        }
        return Result.error("删除失败，授权不存在");
    }
}
