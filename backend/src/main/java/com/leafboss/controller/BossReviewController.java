package com.leafboss.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.BossReview;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Company;
import com.leafboss.service.BossReviewService;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.CompanyService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boss-reviews")
public class BossReviewController {

    @Autowired
    private BossReviewService bossReviewService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private LogUtil logUtil;

    @GetMapping
    public Result<IPage<BossReview>> getBossReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String cardKey,
            @RequestParam(required = false) String keyword) {

        Page<BossReview> pageParam = new Page<>(page, size);
        IPage<BossReview> reviewPage;
        if (keyword != null && !keyword.isEmpty()) {
            reviewPage = bossReviewService.pageWithKeyword(pageParam, keyword);
        } else {
            reviewPage = bossReviewService.pageWithDetails(pageParam, companyId, cardKey);
        }
        return Result.success(reviewPage);
    }

    @PostMapping
    @Transactional
    public Result<Boolean> createBossReview(@RequestBody BossReview review, HttpServletRequest request) {
        // 持卡密即可操作：校验卡密存在且已使用，不校验归属
        if (review.getCardKey() == null || review.getCardKey().trim().isEmpty()) {
            return Result.error("卡密不能为空");
        }

        CardKey key = cardKeyService.findByCardKey(review.getCardKey().trim());
        if (key == null) {
            return Result.error("卡密不存在");
        }
        if (!"已使用".equals(key.getStatus())) {
            return Result.error("卡密未激活，无法发表评论");
        }

        if (review.getContent() == null || review.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        if (review.getContent().length() > 1000) {
            return Result.error("评论内容过长");
        }
        review.setId(null);
        review.setLikeCount(0);
        review.setDislikeCount(0);
        if (review.getCreatedAt() == null) {
            review.setCreatedAt(java.time.LocalDateTime.now());
        }

        boolean saved = bossReviewService.save(review);
        if (saved) {
            Company company = companyService.getById(review.getCompanyId());
            if (company != null) {
                company.setCommentCount(company.getCommentCount() + 1);
                companyService.updateById(company);
            }
            logUtil.logOperation("管理", "发布评论", request);
            return Result.success("评论发布成功", true);
        } else {
            return Result.error("评论发布失败");
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Boolean> deleteBossReview(@PathVariable String id, HttpServletRequest request) {
        // 持卡密即可操作：不校验归属
        BossReview review = bossReviewService.getById(id);
        if (review == null) {
            return Result.error("评论不存在");
        }

        boolean deleted = bossReviewService.removeById(id);
        if (deleted) {
            if (review.getCompanyId() != null) {
                Company company = companyService.getById(review.getCompanyId());
                if (company != null && company.getCommentCount() > 0) {
                    company.setCommentCount(company.getCommentCount() - 1);
                    companyService.updateById(company);
                }
            }
            logUtil.logOperation("管理", "删除评论", request);
            return Result.success("评论删除成功", true);
        } else {
            return Result.error("评论删除失败");
        }
    }
}
