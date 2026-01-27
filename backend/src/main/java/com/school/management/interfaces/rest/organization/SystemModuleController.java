package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.SystemModuleService;
import com.school.management.common.ApiResponse;
import com.school.management.domain.organization.model.SystemModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统模块管理控制器
 */
@Tag(name = "系统模块管理", description = "系统模块树查询接口")
@RestController
@RequestMapping("/v2/system-modules")
@RequiredArgsConstructor
public class SystemModuleController {

    private final SystemModuleService systemModuleService;

    @Operation(summary = "获取模块树形结构")
    @GetMapping("/tree")
    public ApiResponse<List<SystemModuleResponse>> getModuleTree() {
        List<SystemModule> modules = systemModuleService.getModuleTree();
        List<SystemModuleResponse> response = modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取所有启用的模块（平铺列表）")
    @GetMapping
    public ApiResponse<List<SystemModuleResponse>> getAllModules() {
        List<SystemModule> modules = systemModuleService.getAllEnabledModules();
        List<SystemModuleResponse> response = modules.stream()
                .map(this::toResponseWithoutChildren)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取顶级模块")
    @GetMapping("/top-level")
    public ApiResponse<List<SystemModuleResponse>> getTopLevelModules() {
        List<SystemModule> modules = systemModuleService.getTopLevelModules();
        List<SystemModuleResponse> response = modules.stream()
                .map(this::toResponseWithoutChildren)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取子模块")
    @GetMapping("/{parentCode}/children")
    public ApiResponse<List<SystemModuleResponse>> getChildModules(@PathVariable String parentCode) {
        List<SystemModule> modules = systemModuleService.getChildModules(parentCode);
        List<SystemModuleResponse> response = modules.stream()
                .map(this::toResponseWithoutChildren)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    private SystemModuleResponse toResponse(SystemModule module) {
        List<SystemModuleResponse> children = null;
        if (module.getChildren() != null && !module.getChildren().isEmpty()) {
            children = module.getChildren().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        return SystemModuleResponse.builder()
                .id(module.getId())
                .moduleCode(module.getModuleCode())
                .moduleName(module.getModuleName())
                .moduleDesc(module.getModuleDesc())
                .parentCode(module.getParentCode())
                .icon(module.getIcon())
                .sortOrder(module.getSortOrder())
                .isEnabled(module.getIsEnabled())
                .children(children)
                .build();
    }

    private SystemModuleResponse toResponseWithoutChildren(SystemModule module) {
        return SystemModuleResponse.builder()
                .id(module.getId())
                .moduleCode(module.getModuleCode())
                .moduleName(module.getModuleName())
                .moduleDesc(module.getModuleDesc())
                .parentCode(module.getParentCode())
                .icon(module.getIcon())
                .sortOrder(module.getSortOrder())
                .isEnabled(module.getIsEnabled())
                .build();
    }
}
