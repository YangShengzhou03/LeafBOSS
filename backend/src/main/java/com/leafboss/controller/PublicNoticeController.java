package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leafboss.common.Result;
import com.leafboss.entity.Notice;
import com.leafboss.mapper.NoticeMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/notices")
public class PublicNoticeController {

    private final NoticeMapper noticeMapper;

    public PublicNoticeController(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @GetMapping
    public Result getNotices(@RequestParam String productName) {
        List<Notice> notices = noticeMapper.selectList(
                new QueryWrapper<Notice>()
                        .eq("product_name", productName)
                        .orderByDesc("created_at")
        );
        return Result.success(notices);
    }
}
