package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetExcelService;
import com.school.management.application.asset.query.AssetQueryCriteria;
import com.school.management.common.ApiResponse;
import com.school.management.interfaces.rest.asset.dto.ImportResult;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 资产导入导出接口
 */
@Tag(name = "资产导入导出", description = "资产的Excel导入、导出、模板下载")
@Slf4j
@RestController
@RequestMapping("/v2/assets/excel")
@RequiredArgsConstructor
public class AssetExportController {

    private final AssetExcelService excelService;
    private final JwtTokenService jwtTokenService;

    @Operation(summary = "下载导入模板", description = "下载Excel导入模板，包含字段说明和示例数据")
    @GetMapping("/template")
    @PreAuthorize("hasAuthority('asset:manage')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        excelService.downloadTemplate(response);
    }

    @Operation(summary = "导入资产", description = "通过Excel文件批量导入资产，单次最多1000条")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<ImportResult> importAssets(@RequestParam("file") MultipartFile file) throws IOException {
        // 验证文件
        if (file.isEmpty()) {
            return ApiResponse.error("请选择要上传的文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ApiResponse.error("只支持 .xlsx 或 .xls 格式的文件");
        }

        // 限制文件大小 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error("文件大小不能超过 5MB");
        }

        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        ImportResult result = excelService.importAssets(file, userId, userName);
        return ApiResponse.success(result);
    }

    @Operation(summary = "导出资产列表", description = "将资产列表导出为Excel文件")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('asset:list')")
    public void exportAssets(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws IOException {

        AssetQueryCriteria criteria = AssetQueryCriteria.builder()
                .categoryId(categoryId)
                .status(status)
                .locationType(locationType)
                .locationId(locationId)
                .keyword(keyword)
                .build();

        excelService.exportAssets(criteria, response);
    }
}
