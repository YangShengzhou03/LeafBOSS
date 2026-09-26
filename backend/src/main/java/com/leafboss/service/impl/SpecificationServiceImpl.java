package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.dto.SpecificationDTO;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Product;
import com.leafboss.entity.Specification;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.mapper.ProductMapper;
import com.leafboss.mapper.CardKeyMapper;
import com.leafboss.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpecificationServiceImpl extends ServiceImpl<SpecificationMapper, Specification> implements SpecificationService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CardKeyMapper cardKeyMapper;

    @Override
    public List<SpecificationDTO> getSpecificationDTOs() {
        List<Specification> specifications = this.list();
        if (specifications.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取商品信息
        List<Integer> productIds = specifications.stream()
                .map(Specification::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Integer, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<Integer> specificationIds = specifications.stream()
                .map(Specification::getId)
                .collect(Collectors.toList());

        Map<Integer, CardKeyStatistics> statisticsMap = getCardKeyStatisticsBySpecificationIds(specificationIds);

        return specifications.stream().map(spec -> {
            SpecificationDTO dto = new SpecificationDTO();
            dto.setId(spec.getId());
            dto.setProductId(spec.getProductId());
            
            // 正确设置商品名称
            if (spec.getProductId() != null) {
                Product product = productMap.get(spec.getProductId());
                if (product != null) {
                    dto.setProductName(product.getName());
                }
            }
            
            dto.setName(spec.getName());
            dto.setPrice(spec.getPrice());
            dto.setStockQuantity(spec.getStockQuantity());
            dto.setValidDays(spec.getValidDays());
            dto.setStatus(spec.getStatus());
            dto.setCreatedAt(spec.getCreatedAt());
            dto.setUpdatedAt(spec.getUpdatedAt());

            CardKeyStatistics stats = statisticsMap.get(spec.getId());
            if (stats != null) {
                dto.setTotalKeys(stats.getTotalKeys());
                dto.setUsedKeys(stats.getUsedKeys());
                dto.setUnusedKeys(stats.getUnusedKeys());
                dto.setDisabledKeys(stats.getDisabledKeys());
            } else {
                dto.setTotalKeys(0);
                dto.setUsedKeys(0);
                dto.setUnusedKeys(0);
                dto.setDisabledKeys(0);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<SpecificationDTO> getSpecificationDTOsWithPagination(Page<Specification> page, String keyword, Long productId) {
        QueryWrapper<Specification> queryWrapper = new QueryWrapper<>();

        if (productId != null) {
            queryWrapper.eq("product_id", productId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("name", keyword.trim());
        }

        IPage<Specification> specificationPage = this.page(page, queryWrapper);

        List<Integer> specificationIds = specificationPage.getRecords().stream()
                .map(Specification::getId)
                .collect(Collectors.toList());

        if (specificationIds.isEmpty()) {
            Page<SpecificationDTO> resultPage = new Page<>(specificationPage.getCurrent(), specificationPage.getSize(), specificationPage.getTotal());
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        Map<Integer, CardKeyStatistics> statisticsMap = getCardKeyStatisticsBySpecificationIds(specificationIds);

        List<SpecificationDTO> dtoList = specificationPage.getRecords().stream().map(spec -> {
            SpecificationDTO dto = new SpecificationDTO();
            dto.setId(spec.getId());
            dto.setProductId(spec.getProductId());
            dto.setProductName("");
            dto.setName(spec.getName());
            dto.setPrice(spec.getPrice());
            dto.setStockQuantity(spec.getStockQuantity());
            dto.setValidDays(spec.getValidDays());
            dto.setStatus(spec.getStatus());
            dto.setCreatedAt(spec.getCreatedAt());
            dto.setUpdatedAt(spec.getUpdatedAt());

            CardKeyStatistics stats = statisticsMap.get(spec.getId());
            if (stats != null) {
                dto.setTotalKeys(stats.getTotalKeys());
                dto.setUsedKeys(stats.getUsedKeys());
                dto.setUnusedKeys(stats.getUnusedKeys());
                dto.setDisabledKeys(stats.getDisabledKeys());
            } else {
                dto.setTotalKeys(0);
                dto.setUsedKeys(0);
                dto.setUnusedKeys(0);
                dto.setDisabledKeys(0);
            }

            return dto;
        }).collect(Collectors.toList());

        Page<SpecificationDTO> resultPage = new Page<>(specificationPage.getCurrent(), specificationPage.getSize(), specificationPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    private Map<Integer, CardKeyStatistics> getCardKeyStatisticsBySpecificationIds(List<Integer> specificationIds) {
        Map<Integer, CardKeyStatistics> statisticsMap = new HashMap<>();

        QueryWrapper<CardKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("specification_id", specificationIds);
        queryWrapper.select("specification_id", "status", "COUNT(*) as count");
        queryWrapper.groupBy("specification_id", "status");

        List<Map<String, Object>> statisticsList = cardKeyMapper.selectMaps(queryWrapper);

        for (Integer specId : specificationIds) {
            statisticsMap.put(specId, new CardKeyStatistics());
        }

        for (Map<String, Object> stat : statisticsList) {
            Integer specId = (Integer) stat.get("specification_id");
            String status = (String) stat.get("status");
            Long count = (Long) stat.get("count");

            CardKeyStatistics stats = statisticsMap.get(specId);
            if (stats != null) {
                switch (status) {
                    case "未使用":
                        stats.setUnusedKeys(count.intValue());
                        break;
                    case "已使用":
                        stats.setUsedKeys(count.intValue());
                        break;
                    case "已禁用":
                        stats.setDisabledKeys(count.intValue());
                        break;
                }
                stats.setTotalKeys(stats.getUnusedKeys() + stats.getUsedKeys() + stats.getDisabledKeys());
            }
        }

        return statisticsMap;
    }

    private static class CardKeyStatistics {
        private int totalKeys = 0;
        private int usedKeys = 0;
        private int unusedKeys = 0;
        private int disabledKeys = 0;

        public int getTotalKeys() { return totalKeys; }
        public void setTotalKeys(int totalKeys) { this.totalKeys = totalKeys; }
        public int getUsedKeys() { return usedKeys; }
        public void setUsedKeys(int usedKeys) { this.usedKeys = usedKeys; }
        public int getUnusedKeys() { return unusedKeys; }
        public void setUnusedKeys(int unusedKeys) { this.unusedKeys = unusedKeys; }
        public int getDisabledKeys() { return disabledKeys; }
        public void setDisabledKeys(int disabledKeys) { this.disabledKeys = disabledKeys; }
    }

    @Override
    public Specification findByName(String name) {
        QueryWrapper<Specification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        return baseMapper.selectOne(queryWrapper);
    }
}