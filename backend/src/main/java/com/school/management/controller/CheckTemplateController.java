package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.CheckTemplateCreateRequest;
import com.school.management.dto.CheckTemplateResponse;
import com.school.management.dto.CheckTemplateUpdateRequest;
import com.school.management.entity.CheckTemplate;
import com.school.management.service.CheckTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 检查模板控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.inspection.InspectionTemplateController} 替代
 *             V2 API 路径: /api/v2/inspection-templates
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/quantification/templates")
@RequiredArgsConstructor
public class CheckTemplateController {

    private final CheckTemplateService checkTemplateService;

    /**
     * 创建检查模板
     */
    @PostMapping
    @PreAuthorize("hasAuthority('quantification:template:add')")
    public Result<Long> createTemplate(@Valid @RequestBody CheckTemplateCreateRequest request) {
        Long id = checkTemplateService.createTemplate(request);
        return Result.success(id);
    }

    /**
     * 更新检查模板
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:template:edit')")
    public Result<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody CheckTemplateUpdateRequest request) {
        request.setId(id);
        checkTemplateService.updateTemplate(request);
        return Result.success();
    }

    /**
     * 删除检查模板
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:template:delete')")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        checkTemplateService.deleteTemplate(id);
        return Result.success();
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:template:view')")
    public Result<CheckTemplateResponse> getTemplate(@PathVariable Long id) {
        CheckTemplateResponse template = checkTemplateService.getTemplateById(id);
        return Result.success(template);
    }

    /**
     * 获取所有模板列表(包含类别信息)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('quantification:template:view')")
    public Result<List<CheckTemplateResponse>> getAllTemplates() {
        List<CheckTemplateResponse> templates = checkTemplateService.getAllTemplatesWithCategories();
        return Result.success(templates);
    }

    /**
     * 分页查询模板
     */
    @GetMapping
    @PreAuthorize("hasAuthority('quantification:template:view')")
    public Result<PageResult<CheckTemplate>> getTemplatePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer status) {
        IPage<CheckTemplate> page = checkTemplateService.getTemplatePage(pageNum, pageSize, templateName, status);
        return Result.success(PageResult.from(page));
    }
}
