package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.CardKey;
import com.leafboss.entity.Specification;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.SpecificationService;
import com.leafboss.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private SpecificationService specificationService;

    @Autowired
    private UserService userService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

            int dailySales = getActivatedCount(startOfDay, endOfDay);
            double dailyRevenue = getActivatedRevenue(startOfDay, endOfDay);

            LocalDate yesterday = today.minusDays(1);
            LocalDateTime startOfYesterday = yesterday.atStartOfDay();
            LocalDateTime endOfYesterday = today.atStartOfDay();

            int yesterdaySales = getActivatedCount(startOfYesterday, endOfYesterday);
            double yesterdayRevenue = getActivatedRevenue(startOfYesterday, endOfYesterday);

            stats.put("dailySales", dailySales);
            stats.put("dailyRevenue", Math.round(dailyRevenue * 100.0) / 100.0);
            stats.put("yesterdaySales", yesterdaySales);
            stats.put("yesterdayRevenue", Math.round(yesterdayRevenue * 100.0) / 100.0);

            long totalOrders = cardKeyService.count();
            double totalRevenue = getActivatedRevenue(null, null);

            LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            double monthlyRevenue = getActivatedRevenue(startOfMonth, endOfMonth);

            LocalDateTime startOfLastMonth = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfLastMonth = today.withDayOfMonth(1).atStartOfDay();
            double lastMonthRevenue = getActivatedRevenue(startOfLastMonth, endOfLastMonth);

            stats.put("totalOrders", (int)totalOrders);
            stats.put("monthlyRevenue", Math.round(monthlyRevenue * 100.0) / 100.0);
            stats.put("lastMonthRevenue", Math.round(lastMonthRevenue * 100.0) / 100.0);

            // 本周/上周收入
            LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
            LocalDateTime endOfWeek = startOfWeek.plusDays(7);
            double weeklyRevenue = getActivatedRevenue(startOfWeek, endOfWeek);

            LocalDateTime startOfLastWeek = startOfWeek.minusDays(7);
            double lastWeekRevenue = getActivatedRevenue(startOfLastWeek, startOfWeek);

            stats.put("weeklyRevenue", Math.round(weeklyRevenue * 100.0) / 100.0);
            stats.put("lastWeekRevenue", Math.round(lastWeekRevenue * 100.0) / 100.0);

            // 激活率
            long totalCardKeys = cardKeyService.count();
            long activatedKeys = getActivatedCount(null, null);
            double activationRate = totalCardKeys > 0 ? Math.round((activatedKeys * 10000.0 / totalCardKeys)) / 100.0 : 0.0;

            stats.put("totalCardKeys", (int)totalCardKeys);
            stats.put("activationRate", activationRate);

            // 剩余卡密
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> unusedQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            unusedQuery.eq("status", "未使用");
            long cardKeyCount = cardKeyService.count(unusedQuery);
            stats.put("cardKeyCount", (int)cardKeyCount);

            // 仓库总值（未使用卡密的规格价格之和，不含禁用和已使用）
            double stockValue = getStockValue();
            stats.put("stockValue", Math.round(stockValue * 100.0) / 100.0);

            // 用户数
            long userCount = userService.count();
            stats.put("userCount", (int) userCount);

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取仪表盘数据失败", e);
            return Result.error("获取仪表盘数据失败");
        }
    }

    private int getActivatedCount(LocalDateTime start, LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (start != null) queryWrapper.ge("activate_time", start);
        if (end != null) queryWrapper.lt("activate_time", end);
        return (int) cardKeyService.count(queryWrapper);
    }

    private double getStockValue() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("status", "未使用");
        queryWrapper.isNotNull("specification_id");
        List<CardKey> unusedCards = cardKeyService.list(queryWrapper);
        if (unusedCards.isEmpty()) return 0.0;

        List<Integer> specIds = unusedCards.stream()
                .map(CardKey::getSpecificationId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        Map<Integer, Specification> specMap = specificationService.listByIds(specIds).stream()
                .collect(java.util.stream.Collectors.toMap(Specification::getId, spec -> spec));

        double value = 0.0;
        for (CardKey card : unusedCards) {
            Specification spec = specMap.get(card.getSpecificationId());
            if (spec != null && spec.getPrice() != null) {
                value += spec.getPrice();
            }
        }
        return value;
    }

    private double getActivatedRevenue(LocalDateTime start, LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (start != null) queryWrapper.ge("activate_time", start);
        if (end != null) queryWrapper.lt("activate_time", end);
        queryWrapper.isNotNull("specification_id");

        List<CardKey> activatedCards = cardKeyService.list(queryWrapper);
        if (activatedCards.isEmpty()) return 0.0;

        List<Integer> specIds = activatedCards.stream()
                .map(CardKey::getSpecificationId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        Map<Integer, Specification> specMap = specificationService.listByIds(specIds).stream()
                .collect(java.util.stream.Collectors.toMap(Specification::getId, spec -> spec));

        double revenue = 0.0;
        for (CardKey card : activatedCards) {
            Specification spec = specMap.get(card.getSpecificationId());
            if (spec != null && spec.getPrice() != null) {
                revenue += spec.getPrice();
            }
        }
        return revenue;
    }

    @GetMapping("/daily-revenue-trend")
    public Result<Map<String, Object>> getDailyRevenueTrend(
            @RequestParam(defaultValue = "30") int days) {
        try {
            if (days < 1) days = 1;
            if (days > 90) days = 90;
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(days - 1);

            // Collect all activated cards in the range
            LocalDateTime rangeStart = startDate.atStartOfDay();
            LocalDateTime rangeEnd = today.plusDays(1).atStartOfDay();
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> rangeQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            rangeQuery.ge("activate_time", rangeStart);
            rangeQuery.lt("activate_time", rangeEnd);
            rangeQuery.isNotNull("specification_id");
            List<CardKey> allCards = cardKeyService.list(rangeQuery);

            // Build spec name map
            List<Integer> specIds = allCards.stream().map(CardKey::getSpecificationId).distinct().collect(java.util.stream.Collectors.toList());
            Map<Integer, String> specNameMap = new HashMap<>();
            if (!specIds.isEmpty()) {
                for (Specification spec : specificationService.listByIds(specIds)) {
                    specNameMap.put(spec.getId(), spec.getName());
                }
            }

            // Group cards by date and spec
            List<Map<String, Object>> trend = new java.util.ArrayList<>();
            Map<String, Map<String, Integer>> specDailyMap = new java.util.LinkedHashMap<>(); // specName -> {date -> count}
            List<String> specNames = new java.util.ArrayList<>();

            for (int i = 0; i < days; i++) {
                LocalDate date = startDate.plusDays(i);
                String dateStr = date.toString();
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

                int count = 0;
                double revenue = 0.0;
                Map<String, Integer> daySpecCounts = new HashMap<>();

                for (CardKey card : allCards) {
                    if (!card.getActivateTime().isBefore(dayStart) && card.getActivateTime().isBefore(dayEnd)) {
                        count++;
                        String sName = specNameMap.getOrDefault(card.getSpecificationId(), "未知");
                        daySpecCounts.put(sName, daySpecCounts.getOrDefault(sName, 0) + 1);
                        Specification spec = specIds.isEmpty() ? null :
                            specificationService.getById(card.getSpecificationId());
                        if (spec != null && spec.getPrice() != null) revenue += spec.getPrice();
                    }
                }

                Map<String, Object> item = new HashMap<>();
                item.put("date", dateStr);
                item.put("count", count);
                item.put("revenue", Math.round(revenue * 100.0) / 100.0);
                trend.add(item);

                // Accumulate spec daily data
                for (Map.Entry<String, Integer> entry : daySpecCounts.entrySet()) {
                    String sName = entry.getKey();
                    if (!specNames.contains(sName)) specNames.add(sName);
                    specDailyMap.computeIfAbsent(sName, k -> new java.util.LinkedHashMap<>())
                            .put(dateStr, entry.getValue());
                }
            }

            // Build specTrend list: [{name, data: [count per day]}]
            List<Map<String, Object>> specTrend = new java.util.ArrayList<>();
            List<String> dateList = new java.util.ArrayList<>();
            for (int i = 0; i < days; i++) {
                dateList.add(startDate.plusDays(i).toString());
            }
            for (String sName : specNames) {
                Map<String, Object> s = new HashMap<>();
                s.put("name", sName);
                Map<String, Integer> dayMap = specDailyMap.getOrDefault(sName, new HashMap<>());
                List<Integer> data = new java.util.ArrayList<>();
                for (String d : dateList) {
                    data.add(dayMap.getOrDefault(d, 0));
                }
                s.put("data", data);
                specTrend.add(s);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("trend", trend);
            result.put("specTrend", specTrend);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取收入趋势失败", e);
            return Result.error("获取收入趋势失败");
        }
    }

    @GetMapping("/today-sales-distribution")
    public Result<List<Map<String, Object>>> getTodaySalesDistribution() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CardKey> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.ge("activate_time", startOfDay);
            queryWrapper.lt("activate_time", endOfDay);
            queryWrapper.isNotNull("specification_id");

            List<CardKey> todayActivatedCards = cardKeyService.list(queryWrapper);
            if (todayActivatedCards.isEmpty()) return Result.success(new java.util.ArrayList<>());

            Map<Integer, Integer> specCountMap = new HashMap<>();
            for (CardKey card : todayActivatedCards) {
                specCountMap.put(card.getSpecificationId(), specCountMap.getOrDefault(card.getSpecificationId(), 0) + 1);
            }

            List<Integer> specIds = new java.util.ArrayList<>(specCountMap.keySet());
            List<Specification> specs = specificationService.listByIds(specIds);

            List<Map<String, Object>> distribution = new java.util.ArrayList<>();
            for (Specification spec : specs) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", spec.getName());
                item.put("count", specCountMap.get(spec.getId()));
                distribution.add(item);
            }

            return Result.success(distribution);
        } catch (Exception e) {
            log.error("获取当日售出分布失败", e);
            return Result.error("获取当日售出分布失败");
        }
    }
}
