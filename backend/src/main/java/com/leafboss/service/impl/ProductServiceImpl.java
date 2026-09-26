package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.entity.Product;
import com.leafboss.entity.Specification;
import com.leafboss.entity.CardKey;
import com.leafboss.mapper.ProductMapper;
import com.leafboss.mapper.SpecificationMapper;
import com.leafboss.mapper.CardKeyMapper;
import com.leafboss.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private SpecificationMapper specificationMapper;

    @Autowired
    private CardKeyMapper cardKeyMapper;

    @Override
    public Product findByName(String name) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public boolean updateById(Product product) {
        Product existingProduct = getById(product.getId());
        if (existingProduct == null) {
            return false;
        }

        boolean isStatusChangedToInactive = "active".equals(existingProduct.getStatus()) &&
                                           "inactive".equals(product.getStatus());

        boolean updated = super.updateById(product);

        if (updated && isStatusChangedToInactive) {
            disableRelatedSpecificationsAndCardKeys(product.getId());
        }

        return updated;
    }

    private void disableRelatedSpecificationsAndCardKeys(Integer productId) {
        QueryWrapper<Specification> specQueryWrapper = new QueryWrapper<>();
        specQueryWrapper.eq("product_id", productId.longValue());
        List<Specification> specifications = specificationMapper.selectList(specQueryWrapper);

        for (Specification spec : specifications) {
            if ("active".equals(spec.getStatus())) {
                spec.setStatus("inactive");
                specificationMapper.updateById(spec);

                disableCardKeysBySpecificationId(spec.getId());
            }
        }
    }

    private void disableCardKeysBySpecificationId(Integer specificationId) {
        QueryWrapper<CardKey> cardKeyQueryWrapper = new QueryWrapper<>();
        cardKeyQueryWrapper.eq("specification_id", specificationId);
        List<CardKey> cardKeys = cardKeyMapper.selectList(cardKeyQueryWrapper);

        for (CardKey cardKey : cardKeys) {
            if (!"已禁用".equals(cardKey.getStatus())) {
                cardKey.setStatus("已禁用");
                cardKeyMapper.updateById(cardKey);
            }
        }
    }
}