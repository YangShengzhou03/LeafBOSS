package com.leafboss.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("boss_reviews")
public class BossReview {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private String userId;

    @TableField("company_id")
    private Integer companyId;

    @TableField("content")
    private String content;

    @TableField("like_count")
    private Integer likeCount = 0;

    @TableField("dislike_count")
    private Integer dislikeCount = 0;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted = 0;

    /** 非 DB 列：当前登录用户对该评论的投票（like/dislike/null） */
    @TableField(exist = false)
    private String myVote;

    /** 非 DB 列：公司名称 */
    @TableField(exist = false)
    private String companyName;

    /** 非 DB 列：当前登录用户是否有权限删除此评论 */
    @TableField(exist = false)
    private Boolean canDelete;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(Integer dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getMyVote() {
        return myVote;
    }

    public void setMyVote(String myVote) {
        this.myVote = myVote;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }
}
