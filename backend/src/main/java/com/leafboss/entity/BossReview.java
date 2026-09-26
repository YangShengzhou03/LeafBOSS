package com.leafboss.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("boss_reviews")
public class BossReview {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    @TableField("card_key")
    private String cardKey;
    
    @TableField("company_id")
    private Integer companyId;
    
    @TableField("content")
    private String content;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("dislike_count")
    private Integer dislikeCount;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField(value = "company_name", exist = false)
    private String companyName;

    @TableField(value = "my_vote", exist = false)
    private String myVote;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCardKey() {
        return cardKey;
    }

    public void setCardKey(String cardKey) {
        this.cardKey = cardKey;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public String getMyVote() {
        return myVote;
    }

    public void setMyVote(String myVote) {
        this.myVote = myVote;
    }
}
