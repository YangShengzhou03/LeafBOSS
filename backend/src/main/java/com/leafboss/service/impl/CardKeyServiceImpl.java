package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.dto.CardKeyDTO;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Specification;
import com.leafboss.entity.Product;
import com.leafboss.entity.User;
import com.leafboss.entity.UserProduct;
import com.leafboss.mapper.CardKeyMapper;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.mapper.ProductMapper;
import com.leafboss.mapper.UserMapper;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.UserProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CardKeyServiceImpl extends ServiceImpl<CardKeyMapper, CardKey> implements CardKeyService {

    private static final Logger log = LoggerFactory.getLogger(CardKeyServiceImpl.class);

    @Autowired
    private SpecificationMapper specificationMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProductService userProductService;

    @Override
    public CardKey findByCardKey(String cardKey) {
        QueryWrapper<CardKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("card_key", cardKey);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<CardKeyDTO> getCardKeyListWithDetails(Page<CardKey> pageParam, String keyword, Long specificationId, String status, String agentId) {
        QueryWrapper<CardKey> queryWrapper = new QueryWrapper<>();

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }

        if (specificationId != null) {
            queryWrapper.eq("specification_id", specificationId);
        }

        if (agentId != null && !agentId.trim().isEmpty()) {
            queryWrapper.eq("agent_id", agentId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like("card_key", keyword)
                .or()
                .like("user_email", keyword)
            );
        }

        Page<CardKey> cardKeyPage = baseMapper.selectPage(pageParam, queryWrapper);
        List<CardKey> records = cardKeyPage.getRecords();

        if (records.isEmpty()) {
            Page<CardKeyDTO> resultPage = new Page<>(cardKeyPage.getCurrent(), cardKeyPage.getSize(), cardKeyPage.getTotal());
            resultPage.setRecords(new java.util.ArrayList<>());
            return resultPage;
        }

        // 批量获取规格信息，避免 N+1
        List<Integer> specIds = records.stream()
                .map(CardKey::getSpecificationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Specification> specMap = new HashMap<>();
        Map<Integer, Product> productMap = new HashMap<>();

        if (!specIds.isEmpty()) {
            List<Specification> specs = specificationMapper.selectBatchIds(specIds);
            specMap = specs.stream().collect(Collectors.toMap(Specification::getId, s -> s));

            List<Integer> productIds = specs.stream()
                    .map(Specification::getProductId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!productIds.isEmpty()) {
                List<Product> products = productMapper.selectBatchIds(productIds);
                productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
            }
        }

        Map<Integer, Specification> finalSpecMap = specMap;
        Map<Integer, Product> finalProductMap = productMap;

        // 批量获取使用用户的用户名，避免 N+1
        Map<String, String> usernameMap = new HashMap<>();
        List<String> emails = records.stream()
                .map(CardKey::getUserEmail)
                .filter(e -> e != null && !e.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (!emails.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("email", emails);
            usernameMap = userMapper.selectList(userQuery).stream()
                    .collect(Collectors.toMap(User::getEmail, User::getUsername, (a, b) -> a));
        }
        Map<String, String> finalUsernameMap = usernameMap;

        List<CardKeyDTO> dtoList = records.stream().map(cardKey ->
            convertToDTO(cardKey, finalSpecMap, finalProductMap, finalUsernameMap)
        ).collect(Collectors.toList());

        Page<CardKeyDTO> resultPage = new Page<>(cardKeyPage.getCurrent(), cardKeyPage.getSize(), cardKeyPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    @Override
    public boolean activateCard(String cardKey, String userEmail) {
        CardKey card = findByCardKey(cardKey);
        if (card == null || !"未使用".equals(card.getStatus())) {
            return false;
        }

        card.setStatus("已使用");
        card.setUserEmail(userEmail);
        card.setActivateTime(LocalDateTime.now());

        // Calculate expire time from specification validDays
        if (card.getSpecificationId() != null) {
            Specification spec = specificationMapper.selectById(card.getSpecificationId());
            if (spec != null && spec.getValidDays() != null && spec.getValidDays() > 0) {
                card.setExpireTime(card.getActivateTime().plusDays(spec.getValidDays()));
            }
        }

        return updateById(card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> redeemCardKey(String cardKey, String userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询卡密
        CardKey card = findByCardKey(cardKey);
        if (card == null) {
            result.put("success", false);
            result.put("message", "卡密不存在");
            return result;
        }

        // 2. 检查卡密状态
        if (!"未使用".equals(card.getStatus())) {
            result.put("success", false);
            result.put("message", "卡密已被使用或已禁用");
            return result;
        }

        // 3. 获取当前用户信息
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 4. 获取规格信息
        Specification spec = specificationMapper.selectById(card.getSpecificationId());
        if (spec == null) {
            result.put("success", false);
            result.put("message", "规格信息不存在");
            return result;
        }

        // 5. 获取商品信息
        Product product = productMapper.selectById(spec.getProductId());

        // 6. 激活卡密
        boolean activated = activateCard(cardKey, currentUser.getEmail());
        if (!activated) {
            result.put("success", false);
            result.put("message", "卡密激活失败");
            return result;
        }

        // 7. 创建用户商品授权
        UserProduct userProduct = new UserProduct();
        userProduct.setUserId(userId);
        userProduct.setProductId(spec.getProductId());
        userProduct.setSpecId(card.getSpecificationId());
        userProduct.setCardKey(cardKey);
        userProduct.setActivatedAt(LocalDateTime.now());

        // 计算到期时间
        if (spec.getValidDays() != null && spec.getValidDays() > 0) {
            userProduct.setExpiresAt(LocalDateTime.now().plusDays(spec.getValidDays()));
        }

        userProduct.setStatus(1);
        userProductService.save(userProduct);

        // 8. 返回结果
        result.put("success", true);
        result.put("message", "兑换成功");
        result.put("productName", product != null ? product.getName() : null);
        result.put("specificationName", spec.getName());
        result.put("validDays", spec.getValidDays());
        result.put("expiresAt", userProduct.getExpiresAt() != null ? userProduct.getExpiresAt().toString() : null);

        return result;
    }

    @Override
    public boolean batchDeleteUsedCardKeys() {
        try {
            QueryWrapper<CardKey> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "已使用");

            // 直接按条件删除，避免全量加载到内存
            return baseMapper.delete(queryWrapper) >= 0;
        } catch (Exception e) {
            log.error("批量删除已使用卡密失败", e);
            return false;
        }
    }

    private CardKeyDTO convertToDTO(CardKey cardKey, Map<Integer, Specification> specMap, Map<Integer, Product> productMap, Map<String, String> usernameMap) {
        CardKeyDTO dto = new CardKeyDTO();
        dto.setId(cardKey.getId());
        dto.setCardKey(cardKey.getCardKey());
        dto.setSpecificationId(cardKey.getSpecificationId());
        dto.setStatus(cardKey.getStatus());
        dto.setUsername(cardKey.getUserEmail() != null ? usernameMap.get(cardKey.getUserEmail()) : null);
        dto.setUserEmail(cardKey.getUserEmail());
        dto.setAgentId(cardKey.getAgentId());
        dto.setActivateTime(cardKey.getActivateTime());
        dto.setExpireTime(cardKey.getExpireTime());
        dto.setCreatedAt(cardKey.getCreatedAt());
        dto.setUpdatedAt(cardKey.getUpdatedAt());

        if (cardKey.getSpecificationId() != null) {
            Specification spec = specMap.get(cardKey.getSpecificationId());
            if (spec != null) {
                dto.setSpecificationName(spec.getName());
                dto.setProductId(spec.getProductId());

                if (spec.getProductId() != null) {
                    Product product = productMap.get(spec.getProductId());
                    if (product != null) {
                        dto.setProductName(product.getName());
                    }
                }
            }
        }

        return dto;
    }
}
