package com.leafboss.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.entity.AgentAuthorization;

import java.util.List;
import java.util.Map;

public interface AgentAuthorizationService {

    /**
     * 按 ID 查询
     */
    AgentAuthorization getById(Integer id);

    /**
     * 查询代理商对指定商品的有效授权
     */
    AgentAuthorization findActiveAuthorization(String agentId, Integer productId);

    /**
     * 原子扣减余额（生成/导入卡密时调用，余额不足返回 false）
     */
    boolean deductRemaining(String agentId, Integer productId, int count);

    /**
     * 补偿回加余额（生成/导入失败时调用）
     */
    void refundRemaining(String agentId, Integer productId, int count);

    /**
     * 查询代理商已授权的商品列表（去重、仅有效授权）
     */
    List<Map<String, Object>> findAuthorizedProducts(String agentId);

    /**
     * 分页查询代理授权列表（admin 用，带 agent/product 详情）
     */
    Page<Map<String, Object>> listWithDetails(Page<AgentAuthorization> page, String keyword, Integer status);

    /**
     * 创建代理授权
     */
    boolean createAuthorization(AgentAuthorization auth);

    /**
     * 更新代理授权
     */
    boolean updateAuthorization(AgentAuthorization auth);

    /**
     * 吊销（status=0）
     */
    boolean revoke(Integer id);

    /**
     * 恢复（status=1）
     */
    boolean restore(Integer id);

    /**
     * 删除授权记录
     */
    boolean delete(Integer id);
}
