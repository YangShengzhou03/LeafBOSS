package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.dto.UserProductDTO;
import com.leafboss.entity.Product;
import com.leafboss.entity.Specification;
import com.leafboss.entity.User;
import com.leafboss.entity.UserProduct;
import com.leafboss.mapper.ProductMapper;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.mapper.UserMapper;
import com.leafboss.mapper.UserProductMapper;
import com.leafboss.service.UserProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserProductServiceImpl extends ServiceImpl<UserProductMapper, UserProduct> implements UserProductService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SpecificationMapper specificationMapper;

    @Override
    public IPage<UserProductDTO> getUserProductListWithDetails(Page<UserProduct> pageParam, String keyword, Integer status) {
        return getUserProductListWithDetails(pageParam, keyword, status, null);
    }

    @Override
    public IPage<UserProductDTO> getUserProductListWithDetails(Page<UserProduct> pageParam, String keyword, Integer status, String userId) {
        QueryWrapper<UserProduct> wrapper = new QueryWrapper<>();

        if (userId != null) {
            wrapper.eq("user_id", userId);
        }

        if (status != null) {
            wrapper.eq("status", status);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                .like("card_key", keyword)
                .or()
                .like("user_id", keyword)
            );
        }

        wrapper.orderByDesc("created_at");

        Page<UserProduct> page = baseMapper.selectPage(pageParam, wrapper);
        List<UserProduct> records = page.getRecords();

        Page<UserProductDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records.isEmpty()) {
            resultPage.setRecords(new java.util.ArrayList<>());
            return resultPage;
        }

        // 批量获取关联信息，避免 N+1
        Map<String, User> userMap = new HashMap<>();
        List<String> userIds = records.stream()
                .map(UserProduct::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        Map<Integer, Product> productMap = new HashMap<>();
        List<Integer> productIds = records.stream()
                .map(UserProduct::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!productIds.isEmpty()) {
            productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
        }

        Map<Integer, Specification> specMap = new HashMap<>();
        List<Integer> specIds = records.stream()
                .map(UserProduct::getSpecId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!specIds.isEmpty()) {
            specMap = specificationMapper.selectBatchIds(specIds).stream()
                    .collect(Collectors.toMap(Specification::getId, s -> s));
        }

        Map<String, User> finalUserMap = userMap;
        Map<Integer, Product> finalProductMap = productMap;
        Map<Integer, Specification> finalSpecMap = specMap;

        List<UserProductDTO> dtoList = records.stream().map(up -> {
            UserProductDTO dto = new UserProductDTO();
            dto.setId(up.getId());
            dto.setUserId(up.getUserId());
            dto.setProductId(up.getProductId());
            dto.setSpecId(up.getSpecId());
            dto.setCardKey(up.getCardKey());
            dto.setActivatedAt(up.getActivatedAt());
            dto.setExpiresAt(up.getExpiresAt());
            dto.setStatus(up.getStatus());
            dto.setCreatedAt(up.getCreatedAt());

            User user = finalUserMap.get(up.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setEmail(user.getEmail());
            }

            Product product = finalProductMap.get(up.getProductId());
            if (product != null) {
                dto.setProductName(product.getName());
            }

            Specification spec = finalSpecMap.get(up.getSpecId());
            if (spec != null) {
                dto.setSpecName(spec.getName());
                dto.setValidDays(spec.getValidDays());
            }
            // 无规格时，从授权/到期时间反算天数
            if (dto.getValidDays() == null && up.getCreatedAt() != null && up.getExpiresAt() != null) {
                dto.setValidDays((int) java.time.temporal.ChronoUnit.DAYS.between(
                        up.getCreatedAt().toLocalDate(), up.getExpiresAt().toLocalDate()));
            }

            return dto;
        }).collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        return resultPage;
    }
}
