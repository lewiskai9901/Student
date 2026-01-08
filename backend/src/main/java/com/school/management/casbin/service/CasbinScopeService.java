package com.school.management.casbin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.casbin.dto.ScopeAssignmentDTO;
import com.school.management.casbin.dto.ScopeMetadataDTO;
import com.school.management.casbin.entity.PermissionAuditLog;
import com.school.management.casbin.entity.ScopeMetadata;
import com.school.management.casbin.entity.UserScopeAssignment;
import com.school.management.casbin.enums.ScopeType;
import com.school.management.casbin.mapper.PermissionAuditLogMapper;
import com.school.management.casbin.mapper.ScopeMetadataMapper;
import com.school.management.casbin.mapper.UserScopeAssignmentMapper;
import com.school.management.common.PageResult;
import com.school.management.entity.Department;
import com.school.management.entity.Grade;
import com.school.management.entity.Class;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.mapper.GradeMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Casbin 范围管理服务
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class CasbinScopeService {

    private final CasbinEnforcerService enforcerService;
    private final UserScopeAssignmentMapper assignmentMapper;
    private final ScopeMetadataMapper metadataMapper;
    private final PermissionAuditLogMapper auditLogMapper;
    private final ScopeHierarchyCache hierarchyCache;
    private final DepartmentMapper departmentMapper;
    private final GradeMapper gradeMapper;
    private final ClassMapper classMapper;

    // ==================== 范围分配 ====================

    /**
     * 分配用户数据范围
     *
     * @param dto 分配信息
     * @return 分配记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ScopeAssignmentDTO assignScope(ScopeAssignmentDTO dto) {
        // 验证范围类型
        ScopeType scopeType = ScopeType.fromCode(dto.getScopeType());
        if (scopeType == null) {
            throw new BusinessException("无效的范围类型: " + dto.getScopeType());
        }

        // 检查是否已存在
        LambdaQueryWrapper<UserScopeAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserScopeAssignment::getUserId, dto.getUserId())
                .eq(UserScopeAssignment::getScopeExpression, dto.getScopeExpression())
                .eq(UserScopeAssignment::getDeleted, 0);
        if (assignmentMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该用户已拥有此数据范围");
        }

        // 获取当前操作用户
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 创建分配记录
        UserScopeAssignment entity = dto.toEntity();
        entity.setAssignedBy(currentUserId);
        entity.setAssignedAt(LocalDateTime.now());
        entity.setDeleted(0);
        assignmentMapper.insert(entity);

        // 同步到 Casbin
        enforcerService.assignUserScope(dto.getUserId(), dto.getScopeExpression());

        // 记录审计日志
        saveAuditLog("GRANT", "SCOPE", dto.getUserId().toString(),
                null, dto.getScopeExpression());

        log.info("分配用户数据范围: userId={}, scope={}", dto.getUserId(), dto.getScopeExpression());

        return ScopeAssignmentDTO.fromEntity(entity);
    }

    /**
     * 批量分配用户数据范围
     *
     * @param userId 用户ID
     * @param scopes 范围表达式列表
     * @return 分配记录列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ScopeAssignmentDTO> batchAssignScopes(Long userId, List<String> scopes) {
        List<ScopeAssignmentDTO> results = new ArrayList<>();
        Long currentUserId = SecurityUtils.getCurrentUserId();

        for (String scope : scopes) {
            // 检查是否已存在
            LambdaQueryWrapper<UserScopeAssignment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserScopeAssignment::getUserId, userId)
                    .eq(UserScopeAssignment::getScopeExpression, scope)
                    .eq(UserScopeAssignment::getDeleted, 0);
            if (assignmentMapper.selectCount(wrapper) > 0) {
                continue; // 跳过已存在的
            }

            ScopeType scopeType = ScopeType.fromExpression(scope);
            String displayName = getDisplayNameForScope(scope);

            UserScopeAssignment entity = UserScopeAssignment.builder()
                    .userId(userId)
                    .scopeExpression(scope)
                    .scopeType(scopeType != null ? scopeType.getCode() : "UNKNOWN")
                    .displayName(displayName)
                    .assignedBy(currentUserId)
                    .assignedAt(LocalDateTime.now())
                    .deleted(0)
                    .build();
            assignmentMapper.insert(entity);

            // 同步到 Casbin
            enforcerService.assignUserScope(userId, scope);

            results.add(ScopeAssignmentDTO.fromEntity(entity));
        }

        // 记录审计日志
        saveAuditLog("GRANT", "SCOPE", userId.toString(),
                null, String.join(",", scopes));

        log.info("批量分配用户数据范围: userId={}, count={}", userId, results.size());

        return results;
    }

    /**
     * 移除用户数据范围
     *
     * @param userId          用户ID
     * @param scopeExpression 范围表达式
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeScope(Long userId, String scopeExpression) {
        // 使用 MyBatis Plus 的逻辑删除（@TableLogic 会自动处理）
        LambdaQueryWrapper<UserScopeAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserScopeAssignment::getUserId, userId)
                .eq(UserScopeAssignment::getScopeExpression, scopeExpression);

        // remove() 方法配合 @TableLogic 会自动执行软删除
        int deleted = assignmentMapper.delete(wrapper);
        log.debug("软删除用户范围分配记录: userId={}, scope={}, affected={}", userId, scopeExpression, deleted);

        // 从 Casbin 移除
        enforcerService.removeUserScope(userId, scopeExpression);

        // 记录审计日志
        saveAuditLog("REVOKE", "SCOPE", userId.toString(),
                scopeExpression, null);

        log.info("移除用户数据范围: userId={}, scope={}", userId, scopeExpression);
    }

    /**
     * 移除用户所有数据范围
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeAllScopes(Long userId) {
        // 获取旧范围用于审计
        List<String> oldScopes = getUserScopes(userId).stream()
                .map(ScopeAssignmentDTO::getScopeExpression)
                .collect(Collectors.toList());

        // 使用 MyBatis Plus 的逻辑删除
        LambdaQueryWrapper<UserScopeAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserScopeAssignment::getUserId, userId);
        int deleted = assignmentMapper.delete(wrapper);
        log.debug("软删除用户所有范围分配记录: userId={}, affected={}", userId, deleted);

        // 从 Casbin 移除
        enforcerService.removeAllUserScopes(userId);

        // 记录审计日志
        saveAuditLog("REVOKE", "SCOPE", userId.toString(),
                String.join(",", oldScopes), null);

        log.info("移除用户所有数据范围: userId={}", userId);
    }

    // ==================== 范围查询 ====================

    /**
     * 获取用户数据范围列表
     *
     * @param userId 用户ID
     * @return 范围列表
     */
    public List<ScopeAssignmentDTO> getUserScopes(Long userId) {
        return assignmentMapper.selectByUserId(userId);
    }

    /**
     * 分页查询用户范围分配
     *
     * @param userId   用户ID（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<ScopeAssignmentDTO> pageUserScopes(Long userId, int pageNum, int pageSize) {
        Page<UserScopeAssignment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserScopeAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserScopeAssignment::getDeleted, 0);
        if (userId != null) {
            wrapper.eq(UserScopeAssignment::getUserId, userId);
        }
        wrapper.orderByDesc(UserScopeAssignment::getAssignedAt);

        Page<UserScopeAssignment> result = assignmentMapper.selectPage(page, wrapper);
        List<ScopeAssignmentDTO> dtoList = result.getRecords().stream()
                .map(ScopeAssignmentDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageResult<>(dtoList, result.getTotal(), (long) pageSize, (long) pageNum);
    }

    /**
     * 获取拥有指定范围的用户列表
     *
     * @param scopeExpression 范围表达式
     * @return 用户范围分配列表
     */
    public List<ScopeAssignmentDTO> getUsersByScope(String scopeExpression) {
        return assignmentMapper.selectByScopeExpression(scopeExpression);
    }

    // ==================== 范围元数据 ====================

    /**
     * 获取所有范围元数据
     *
     * @return 范围元数据列表
     */
    public List<ScopeMetadataDTO> getAllMetadata() {
        LambdaQueryWrapper<ScopeMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ScopeMetadata::getSortOrder);
        return metadataMapper.selectList(wrapper).stream()
                .map(this::toMetadataDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按类型获取范围元数据
     *
     * @param scopeType 范围类型
     * @return 范围元数据列表
     */
    public List<ScopeMetadataDTO> getMetadataByType(String scopeType) {
        LambdaQueryWrapper<ScopeMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScopeMetadata::getScopeType, scopeType)
                .orderByAsc(ScopeMetadata::getSortOrder);
        return metadataMapper.selectList(wrapper).stream()
                .map(this::toMetadataDTO)
                .collect(Collectors.toList());
    }

    /**
     * 同步范围元数据
     * 从部门、年级、班级表同步元数据到scope_metadata表
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncMetadata() {
        log.info("开始同步范围元数据...");
        LocalDateTime now = LocalDateTime.now();
        int sortOrder = 0;

        // 1. 同步部门元数据
        List<Department> departments = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>().eq(Department::getStatus, 1)
        );
        log.info("同步部门数据: {} 条", departments.size());

        for (Department dept : departments) {
            String scopeExpression = ScopeType.DEPT.getPrefix() + dept.getId();
            syncSingleMetadata(
                    scopeExpression,
                    dept.getDeptName(),
                    ScopeType.DEPT.getCode(),
                    dept.getId(),
                    "DEPARTMENT",
                    dept.getParentId() != null ? ScopeType.DEPT.getPrefix() + dept.getParentId() : null,
                    ++sortOrder,
                    now
            );
        }

        // 2. 同步年级元数据
        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>().eq(Grade::getStatus, 1)
        );
        log.info("同步年级数据: {} 条", grades.size());

        for (Grade grade : grades) {
            String scopeExpression = ScopeType.GRADE.getPrefix() + grade.getId();
            syncSingleMetadata(
                    scopeExpression,
                    grade.getGradeName(),
                    ScopeType.GRADE.getCode(),
                    grade.getId(),
                    "GRADE",
                    null,
                    ++sortOrder,
                    now
            );
        }

        // 3. 同步班级元数据
        List<Class> classes = classMapper.selectList(
                new LambdaQueryWrapper<Class>().eq(Class::getStatus, 1)
        );
        log.info("同步班级数据: {} 条", classes.size());

        for (Class clazz : classes) {
            String scopeExpression = ScopeType.CLASS.getPrefix() + clazz.getId();
            // 班级的父范围是部门+年级（如果有）
            String parentScope = null;
            if (clazz.getDepartmentId() != null && clazz.getGradeId() != null) {
                parentScope = ScopeType.DEPT_GRADE.getPrefix() + clazz.getDepartmentId() + ":" + clazz.getGradeId();
            } else if (clazz.getDepartmentId() != null) {
                parentScope = ScopeType.DEPT.getPrefix() + clazz.getDepartmentId();
            } else if (clazz.getGradeId() != null) {
                parentScope = ScopeType.GRADE.getPrefix() + clazz.getGradeId();
            }

            syncSingleMetadata(
                    scopeExpression,
                    clazz.getClassName(),
                    ScopeType.CLASS.getCode(),
                    clazz.getId(),
                    "CLASS",
                    parentScope,
                    ++sortOrder,
                    now
            );
        }

        // 4. 同步部门+年级组合元数据
        for (Department dept : departments) {
            for (Grade grade : grades) {
                String scopeExpression = ScopeType.DEPT_GRADE.getPrefix() + dept.getId() + ":" + grade.getId();
                syncSingleMetadata(
                        scopeExpression,
                        dept.getDeptName() + "-" + grade.getGradeName(),
                        ScopeType.DEPT_GRADE.getCode(),
                        null,
                        "DEPT_GRADE",
                        ScopeType.DEPT.getPrefix() + dept.getId(),
                        ++sortOrder,
                        now
                );
            }
        }

        // 刷新层级缓存
        hierarchyCache.refreshHierarchy();
        log.info("范围元数据同步完成，共同步 {} 条记录", sortOrder);
    }

    /**
     * 同步单个元数据记录（存在则更新，不存在则插入）
     */
    private void syncSingleMetadata(String scopeExpression, String displayName, String scopeType,
                                     Long refId, String refType, String parentScope,
                                     Integer sortOrder, LocalDateTime now) {
        ScopeMetadata existing = metadataMapper.selectByExpression(scopeExpression);
        if (existing != null) {
            // 更新现有记录
            existing.setDisplayName(displayName);
            existing.setScopeType(scopeType);
            existing.setRefId(refId);
            existing.setRefType(refType);
            existing.setParentScope(parentScope);
            existing.setSortOrder(sortOrder);
            existing.setUpdatedAt(now);
            metadataMapper.updateById(existing);
        } else {
            // 插入新记录
            ScopeMetadata metadata = ScopeMetadata.builder()
                    .scopeExpression(scopeExpression)
                    .displayName(displayName)
                    .scopeType(scopeType)
                    .refId(refId)
                    .refType(refType)
                    .parentScope(parentScope)
                    .sortOrder(sortOrder)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            metadataMapper.insert(metadata);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取范围的显示名称
     */
    private String getDisplayNameForScope(String scopeExpression) {
        ScopeMetadata metadata = metadataMapper.selectByExpression(scopeExpression);
        return metadata != null ? metadata.getDisplayName() : scopeExpression;
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(String actionType, String targetType, String targetId,
                              String oldValue, String newValue) {
        try {
            Long operatorId = SecurityUtils.getCurrentUserId();
            String operatorName = SecurityUtils.getCurrentUsername();

            PermissionAuditLog log = PermissionAuditLog.builder()
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .operationTime(LocalDateTime.now())
                    .build();

            auditLogMapper.insert(log);
        } catch (Exception e) {
            // 审计日志失败不影响主流程
            CasbinScopeService.log.warn("保存审计日志失败", e);
        }
    }

    /**
     * 转换为元数据DTO
     */
    private ScopeMetadataDTO toMetadataDTO(ScopeMetadata entity) {
        ScopeType type = ScopeType.fromCode(entity.getScopeType());
        return ScopeMetadataDTO.builder()
                .id(entity.getId())
                .scopeExpression(entity.getScopeExpression())
                .displayName(entity.getDisplayName())
                .scopeType(entity.getScopeType())
                .scopeTypeName(type != null ? type.getName() : null)
                .refId(entity.getRefId())
                .refType(entity.getRefType())
                .parentScope(entity.getParentScope())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
