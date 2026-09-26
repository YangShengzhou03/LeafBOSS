package com.leafboss.dto;

import java.time.LocalDateTime;

public class SpecificationDTO {
    
    private Integer id;
    private Integer productId;
    private String productName;
    private String name;
    private Double price;
    private Integer stockQuantity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Integer totalKeys;     
    private Integer usedKeys;      
    private Integer unusedKeys;    
    private Integer disabledKeys;  

    public SpecificationDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalKeys() {
        return totalKeys;
    }

    public void setTotalKeys(Integer totalKeys) {
        this.totalKeys = totalKeys;
    }

    public Integer getUsedKeys() {
        return usedKeys;
    }

    public void setUsedKeys(Integer usedKeys) {
        this.usedKeys = usedKeys;
    }

    public Integer getUnusedKeys() {
        return unusedKeys;
    }

    public void setUnusedKeys(Integer unusedKeys) {
        this.unusedKeys = unusedKeys;
    }

    public Integer getDisabledKeys() {
        return disabledKeys;
    }

    public void setDisabledKeys(Integer disabledKeys) {
        this.disabledKeys = disabledKeys;
    }
}