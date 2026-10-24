package com.leafboss.dto;

import java.util.List;

/**
 * 批量导入卡密请求体
 */
public class BatchImportRequest {

    private Integer specId;

    private List<String> keys;

    private String agentId;

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(Integer specId) {
        this.specId = specId;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
}
