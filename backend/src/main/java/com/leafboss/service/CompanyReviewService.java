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

    /**
     * 管理员分页获取全部评论
     * @param page 页码
     * @param size 每页条数
     * @param companyName 公司名称（可选，为空则查全部）
     * @return 分页结果
     */
    Page<BossReview> listAllForAdmin(int page, int size, String companyName);

    /**
     * 管理员删除任意评论
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean deleteReviewAsAdmin(Integer commentId);

    /**
     * 管理员直接插入评论（绕过频率限制）
     * @param review 评论实体（需包含 companyId, content, userId）
     */
    void createReviewForAdmin(BossReview review);

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
