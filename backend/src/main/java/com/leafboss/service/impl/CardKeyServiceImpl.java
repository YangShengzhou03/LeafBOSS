package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.dto.BatchImportRequest;
import com.leafboss.dto.CardKeyDTO;
import com.leafboss.entity.AgentAuthorization;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Specification;
import com.leafboss.entity.Product;
import com.leafboss.entity.User;
import com.leafboss.entity.UserProduct;
import com.leafboss.mapper.CardKeyMapper;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.mapper.ProductMapper;
import com.leafboss.mapper.UserMapper;
import com.leafboss.service.AgentAuthorizationService;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.UserProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private AgentAuthorizationService agentAuthorizationService;

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

        // 批量获取代理商用户名，避免 N+1
        List<String> agentIds = records.stream()
                .map(CardKey::getAgentId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> agentNameMap = new HashMap<>();
        if (!agentIds.isEmpty()) {
            agentNameMap = userMapper.selectBatchIds(agentIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        }
        Map<String, String> finalAgentNameMap = agentNameMap;

        List<CardKeyDTO> dtoList = records.stream().map(cardKey ->
            convertToDTO(cardKey, finalSpecMap, finalProductMap, finalUsernameMap, finalAgentNameMap)
        ).collect(Collectors.toList());

        Page<CardKeyDTO> resultPage = new Page<>(cardKeyPage.getCurrent(), cardKeyPage.getSize(), cardKeyPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    @Override
    public boolean activateCard(String cardKey, String userEmail) {
        CardKey card = findByCardKey(cardKey);
        if (card == null) {
            return false;
        }

        LocalDateTime activateTime = LocalDateTime.now();
        LocalDateTime expireTime = null;
        if (card.getSpecificationId() != null) {
            Specification spec = specificationMapper.selectById(card.getSpecificationId());
            if (spec != null && spec.getValidDays() != null && spec.getValidDays() > 0) {
                expireTime = activateTime.plusDays(spec.getValidDays());
            }
        }

        // 条件更新保证并发下只有第一个请求能激活成功
        UpdateWrapper<CardKey> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("card_key", cardKey)
                .eq("status", "未使用")
                .set("status", "已使用")
                .set("user_email", userEmail)
                .set("activate_time", activateTime)
                .set("updated_at", activateTime);
        if (expireTime != null) {
            updateWrapper.set("expire_time", expireTime);
        }

        return baseMapper.update(null, updateWrapper) > 0;
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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImport(BatchImportRequest req, String operatorId, boolean isAdmin) {
        if (req.getSpecId() == null || req.getSpecId() <= 0) {
            throw new IllegalArgumentException("规格ID不能为空");
        }
        List<String> keys = req.getKeys();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("卡密列表不能为空");
        }
        if (keys.size() > 10000) {
            throw new IllegalArgumentException("单次最多导入10000个卡密");
        }
        // 校验先于扣减，避免扣减后才发现非法数据导致余额泄漏
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("卡密列表包含非法项");
            }
        }

        String agentId = req.getAgentId();
        int importCount = keys.size();
        if (!isAdmin) {
            User currentUser = operatorId != null ? userMapper.selectById(operatorId) : null;
            if (currentUser == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            // 数据隔离：非 admin 用户导入的卡密 agentId 强制设为自己
            agentId = operatorId;
            Specification spec = specificationMapper.selectById(req.getSpecId());
            if (spec == null) {
                throw new IllegalArgumentException("规格不存在");
            }
            AgentAuthorization authorization = agentAuthorizationService.findActiveAuthorization(operatorId, spec.getProductId());
            if (authorization == null) {
                throw new IllegalArgumentException("未获得该商品的销售授权，请联系管理员");
            }
            if (authorization.getRemainingCount() <= 0) {
                throw new IllegalArgumentException("该商品余额已用完");
            }
            if (importCount > authorization.getRemainingCount()) {
                importCount = authorization.getRemainingCount();
            }
            if (!agentAuthorizationService.deductRemaining(operatorId, spec.getProductId(), importCount)) {
                throw new IllegalArgumentException("余额不足");
            }
        } else if (agentId != null && !agentId.trim().isEmpty()) {
            // 管理员代代理商导入时，校验该代理商确实拥有所选商品的有效授权
            Specification spec = specificationMapper.selectById(req.getSpecId());
            if (spec == null) {
                throw new IllegalArgumentException("规格不存在");
            }
            if (agentAuthorizationService.findActiveAuthorization(agentId, spec.getProductId()) == null) {
                throw new IllegalArgumentException("该代理商未获得此商品的销售授权");
            }
        }

        // 扣减与写入同一事务：写入失败时扣减一并回滚，无需补偿
        LocalDateTime now = LocalDateTime.now();
        List<CardKey> cardKeys = new ArrayList<>();
        for (int i = 0; i < importCount; i++) {
            CardKey cardKey = new CardKey();
            cardKey.setCardKey(keys.get(i).trim());
            cardKey.setSpecificationId(req.getSpecId());
            cardKey.setAgentId(agentId);
            cardKey.setStatus("未使用");
            cardKey.setCreatedAt(now);
            cardKey.setUpdatedAt(now);
            cardKeys.add(cardKey);
        }
        saveBatch(cardKeys);

        Map<String, Object> result = new HashMap<>();
        if (importCount < keys.size()) {
            result.put("message", "部分导入成功：额度不足，已导入 " + importCount + " 个，" + (keys.size() - importCount) + " 个被跳过");
            result.put("imported", importCount);
            result.put("skipped", keys.size() - importCount);
        } else {
            result.put("message", "批量导入成功，共导入 " + importCount + " 个卡密");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String cardKey, String status) {
        if (cardKey == null || cardKey.isEmpty()) {
            throw new IllegalArgumentException("卡密不能为空");
        }
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("状态不能为空");
        }
        CardKey card = findByCardKey(cardKey);
        if (card == null) {
            throw new IllegalArgumentException("卡密不存在");
        }
        String oldStatus = card.getStatus();
        card.setStatus(status);
        if (!updateById(card)) {
            throw new IllegalArgumentException("状态更新失败");
        }

        // 同步更新关联的用户授权状态
        UserProduct userProduct = userProductService.findByCardKey(cardKey);
        if (userProduct != null) {
            if ("已禁用".equals(status) || "已吊销".equals(status)) {
                userProduct.setStatus(0);
            } else if ("已使用".equals(status) && ("已禁用".equals(oldStatus) || "已吊销".equals(oldStatus))) {
                userProduct.setStatus(1);
            }
            userProductService.updateById(userProduct);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByCardKey(String cardKey) {
        CardKey card = requireCard(cardKey);
        // 删除前吊销关联的用户授权
        revokeLinkedUserProduct(cardKey);
        if (!removeById(card.getId())) {
            throw new IllegalArgumentException("删除失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agentRevoke(String cardKey, String agentId) {
        CardKey card = requireOwnedCard(cardKey, agentId);
        card.setStatus("已吊销");
        if (!updateById(card)) {
            throw new IllegalArgumentException("操作失败");
        }
        // 同步吊销关联的用户授权
        revokeLinkedUserProduct(cardKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agentRestore(String cardKey, String agentId) {
        CardKey card = requireOwnedCard(cardKey, agentId);
        if (!"已吊销".equals(card.getStatus())) {
            throw new IllegalArgumentException("仅已吊销的卡密可恢复");
        }
        // 恢复到已使用/未使用状态（根据是否有用户授权判断）
        UserProduct userProduct = userProductService.findByCardKey(cardKey);
        card.setStatus(userProduct != null ? "已使用" : "未使用");
        if (!updateById(card)) {
            throw new IllegalArgumentException("操作失败");
        }
        if (userProduct != null) {
            userProduct.setStatus(1);
            userProductService.updateById(userProduct);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agentDelete(String cardKey, String agentId) {
        CardKey card = requireOwnedCard(cardKey, agentId);
        // 删除前吊销关联的用户授权
        revokeLinkedUserProduct(cardKey);
        if (!removeById(card.getId())) {
            throw new IllegalArgumentException("删除失败");
        }
    }

    private CardKey requireCard(String cardKey) {
        if (cardKey == null || cardKey.isEmpty()) {
            throw new IllegalArgumentException("卡密不能为空");
        }
        CardKey card = findByCardKey(cardKey);
        if (card == null) {
            throw new IllegalArgumentException("卡密不存在");
        }
        return card;
    }

    private CardKey requireOwnedCard(String cardKey, String agentId) {
        CardKey card = requireCard(cardKey);
        if (agentId == null || !agentId.equals(card.getAgentId())) {
            throw new IllegalArgumentException("无权操作该卡密");
        }
        return card;
    }

    private void revokeLinkedUserProduct(String cardKey) {
        UserProduct userProduct = userProductService.findByCardKey(cardKey);
        if (userProduct != null) {
            userProduct.setStatus(0);
            userProductService.updateById(userProduct);
        }
    }

    private CardKeyDTO convertToDTO(CardKey cardKey, Map<Integer, Specification> specMap, Map<Integer, Product> productMap, Map<String, String> usernameMap, Map<String, String> agentNameMap) {
        CardKeyDTO dto = new CardKeyDTO();
        dto.setId(cardKey.getId());
        dto.setCardKey(cardKey.getCardKey());
        dto.setSpecificationId(cardKey.getSpecificationId());
        dto.setStatus(cardKey.getStatus());
        dto.setUsername(cardKey.getUserEmail() != null ? usernameMap.get(cardKey.getUserEmail()) : null);
        dto.setUserEmail(cardKey.getUserEmail());
        dto.setAgentId(cardKey.getAgentId());
        dto.setAgentName(cardKey.getAgentId() != null ? agentNameMap.get(cardKey.getAgentId()) : null);
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
