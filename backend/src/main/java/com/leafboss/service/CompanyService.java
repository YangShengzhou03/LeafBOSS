package com.leafboss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.entity.Company;

public interface CompanyService extends IService<Company> {
    Company getOrCreateByName(String name);
}
