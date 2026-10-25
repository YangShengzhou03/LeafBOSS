package com.leafboss.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("app_versions")
public class AppVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("product_name")
    private String productName;

    @TableField("version")
    private String version;

    @TableField("download_url")
    private String downloadUrl;

    @TableField("release_notes")
    private String releaseNotes;

    @TableField("force_update")
    private Boolean forceUpdate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
    public Boolean getForceUpdate() { return forceUpdate; }
    public void setForceUpdate(Boolean forceUpdate) { this.forceUpdate = forceUpdate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
