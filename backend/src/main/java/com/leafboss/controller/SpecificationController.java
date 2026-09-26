package com.leafboss.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.dto.SpecificationDTO;
import com.leafboss.entity.Specification;
import com.leafboss.entity.Product;
import com.leafboss.service.SpecificationService;
import com.leafboss.service.ProductService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specifications")
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private LogUtil logUtil;

    @PostMapping
    public Result<Boolean> createSpecification(@RequestBody Specification specification, HttpServletRequest request) {
        if (specificationService.findByName(specification.getName()) != null) {
            return Result.error("规格名称已存在");
        }

        if (specification.getStatus() == null || specification.getStatus().trim().isEmpty()) {
            specification.setStatus("active");
        }

        boolean saved = specificationService.save(specification);

        if (saved) {
            String productName = "未知商品";
            if (specification.getProductId() != null) {
                Product product = productService.getById(specification.getProductId());
                if (product != null) {
                    productName = product.getName();
                }
            }

            logUtil.logSpecificationOperation("管理", "创建规格: " + productName + "-" + specification.getName(), request);

            return Result.success("规格创建成功", true);
        } else {
            return Result.error("规格创建失败");
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateSpecification(@PathVariable String id, @RequestBody Specification specification, HttpServletRequest request) {
        Specification existingSpecification = specificationService.getById(Integer.parseInt(id));
        if (existingSpecification == null) {
            return Result.error("规格不存在");
        }

        Specification specWithSameName = specificationService.findByName(specification.getName());
        if (specWithSameName != null && !specWithSameName.getId().equals(Integer.parseInt(id))) {
            return Result.error("规格名称已存在");
        }

        specification.setId(Integer.parseInt(id));
        boolean updated = specificationService.updateById(specification);

        if (updated) {
            String productName = "未知商品";
            if (specification.getProductId() != null) {
                Product product = productService.getById(specification.getProductId());
                if (product != null) {
                    productName = product.getName();
                }
            }

            logUtil.logSpecificationOperation("管理", "更新规格: " + productName + "-" + specification.getName(), request);

            return Result.success("规格更新成功", true);
        } else {
            return Result.error("规格更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSpecification(@PathVariable String id, HttpServletRequest request) {
        Specification specification = specificationService.getById(Integer.parseInt(id));
        if (specification == null) {
            return Result.error("规格不存在");
        }

        boolean deleted = specificationService.removeById(Integer.parseInt(id));

        if (deleted) {
            String productName = "未知商品";
            if (specification.getProductId() != null) {
                Product product = productService.getById(specification.getProductId());
                if (product != null) {
                    productName = product.getName();
                }
            }

            logUtil.logSpecificationOperation("管理", "删除规格: " + productName + "-" + specification.getName(), request);

            return Result.success("规格删除成功", true);
        } else {
            return Result.error("规格删除失败");
        }
    }

    @GetMapping("/dto")
    public Result<List<SpecificationDTO>> getSpecificationDTOs() {
        List<SpecificationDTO> specificationDTOs = specificationService.getSpecificationDTOs();
        return Result.success(specificationDTOs);
    }

    @GetMapping("/dto/pagination")
    public Result<IPage<SpecificationDTO>> getSpecificationDTOsWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId) {
        Page<Specification> pageInfo = new Page<>(page, size);
        IPage<SpecificationDTO> result = specificationService.getSpecificationDTOsWithPagination(pageInfo, keyword, productId);
        return Result.success(result);
    }
}