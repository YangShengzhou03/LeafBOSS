package com.leafboss.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.entity.Admin;

public interface AdminService extends IService<Admin> {

    Admin findByUsername(String username);

    Admin findByEmail(String email);

    Page<Admin> page(Page<Admin> pageInfo, String keyword, String status);
}