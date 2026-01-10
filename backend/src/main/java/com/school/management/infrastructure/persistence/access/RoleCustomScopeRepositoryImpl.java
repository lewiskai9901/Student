package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.access.model.RoleCustomScope;
import com.school.management.domain.access.repository.RoleCustomScopeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色自定义数据范围仓储实现
 */
@Repository
@RequiredArgsConstructor
public class RoleCustomScopeRepositoryImpl implements RoleCustomScopeRepository {

    private final RoleCustomScopeMapper mapper;

    @Override
    public List<RoleCustomScope> findByRoleIdAndModule(Long roleId, String moduleCode) {
        LambdaQueryWrapper<RoleCustomScopePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleCustomScopePO::getRoleId, roleId)
               .eq(RoleCustomScopePO::getModuleCode, moduleCode);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoleCustomScope> findByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleCustomScopePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleCustomScopePO::getRoleId, roleId);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveAll(Long roleId, String moduleCode, List<Long> orgUnitIds) {
        // 先删除旧的配置
        deleteByRoleIdAndModule(roleId, moduleCode);

        // 插入新配置
        if (orgUnitIds != null && !orgUnitIds.isEmpty()) {
            for (Long orgUnitId : orgUnitIds) {
                RoleCustomScopePO po = new RoleCustomScopePO();
                po.setRoleId(roleId);
                po.setModuleCode(moduleCode);
                po.setOrgUnitId(orgUnitId);
                mapper.insert(po);
            }
        }
    }

    @Override
    @Transactional
    public void deleteByRoleIdAndModule(Long roleId, String moduleCode) {
        LambdaQueryWrapper<RoleCustomScopePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleCustomScopePO::getRoleId, roleId)
               .eq(RoleCustomScopePO::getModuleCode, moduleCode);
        mapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void deleteByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleCustomScopePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleCustomScopePO::getRoleId, roleId);
        mapper.delete(wrapper);
    }

    private RoleCustomScope toDomain(RoleCustomScopePO po) {
        RoleCustomScope scope = new RoleCustomScope();
        scope.setId(po.getId());
        scope.setRoleId(po.getRoleId());
        scope.setModuleCode(po.getModuleCode());
        scope.setOrgUnitId(po.getOrgUnitId());
        return scope;
    }
}
