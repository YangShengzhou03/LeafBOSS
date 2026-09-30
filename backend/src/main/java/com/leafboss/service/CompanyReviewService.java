package com.leafboss.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.BossReview;

/**
 * Token 鉴权公司评论服务接口
 */
public interface CompanyReviewService {

    /**
     * 分页获取公司评论
     * @param companyName 公司名称
     * @param page 页码
     * @param size 每页条数
     * @param userId 当前登录用户ID（可为null，表示未登录）
     * @return 分页结果
     */
    Page<BossReview> listByCompany(String companyName, int page, int size, String userId);

    /**
     * 提交评论
     * @param companyName 公司名称
     * @param content 评论内容
     * @param userId 用户ID
     * @return 新评论
     */
    BossReview createReview(String companyName, String content, String userId);

    /**
     * 删除评论（仅作者本人）
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteReview(Integer commentId, String userId);

    /**
     * 投票/取消投票（toggle 语义）
     * @param commentId 评论ID
     * @param vote like/dislike
     * @param userId 用户ID
     * @return [likeCount, dislikeCount, myVote]
     */
    VoteResult vote(Integer commentId, String vote, String userId);

    class VoteResult {
        public int likeCount;
        public int dislikeCount;
        public String myVote;

        public VoteResult(int likeCount, int dislikeCount, String myVote) {
            this.likeCount = likeCount;
            this.dislikeCount = dislikeCount;
            this.myVote = myVote;
        }
    }
}
