package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.OperationLog;
import com.leafboss.service.OperationLogService;
import com.leafboss.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private LogUtil logUtil;

    @GetMapping
    public Result<IPage<OperationLog>> getOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) List<String> operationTypes) {
        Page<OperationLog> pageInfo = new Page<>(page, size);
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();

        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("created_at", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("created_at", endDate + " 23:59:59");
        }

        if (operationTypes != null && !operationTypes.isEmpty()) {
            queryWrapper.in("operation_type", operationTypes);
        } else if (operationType != null && !operationType.isEmpty()) {
            queryWrapper.eq("operation_type", operationType);
        }

        queryWrapper.orderByDesc("created_at");
        IPage<OperationLog> result = operationLogService.page(pageInfo, queryWrapper);
        return Result.success(result);
    }

    @DeleteMapping
    public Result<Boolean> clearLogs(HttpServletRequest request) {
        boolean result = operationLogService.clearLogs();
        if (result) {
            logUtil.logOperation("管理", "清空了日志", request);
            return Result.success("日志清空成功", true);
        }
        return Result.error("日志清空失败");
    }
}