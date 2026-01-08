package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.management.entity.UserDepartment;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.UserDepartmentMapper;
import com.school.management.service.UserDepartmentService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户部门关联服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDepartmentServiceImpl implements UserDepartmentService {

    private final UserDepartmentMapper userDepartmentMapper;

    @Override
    public List<Long> getDepartmentIdsByUserId(Long userId) {
        return userDepartmentMapper.selectDepartmentIdsByUserId(userId);
    }

    @Override
    public List<UserDepartment> getByUserId(Long userId) {
        return userDepartmentMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDepartments(Long userId, List<Long> departmentIds, Long primaryDeptId) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            throw new BusinessException("部门列表不能为空");
        }

        // 验证主部门在部门列表中
        if (primaryDeptId != null && !departmentIds.contains(primaryDeptId)) {
            throw new BusinessException("主部门必须在部门列表中");
        }

        // 删除现有关联
        userDepartmentMapper.delete(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId));

        // 批量插入新关联
        Long currentUserId = SecurityUtils.getCurrentUserId();
        for (Long deptId : departmentIds) {
            UserDepartment ud = new UserDepartment();
            ud.setUserId(userId);
            ud.setDepartmentId(deptId);
            ud.setIsPrimary(deptId.equals(primaryDeptId) ? 1 : 0);
            ud.setCreatedBy(currentUserId);
            userDepartmentMapper.insert(ud);
        }

        log.info("为用户 {} 分配了 {} 个部门", userId, departmentIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDepartment(Long userId, Long departmentId) {
        // 检查是否为主部门
        UserDepartment ud = userDepartmentMapper.selectOne(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId)
            .eq("department_id", departmentId));

        if (ud != null && ud.getIsPrimary() == 1) {
            // 检查是否还有其他部门
            long count = userDepartmentMapper.selectCount(new QueryWrapper<UserDepartment>()
                .eq("user_id", userId));

            if (count <= 1) {
                throw new BusinessException("无法移除唯一的主部门");
            }
        }

        userDepartmentMapper.delete(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId)
            .eq("department_id", departmentId));

        log.info("移除用户 {} 的部门 {}", userId, departmentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimaryDepartment(Long userId, Long departmentId) {
        // 验证该部门已关联
        UserDepartment ud = userDepartmentMapper.selectOne(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId)
            .eq("department_id", departmentId));

        if (ud == null) {
            throw new BusinessException("该用户未关联此部门");
        }

        // 清除所有主部门标记
        UserDepartment updateEntity = new UserDepartment();
        updateEntity.setIsPrimary(0);
        userDepartmentMapper.update(updateEntity, new QueryWrapper<UserDepartment>()
            .eq("user_id", userId));

        // 设置新的主部门
        ud.setIsPrimary(1);
        userDepartmentMapper.updateById(ud);

        log.info("设置用户 {} 的主部门为 {}", userId, departmentId);
    }
}
