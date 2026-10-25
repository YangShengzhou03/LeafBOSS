package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.AppVersion;
import com.leafboss.mapper.AppVersionMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/app-versions")
public class VersionController {

    private final AppVersionMapper appVersionMapper;

    public VersionController(AppVersionMapper appVersionMapper) {
        this.appVersionMapper = appVersionMapper;
    }

    /**
     * 管理端：分页获取版本列表
     */
    @GetMapping
    public Result getVersionList(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        IPage<AppVersion> pageResult = appVersionMapper.selectPage(
                new Page<>(page, size),
                new QueryWrapper<AppVersion>()
                        .orderByAsc("product_name")
                        .orderByDesc("created_at")
        );
        return Result.success(pageResult);
    }

    /**
     * 管理端：发布新版本
     */
    @PostMapping
    public Result createVersion(@RequestBody AppVersion version) {
        String error = validate(version);
        if (error != null) {
            return Result.error(error);
        }
        AppVersion existing = appVersionMapper.selectOne(
                new QueryWrapper<AppVersion>()
                        .eq("product_name", version.getProductName())
                        .eq("version", version.getVersion())
        );
        if (existing != null) {
            return Result.error("该应用已存在相同版本号");
        }
        // 新版本不得低于该应用已发布的最高版本
        AppVersion highest = getHighestVersion(version.getProductName(), null);
        if (highest != null && PublicUpdateController.compareVersion(version.getVersion(), highest.getVersion()) < 0) {
            return Result.error("版本号不得低于该应用已发布的最高版本 " + highest.getVersion());
        }
        version.setId(null);
        version.setCreatedAt(LocalDateTime.now());
        appVersionMapper.insert(version);
        return Result.success("版本已发布");
    }

    /**
     * 管理端：更新版本
     */
    @PutMapping("/{id}")
    public Result updateVersion(@PathVariable Long id, @RequestBody AppVersion version) {
        AppVersion existing = appVersionMapper.selectById(id);
        if (existing == null) {
            return Result.error("版本不存在");
        }
        String error = validate(version);
        if (error != null) {
            return Result.error(error);
        }
        // 版本号唯一性校验（排除自身）
        AppVersion duplicate = appVersionMapper.selectOne(
                new QueryWrapper<AppVersion>()
                        .eq("product_name", version.getProductName())
                        .eq("version", version.getVersion())
                        .ne("id", id)
        );
        if (duplicate != null) {
            return Result.error("该应用已存在相同版本号");
        }
        // 修改版本号时不得低于该应用其余版本中的最高版本
        AppVersion highest = getHighestVersion(version.getProductName(), id);
        if (highest != null && PublicUpdateController.compareVersion(version.getVersion(), highest.getVersion()) < 0) {
            return Result.error("版本号不得低于该应用已发布的最高版本 " + highest.getVersion());
        }
        existing.setProductName(version.getProductName());
        existing.setVersion(version.getVersion());
        existing.setDownloadUrl(version.getDownloadUrl());
        existing.setReleaseNotes(version.getReleaseNotes());
        existing.setForceUpdate(version.getForceUpdate());
        appVersionMapper.updateById(existing);
        return Result.success("版本已更新");
    }

    /**
     * 管理端：删除版本
     */
    @DeleteMapping("/{id}")
    public Result deleteVersion(@PathVariable Long id) {
        boolean deleted = appVersionMapper.deleteById(id) > 0;
        return deleted ? Result.success("版本已删除") : Result.error("版本不存在");
    }

    private static final String DEFAULT_RELEASE_NOTES = "修复已知问题，优化功能体验，提升运行稳定性";

    private String validate(AppVersion version) {
        if (version.getProductName() == null || version.getProductName().isBlank()) {
            return "应用标识不能为空";
        }
        if (version.getVersion() == null || version.getVersion().isBlank()) {
            return "版本号不能为空";
        }
        if (!version.getVersion().trim().matches("\\d+\\.\\d+\\.\\d+")) {
            return "版本号格式应为 x.y.z，如 1.2.0";
        }
        if (version.getDownloadUrl() == null || version.getDownloadUrl().isBlank()) {
            return "下载链接不能为空";
        }
        if (!version.getDownloadUrl().startsWith("https://")) {
            return "下载链接必须以 https:// 开头";
        }
        // 更新说明为空时自动预设默认文案
        if (version.getReleaseNotes() == null || version.getReleaseNotes().isBlank()) {
            version.setReleaseNotes(DEFAULT_RELEASE_NOTES);
        }
        return null;
    }

    /**
     * 获取指定应用当前最高版本号（excludeId 用于更新场景排除自身）
     */
    private AppVersion getHighestVersion(String productName, Long excludeId) {
        java.util.List<AppVersion> list = appVersionMapper.selectList(
                new QueryWrapper<AppVersion>().eq("product_name", productName)
        );
        AppVersion highest = null;
        for (AppVersion v : list) {
            if (excludeId != null && excludeId.equals(v.getId())) {
                continue;
            }
            if (highest == null || PublicUpdateController.compareVersion(v.getVersion(), highest.getVersion()) > 0) {
                highest = v;
            }
        }
        return highest;
    }
}
