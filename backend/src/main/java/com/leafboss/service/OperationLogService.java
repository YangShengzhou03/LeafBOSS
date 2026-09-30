package com.leafboss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    boolean clearLogs();

    void logOperation(String operationType, String description, String ipAddress);
}