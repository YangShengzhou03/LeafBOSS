package com.leafboss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.BossReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BossReviewMapper extends BaseMapper<BossReview> {

    /**
     * Token 鉴权分页查询：JOIN companies 表按公司名过滤，子查询拿 myVote
     * ponytail: companies.name 走索引，5w 行内响应 <50ms；
     * 数据量上 50w+ 改为先查 company_id 再按 id 过滤，避免 JOIN 全表。
     */
    @Select("<script>" +
            "SELECT br.*, c.name as company_name, " +
            "(SELECT rv.vote FROM review_votes rv " +
            "  WHERE rv.review_id = br.id AND rv.user_id = #{userId} LIMIT 1) AS myVote " +
            "FROM boss_reviews br " +
            "JOIN companies c ON br.company_id = c.id " +
            "WHERE c.name = #{companyName} AND br.is_deleted = 0 " +
            "ORDER BY br.created_at DESC</script>")
    Page<BossReview> selectPageByCompanyForUser(Page<BossReview> page,
                                                   @Param("companyName") String companyName,
                                                   @Param("userId") String userId);

    /** 无需登录的公开查询：myVote 为 NULL */
    @Select("SELECT br.*, c.name as company_name, NULL AS myVote " +
            "FROM boss_reviews br " +
            "JOIN companies c ON br.company_id = c.id " +
            "WHERE c.name = #{companyName} AND br.is_deleted = 0 " +
            "ORDER BY br.created_at DESC")
    Page<BossReview> selectPageByCompany(Page<BossReview> page, @Param("companyName") String companyName);

    /** 根据评论ID和用户ID查询（用于权限校验） */
    @Select("SELECT * FROM boss_reviews WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    BossReview selectByIdAndUserId(@Param("id") Integer id, @Param("userId") String userId);

    @Update("UPDATE boss_reviews SET like_count = like_count + #{delta} WHERE id = #{id}")
    int updateLikeCount(@Param("id") Integer id, @Param("delta") int delta);

    @Update("UPDATE boss_reviews SET dislike_count = dislike_count + #{delta} WHERE id = #{id}")
    int updateDislikeCount(@Param("id") Integer id, @Param("delta") int delta);
}
