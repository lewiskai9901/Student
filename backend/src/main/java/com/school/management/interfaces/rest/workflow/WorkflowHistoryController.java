package com.school.management.interfaces.rest.workflow;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程历史查询 — 已结束实例 / 已完成任务.
 */
@RestController
@RequestMapping("/workflow/history")
@RequiredArgsConstructor
public class WorkflowHistoryController {

    private final HistoryService historyService;

    /** 历史实例(已结束 + 进行中都查) */
    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('workflow:history:view')")
    public Result<List<Map<String, Object>>> listInstances(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String startedBy,
            @RequestParam(defaultValue = "100") int limit) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if (processKey != null) query.processDefinitionKey(processKey);
        if (businessKey != null) query.processInstanceBusinessKey(businessKey);
        if (startedBy != null) query.startedBy(startedBy);
        List<HistoricProcessInstance> instances = query
            .orderByProcessInstanceStartTime().desc().listPage(0, limit);
        return Result.success(instances.stream().map(pi -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", pi.getId());
            row.put("processDefinitionId", pi.getProcessDefinitionId());
            row.put("businessKey", pi.getBusinessKey());
            row.put("startTime", pi.getStartTime());
            row.put("endTime", pi.getEndTime());
            row.put("startUserId", pi.getStartUserId());
            row.put("durationInMillis", pi.getDurationInMillis());
            return row;
        }).collect(Collectors.toList()));
    }

    /** 实例的所有历史任务(审计轨迹) */
    @GetMapping("/instances/{id}/tasks")
    @PreAuthorize("hasAuthority('workflow:history:view')")
    public Result<List<Map<String, Object>>> instanceTasks(@PathVariable String id) {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(id).orderByHistoricTaskInstanceStartTime().asc().list();
        return Result.success(tasks.stream().map(t -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("name", t.getName());
            row.put("assignee", t.getAssignee());
            row.put("startTime", t.getStartTime());
            row.put("endTime", t.getEndTime());
            row.put("durationInMillis", t.getDurationInMillis());
            row.put("deleteReason", t.getDeleteReason());
            return row;
        }).collect(Collectors.toList()));
    }

    /** 我经手过的任务(已完成 + 我作为 assignee) */
    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> myHistoricTasks(@RequestParam(defaultValue = "50") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.success(List.of());
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
            .taskAssignee(String.valueOf(userId))
            .finished()
            .orderByHistoricTaskInstanceEndTime().desc()
            .listPage(0, limit);
        return Result.success(tasks.stream().map(t -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("name", t.getName());
            row.put("processInstanceId", t.getProcessInstanceId());
            row.put("startTime", t.getStartTime());
            row.put("endTime", t.getEndTime());
            return row;
        }).collect(Collectors.toList()));
    }
}
