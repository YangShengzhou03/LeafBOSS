package com.leafboss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.dto.CardKeyDTO;
import com.leafboss.entity.CardKey;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

public interface CardKeyService extends IService<CardKey> {

    CardKey findByCardKey(String cardKey);

    IPage<CardKeyDTO> getCardKeyListWithDetails(Page<CardKey> pageParam, String keyword, Long specificationId, String status, String agentId);

    boolean activateCard(String cardKey, String userEmail);

    boolean batchDeleteUsedCardKeys();

    /**
     * 兑换卡密：激活卡密并创建用户商品授权
     * @param cardKey 卡密代码
     * @param userId 当前用户ID
     * @return 兑换结果（成功时包含商品信息）
     */
    Map<String, Object> redeemCardKey(String cardKey, String userId);
}
