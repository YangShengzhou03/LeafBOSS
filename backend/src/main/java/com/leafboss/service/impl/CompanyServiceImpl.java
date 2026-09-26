package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.entity.Company;
import com.leafboss.mapper.CompanyMapper;
import com.leafboss.service.CompanyService;
import org.springframework.stereotype.Service;

@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {

    @Override
    public Company getOrCreateByName(String name) {
        baseMapper.insertOrIgnore(name);
        return getOne(new QueryWrapper<Company>().eq("name", name));
    }
}
