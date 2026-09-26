package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.Product;
import com.leafboss.service.ProductService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private LogUtil logUtil;

    @GetMapping
    public Result<IPage<Product>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {

        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }

        IPage<Product> productPage = productService.page(pageParam, queryWrapper);
        return Result.success(productPage);
    }

    @PostMapping
    public Result<Boolean> createProduct(@RequestBody Product product, HttpServletRequest request) {
        if (productService.findByName(product.getName()) != null) {
            return Result.error("商品名称已存在");
        }

        if (product.getStatus() == null || product.getStatus().trim().isEmpty()) {
            product.setStatus("active");
        }

        boolean saved = productService.save(product);

        if (saved) {
            logUtil.logProductOperation("管理", "创建商品: " + product.getName(), request);

            return Result.success("商品创建成功", true);
        } else {
            return Result.error("商品创建失败");
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateProduct(@PathVariable String id, @RequestBody Product product, HttpServletRequest request) {
        Product existingProduct = productService.getById(Integer.parseInt(id));
        if (existingProduct == null) {
            return Result.error("商品不存在");
        }

        Product productWithSameName = productService.findByName(product.getName());
        if (productWithSameName != null && !productWithSameName.getId().equals(Integer.parseInt(id))) {
            return Result.error("商品名称已存在");
        }

        product.setId(Integer.parseInt(id));
        boolean updated = productService.updateById(product);

        if (updated) {
            logUtil.logProductOperation("管理", "更新商品: " + product.getName(), request);

            return Result.success("商品更新成功", true);
        } else {
            return Result.error("商品更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteProduct(@PathVariable String id, HttpServletRequest request) {
        Product product = productService.getById(Integer.parseInt(id));
        if (product == null) {
            return Result.error("商品不存在");
        }

        boolean deleted = productService.removeById(Integer.parseInt(id));

        if (deleted) {
            logUtil.logProductOperation("管理", "删除商品: " + product.getName(), request);

            return Result.success("商品删除成功", true);
        } else {
            return Result.error("商品删除失败");
        }
    }
}
