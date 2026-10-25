package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.BossReview;
import com.leafboss.entity.Company;
import com.leafboss.entity.ReviewVote;
import com.leafboss.mapper.BossReviewMapper;
import com.leafboss.mapper.CompanyMapper;
import com.leafboss.mapper.ReviewVoteMapper;
import com.leafboss.service.CompanyReviewService;
import com.leafboss.service.CompanyService;
import org.springframework.dao.DuplicateKeyException;
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

    @Resource
    private CompanyMapper companyMapper;

    @Override
    public Page<BossReview> listByCompany(String companyName, int page, int size, String userId) {
        // 浏览评论时确保公司存在（不存在则自动创建，getOrCreateByName 借助唯一索引保证并发安全）
        if (companyName != null && !companyName.isBlank()) {
            companyService.getOrCreateByName(companyName);
        }
        Page<BossReview> pageObj = new Page<>(page, Math.min(size, 50));
        // 该接口经拦截器后必然已登录，userId 始终存在
        Page<BossReview> result = reviewMapper.selectPageByCompanyForUser(pageObj, companyName, userId);
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

        companyMapper.updateCommentCount(company.getId(), 1);

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

        companyMapper.updateCommentCount(existing.getCompanyId(), -1);

        return true;
    }

    @Override
    public Page<BossReview> listAllForAdmin(int page, int size, String companyName) {
        Page<BossReview> pageObj = new Page<>(page, Math.min(size, 50));
        return reviewMapper.selectPageForAdmin(pageObj, companyName);
    }

    @Override
    @Transactional
    public boolean deleteReviewAsAdmin(Integer commentId) {
        BossReview existing = reviewMapper.selectById(commentId);
        if (existing == null) {
            return false;
        }
        reviewMapper.deleteById(commentId);

        companyMapper.updateCommentCount(existing.getCompanyId(), -1);

        return true;
    }

    @Override
    @Transactional
    public void createReviewForAdmin(BossReview review) {
        reviewMapper.insert(review);

        companyMapper.updateCommentCount(review.getCompanyId(), 1);
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
                if (tryInsertVote(commentId, userId, vote)) {
                    incrementCount(commentId, vote);
                }
            }
        } else {
            // 新投票
            if (tryInsertVote(commentId, userId, vote)) {
                incrementCount(commentId, vote);
            }
        }

        BossReview updated = reviewMapper.selectById(commentId);
        return new VoteResult(updated.getLikeCount(), updated.getDislikeCount(), currentVote);
    }

    /** 并发下唯一索引 uk_review_user 会拒绝重复插入，此时视为已投过票，不再重复计数 */
    private boolean tryInsertVote(Integer commentId, String userId, String vote) {
        try {
            voteMapper.insertVote(commentId, userId, vote);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
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
