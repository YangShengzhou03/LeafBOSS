package com.leafboss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leafboss.entity.ReviewVote;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewVoteMapper extends BaseMapper<ReviewVote> {

    /** Token 鉴权：查找用户对某评论的投票 */
    @Select("SELECT * FROM review_votes WHERE review_id = #{reviewId} AND user_id = #{userId}")
    ReviewVote selectByReviewIdAndUserId(@Param("reviewId") Integer reviewId, @Param("userId") String userId);

    /** Token 鉴权：删除投票（toggle off） */
    @Delete("DELETE FROM review_votes WHERE review_id = #{reviewId} AND user_id = #{userId}")
    int deleteByReviewIdAndUserId(@Param("reviewId") Integer reviewId, @Param("userId") String userId);

    @Insert("INSERT INTO review_votes (review_id, user_id, vote, created_at) VALUES (#{reviewId}, #{userId}, #{vote}, NOW())")
    int insertVote(@Param("reviewId") Integer reviewId, @Param("userId") String userId, @Param("vote") String vote);
}
