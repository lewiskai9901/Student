package com.school.management.interfaces.rest.evaluation;

import com.school.management.application.evaluation.EvalConditionOptionsService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评选条件编辑器辅助选项 REST 控制器
 */
@RestController
@RequestMapping("/eval/options")
@Tag(name = "评级中心 - 条件选项", description = "Evaluation Condition Options API")
@RequiredArgsConstructor
public class EvalOptionsController {

    private final EvalConditionOptionsService optionsService;

    @GetMapping("/projects")
    @Operation(summary = "获取可选的检查项目列表")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> listProjects() {
        return Result.success(optionsService.listProjects());
    }

    @GetMapping("/sections/{projectId}")
    @Operation(summary = "获取项目下的分区列表")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> listSections(@PathVariable Long projectId) {
        return Result.success(optionsService.listSections(projectId));
    }

    @GetMapping("/event-types")
    @Operation(summary = "获取事件类型列表")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> listEventTypes() {
        return Result.success(optionsService.listEventTypes());
    }
}
