package com.leafboss.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.UserProduct;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.UserProductService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user-products")
public class UserProductController {

    @Autowired
    private UserProductService userProductService;

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private LogUtil logUtil;

    @GetMapping
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<UserProduct> pageParam = new Page<>(page, size);
        return Result.success(userProductService.getUserProductListWithDetails(pageParam, keyword, status));
    }

    @GetMapping("/user/me")
    public Result<?> getCurrentUserProducts(HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        if (userId == null) {
            return Result.error("未登录");
        }
        Page<UserProduct> pageParam = new Page<>(1, 100);
        // 只返回已授权（status=1）的商品
        return Result.success(userProductService.getUserProductListWithDetails(pageParam, null, 1, userId).getRecords());
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody UserProduct userProduct, HttpServletRequest request) {
        if (userProduct.getUserId() == null || userProduct.getProductId() == null) {
            return Result.error("用户和商品不能为空");
        }
        if (userProduct.getExpiresAt() == null) {
            return Result.error("到期时间不能为空");
        }
        if (userProduct.getStatus() == null) {
            userProduct.setStatus(1);
        }
        if (userProduct.getActivatedAt() == null) {
            userProduct.setActivatedAt(LocalDateTime.now());
        }
        boolean saved = userProductService.save(userProduct);
        if (saved) {
            logUtil.logOperation("管理", "创建用户授权", request);
            return Result.success("授权创建成功", true);
        }
        return Result.error("授权创建失败");
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Integer id, @RequestBody UserProduct userProduct, HttpServletRequest request) {
        UserProduct existing = userProductService.getById(id);
        if (existing == null) {
            return Result.error("授权不存在");
        }

        // 只更新提供的字段
        if (userProduct.getSpecId() != null) {
            existing.setSpecId(userProduct.getSpecId());
        }
        if (userProduct.getCardKey() != null) {
            existing.setCardKey(userProduct.getCardKey());
        }
        if (userProduct.getExpiresAt() != null) {
            existing.setExpiresAt(userProduct.getExpiresAt());
        }
        if (userProduct.getStatus() != null) {
            existing.setStatus(userProduct.getStatus());
        }

        boolean updated = userProductService.updateById(existing);
        if (updated) {
            logUtil.logOperation("管理", "更新用户授权", request);
            return Result.success("授权更新成功", true);
        }
        return Result.error("授权更新失败");
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/{id}/revoke")
    public Result revoke(@PathVariable Integer id, HttpServletRequest request) {
        UserProduct existing = userProductService.getById(id);
        if (existing == null) {
            return Result.error("授权不存在");
        }
        existing.setStatus(0);
        boolean updated = userProductService.updateById(existing);

        // 同步更新关联的卡密状态为已吊销
        if (updated && existing.getCardKey() != null) {
            CardKey cardKey = cardKeyService.findByCardKey(existing.getCardKey());
            if (cardKey != null) {
                cardKey.setStatus("已吊销");
                cardKeyService.updateById(cardKey);
            }
        }

        if (updated) {
            logUtil.logOperation("管理", "吊销用户授权", request);
            return Result.success("授权已吊销", true);
        }
        return Result.error("吊销失败");
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/{id}/restore")
    public Result restore(@PathVariable Integer id, HttpServletRequest request) {
        UserProduct existing = userProductService.getById(id);
        if (existing == null) {
            return Result.error("授权不存在");
        }
        existing.setStatus(1);
        boolean updated = userProductService.updateById(existing);

        // 同步恢复关联的卡密状态为已使用
        if (updated && existing.getCardKey() != null) {
            CardKey cardKey = cardKeyService.findByCardKey(existing.getCardKey());
            if (cardKey != null) {
                cardKey.setStatus("已使用");
                cardKeyService.updateById(cardKey);
            }
        }

        if (updated) {
            logUtil.logOperation("管理", "恢复用户授权", request);
            return Result.success("授权已恢复", true);
        }
        return Result.error("恢复失败");
    }

    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id, HttpServletRequest request) {
        UserProduct existing = userProductService.getById(id);
        if (existing == null) {
            return Result.error("授权不存在");
        }

        // 删除前重置关联的卡密状态
        if (existing.getCardKey() != null) {
            CardKey cardKey = cardKeyService.findByCardKey(existing.getCardKey());
            if (cardKey != null) {
                cardKey.setStatus("未使用");
                cardKey.setUserEmail(null);
                cardKey.setActivateTime(null);
                cardKey.setExpireTime(null);
                cardKeyService.updateById(cardKey);
            }
        }

        boolean removed = userProductService.removeById(id);
        if (removed) {
            logUtil.logOperation("管理", "删除用户授权", request);
            return Result.success("授权删除成功", true);
        }
        return Result.error("授权删除失败");
    }

}
