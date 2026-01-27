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
    public List<Long> getOrgUnitIdsByUserId(Long userId) {
        return userDepartmentMapper.selectOrgUnitIdsByUserId(userId);
    }

    @Override
    public List<UserDepartment> getByUserId(Long userId) {
        return userDepartmentMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDepartments(Long userId, List<Long> orgUnitIds, Long primaryDeptId) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            throw new BusinessException("组织单元列表不能为空");
        }

        // 验证主组织单元在组织单元列表中
        if (primaryDeptId != null && !orgUnitIds.contains(primaryDeptId)) {
            throw new BusinessException("主组织单元必须在组织单元列表中");
        }

        // 删除现有关联
        userDepartmentMapper.delete(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId));

        // 批量插入新关联
        Long currentUserId = SecurityUtils.getCurrentUserId();
        for (Long orgUnitId : orgUnitIds) {
            UserDepartment ud = new UserDepartment();
            ud.setUserId(userId);
            ud.setOrgUnitId(orgUnitId);
            ud.setIsPrimary(orgUnitId.equals(primaryDeptId) ? 1 : 0);
            ud.setCreatedBy(currentUserId);
            userDepartmentMapper.insert(ud);
        }

        log.info("为用户 {} 分配了 {} 个组织单元", userId, orgUnitIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDepartment(Long userId, Long orgUnitId) {
        // 检查是否为主组织单元
        UserDepartment ud = userDepartmentMapper.selectOne(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId)
            .eq("org_unit_id", orgUnitId));

        if (ud != null && ud.getIsPrimary() == 1) {
            // 检查是否还有其他组织单元
            long count = userDepartmentMapper.selectCount(new QueryWrapper<UserDepartment>()
                .eq("user_id", userId));

            if (count <= 1) {
                throw new BusinessException("无法移除唯一的主组织单元");
            }
        }

        userDepartmentMapper.delete(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId)
            .eq("org_unit_id", orgUnitId));

        log.info("移除用户 {} 的组织单元 {}", userId, orgUnitId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimaryDepartment(Long userId, Long orgUnitId) {
        // 验证该组织单元已关联
        UserDepartment ud = userDepartmentMapper.selectOne(new QueryWrapper<UserDepartment>()
            .eq("user_id", userId)
            .eq("org_unit_id", orgUnitId));

        if (ud == null) {
            throw new BusinessException("该用户未关联此组织单元");
        }

        // 清除所有主组织单元标记
        UserDepartment updateEntity = new UserDepartment();
        updateEntity.setIsPrimary(0);
        userDepartmentMapper.update(updateEntity, new QueryWrapper<UserDepartment>()
            .eq("user_id", userId));

        // 设置新的主组织单元
        ud.setIsPrimary(1);
        userDepartmentMapper.updateById(ud);

        log.info("设置用户 {} 的主组织单元为 {}", userId, orgUnitId);
    }
}
