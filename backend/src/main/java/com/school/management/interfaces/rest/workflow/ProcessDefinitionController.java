package com.school.management.interfaces.rest.workflow;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程定义管理 — 部署/列表/查看/删除/挂起/激活.
 */
@RestController
@RequestMapping("/workflow/process-definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final RepositoryService repositoryService;

    /** 上传 .bpmn20.xml 部署一个新流程版本 */
    @PostMapping("/deploy")
    @PreAuthorize("hasAuthority('workflow:definition:deploy')")
    public Result<Map<String, Object>> deploy(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String name) {
        try (InputStream in = file.getInputStream()) {
            Deployment d = repositoryService.createDeployment()
                .name(name != null ? name : file.getOriginalFilename())
                .addInputStream(file.getOriginalFilename(), in)
                .deploy();
            Map<String, Object> row = new HashMap<>();
            row.put("id", d.getId());
            row.put("name", d.getName());
            row.put("deploymentTime", d.getDeploymentTime());
            return Result.success(row);
        } catch (Exception e) {
            return Result.error("部署失败: " + e.getMessage());
        }
    }

    /** 列出所有 latest 版本流程定义 */
    @GetMapping
    @PreAuthorize("hasAuthority('workflow:definition:view')")
    public Result<List<Map<String, Object>>> list() {
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
            .latestVersion().orderByProcessDefinitionKey().asc().list();
        List<Map<String, Object>> rows = defs.stream().map(this::toRow).collect(Collectors.toList());
        return Result.success(rows);
    }

    /** 单个流程定义详情 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:definition:view')")
    public Result<Map<String, Object>> getById(@PathVariable String id) {
        ProcessDefinition d = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(id).singleResult();
        if (d == null) return Result.error("流程定义不存在: " + id);
        return Result.success(toRow(d));
    }

    /** 删除部署(级联删流程实例) */
    @DeleteMapping("/deployments/{deploymentId}")
    @PreAuthorize("hasAuthority('workflow:definition:delete')")
    public Result<Void> deleteDeployment(@PathVariable String deploymentId,
                                         @RequestParam(defaultValue = "false") boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
        return Result.success(null);
    }

    /** 暂停流程定义 */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('workflow:definition:manage')")
    public Result<Void> suspend(@PathVariable String id) {
        repositoryService.suspendProcessDefinitionById(id);
        return Result.success(null);
    }

    /** 激活流程定义 */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('workflow:definition:manage')")
    public Result<Void> activate(@PathVariable String id) {
        repositoryService.activateProcessDefinitionById(id);
        return Result.success(null);
    }

    private Map<String, Object> toRow(ProcessDefinition d) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", d.getId());
        row.put("key", d.getKey());
        row.put("name", d.getName() != null ? d.getName() : d.getKey());
        row.put("version", d.getVersion());
        row.put("deploymentId", d.getDeploymentId());
        row.put("suspended", d.isSuspended());
        return row;
    }
}
