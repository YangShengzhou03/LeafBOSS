package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.Notice;
import com.leafboss.mapper.NoticeMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeMapper noticeMapper;

    public NoticeController(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    /**
     * 公共接口：按商品名获取通知（无需鉴权）
     */
    @GetMapping("/public")
    public Result getPublicNotices(@RequestParam String productName) {
        List<Notice> notices = noticeMapper.selectList(
                new QueryWrapper<Notice>()
                        .eq("product_name", productName)
                        .orderByDesc("created_at")
        );
        return Result.success(notices);
    }

    /**
     * 管理端：分页获取通知列表
     */
    @GetMapping
    public Result getNoticeList(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        IPage<Notice> pageResult = noticeMapper.selectPage(
                new Page<>(page, size),
                new QueryWrapper<Notice>().orderByDesc("created_at")
        );
        return Result.success(pageResult.getRecords());
    }

    /**
     * 管理端：创建通知
     */
    @PostMapping
    public Result createNotice(@RequestBody Notice notice) {
        notice.setId(null);
        notice.setCreatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        return Result.success("通知已创建");
    }

    /**
     * 管理端：更新通知
     */
    @PutMapping("/{id}")
    public Result updateNotice(@PathVariable Long id, @RequestBody Notice notice) {
        Notice existing = noticeMapper.selectById(id);
        if (existing == null) {
            return Result.error("通知不存在");
        }
        existing.setProductName(notice.getProductName());
        existing.setContent(notice.getContent());
        existing.setLevel(notice.getLevel());
        noticeMapper.updateById(existing);
        return Result.success("通知已更新");
    }

    /**
     * 管理端：删除通知
     */
    @DeleteMapping("/{id}")
    public Result deleteNotice(@PathVariable Long id) {
        noticeMapper.deleteById(id);
        return Result.success("通知已删除");
    }
}
