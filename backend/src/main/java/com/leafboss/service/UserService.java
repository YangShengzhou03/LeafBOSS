package com.leafboss.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.entity.User;

public interface UserService extends IService<User> {
    
    User findByUsername(String username);
    
    User findByEmail(String email);
    
    Page<User> page(Page<User> pageInfo, String keyword, String status, String role);
}
