package com.leafboss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leafboss.entity.CardKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CardKeyMapper extends BaseMapper<CardKey> {

    /** 已激活卡密数（状态为已使用/已吊销），可选激活时间范围 */
    @Select("<script>SELECT COUNT(*) FROM card_keys WHERE status IN ('已使用', '已吊销')" +
            "<if test='start != null'> AND activate_time &gt;= #{start}</if>" +
            "<if test='end != null'> AND activate_time &lt; #{end}</if></script>")
    long countActivated(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 已激活卡密的收入合计（关联规格价格），可选激活时间范围 */
    @Select("<script>SELECT COALESCE(SUM(s.price), 0) FROM card_keys c JOIN specifications s ON c.specification_id = s.id " +
            "WHERE c.status IN ('已使用', '已吊销') AND c.specification_id IS NOT NULL" +
            "<if test='start != null'> AND c.activate_time &gt;= #{start}</if>" +
            "<if test='end != null'> AND c.activate_time &lt; #{end}</if></script>")
    double sumActivatedRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 未使用卡密的库存价值（关联规格价格） */
    @Select("SELECT COALESCE(SUM(s.price), 0) FROM card_keys c JOIN specifications s ON c.specification_id = s.id " +
            "WHERE c.status = '未使用' AND c.specification_id IS NOT NULL")
    double sumStockValue();

    /** 按天统计激活数与收入 */
    @Select("SELECT DATE(c.activate_time) AS day, COUNT(*) AS cnt, COALESCE(SUM(s.price), 0) AS revenue " +
            "FROM card_keys c JOIN specifications s ON c.specification_id = s.id " +
            "WHERE c.activate_time >= #{start} AND c.activate_time < #{end} AND c.specification_id IS NOT NULL " +
            "GROUP BY DATE(c.activate_time)")
    List<Map<String, Object>> sumDailyRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按天按规格统计激活数 */
    @Select("SELECT DATE(c.activate_time) AS day, s.name AS specName, COUNT(*) AS cnt " +
            "FROM card_keys c JOIN specifications s ON c.specification_id = s.id " +
            "WHERE c.activate_time >= #{start} AND c.activate_time < #{end} AND c.specification_id IS NOT NULL " +
            "GROUP BY DATE(c.activate_time), s.name")
    List<Map<String, Object>> countDailyBySpec(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按规格统计指定时间范围内的激活数（当日售出分布） */
    @Select("SELECT s.name AS name, COUNT(*) AS cnt " +
            "FROM card_keys c JOIN specifications s ON c.specification_id = s.id " +
            "WHERE c.activate_time >= #{start} AND c.activate_time < #{end} AND c.specification_id IS NOT NULL " +
            "GROUP BY s.name")
    List<Map<String, Object>> countBySpecBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
