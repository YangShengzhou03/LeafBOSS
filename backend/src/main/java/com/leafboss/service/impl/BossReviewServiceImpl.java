package com.leafboss.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.entity.BossReview;
import com.leafboss.mapper.BossReviewMapper;
import com.leafboss.service.BossReviewService;
import org.springframework.stereotype.Service;

@Service
public class BossReviewServiceImpl extends ServiceImpl<BossReviewMapper, BossReview> implements BossReviewService {
    
    @Override
    public Page<BossReview> pageByCompanyName(Page<BossReview> pageInfo, String companyName) {
        return baseMapper.selectByCompanyName(pageInfo, companyName);
    }

    @Override
    public Page<BossReview> pageWithDetails(Page<BossReview> pageInfo, Integer companyId, String cardKey) {
        return baseMapper.selectPageWithDetails(pageInfo, companyId, cardKey);
    }

    @Override
    public Page<BossReview> pageWithKeyword(Page<BossReview> pageInfo, String keyword) {
        return baseMapper.selectPageWithKeyword(pageInfo, keyword);
    }
}
