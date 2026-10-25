package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.entity.OperationLog;
import com.leafboss.mapper.OperationLogMapper;
import com.leafboss.service.OperationLogService;
import com.leafboss.utils.IpRegionUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public boolean clearLogs() {
        try {
            QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
            return this.remove(queryWrapper);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void logOperation(String operationType, String description, String ipAddress) {
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationType(operationType);
        operationLog.setDescription(description);
        operationLog.setIpAddress(ipAddress);
        operationLog.setIpRegion(IpRegionUtil.getCityInfo(ipAddress));
        operationLog.setCreatedAt(LocalDateTime.now());
        
        this.save(operationLog);
    }
}