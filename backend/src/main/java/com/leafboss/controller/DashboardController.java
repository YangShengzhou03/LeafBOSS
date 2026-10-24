package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.CardKey;
import com.leafboss.mapper.CardKeyMapper;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private CardKeyMapper cardKeyMapper;

    @Autowired
    private UserService userService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        stats.put("dailySales", (int) cardKeyMapper.countActivated(startOfDay, endOfDay));
        stats.put("dailyRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfDay, endOfDay)));

        LocalDateTime startOfYesterday = today.minusDays(1).atStartOfDay();
        stats.put("yesterdaySales", (int) cardKeyMapper.countActivated(startOfYesterday, startOfDay));
        stats.put("yesterdayRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfYesterday, startOfDay)));

        stats.put("totalOrders", (int) cardKeyService.count());
        stats.put("monthlyRevenue", round(cardKeyMapper.sumActivatedRevenue(today.withDayOfMonth(1).atStartOfDay(), today.plusMonths(1).withDayOfMonth(1).atStartOfDay())));
        stats.put("lastMonthRevenue", round(cardKeyMapper.sumActivatedRevenue(today.minusMonths(1).withDayOfMonth(1).atStartOfDay(), today.withDayOfMonth(1).atStartOfDay())));

        // 本周/上周收入
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        stats.put("weeklyRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfWeek, startOfWeek.plusDays(7))));
        stats.put("lastWeekRevenue", round(cardKeyMapper.sumActivatedRevenue(startOfWeek.minusDays(7), startOfWeek)));

        // 激活率
        long totalCardKeys = cardKeyService.count();
        long activatedKeys = cardKeyMapper.countActivated(null, null);
        stats.put("totalCardKeys", (int) totalCardKeys);
        stats.put("activationRate", totalCardKeys > 0 ? Math.round(activatedKeys * 10000.0 / totalCardKeys) / 100.0 : 0.0);

        // 剩余卡密
        stats.put("cardKeyCount", cardKeyMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey>().eq("status", "未使用")));

        // 仓库总值（未使用卡密的规格价格之和）
        stats.put("stockValue", round(cardKeyMapper.sumStockValue()));

        // 用户数
        stats.put("userCount", (int) userService.count());

        return Result.success(stats);
    }

    @GetMapping("/daily-revenue-trend")
    public Result<Map<String, Object>> getDailyRevenueTrend(
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1) days = 1;
        if (days > 90) days = 90;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = today.plusDays(1).atStartOfDay();

        // 每日基础数据：全部初始化为 0，再用 SQL 聚合结果覆盖
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

        // 按天聚合：一条 SQL 替代 内存循环 + 逐条查规格
        for (Map<String, Object> row : cardKeyMapper.sumDailyRevenue(rangeStart, rangeEnd)) {
            String dateStr = toLocalDate(row.get("day")).toString();
            Integer idx = trendIndex.get(dateStr);
            if (idx == null) continue;
            Map<String, Object> item = trend.get(idx);
            item.put("count", ((Number) row.get("cnt")).intValue());
            item.put("revenue", round(((Number) row.get("revenue")).doubleValue()));
        }

        // 按天按规格聚合，构建 specTrend: [{name, data: [count per day]}]
        Map<String, Map<String, Integer>> specDailyMap = new LinkedHashMap<>();
        for (Map<String, Object> row : cardKeyMapper.countDailyBySpec(rangeStart, rangeEnd)) {
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
        return Result.success(result);
    }

    @GetMapping("/today-sales-distribution")
    public Result<List<Map<String, Object>>> getTodaySalesDistribution() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = cardKeyMapper.countBySpecBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row.get("name"));
            item.put("count", ((Number) row.get("cnt")).intValue());
            distribution.add(item);
        }
        return Result.success(distribution);
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
