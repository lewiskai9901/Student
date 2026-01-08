package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.management.dto.UserDataScopeDTO;
import com.school.management.dto.request.UserDataScopeRequest;
import com.school.management.entity.UserDataScope;
import com.school.management.enums.ScopeType;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.mapper.GradeMapper;
import com.school.management.mapper.UserDataScopeMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.service.UserDataScopeService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户数据范围服务实现
 *
 * @author system
 * @version 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataScopeServiceImpl implements UserDataScopeService {

    private final UserDataScopeMapper userDataScopeMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final GradeMapper gradeMapper;
    private final ClassMapper classMapper;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "userDataScopes";

    // ==================== 基础CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, key = "#request.userId")
    public UserDataScopeDTO addScope(UserDataScopeRequest request) {
        // 验证参数
        validateRequest(request);

        // 检查是否已存在
        if (userDataScopeMapper.existsByUserAndScope(
                request.getUserId(), request.getScopeType(), request.getScopeId())) {
            throw new BusinessException("该用户已拥有此数据范围");
        }

        // 获取范围名称
        String scopeName = getScopeName(request.getScopeType(), request.getScopeId());

        // 创建实体
        UserDataScope entity = UserDataScope.builder()
                .userId(request.getUserId())
                .scopeType(request.getScopeType())
                .scopeId(request.getScopeId())
                .scopeName(scopeName)
                .includeChildren(request.getIncludeChildren() != null ? request.getIncludeChildren() : 1)
                .build();
        entity.setCreatedBy(SecurityUtils.getCurrentUserId());

        userDataScopeMapper.insert(entity);
        log.info("添加用户数据范围: userId={}, scopeType={}, scopeId={}",
                request.getUserId(), request.getScopeType(), request.getScopeId());

        return convertToDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, key = "#request.userId")
    public List<UserDataScopeDTO> batchAddScopes(UserDataScopeRequest.BatchAdd request) {
        List<UserDataScopeDTO> result = new ArrayList<>();

        for (UserDataScopeRequest.ScopeItem item : request.getScopes()) {
            // 检查是否已存在
            if (userDataScopeMapper.existsByUserAndScope(
                    request.getUserId(), item.getScopeType(), item.getScopeId())) {
                continue; // 跳过已存在的
            }

            String scopeName = getScopeName(item.getScopeType(), item.getScopeId());

            UserDataScope entity = UserDataScope.builder()
                    .userId(request.getUserId())
                    .scopeType(item.getScopeType())
                    .scopeId(item.getScopeId())
                    .scopeName(scopeName)
                    .includeChildren(item.getIncludeChildren() != null ? item.getIncludeChildren() : 1)
                    .build();
            entity.setCreatedBy(SecurityUtils.getCurrentUserId());

            userDataScopeMapper.insert(entity);
            result.add(convertToDTO(entity));
        }

        log.info("批量添加用户数据范围: userId={}, count={}", request.getUserId(), result.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScope(Long id) {
        UserDataScope scope = userDataScopeMapper.selectById(id);
        if (scope == null || scope.getDeleted() == 1) {
            throw new BusinessException("数据范围不存在");
        }

        // 软删除
        scope.setDeleted(1);
        userDataScopeMapper.updateById(scope);

        // 清除缓存
        evictCache(scope.getUserId());

        log.info("删除用户数据范围: id={}, userId={}", id, scope.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteScopes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 获取要删除的记录，用于清除缓存
        List<UserDataScope> scopes = userDataScopeMapper.selectBatchIds(ids);
        Set<Long> userIds = new HashSet<>();
        for (UserDataScope scope : scopes) {
            userIds.add(scope.getUserId());
        }

        // 批量软删除
        LambdaUpdateWrapper<UserDataScope> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(UserDataScope::getId, ids)
                .set(UserDataScope::getDeleted, 1);
        userDataScopeMapper.update(null, wrapper);

        // 清除缓存
        for (Long userId : userIds) {
            evictCache(userId);
        }

        log.info("批量删除用户数据范围: ids={}", ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = CACHE_NAME, key = "#userId"),
        @CacheEvict(value = CACHE_NAME, key = "'dept:' + #userId"),
        @CacheEvict(value = CACHE_NAME, key = "'grade:' + #userId"),
        @CacheEvict(value = CACHE_NAME, key = "'class:' + #userId")
    })
    public void deleteAllByUserId(Long userId) {
        userDataScopeMapper.deleteByUserIdAndType(userId, null);
        log.info("删除用户所有数据范围: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, key = "#userId")
    public void deleteByUserIdAndType(Long userId, ScopeType scopeType) {
        userDataScopeMapper.deleteByUserIdAndType(userId, scopeType.getCode());
        log.info("删除用户指定类型数据范围: userId={}, scopeType={}", userId, scopeType);
    }

    // ==================== 查询方法 ====================

    @Override
    public List<UserDataScopeDTO> getScopesByUserId(Long userId) {
        return userDataScopeMapper.selectDTOByUserId(userId);
    }

    @Override
    public List<UserDataScopeDTO> getUsersByScopeTypeAndId(ScopeType scopeType, Long scopeId) {
        return userDataScopeMapper.selectByScopeTypeAndId(scopeType.getCode(), scopeId);
    }

    @Override
    public boolean hasScope(Long userId, ScopeType scopeType, Long scopeId) {
        return userDataScopeMapper.existsByUserAndScope(userId, scopeType.getCode(), scopeId);
    }

    // ==================== 权限计算方法 ====================

    @Override
    @Cacheable(value = CACHE_NAME, key = "'dept:' + #userId")
    public Set<Long> getAccessibleDeptIds(Long userId) {
        List<Long> deptIds = userDataScopeMapper.selectAccessibleDeptIds(userId);
        return deptIds != null ? new HashSet<>(deptIds) : Collections.emptySet();
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'grade:' + #userId")
    public Set<Long> getAccessibleGradeIds(Long userId) {
        List<Long> gradeIds = userDataScopeMapper.selectAccessibleGradeIds(userId);
        return gradeIds != null ? new HashSet<>(gradeIds) : Collections.emptySet();
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'class:' + #userId")
    public Set<Long> getAccessibleClassIds(Long userId) {
        // 先检查用户是否有任何数据范围配置
        if (!hasAnyScope(userId)) {
            // 没有配置数据范围，根据业务规则可以返回空集合或null（表示无权限）
            return Collections.emptySet();
        }

        List<Long> classIds = userDataScopeMapper.selectAccessibleClassIds(userId);
        return classIds != null ? new HashSet<>(classIds) : Collections.emptySet();
    }

    @Override
    public boolean canAccessClass(Long userId, Long classId) {
        if (userId == null || classId == null) {
            return false;
        }

        Set<Long> accessibleClassIds = getAccessibleClassIds(userId);
        if (accessibleClassIds == null) {
            return true; // null表示有全部权限
        }

        return accessibleClassIds.contains(classId);
    }

    @Override
    public boolean canAccessGrade(Long userId, Long gradeId) {
        if (userId == null || gradeId == null) {
            return false;
        }

        Set<Long> accessibleGradeIds = getAccessibleGradeIds(userId);
        if (accessibleGradeIds == null) {
            return true;
        }

        return accessibleGradeIds.contains(gradeId);
    }

    @Override
    public boolean canAccessDept(Long userId, Long deptId) {
        if (userId == null || deptId == null) {
            return false;
        }

        Set<Long> accessibleDeptIds = getAccessibleDeptIds(userId);
        if (accessibleDeptIds == null) {
            return true;
        }

        return accessibleDeptIds.contains(deptId);
    }

    @Override
    public boolean hasAnyScope(Long userId) {
        LambdaQueryWrapper<UserDataScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDataScope::getUserId, userId)
                .eq(UserDataScope::getDeleted, 0);
        return userDataScopeMapper.selectCount(wrapper) > 0;
    }

    // ==================== 辅助方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOnScopeDeleted(ScopeType scopeType, Long scopeId) {
        // 获取受影响的用户ID
        List<UserDataScopeDTO> affected = userDataScopeMapper.selectByScopeTypeAndId(scopeType.getCode(), scopeId);
        Set<Long> userIds = new HashSet<>();
        for (UserDataScopeDTO dto : affected) {
            userIds.add(dto.getUserId());
        }

        // 删除数据范围
        userDataScopeMapper.deleteByScopeTypeAndId(scopeType.getCode(), scopeId);

        // 清除缓存
        for (Long userId : userIds) {
            evictCache(userId);
        }

        log.info("清理已删除范围的用户数据范围: scopeType={}, scopeId={}, affectedUsers={}",
                scopeType, scopeId, userIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScopeName(ScopeType scopeType, Long scopeId, String newName) {
        LambdaUpdateWrapper<UserDataScope> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserDataScope::getScopeType, scopeType.getCode())
                .eq(UserDataScope::getScopeId, scopeId)
                .eq(UserDataScope::getDeleted, 0)
                .set(UserDataScope::getScopeName, newName);
        userDataScopeMapper.update(null, wrapper);

        log.info("更新范围名称: scopeType={}, scopeId={}, newName={}", scopeType, scopeId, newName);
    }

    // ==================== 私有方法 ====================

    /**
     * 验证请求参数
     */
    private void validateRequest(UserDataScopeRequest request) {
        // 验证用户是否存在
        if (userMapper.selectById(request.getUserId()) == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证范围类型
        ScopeType scopeType = ScopeType.fromCode(request.getScopeType());
        if (scopeType == null) {
            throw new BusinessException("无效的范围类型: " + request.getScopeType());
        }

        // 验证范围ID是否存在
        boolean exists = false;
        switch (scopeType) {
            case DEPARTMENT:
                exists = departmentMapper.selectById(request.getScopeId()) != null;
                break;
            case GRADE:
                exists = gradeMapper.selectById(request.getScopeId()) != null;
                break;
            case CLASS:
                exists = classMapper.selectById(request.getScopeId()) != null;
                break;
        }
        if (!exists) {
            throw new BusinessException("范围不存在: " + scopeType.getName() + " ID=" + request.getScopeId());
        }
    }

    /**
     * 获取范围名称
     */
    private String getScopeName(String scopeTypeCode, Long scopeId) {
        ScopeType scopeType = ScopeType.fromCode(scopeTypeCode);
        if (scopeType == null) {
            return null;
        }

        switch (scopeType) {
            case DEPARTMENT:
                var dept = departmentMapper.selectById(scopeId);
                return dept != null ? dept.getDeptName() : null;
            case GRADE:
                var grade = gradeMapper.selectById(scopeId);
                return grade != null ? grade.getGradeName() : null;
            case CLASS:
                var cls = classMapper.selectById(scopeId);
                return cls != null ? cls.getClassName() : null;
            default:
                return null;
        }
    }

    /**
     * 转换为DTO
     */
    private UserDataScopeDTO convertToDTO(UserDataScope entity) {
        ScopeType scopeType = ScopeType.fromCode(entity.getScopeType());
        return UserDataScopeDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .scopeType(entity.getScopeType())
                .scopeTypeName(scopeType != null ? scopeType.getName() : null)
                .scopeId(entity.getScopeId())
                .scopeName(entity.getScopeName())
                .includeChildren(entity.getIncludeChildren())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    /**
     * 清除用户缓存
     */
    private void evictCache(Long userId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(userId);
            cache.evict("dept:" + userId);
            cache.evict("grade:" + userId);
            cache.evict("class:" + userId);
            log.debug("清除用户数据范围缓存: userId={}", userId);
        }
    }
}
