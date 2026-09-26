package com.leafboss.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leafboss.entity.Company;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

    // ponytail: 借助 uk_companies_name 唯一索引原子化"不存在则插入"，消除先查后插的竞态窗口
    @Insert("INSERT INTO companies (name) VALUES (#{name}) ON DUPLICATE KEY UPDATE name = name")
    int insertOrIgnore(@Param("name") String name);

    // 不存在则创建（view_count 从 0 开始记 1），存在则原子自增，一条 SQL 完成建公司+计数
    @Insert("INSERT INTO companies (name, view_count) VALUES (#{name}, 1) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + 1")
    int upsertViewCount(@Param("name") String name);
}
