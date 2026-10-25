package com.leafboss.dto;

/**
 * 卡密状态变更请求体
 */
public class CardKeyStatusRequest {

    private String cardKey;

    private String status;

    public String getCardKey() {
        return cardKey;
    }

    public void setCardKey(String cardKey) {
        this.cardKey = cardKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
