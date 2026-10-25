package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leafboss.common.Result;
import com.leafboss.entity.AppVersion;
import com.leafboss.mapper.AppVersionMapper;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公开接口：应用自动更新检查
 * 应用端传入应用标识与当前版本，返回该应用最新版本信息及是否有更新
 */
@RestController
@RequestMapping("/api/public/updates")
public class PublicUpdateController {

    private final AppVersionMapper appVersionMapper;

    public PublicUpdateController(AppVersionMapper appVersionMapper) {
        this.appVersionMapper = appVersionMapper;
    }

    @GetMapping
    public Result checkUpdate(@RequestParam("productName") String appName,
                              @RequestParam("version") String currentVersion) {
        // 参数校验前置，避免无效请求触发数据库查询
        if (appName == null || appName.isBlank()) {
            return Result.error("应用标识不能为空");
        }
        if (currentVersion == null || currentVersion.isBlank()) {
            return Result.error("当前版本号不能为空");
        }
        // 取该应用所有版本中语义化版本号最高的一个（而非仅按发布时间，防止回滚发布后误报更新）
        java.util.List<AppVersion> versions = appVersionMapper.selectList(
                new QueryWrapper<AppVersion>().eq("product_name", appName)
        );
        AppVersion latest = null;
        for (AppVersion v : versions) {
            if (latest == null || compareVersion(v.getVersion(), latest.getVersion()) > 0) {
                latest = v;
            }
        }
        if (latest == null) {
            return Result.error("未知的应用标识: " + appName);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hasUpdate", compareVersion(latest.getVersion(), currentVersion) > 0);
        data.put("latestVersion", latest.getVersion());
        data.put("currentVersion", currentVersion);
        data.put("downloadUrl", latest.getDownloadUrl());
        data.put("releaseNotes", latest.getReleaseNotes());
        data.put("forceUpdate", Boolean.TRUE.equals(latest.getForceUpdate()));
        data.put("publishedAt", latest.getCreatedAt());
        return Result.success(data);
    }

    /**
     * 语义化版本比较：逐段按数字比较，返回正数表示 v1 更新、负数表示 v2 更新、0 表示相同
     * 兼容 v 前缀与不等长版本段（如 1.2 与 1.2.0 视为相同）
     */
    static int compareVersion(String v1, String v2) {
        String[] s1 = normalize(v1).split("\\.");
        String[] s2 = normalize(v2).split("\\.");
        int len = Math.max(s1.length, s2.length);
        for (int i = 0; i < len; i++) {
            int a = i < s1.length ? parseIntOrZero(s1[i]) : 0;
            int b = i < s2.length ? parseIntOrZero(s2[i]) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    private static String normalize(String version) {
        String v = version.trim().toLowerCase();
        if (v.startsWith("v")) {
            v = v.substring(1);
        }
        return v;
    }

    private static int parseIntOrZero(String segment) {
        try {
            return Integer.parseInt(segment.trim());
        } catch (NumberFormatException e) {
            // ponytail: 非纯数字段（如 1.2.0-beta）按 0 处理；如需支持预发布标签再引入完整 semver 库
            return 0;
        }
    }
}
