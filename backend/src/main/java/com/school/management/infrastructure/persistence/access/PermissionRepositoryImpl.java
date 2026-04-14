package com.school.management.infrastructure.persistence.access;

import com.school.management.domain.access.model.Permission;
import com.school.management.domain.access.model.PermissionScope;
import com.school.management.domain.access.model.PermissionType;
import com.school.management.domain.access.repository.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of PermissionRepository.
 */
@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final DddPermissionMapper permissionMapper;

    public PermissionRepositoryImpl(DddPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Permission save(Permission aggregate) {
        PermissionPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            permissionMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            permissionMapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<Permission> findById(Long id) {
        PermissionPO po = permissionMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Permission aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            permissionMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        permissionMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return permissionMapper.selectById(id) != null;
    }

    @Override
    public Optional<Permission> findByPermissionCode(String permissionCode) {
        PermissionPO po = permissionMapper.findByPermissionCode(permissionCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Permission> findByType(PermissionType type) {
        return permissionMapper.findByResourceType(toResourceType(type)).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findAllEnabled() {
        return permissionMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findByParentId(Long parentId) {
        return permissionMapper.findByParentId(parentId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findRoots() {
        return permissionMapper.findRoots().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findByResource(String resource) {
        return permissionMapper.findByResource(resource).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPermissionCode(String permissionCode) {
        return permissionMapper.existsByPermissionCode(permissionCode);
    }

    @Override
    public List<Permission> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return permissionMapper.findByIds(ids).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private PermissionPO toPO(Permission domain) {
        PermissionPO po = new PermissionPO();
        po.setId(domain.getId());
        po.setPermissionCode(domain.getPermissionCode());
        po.setPermissionName(domain.getPermissionName());
        po.setPermissionDesc(domain.getDescription());
        po.setResourceType(toResourceType(domain.getType()));
        po.setPermissionScope(domain.getScope() != null ? domain.getScope().name() : PermissionScope.MANAGEMENT.name());
        po.setParentId(domain.getParentId());
        po.setPath(domain.getPath());
        po.setComponent(domain.getComponent());
        po.setIcon(domain.getIcon());
        po.setSortOrder(domain.getSortOrder());
        po.setStatus(domain.getIsEnabled() ? 1 : 0);
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private Permission toDomain(PermissionPO po) {
        // Parse resource and action from permission code (e.g., "system:user:view" -> resource="system:user", action="view")
        String resource = null;
        String action = null;
        String code = po.getPermissionCode();
        if (code != null && code.contains(":")) {
            int lastColon = code.lastIndexOf(':');
            resource = code.substring(0, lastColon);
            action = code.substring(lastColon + 1);
        }

        return Permission.builder()
            .id(po.getId())
            .permissionCode(po.getPermissionCode())
            .permissionName(po.getPermissionName())
            .description(po.getPermissionDesc())
            .resource(resource)
            .action(action)
            .type(fromResourceType(po.getResourceType()))
            .scope(PermissionScope.fromCode(po.getPermissionScope()))
            .parentId(po.getParentId())
            .path(po.getPath())
            .component(po.getComponent())
            .icon(po.getIcon())
            .sortOrder(po.getSortOrder())
            .isEnabled(po.getStatus() != null && po.getStatus() == 1)
            .build();
    }

    /**
     * Convert PermissionType to database resource_type:
     * 1=菜单(Menu), 2=按钮(Operation), 3=接口(API)
     */
    private Integer toResourceType(PermissionType type) {
        if (type == null) return 2; // default to button/operation
        return switch (type) {
            case MENU -> 1;
            case OPERATION -> 2;
            case API -> 3;
            case DATA -> 2; // map DATA to operation
        };
    }

    /**
     * Convert database resource_type to PermissionType:
     * 1=菜单(Menu), 2=按钮(Operation), 3=接口(API)
     */
    private PermissionType fromResourceType(Integer resourceType) {
        if (resourceType == null) return PermissionType.OPERATION;
        return switch (resourceType) {
            case 1 -> PermissionType.MENU;
            case 2 -> PermissionType.OPERATION;
            case 3 -> PermissionType.API;
            default -> PermissionType.OPERATION;
        };
    }
}
