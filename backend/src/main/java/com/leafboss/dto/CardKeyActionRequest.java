package com.leafboss.dto;

/**
 * 卡密操作请求体（代理商吊销/恢复）
 */
public class CardKeyActionRequest {

    private String cardKey;

    public String getCardKey() {
        return cardKey;
    }

    public void setCardKey(String cardKey) {
        this.cardKey = cardKey;
    }
}
