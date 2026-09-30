package com.leafboss.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.Company;
import com.leafboss.service.CompanyService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private LogUtil logUtil;

    @GetMapping
    public Result<IPage<Company>> getCompanies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        
        Page<Company> pageParam = new Page<>(page, size);
        QueryWrapper<Company> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        queryWrapper.orderByDesc("created_at");
        IPage<Company> companyPage = companyService.page(pageParam, queryWrapper);
        return Result.success(companyPage);
    }

    @PostMapping
    public Result<Boolean> createCompany(@RequestBody Company company, HttpServletRequest request) {
        if (company.getCommentCount() == null) {
            company.setCommentCount(0);
        }
        boolean saved = companyService.save(company);
        if (saved) {
            logUtil.logOperation("管理", "创建公司", request);
            return Result.success("公司创建成功", true);
        } else {
            return Result.error("公司创建失败");
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateCompany(@PathVariable String id, @RequestBody Company company, HttpServletRequest request) {
        company.setId(Integer.parseInt(id));
        boolean updated = companyService.updateById(company);
        if (updated) {
            logUtil.logOperation("管理", "更新公司", request);
            return Result.success("公司更新成功", true);
        } else {
            return Result.error("公司更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCompany(@PathVariable String id, HttpServletRequest request) {
        Company company = companyService.getById(Integer.parseInt(id));
        boolean deleted = companyService.removeById(Integer.parseInt(id));
        if (deleted) {
            logUtil.logOperation("管理", "删除公司", request);
            return Result.success("公司删除成功", true);
        } else {
            return Result.error("公司删除失败");
        }
    }
}
