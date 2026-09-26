package com.leafboss.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.entity.BossReview;

public interface BossReviewService extends IService<BossReview> {
    
    Page<BossReview> pageByCompanyName(Page<BossReview> pageInfo, String companyName);

    Page<BossReview> pageWithDetails(Page<BossReview> pageInfo, Integer companyId, String cardKey);

    Page<BossReview> pageWithKeyword(Page<BossReview> pageInfo, String keyword);
}
