package com.leafboss.service;

import com.leafboss.entity.CardKey;
import com.leafboss.mapper.CardKeyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计服务。agentId 为 null 时统计全部数据（管理员视角），
 * 否则仅统计该代理商名下的卡密（代理视角）。
 */
@Service
public class DashboardService {

    @Autowired
    private CardKeyMapper cardKeyMapper;

    /** 基础统计指标（收入/销量/库存） */
    public Map<String, Object> getStats(String agentId) {
        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        stats.put("dailySales", (int) cardKeyMapper.countActivated(startOfDay, endOfDay, agentId));
        stats.put("dailyRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfDay, endOfDay, agentId)));

        LocalDateTime startOfYesterday = today.minusDays(1).atStartOfDay();
        stats.put("yesterdaySales", (int) cardKeyMapper.countActivated(startOfYesterday, startOfDay, agentId));
        stats.put("yesterdayRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfYesterday, startOfDay, agentId)));

        stats.put("monthlyRevenue", round(cardKeyMapper.sumActivatedRevenue(today.withDayOfMonth(1).atStartOfDay(), today.plusMonths(1).withDayOfMonth(1).atStartOfDay(), agentId)));
        stats.put("lastMonthRevenue", round(cardKeyMapper.sumActivatedRevenue(today.minusMonths(1).withDayOfMonth(1).atStartOfDay(), today.withDayOfMonth(1).atStartOfDay(), agentId)));

        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        stats.put("weeklyRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfWeek, startOfWeek.plusDays(7), agentId)));
        stats.put("lastWeekRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfWeek.minusDays(7), startOfWeek, agentId)));

        // 剩余卡密与库存总值
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> totalQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey>();
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> unusedQuery =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey>().eq("status", "未使用");
        if (agentId != null) {
            totalQuery.eq("agent_id", agentId);
            unusedQuery.eq("agent_id", agentId);
        }
        stats.put("totalCardKeys", cardKeyMapper.selectCount(totalQuery));
        stats.put("usedCardKeys", (int) cardKeyMapper.countActivated(null, null, agentId));
        stats.put("cardKeyCount", cardKeyMapper.selectCount(unusedQuery));
        stats.put("stockValue", round(cardKeyMapper.sumStockValue(agentId)));

        return stats;
    }

    /** 收入/销量趋势（按天、按规格），days 限制在 1-90 */
    public Map<String, Object> getDailyRevenueTrend(int days, String agentId) {
        if (days < 1) days = 1;
        if (days > 90) days = 90;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = today.plusDays(1).atStartOfDay();

        Map<String, Integer> trendIndex = new HashMap<>();
        List<Map<String, Object>> trend = new ArrayList<>();
        List<String> dateList = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String dateStr = startDate.plusDays(i).toString();
            dateList.add(dateStr);
            trendIndex.put(dateStr, i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("count", 0);
            item.put("revenue", 0.0);
            trend.add(item);
        }

        for (Map<String, Object> row : cardKeyMapper.sumDailyRevenue(rangeStart, rangeEnd, agentId)) {
            String dateStr = toLocalDate(row.get("day")).toString();
            Integer idx = trendIndex.get(dateStr);
            if (idx == null) continue;
            Map<String, Object> item = trend.get(idx);
            item.put("count", ((Number) row.get("cnt")).intValue());
            item.put("revenue", round(((Number) row.get("revenue")).doubleValue()));
        }

        Map<String, Map<String, Integer>> specDailyMap = new LinkedHashMap<>();
        for (Map<String, Object> row : cardKeyMapper.countDailyBySpec(rangeStart, rangeEnd, agentId)) {
            String dateStr = toLocalDate(row.get("day")).toString();
            String specName = String.valueOf(row.get("specName"));
            specDailyMap.computeIfAbsent(specName, k -> new HashMap<>())
                    .put(dateStr, ((Number) row.get("cnt")).intValue());
        }
        List<Map<String, Object>> specTrend = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : specDailyMap.entrySet()) {
            Map<String, Object> s = new HashMap<>();
            s.put("name", entry.getKey());
            List<Integer> data = new ArrayList<>();
            for (String d : dateList) {
                data.add(entry.getValue().getOrDefault(d, 0));
            }
            s.put("data", data);
            specTrend.add(s);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("trend", trend);
        result.put("specTrend", specTrend);
        return result;
    }

    /** 当日售出按规格分布 */
    public List<Map<String, Object>> getTodaySalesDistribution(String agentId) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = cardKeyMapper.countBySpecBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), agentId);

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row.get("name"));
            item.put("count", ((Number) row.get("cnt")).intValue());
            distribution.add(item);
        }
        return distribution;
    }

    /** 未使用卡密按规格分布（代理库存饼图） */
    public List<Map<String, Object>> getUnusedDistribution(String agentId) {
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map<String, Object> row : cardKeyMapper.countUnusedBySpec(agentId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row.get("name"));
            item.put("count", ((Number) row.get("cnt")).intValue());
            distribution.add(item);
        }
        return distribution;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
