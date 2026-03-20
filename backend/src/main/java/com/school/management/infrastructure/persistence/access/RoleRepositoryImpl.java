package com.school.management.infrastructure.persistence.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of RoleRepository.
 */
@Slf4j
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final DddRoleMapper roleMapper;
    private final DddRolePermissionMapper rolePermissionMapper;

    public RoleRepositoryImpl(DddRoleMapper roleMapper, DddRolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public Role save(Role aggregate) {
        RolePO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            roleMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            roleMapper.updateById(po);
        }

        // Save role permissions
        saveRolePermissions(aggregate);

        return aggregate;
    }

    private void saveRolePermissions(Role role) {
        if (role.getId() == null) {
            log.warn("saveRolePermissions: role.getId() is null, skipping");
            return;
        }

        // Get current permissions in database
        List<Long> currentPermissionIds = rolePermissionMapper.findPermissionIdsByRoleId(role.getId());
        Set<Long> currentSet = new HashSet<>(currentPermissionIds);
        Set<Long> newSet = role.getPermissionIds();

        log.info("saveRolePermissions: roleId={}, currentDB={}, newFromRole={}",
            role.getId(), currentSet.size(), newSet.size());

        // Find permissions to add
        Set<Long> toAdd = new HashSet<>(newSet);
        toAdd.removeAll(currentSet);

        // Find permissions to remove
        Set<Long> toRemove = new HashSet<>(currentSet);
        toRemove.removeAll(newSet);

        log.info("saveRolePermissions: toAdd={}, toRemove={}", toAdd.size(), toRemove.size());

        // Add new permissions
        for (Long permissionId : toAdd) {
            RolePermissionPO rp = new RolePermissionPO();
            rp.setRoleId(role.getId());
            rp.setPermissionId(permissionId);
            rolePermissionMapper.insert(rp);
        }

        // Remove old permissions
        for (Long permissionId : toRemove) {
            rolePermissionMapper.deleteByRoleIdAndPermissionId(role.getId(), permissionId);
        }

        log.info("saveRolePermissions: completed for roleId={}", role.getId());
    }

    @Override
    public Optional<Role> findById(Long id) {
        RolePO po = roleMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomainWithPermissions(po));
    }

    @Override
    public void delete(Role aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            rolePermissionMapper.deleteByRoleId(aggregate.getId());
            roleMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        rolePermissionMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return roleMapper.selectById(id) != null;
    }

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        RolePO po = roleMapper.findByRoleCode(roleCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomainWithPermissions(po));
    }

    @Override
    public List<Role> findByRoleType(String roleType) {
        return roleMapper.findAll().stream()
            .filter(po -> roleType.equals(po.getRoleType()))
            .map(this::toDomainWithPermissions)
            .collect(Collectors.toList());
    }

    @Override
    public List<Role> findAllEnabled() {
        return roleMapper.findAllEnabled().stream()
            .map(this::toDomainWithPermissions)
            .collect(Collectors.toList());
    }

    @Override
    public List<Role> findSystemRoles() {
        // The database doesn't have is_system column, filter by role_code pattern.
        // Optimization: filter on PO level first to avoid loading permissions for non-system roles.
        return roleMapper.findAll().stream()
            .filter(po -> po.getRoleCode() != null &&
                (po.getRoleCode().startsWith("ROLE_ADMIN") || po.getRoleCode().equals("admin")))
            .map(this::toDomainWithPermissions)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return roleMapper.existsByRoleCode(roleCode);
    }

    @Override
    public List<Role> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return roleMapper.findByIds(ids).stream()
            .map(this::toDomainWithPermissions)
            .collect(Collectors.toList());
    }

    @Override
    public List<Role> findAllWithPermissions() {
        return roleMapper.findAllEnabled().stream()
            .map(this::toDomainWithPermissions)
            .collect(Collectors.toList());
    }

    private RolePO toPO(Role domain) {
        RolePO po = new RolePO();
        po.setId(domain.getId());
        po.setRoleName(domain.getRoleName());
        po.setRoleCode(domain.getRoleCode());
        po.setRoleType(domain.getRoleType());
        po.setRoleDesc(domain.getDescription());
        po.setSortOrder(domain.getLevel()); // Use level as sortOrder
        po.setStatus(domain.getIsEnabled() ? 1 : 0);
        po.setTenantId(domain.getTenantId());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private Role toDomainWithPermissions(RolePO po) {
        List<Long> permissionIds = rolePermissionMapper.findPermissionIdsByRoleId(po.getId());

        String roleType = po.getRoleType() != null ? po.getRoleType() : "CUSTOM";

        boolean isSystem = "SUPER_ADMIN".equals(roleType) || "SYSTEM_ADMIN".equals(roleType);

        return Role.builder()
            .id(po.getId())
            .roleCode(po.getRoleCode())
            .roleName(po.getRoleName())
            .description(po.getRoleDesc())
            .roleType(roleType)
            .level(po.getSortOrder() != null ? po.getSortOrder() : 0)
            .isSystem(isSystem)
            .isEnabled(po.getStatus() != null && po.getStatus() == 1)
            .createdBy(null)
            .permissionIds(new HashSet<>(permissionIds))
            .dataScope(DataScope.ALL)
            .tenantId(po.getTenantId())
            .build();
    }
}
