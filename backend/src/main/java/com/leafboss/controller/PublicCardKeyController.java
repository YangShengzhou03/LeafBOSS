package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Specification;
import com.leafboss.entity.Product;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.SpecificationService;
import com.leafboss.service.ProductService;
import com.leafboss.service.OperationLogService;
import com.leafboss.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/public/card-keys")
public class PublicCardKeyController {

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private SpecificationService specificationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/verify/{cardKey}")
    public Result<String> verifyAndActivateInstallationKey(@PathVariable String cardKey, HttpServletRequest request) {
        try {
            CardKey card = cardKeyService.findByCardKey(cardKey);

            if (card == null) {
                operationLogService.logOperation("卡密", "公共接口验证失败 - 卡密不存在: " + cardKey, LogUtil.getClientIpAddress(request));
                return Result.error(404, "卡密不存在，请检查卡密是否正确或获取有效卡密");
            }

            String status = card.getStatus();

            switch (status) {
                case "未使用":
                    boolean activated = cardKeyService.activateCard(cardKey, "system@leafboss.com");
                    if (!activated) {
                        operationLogService.logOperation("卡密", "验证安装卡密失败 - 激活失败: " + cardKey, LogUtil.getClientIpAddress(request));
                        return Result.error(500, "卡密验证成功但使用失败，请联系开发者处理");
                    }

                    Specification spec = specificationService.getById(card.getSpecificationId());
                    if (spec != null) {
                        Product product = productService.getById(spec.getProductId());
                        if (product != null) {
                            String productSpecInfo = product.getName() + "-" + spec.getName();
                            operationLogService.logOperation("卡密", "验证安装卡密成功 - 卡密: " + cardKey + ", 商品规格: " + productSpecInfo, LogUtil.getClientIpAddress(request));
                            return Result.success(productSpecInfo);
                        } else {
                            operationLogService.logOperation("卡密", "验证安装卡密成功但商品不存在 - 卡密: " + cardKey + ", 规格ID: " + spec.getId(), LogUtil.getClientIpAddress(request));
                            return Result.success("未知商品-" + spec.getName());
                        }
                    } else {
                        operationLogService.logOperation("卡密", "验证安装卡密成功但规格不存在 - 卡密: " + cardKey + ", 规格ID: " + card.getSpecificationId(), LogUtil.getClientIpAddress(request));
                        return Result.success("未知商品规格");
                    }

                case "已使用":
                    operationLogService.logOperation("卡密", "验证安装卡密失败 - 卡密已被使用: " + cardKey, LogUtil.getClientIpAddress(request));
                    return Result.error(400, "该卡密已被使用，请确认是否已在其他设备使用");

                case "已禁用":
                    operationLogService.logOperation("卡密", "验证安装卡密失败 - 卡密被禁用: " + cardKey, LogUtil.getClientIpAddress(request));
                    return Result.error(400, "该卡密已被禁用，请联系开发者了解原因");

                default:
                    operationLogService.logOperation("卡密", "验证安装卡密失败 - 卡密状态异常: " + cardKey + ", 状态: " + status, LogUtil.getClientIpAddress(request));
                    return Result.error(400, "卡密状态异常，请联系开发者处理");
            }

        } catch (Exception e) {
            operationLogService.logOperation("卡密", "验证安装卡密过程中发生系统异常: " + cardKey + ", 错误: " + e.getMessage(), LogUtil.getClientIpAddress(request));
            return Result.error(500, "验证过程中发生系统错误，请稍后重试");
        }
    }
}