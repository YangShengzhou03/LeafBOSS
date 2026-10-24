package com.leafboss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leafboss.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    Product findByName(String name);
}