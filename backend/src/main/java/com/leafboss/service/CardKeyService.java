package com.leafboss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.dto.BatchImportRequest;
import com.leafboss.dto.CardKeyDTO;
import com.leafboss.entity.CardKey;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

public interface CardKeyService extends IService<CardKey> {

    CardKey findByCardKey(String cardKey);

    IPage<CardKeyDTO> getCardKeyListWithDetails(Page<CardKey> pageParam, String keyword, Long specificationId, String status, String agentId);

    boolean activateCard(String cardKey, String userEmail);

    /**
     * 兑换卡密：激活卡密并创建用户商品授权
     * @param cardKey 卡密代码
     * @param userId 当前用户ID
     * @return 兑换结果（成功时包含商品信息）
     */
    Map<String, Object> redeemCardKey(String cardKey, String userId);

    /**
     * 批量导入卡密（校验先于扣减，扣减与写入同事务）
     * @param req 导入请求
     * @param operatorId 当前登录用户 ID
     * @param isAdmin 是否管理员
     * @return 导入结果（message/imported/skipped）
     */
    Map<String, Object> batchImport(BatchImportRequest req, String operatorId, boolean isAdmin);

    /**
     * 变更卡密状态并同步关联的用户授权状态
     */
    void updateStatus(String cardKey, String status);

    /**
     * 按卡密删除（先吊销关联的用户授权）
     */
    void deleteByCardKey(String cardKey);

    /**
     * 代理商吊销自己的卡密
     */
    void agentRevoke(String cardKey, String agentId);

    /**
     * 代理商恢复自己的卡密
     */
    void agentRestore(String cardKey, String agentId);

    /**
     * 代理商删除自己的卡密
     */
    void agentDelete(String cardKey, String agentId);
}
