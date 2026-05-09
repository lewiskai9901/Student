package com.school.management.interfaces.rest.workflow;

import com.school.management.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程实例管理 — 启动/查询/取消.
 */
@RestController
@RequestMapping("/workflow/process-instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final RuntimeService runtimeService;

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('workflow:instance:start')")
    public Result<Map<String, Object>> start(@RequestBody StartRequest req) {
        Map<String, Object> vars = req.getVariables() != null ? req.getVariables() : Map.of();
        ProcessInstance pi;
        if (req.getProcessDefinitionId() != null) {
            pi = runtimeService.startProcessInstanceById(
                req.getProcessDefinitionId(), req.getBusinessKey(), vars);
        } else {
            pi = runtimeService.startProcessInstanceByKey(
                req.getProcessKey(), req.getBusinessKey(), vars);
        }
        return Result.success(toRow(pi));
    }

    /** 列出活跃实例(按 businessKey/processKey/startedBy 过滤) */
    @GetMapping
    @PreAuthorize("hasAuthority('workflow:instance:view')")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String startedBy) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        if (processKey != null) query.processDefinitionKey(processKey);
        if (businessKey != null) query.processInstanceBusinessKey(businessKey);
        if (startedBy != null) query.startedBy(startedBy);
        List<ProcessInstance> instances = query.orderByProcessInstanceId().asc().list();
        List<Map<String, Object>> rows = instances.stream().map(this::toRow).collect(Collectors.toList());
        return Result.success(rows);
    }

    /** 单个实例详情 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:instance:view')")
    public Result<Map<String, Object>> getById(@PathVariable String id) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
            .processInstanceId(id).singleResult();
        if (pi == null) return Result.error("实例不存在或已结束: " + id);
        return Result.success(toRow(pi));
    }

    /** 取消实例 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('workflow:instance:cancel')")
    public Result<Void> cancel(@PathVariable String id, @RequestBody(required = false) CancelRequest req) {
        String reason = (req != null && req.getReason() != null) ? req.getReason() : "用户取消";
        runtimeService.deleteProcessInstance(id, reason);
        return Result.success(null);
    }

    private Map<String, Object> toRow(ProcessInstance pi) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", pi.getId());
        row.put("processDefinitionId", pi.getProcessDefinitionId());
        row.put("businessKey", pi.getBusinessKey() != null ? pi.getBusinessKey() : "");
        row.put("ended", pi.isEnded());
        row.put("suspended", pi.isSuspended());
        row.put("startTime", pi.getStartTime());
        return row;
    }

    @Data public static class StartRequest {
        private String processKey;
        private String processDefinitionId;
        private String businessKey;
        private Map<String, Object> variables;
    }

    @Data public static class CancelRequest {
        private String reason;
    }
}
