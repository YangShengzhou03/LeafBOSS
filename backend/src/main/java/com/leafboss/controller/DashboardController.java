package com.leafboss.controller;

import com.leafboss.common.Result;
import com.leafboss.entity.CardKey;
import com.leafboss.service.CardKeyService;
import com.leafboss.service.DashboardService;
import com.leafboss.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    @Autowired
    private CardKeyService cardKeyService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = dashboardService.getStats(null);

        // 管理员专属指标
        long totalCardKeys = cardKeyService.count();
        long activatedKeys = cardKeyService.lambdaQuery()
                .in(CardKey::getStatus, "已使用", "已吊销").count();
        stats.put("totalCardKeys", (int) totalCardKeys);
        stats.put("activationRate", totalCardKeys > 0 ? Math.round(activatedKeys * 10000.0 / totalCardKeys) / 100.0 : 0.0);
        stats.put("userCount", (int) userService.count());

        return Result.success(stats);
    }

    @GetMapping("/daily-revenue-trend")
    public Result<Map<String, Object>> getDailyRevenueTrend(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(dashboardService.getDailyRevenueTrend(days, null));
    }

    @GetMapping("/today-sales-distribution")
    public Result<List<Map<String, Object>>> getTodaySalesDistribution() {
        return Result.success(dashboardService.getTodaySalesDistribution(null));
    }
}
