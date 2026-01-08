package com.school.management.controller.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.task.WorkflowTemplateDTO;
import com.school.management.exception.BusinessException;
import com.school.management.dto.task.WorkflowTemplateRequest;
import com.school.management.util.SecurityUtils;
import com.school.management.service.task.FlowableProcessService;
import com.school.management.service.task.WorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程模板控制器
 */
@Tag(name = "流程模板管理", description = "流程模板的创建、编辑、部署等操作")
@RestController
@RequestMapping("/task/workflow-templates")
@RequiredArgsConstructor
public class WorkflowTemplateController {

    private final WorkflowTemplateService workflowTemplateService;
    private final FlowableProcessService flowableProcessService;

    @Operation(summary = "分页查询流程模板")
    @GetMapping
    @PreAuthorize("hasAuthority('workflow:view')")
    public Result<IPage<WorkflowTemplateDTO>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Integer status) {
        IPage<WorkflowTemplateDTO> page = workflowTemplateService.pageQuery(pageNum, pageSize, keyword, templateType, status);
        return Result.success(page);
    }

    @Operation(summary = "获取启用的流程模板列表")
    @GetMapping("/enabled")
    @PreAuthorize("hasAnyAuthority('task:create', 'workflow:view')")
    public Result<List<WorkflowTemplateDTO>> listEnabled(@RequestParam(required = false) String templateType) {
        List<WorkflowTemplateDTO> list = workflowTemplateService.listEnabled(templateType);
        return Result.success(list);
    }

    @Operation(summary = "获取流程模板详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:view')")
    public Result<WorkflowTemplateDTO> getDetail(@PathVariable Long id) {
        WorkflowTemplateDTO dto = workflowTemplateService.getDetail(id);
        return Result.success(dto);
    }

    @Operation(summary = "创建流程模板")
    @PostMapping
    @PreAuthorize("hasAuthority('workflow:create')")
    public Result<WorkflowTemplateDTO> create(@Valid @RequestBody WorkflowTemplateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        WorkflowTemplateDTO dto = workflowTemplateService.create(request, userId, userName);
        return Result.success(dto);
    }

    @Operation(summary = "更新流程模板")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:update')")
    public Result<WorkflowTemplateDTO> update(@PathVariable Long id, @Valid @RequestBody WorkflowTemplateRequest request) {
        request.setId(id);
        Long userId = SecurityUtils.getCurrentUserId();
        WorkflowTemplateDTO dto = workflowTemplateService.update(request, userId);
        return Result.success(dto);
    }

    @Operation(summary = "删除流程模板")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = workflowTemplateService.delete(id);
        return Result.success(success);
    }

    @Operation(summary = "部署流程定义")
    @PostMapping("/{id}/deploy")
    @PreAuthorize("hasAuthority('workflow:deploy')")
    public Result<WorkflowTemplateDTO> deploy(@PathVariable Long id) {
        WorkflowTemplateDTO dto = workflowTemplateService.deployProcess(id);
        return Result.success(dto);
    }

    @Operation(summary = "设为默认模板")
    @PostMapping("/{id}/set-default")
    @PreAuthorize("hasAuthority('workflow:update')")
    public Result<Boolean> setDefault(@PathVariable Long id, @RequestParam String templateType) {
        boolean success = workflowTemplateService.setDefault(id, templateType);
        return Result.success(success);
    }

    @Operation(summary = "获取流程图")
    @GetMapping("/process-diagram/{processInstanceId}")
    @PreAuthorize("hasAnyAuthority('task:view', 'workflow:view')")
    public Result<String> getProcessDiagram(@PathVariable String processInstanceId) {
        String diagram = flowableProcessService.getProcessDiagram(processInstanceId);
        return Result.success(diagram);
    }

    @Operation(summary = "获取可回退节点")
    @GetMapping("/rejectable-nodes/{taskId}")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<List<Map<String, String>>> getRejectableNodes(@PathVariable String taskId) {
        List<Map<String, String>> nodes = flowableProcessService.getRejectableNodes(taskId);
        return Result.success(nodes);
    }

    @Operation(summary = "获取流程模板的用户任务节点")
    @GetMapping("/{id}/user-task-nodes")
    @PreAuthorize("hasAuthority('workflow:view')")
    public Result<List<Map<String, Object>>> getUserTaskNodes(@PathVariable Long id) {
        WorkflowTemplateDTO template = workflowTemplateService.getDetail(id);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        List<Map<String, Object>> nodes;
        if (template.getBpmnXml() != null && !template.getBpmnXml().isEmpty()) {
            // 从BPMN XML解析节点
            nodes = flowableProcessService.parseUserTaskNodes(template.getBpmnXml());
        } else {
            nodes = new java.util.ArrayList<>();
        }

        return Result.success(nodes);
    }

    @Operation(summary = "获取流程进度")
    @GetMapping("/process-progress/{processInstanceId}")
    @PreAuthorize("hasAnyAuthority('task:view', 'workflow:view')")
    public Result<Map<String, Object>> getProcessProgress(@PathVariable String processInstanceId) {
        Map<String, Object> progress = flowableProcessService.getProcessProgress(processInstanceId);
        return Result.success(progress);
    }
}
