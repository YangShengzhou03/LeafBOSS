package com.leafboss.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leafboss.common.Result;
import com.leafboss.entity.BossReview;
import com.leafboss.entity.Company;
import com.leafboss.service.CompanyReviewService;
import com.leafboss.service.CompanyService;
import com.leafboss.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员评论管理接口
 * 路径前缀：/api/admin/reviews
 * 需要 admin 角色（JwtInterceptor 拦截）。
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    @Resource
    private CompanyReviewService companyReviewService;

    @Resource
    private CompanyService companyService;

    @Resource
    private LogUtil logUtil;

    /**
     * 分页获取全部评论（可选按公司名过滤）
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "company_name", required = false) String companyName) {

        Page<BossReview> result = companyReviewService.listAllForAdmin(page, size, companyName);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    /**
     * 删除任意评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(
            @PathVariable("commentId") Integer commentId,
            HttpServletRequest request) {

        boolean deleted = companyReviewService.deleteReviewAsAdmin(commentId);
        if (!deleted) {
            return Result.error(404, "评论不存在");
        }
        logUtil.logOperation("管理", "删除评论", request);
        return Result.success(null);
    }

    /**
     * 导出全部评论为 xlsx
     * GET /api/admin/reviews/export?company_name=...
     */
    @GetMapping("/export")
    public void export(
            @RequestParam(value = "company_name", required = false) String companyName,
            HttpServletResponse response) throws Exception {

        // 拉取全部数据（上限 10000 条）
        List<BossReview> allReviews = new ArrayList<>();
        int page = 1;
        int size = 200;
        while (page <= 50) {
            Page<BossReview> result = companyReviewService.listAllForAdmin(page, size, companyName);
            allReviews.addAll(result.getRecords());
            if (result.getRecords().size() < size) break;
            page++;
        }

        // 生成 xlsx
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("评论数据");

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "公司", "评论内容", "用户ID", "点赞数", "踩数", "评论时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据行
        int rowNum = 1;
        for (BossReview r : allReviews) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getId());
            row.createCell(1).setCellValue(r.getCompanyName() != null ? r.getCompanyName() : "");
            row.createCell(2).setCellValue(r.getContent() != null ? r.getContent() : "");
            row.createCell(3).setCellValue(r.getUserId() != null ? r.getUserId() : "");
            row.createCell(4).setCellValue(r.getLikeCount() != null ? r.getLikeCount() : 0);
            row.createCell(5).setCellValue(r.getDislikeCount() != null ? r.getDislikeCount() : 0);
            row.createCell(6).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
        }

        // 自动列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 输出
        String filename = "reviews_" + LocalDateTime.now().toString().substring(0, 19).replace(':', '-') + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 导入评论（xlsx 文件）
     * POST /api/admin/reviews/import
     * 列顺序：公司名 | 评论内容
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importReviews(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error(400, "仅支持 .xlsx 或 .xls 文件");
        }

        int successCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            Sheet sheet = workbook.getSheetAt(0);
            String adminUserId = "admin_import_" + System.currentTimeMillis();

            // 从第2行开始（跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell companyCell = row.getCell(0);
                Cell contentCell = row.getCell(1);

                String companyName = companyCell != null ? getCellStringValue(companyCell).trim() : "";
                String content = contentCell != null ? getCellStringValue(contentCell).trim() : "";

                if (companyName.isEmpty() || content.isEmpty()) {
                    if (companyName.isEmpty() && content.isEmpty()) continue; // 空行跳过
                    skipCount++;
                    errors.add("第" + (i + 1) + "行：公司名或内容为空");
                    continue;
                }

                if (content.length() > 500) {
                    content = content.substring(0, 500);
                }

                try {
                    Company company = companyService.getOrCreateByName(companyName);
                    BossReview review = new BossReview();
                    review.setCompanyId(company.getId());
                    review.setContent(content);
                    review.setUserId(adminUserId);
                    review.setLikeCount(0);
                    review.setDislikeCount(0);
                    review.setCreatedAt(LocalDateTime.now());
                    review.setIsDeleted(0);

                    companyReviewService.createReviewForAdmin(review);
                    successCount++;
                } catch (Exception e) {
                    skipCount++;
                    errors.add("第" + (i + 1) + "行：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            return Result.error(500, "文件读取失败：" + e.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("skipCount", skipCount);
        data.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);

        logUtil.logOperation("管理", "导入评论 " + successCount + " 条", request);
        return Result.success(data);
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
