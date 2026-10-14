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
}
