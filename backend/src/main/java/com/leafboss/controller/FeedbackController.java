package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leafboss.common.Result;
import com.leafboss.entity.Feedback;
import com.leafboss.mapper.FeedbackMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;

    public FeedbackController(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @GetMapping
    public Result getFeedbackList() {
        List<Feedback> list = feedbackMapper.selectList(
                new QueryWrapper<Feedback>().orderByDesc("created_at")
        );
        return Result.success(list);
    }

    @DeleteMapping("/{id}")
    public Result deleteFeedback(@PathVariable Long id) {
        feedbackMapper.deleteById(id);
        return Result.success("反馈已删除");
    }
}
