package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.BossReview;
import com.leafboss.entity.Company;
import com.leafboss.entity.ReviewVote;
import com.leafboss.mapper.BossReviewMapper;
import com.leafboss.mapper.ReviewVoteMapper;
import com.leafboss.service.CompanyReviewService;
import com.leafboss.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class CompanyReviewServiceImpl implements CompanyReviewService {

    /** 同一用户 24 小时内对同一公司最多评论数 */
    private static final int MAX_REVIEWS_PER_24H = 3;

    @Resource
    private BossReviewMapper reviewMapper;

    @Resource
    private ReviewVoteMapper voteMapper;

    @Resource
    private CompanyService companyService;

    @Override
    public Page<BossReview> listByCompany(String companyName, int page, int size, String userId) {
        Page<BossReview> pageObj = new Page<>(page, Math.min(size, 50));
        Page<BossReview> result;
        if (userId != null) {
            result = reviewMapper.selectPageByCompanyForUser(pageObj, companyName, userId);
        } else {
            result = reviewMapper.selectPageByCompany(pageObj, companyName);
        }
        // 服务端计算每条评论是否可被当前用户删除
        for (BossReview review : result.getRecords()) {
            review.setCanDelete(userId != null && userId.equals(review.getUserId()));
        }
        return result;
    }

    @Override
    @Transactional
    public BossReview createReview(String companyName, String content, String userId) {
        Company company = companyService.getOrCreateByName(companyName);

        // 频率校验：24h 内 ≤ 3 条/公司
        QueryWrapper<BossReview> countQuery = new QueryWrapper<>();
        countQuery.eq("user_id", userId)
                 .eq("company_id", company.getId())
                 .ge("created_at", LocalDateTime.now().minusHours(24))
                 .eq("is_deleted", 0);
        Long recentCount = reviewMapper.selectCount(countQuery);
        if (recentCount >= MAX_REVIEWS_PER_24H) {
            throw new IllegalStateException("24小时内对同一公司最多评论" + MAX_REVIEWS_PER_24H + "条");
        }

        BossReview review = new BossReview();
        review.setCompanyId(company.getId());
        review.setContent(content);
        review.setUserId(userId);
        review.setLikeCount(0);
        review.setDislikeCount(0);
        reviewMapper.insert(review);
        return review;
    }

    @Override
    @Transactional
    public boolean deleteReview(Integer commentId, String userId) {
        BossReview existing = reviewMapper.selectByIdAndUserId(commentId, userId);
        if (existing == null) {
            return false;
        }
        reviewMapper.deleteById(commentId);
        return true;
    }

    @Override
    @Transactional
    public VoteResult vote(Integer commentId, String vote, String userId) {
        BossReview review = reviewMapper.selectById(commentId);
        if (review == null) {
            throw new IllegalArgumentException("评论不存在");
        }

        ReviewVote existing = voteMapper.selectByReviewIdAndUserId(commentId, userId);
        String currentVote = vote;

        if (existing != null) {
            if (existing.getVote().equals(vote)) {
                // 相同投票 → 取消（toggle off）
                voteMapper.deleteByReviewIdAndUserId(commentId, userId);
                decrementCount(commentId, vote);
                currentVote = null;
            } else {
                // 相反投票 → 切换
                voteMapper.deleteByReviewIdAndUserId(commentId, userId);
                decrementCount(commentId, existing.getVote());
                voteMapper.insertVote(commentId, userId, vote);
                incrementCount(commentId, vote);
            }
        } else {
            // 新投票
            voteMapper.insertVote(commentId, userId, vote);
            incrementCount(commentId, vote);
        }

        BossReview updated = reviewMapper.selectById(commentId);
        return new VoteResult(updated.getLikeCount(), updated.getDislikeCount(), currentVote);
    }

    private void incrementCount(Integer commentId, String vote) {
        if ("like".equals(vote)) {
            reviewMapper.updateLikeCount(commentId, 1);
        } else {
            reviewMapper.updateDislikeCount(commentId, 1);
        }
    }

    private void decrementCount(Integer commentId, String vote) {
        if ("like".equals(vote)) {
            reviewMapper.updateLikeCount(commentId, -1);
        } else {
            reviewMapper.updateDislikeCount(commentId, -1);
        }
    }
}
