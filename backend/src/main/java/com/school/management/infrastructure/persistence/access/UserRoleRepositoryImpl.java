package com.school.management.infrastructure.persistence.access;

import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.access.repository.UserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of UserRoleRepository.
 */
@Repository
public class UserRoleRepositoryImpl implements UserRoleRepository {

    private final DddUserRoleMapper userRoleMapper;

    public UserRoleRepositoryImpl(DddUserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public UserRole save(UserRole aggregate) {
        UserRolePO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            userRoleMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            userRoleMapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<UserRole> findById(Long id) {
        UserRolePO po = userRoleMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(UserRole aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            userRoleMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        userRoleMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return userRoleMapper.selectById(id) != null;
    }

    @Override
    public List<UserRole> findByUserId(Long userId) {
        return userRoleMapper.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserRole> findActiveByUserId(Long userId) {
        return userRoleMapper.findActiveByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserRole> findByRoleId(Long roleId) {
        return userRoleMapper.findByRoleId(roleId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserRole> findByUserIdAndScope(Long userId, String scopeType, Long scopeId) {
        return userRoleMapper.findByUserIdAndScope(userId, scopeType, scopeId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndRoleId(Long userId, Long roleId) {
        return userRoleMapper.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public boolean existsByUserIdAndRoleIdAndScope(Long userId, Long roleId, String scopeType, Long scopeId) {
        return userRoleMapper.existsByUserIdAndRoleIdAndScope(userId, roleId, scopeType, scopeId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        userRoleMapper.deleteByUserId(userId);
    }

    @Override
    public void deleteByUserIdAndRoleId(Long userId, Long roleId) {
        userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserIdAndRoleIdAndScope(Long userId, Long roleId, String scopeType, Long scopeId) {
        userRoleMapper.deleteByUserIdAndRoleIdAndScope(userId, roleId, scopeType, scopeId);
    }

    private UserRolePO toPO(UserRole domain) {
        UserRolePO po = new UserRolePO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setRoleId(domain.getRoleId());
        po.setScopeType(domain.getScopeType() != null ? domain.getScopeType() : ScopeType.ALL);
        po.setScopeId(domain.getScopeId() != null ? domain.getScopeId() : 0L);
        po.setAssignedAt(domain.getAssignedAt());
        po.setAssignedBy(domain.getAssignedBy());
        po.setExpiresAt(domain.getExpiresAt());
        po.setIsActive(domain.getIsActive());
        return po;
    }

    private UserRole toDomain(UserRolePO po) {
        return UserRole.builder()
            .id(po.getId())
            .userId(po.getUserId())
            .roleId(po.getRoleId())
            .scopeType(po.getScopeType())
            .scopeId(po.getScopeId())
            .assignedAt(po.getAssignedAt())
            .assignedBy(po.getAssignedBy())
            .expiresAt(po.getExpiresAt())
            .isActive(po.getIsActive())
            .build();
    }
}
