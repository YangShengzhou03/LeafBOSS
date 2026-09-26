package com.leafboss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.BossReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BossReviewMapper extends BaseMapper<BossReview> {
    
    @Select("SELECT br.*, c.name as company_name FROM boss_reviews br " +
            "LEFT JOIN companies c ON br.company_id = c.id " +
            "WHERE c.name = #{companyName} " +
            "ORDER BY br.created_at DESC")
    Page<BossReview> selectByCompanyName(Page<BossReview> page, @Param("companyName") String companyName);

    @Select("<script>" +
            "SELECT br.*, c.name as company_name FROM boss_reviews br " +
            "LEFT JOIN companies c ON br.company_id = c.id " +
            "<where>" +
            "<if test='companyId != null'>AND br.company_id = #{companyId}</if> " +
            "<if test='cardKey != null and cardKey != \"\"'>AND br.card_key = #{cardKey}</if>" +
            "</where>" +
            "ORDER BY br.created_at DESC</script>")
    Page<BossReview> selectPageWithDetails(Page<BossReview> page, @Param("companyId") Integer companyId, @Param("cardKey") String cardKey);

    // ponytail: 关键词搜索的 OR 分支（公司名模糊匹配）会让优化器退化为全表扫描，5w 行时约几十 ms，可接受；
    // 数据量再上一个量级（50w+）时改为 UNION 拆分两条索引查询，或公司名搜索独立走下拉筛选。
    @Select("<script>" +
            "SELECT br.*, c.name as company_name FROM boss_reviews br " +
            "LEFT JOIN companies c ON br.company_id = c.id " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (MATCH(br.card_key, br.content) AGAINST(#{keyword} IN BOOLEAN MODE) " +
            "OR br.company_id IN (SELECT id FROM companies WHERE name LIKE CONCAT('%', #{keyword}, '%')))" +
            "</if>" +
            "</where>" +
            "ORDER BY br.created_at DESC</script>")
    Page<BossReview> selectPageWithKeyword(Page<BossReview> page, @Param("keyword") String keyword);
}
