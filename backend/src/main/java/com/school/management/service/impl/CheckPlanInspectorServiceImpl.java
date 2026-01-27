package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.inspector.InspectorCreateRequest;
import com.school.management.dto.inspector.InspectorDTO;
import com.school.management.dto.inspector.InspectorUpdateRequest;
import com.school.management.entity.CheckPlanInspector;
import com.school.management.entity.CheckTaskAssignment;
import com.school.management.entity.InspectorPermission;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckPlanInspectorMapper;
import com.school.management.mapper.CheckTaskAssignmentMapper;
import com.school.management.mapper.InspectorPermissionMapper;
import com.school.management.service.CheckPlanInspectorService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查计划打分人员Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckPlanInspectorServiceImpl implements CheckPlanInspectorService {

    private final CheckPlanInspectorMapper inspectorMapper;
    private final InspectorPermissionMapper permissionMapper;
    private final CheckTaskAssignmentMapper taskAssignmentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<InspectorDTO> getInspectorsByPlanId(Long planId) {
        List<CheckPlanInspector> inspectors = inspectorMapper.selectByPlanIdWithUser(planId);
        return inspectors.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public InspectorDTO getInspectorById(Long id) {
        CheckPlanInspector inspector = inspectorMapper.selectById(id);
        if (inspector == null || inspector.getDeleted() == 1) {
            throw new BusinessException("打分人员不存在");
        }
        return toDTO(inspector);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addInspector(Long planId, InspectorCreateRequest request) {
        // 检查用户是否已经是该计划的打分人员
        int count = inspectorMapper.countByPlanIdAndUserId(planId, request.getUserId());
        if (count > 0) {
            throw new BusinessException("该用户已经是此计划的打分人员");
        }

        // 创建打分人员记录
        CheckPlanInspector inspector = new CheckPlanInspector();
        inspector.setPlanId(planId);
        inspector.setUserId(request.getUserId());
        inspector.setStatus(1);
        inspector.setRemark(request.getRemark());
        inspector.setCreatedBy(SecurityUtils.getCurrentUserId());
        inspectorMapper.insert(inspector);

        // 创建权限配置
        savePermissions(inspector.getId(), planId, request.getUserId(), request.getPermissions());

        log.info("添加打分人员成功: planId={}, userId={}, inspectorId={}", planId, request.getUserId(), inspector.getId());
        return inspector.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInspector(Long planId, InspectorUpdateRequest request) {
        CheckPlanInspector inspector = inspectorMapper.selectById(request.getId());
        if (inspector == null || inspector.getDeleted() == 1) {
            throw new BusinessException("打分人员不存在");
        }
        if (!inspector.getPlanId().equals(planId)) {
            throw new BusinessException("打分人员不属于该计划");
        }

        // 更新基本信息
        if (request.getStatus() != null) {
            inspector.setStatus(request.getStatus());
        }
        if (request.getRemark() != null) {
            inspector.setRemark(request.getRemark());
        }
        inspectorMapper.updateById(inspector);

        // 删除旧的权限配置，创建新的
        permissionMapper.deleteByInspectorId(request.getId());
        savePermissions(inspector.getId(), planId, inspector.getUserId(), request.getPermissions());

        log.info("更新打分人员成功: inspectorId={}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInspector(Long planId, Long id) {
        CheckPlanInspector inspector = inspectorMapper.selectById(id);
        if (inspector == null || inspector.getDeleted() == 1) {
            throw new BusinessException("打分人员不存在");
        }
        if (!inspector.getPlanId().equals(planId)) {
            throw new BusinessException("打分人员不属于该计划");
        }

        // 逻辑删除打分人员
        inspector.setDeleted(1);
        inspectorMapper.updateById(inspector);

        // 删除权限配置
        permissionMapper.deleteByInspectorId(id);

        log.info("删除打分人员成功: inspectorId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTasksForDailyCheck(Long dailyCheckId, Long planId, List<Long> targetClassIds, List<String> categoryIds) {
        // 获取该计划的所有打分人员权限配置
        List<InspectorPermission> allPermissions = permissionMapper.selectByPlanId(planId);
        if (allPermissions.isEmpty()) {
            log.info("计划没有配置打分人员，跳过任务分配: planId={}", planId);
            return;
        }

        // 按用户分组
        Map<Long, List<InspectorPermission>> userPermissions = allPermissions.stream()
                .collect(Collectors.groupingBy(InspectorPermission::getUserId));

        Set<Long> targetClassIdSet = new HashSet<>(targetClassIds);
        Set<String> categoryIdSet = new HashSet<>(categoryIds);

        for (Map.Entry<Long, List<InspectorPermission>> entry : userPermissions.entrySet()) {
            Long userId = entry.getKey();
            List<InspectorPermission> permissions = entry.getValue();

            // 计算该用户在此次日常检查中的权限范围
            Set<String> assignedCategories = new HashSet<>();
            Set<Long> assignedClasses = new HashSet<>();

            for (InspectorPermission perm : permissions) {
                // 检查类别是否在本次检查范围内
                if (!categoryIdSet.contains(perm.getCategoryId())) {
                    continue;
                }

                // 获取该权限配置的班级范围
                Set<Long> permClassIds = parseClassIds(perm.getClassIds());

                // 计算交集：权限班级范围 ∩ 日常检查目标班级
                Set<Long> intersection;
                if (permClassIds == null || permClassIds.isEmpty()) {
                    // 权限配置为全部，则使用日常检查的目标班级
                    intersection = targetClassIdSet;
                } else {
                    intersection = permClassIds.stream()
                            .filter(targetClassIdSet::contains)
                            .collect(Collectors.toSet());
                }

                if (!intersection.isEmpty()) {
                    assignedCategories.add(perm.getCategoryId());
                    assignedClasses.addAll(intersection);
                }
            }

            // 如果有分配的任务，创建任务分配记录
            if (!assignedCategories.isEmpty() && !assignedClasses.isEmpty()) {
                CheckTaskAssignment assignment = new CheckTaskAssignment();
                assignment.setDailyCheckId(dailyCheckId);
                assignment.setPlanId(planId);
                assignment.setUserId(userId);
                assignment.setCategoryIds(toJson(new ArrayList<>(assignedCategories)));
                assignment.setClassIds(toJson(new ArrayList<>(assignedClasses)));
                assignment.setStatus(CheckTaskAssignment.STATUS_PENDING);
                assignment.setNotified(0);
                taskAssignmentMapper.insert(assignment);

                log.info("创建检查任务分配: dailyCheckId={}, userId={}, categories={}, classes={}",
                        dailyCheckId, userId, assignedCategories.size(), assignedClasses.size());
            }
        }
    }

    @Override
    public IPage<CheckTaskAssignment> getMyTasks(Long userId, Integer status, Long planId, int pageNum, int pageSize) {
        Page<CheckTaskAssignment> page = new Page<>(pageNum, pageSize);
        return taskAssignmentMapper.selectPageByUserId(page, userId, status, planId);
    }

    @Override
    public CheckTaskAssignment getTaskDetail(Long taskId, Long userId) {
        CheckTaskAssignment task = taskAssignmentMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权访问此任务");
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long taskId, Long userId) {
        CheckTaskAssignment task = getTaskDetail(taskId, userId);
        if (task.getStatus() != CheckTaskAssignment.STATUS_PENDING) {
            throw new BusinessException("任务状态不正确，无法开始");
        }
        task.setStatus(CheckTaskAssignment.STATUS_IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        taskAssignmentMapper.updateById(task);
        log.info("开始执行任务: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, Long userId) {
        CheckTaskAssignment task = getTaskDetail(taskId, userId);
        if (task.getStatus() != CheckTaskAssignment.STATUS_IN_PROGRESS) {
            throw new BusinessException("任务状态不正确，无法完成");
        }
        task.setStatus(CheckTaskAssignment.STATUS_COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        taskAssignmentMapper.updateById(task);
        log.info("完成任务: taskId={}, userId={}", taskId, userId);
    }

    @Override
    public int countPendingTasks(Long userId) {
        return taskAssignmentMapper.countPendingByUserId(userId);
    }

    // ========== 私有方法 ==========

    private void savePermissions(Long inspectorId, Long planId, Long userId, List<InspectorCreateRequest.PermissionConfig> permissions) {
        for (InspectorCreateRequest.PermissionConfig config : permissions) {
            InspectorPermission perm = new InspectorPermission();
            perm.setInspectorId(inspectorId);
            perm.setPlanId(planId);
            perm.setUserId(userId);
            perm.setCategoryId(config.getCategoryId());
            perm.setCategoryName(config.getCategoryName());
            if (config.getClassIds() != null && !config.getClassIds().isEmpty()) {
                perm.setClassIds(toJson(config.getClassIds()));
            }
            permissionMapper.insert(perm);
        }
    }

    private InspectorDTO toDTO(CheckPlanInspector inspector) {
        InspectorDTO dto = new InspectorDTO();
        dto.setId(inspector.getId());
        dto.setPlanId(inspector.getPlanId());
        dto.setUserId(inspector.getUserId());
        // 这些字段可能为空（如果是通过selectById查询的，没有关联用户信息）
        if (inspector.getUserName() != null) {
            dto.setUserName(inspector.getUserName());
        }
        if (inspector.getUsername() != null) {
            dto.setUsername(inspector.getUsername());
        }
        if (inspector.getOrgUnitName() != null) {
            dto.setOrgUnitName(inspector.getOrgUnitName());
        }
        dto.setStatus(inspector.getStatus());
        dto.setRemark(inspector.getRemark());
        dto.setCreatedAt(inspector.getCreatedAt());

        // 加载权限配置
        List<InspectorPermission> permissions = permissionMapper.selectByInspectorId(inspector.getId());
        List<InspectorDTO.PermissionDTO> permDTOs = permissions.stream().map(perm -> {
            InspectorDTO.PermissionDTO permDTO = new InspectorDTO.PermissionDTO();
            permDTO.setId(perm.getId());
            permDTO.setCategoryId(perm.getCategoryId());
            permDTO.setCategoryName(perm.getCategoryName());
            permDTO.setClassIds(parseClassIdsToList(perm.getClassIds()));
            return permDTO;
        }).collect(Collectors.toList());
        dto.setPermissions(permDTOs);

        return dto;
    }

    private Set<Long> parseClassIds(String classIdsJson) {
        if (classIdsJson == null || classIdsJson.isEmpty()) {
            return null;
        }
        try {
            List<Object> ids = objectMapper.readValue(classIdsJson, new TypeReference<List<Object>>() {});
            return ids.stream()
                    .map(id -> id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id)))
                    .collect(Collectors.toSet());
        } catch (JsonProcessingException e) {
            log.error("解析班级ID列表失败: {}", classIdsJson, e);
            return null;
        }
    }

    private List<Long> parseClassIdsToList(String classIdsJson) {
        Set<Long> set = parseClassIds(classIdsJson);
        return set == null ? null : new ArrayList<>(set);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("序列化JSON失败", e);
            return null;
        }
    }
}
