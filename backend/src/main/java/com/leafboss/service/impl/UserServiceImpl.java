package com.leafboss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leafboss.entity.User;
import com.leafboss.mapper.UserMapper;
import com.leafboss.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return this.getOne(queryWrapper);
    }

    @Override
    public User findByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return this.getOne(queryWrapper);
    }

    @Override
    public Page<User> page(Page<User> pageInfo, String keyword, String status, String role) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like("username", keyword)
                .or()
                .like("email", keyword)
            );
        }
        
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        if (role != null && !role.trim().isEmpty()) {
            queryWrapper.eq("role", role);
        }
        
        queryWrapper.orderByDesc("registered_at");
        
        return this.page(pageInfo, queryWrapper);
    }
}
