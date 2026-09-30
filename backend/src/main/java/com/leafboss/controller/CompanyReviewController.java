package com.leafboss.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.BossReview;
import com.leafboss.service.CompanyReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Token 鉴权公司评论接口
 * 路径前缀：/api/v1/company-reviews
 *
 * 所有端点需登录（JwtInterceptor 拦截未登录请求）。
 */
@RestController
@RequestMapping("/api/v1/company-reviews")
public class CompanyReviewController {

    @Resource
    private CompanyReviewService companyReviewService;

    /**
     * 分页获取公司评论
     * GET /api/v1/company-reviews?company_name=...&page=1&size=10
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam("company_name") String companyName,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("currentUserId");
        Page<BossReview> result = companyReviewService.listByCompany(companyName, page, size, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    /**
     * 提交评论
     * POST /api/v1/company-reviews
     */
    @PostMapping
    public Result<Map<String, String>> create(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String companyName = body.get("company_name");
        String content = body.get("content");
        String userId = (String) request.getAttribute("currentUserId");

        if (companyName == null || companyName.isBlank()) {
            return Result.error(400, "公司名称不能为空");
        }
        if (content == null || content.isBlank()) {
            return Result.error(400, "评论内容不能为空");
        }
        if (content.length() > 500) {
            return Result.error(400, "评论内容不能超过500字符");
        }

        try {
            BossReview review = companyReviewService.createReview(companyName, content.trim(), userId);

            Map<String, String> data = new HashMap<>();
            data.put("id", String.valueOf(review.getId()));
            data.put("createdAt", String.valueOf(review.getCreatedAt()));
            return Result.success(data);
        } catch (IllegalStateException e) {
            return Result.error(429, e.getMessage());
        }
    }

    /**
     * 删除评论（仅作者本人可删除）
     * DELETE /api/v1/company-reviews/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(
            @PathVariable("commentId") Integer commentId,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("currentUserId");
        boolean deleted = companyReviewService.deleteReview(commentId, userId);
        if (!deleted) {
            return Result.error(403, "评论不存在或无权限删除");
        }
        return Result.success(null);
    }

    /**
     * 点赞/踩（toggle 语义）
     * POST /api/v1/company-reviews/{commentId}/vote
     */
    @PostMapping("/{commentId}/vote")
    public Result<Map<String, Object>> vote(
            @PathVariable("commentId") Integer commentId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String vote = body.get("vote");
        String userId = (String) request.getAttribute("currentUserId");

        if (!"like".equals(vote) && !"dislike".equals(vote)) {
            return Result.error(400, "投票类型必须为 like 或 dislike");
        }

        try {
            CompanyReviewService.VoteResult result = companyReviewService.vote(commentId, vote, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("likeCount", result.likeCount);
            data.put("dislikeCount", result.dislikeCount);
            data.put("myVote", result.myVote);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        }
    }
}
