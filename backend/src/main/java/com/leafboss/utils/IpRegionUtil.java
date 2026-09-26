package com.leafboss.utils;

import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class IpRegionUtil {

    private static final Logger log = LoggerFactory.getLogger(IpRegionUtil.class);
    private static volatile Ip2Region ip2Region;

    @PostConstruct
    public void init() {
        try {
            // JAR 内资源无法直接作为 File 使用，先解压到临时文件
            Path tempXdb = Files.createTempFile("ip2region_v4", ".xdb");
            try (InputStream is = new ClassPathResource("ip2region/ip2region_v4.xdb").getInputStream()) {
                Files.copy(is, tempXdb, StandardCopyOption.REPLACE_EXISTING);
            }

            Config v4Config = Config.custom()
                    .setCachePolicy(Config.VIndexCache)
                    .setSearchers(10)
                    .setXdbPath(tempXdb.toAbsolutePath().toString())
                    .asV4();

            ip2Region = Ip2Region.create(v4Config, null);
            log.info("ip2region 服务初始化成功");
        } catch (Exception e) {
            log.error("ip2region 服务初始化失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (ip2Region != null) {
            try {
                ip2Region.close();
            } catch (Exception e) {
                log.warn("ip2region 关闭失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 查询 IP 归属地，返回格式：中国|广东省|深圳市|电信|CN
     * 解析后返回 "省份城市" 格式，如 "广东深圳"
     */
    public static String getCityInfo(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "";
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "本地";
        }
        try {
            if (ip2Region == null) {
                return "";
            }
            String region = ip2Region.search(ip);
            if (region == null || region.isEmpty()) {
                return "";
            }
            return parseRegion(region);
        } catch (Exception e) {
            log.warn("IP归属地查询失败 [{}]: {}", ip, e.getMessage());
            return "";
        }
    }

    /**
     * 将 "中国|广东省|深圳市|电信|CN" 解析为 "广东深圳"
     */
    private static String parseRegion(String region) {
        String[] parts = region.split("\\|");
        if (parts.length < 3) {
            return "";
        }

        String province = parts[1];
        String city = parts[2];

        // 去除 "省"、"市" 后缀
        province = province.replace("省", "").replace("市", "");
        city = city.replace("市", "");

        // 处理直辖市：北京、上海、天津、重庆
        if (province.equals(city) || city.isEmpty() || "0".equals(city)) {
            return province;
        }

        return province + city;
    }
}
