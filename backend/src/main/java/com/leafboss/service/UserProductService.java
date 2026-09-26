package com.leafboss.service;

import com.leafboss.entity.UserProduct;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.dto.UserProductDTO;

public interface UserProductService extends IService<UserProduct> {

    IPage<UserProductDTO> getUserProductListWithDetails(Page<UserProduct> pageParam, String keyword, Integer status);

    IPage<UserProductDTO> getUserProductListWithDetails(Page<UserProduct> pageParam, String keyword, Integer status, String userId);
}
