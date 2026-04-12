package com.school.management.infrastructure.persistence.user;

import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.valueobject.UserStatus;
import com.school.management.domain.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of UserRepository
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserDomainMapper userMapper;

    public UserRepositoryImpl(UserDomainMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        UserPO po = toPO(user);

        if (user.getId() == null) {
            userMapper.insert(po);
            user.setId(po.getId());
        } else {
            userMapper.updateById(po);
        }

        // 保存角色关联
        if (user.getId() != null && user.getRoleIds() != null) {
            // 先删除旧的角色关联
            userMapper.deleteUserRoles(user.getId());
            // 再插入新的角色关联
            for (Long roleId : user.getRoleIds()) {
                userMapper.insertUserRole(user.getId(), roleId);
            }
        }

        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        UserPO po = userMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        User user = toDomain(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            user.assignRoles(roleIds);
        }
        return Optional.of(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserPO po = userMapper.findByUsername(username);
        if (po == null) {
            return Optional.empty();
        }
        User user = toDomain(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            user.assignRoles(roleIds);
        }
        return Optional.of(user);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        UserPO po = userMapper.findByPhone(phone);
        if (po == null) {
            return Optional.empty();
        }
        User user = toDomain(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            user.assignRoles(roleIds);
        }
        return Optional.of(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserPO po = userMapper.findByEmail(email);
        if (po == null) {
            return Optional.empty();
        }
        User user = toDomain(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            user.assignRoles(roleIds);
        }
        return Optional.of(user);
    }

    @Override
    public Optional<User> findByEmployeeNo(String employeeNo) {
        UserPO po = userMapper.findByEmployeeNo(employeeNo);
        if (po == null) {
            return Optional.empty();
        }
        User user = toDomain(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            user.assignRoles(roleIds);
        }
        return Optional.of(user);
    }

    @Override
    public Optional<User> findByWechatOpenid(String openid) {
        UserPO po = userMapper.findByWechatOpenid(openid);
        if (po == null) {
            return Optional.empty();
        }
        User user = toDomain(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            user.assignRoles(roleIds);
        }
        return Optional.of(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    @Override
    public boolean existsByUsernameAndIdNot(String username, Long excludeId) {
        return userMapper.existsByUsernameAndIdNot(username, excludeId);
    }

    @Override
    public List<User> findByOrgUnitId(Long orgUnitId) {
        return userMapper.findByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByOrgUnitIdIn(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.findByOrgUnitIdIn(orgUnitIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(User user) {
        if (user != null && user.getId() != null) {
            userMapper.softDeleteById(Long.parseLong(user.getId().toString()));
        }
    }

    @Override
    public void deleteById(Long id) {
        userMapper.softDeleteById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            userMapper.deleteByIds(ids);
        }
    }

    @Override
    public List<User> findAll(int page, int size) {
        int offset = (page - 1) * size;
        return userMapper.findAllPaged(offset, size).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return userMapper.countAll();
    }

    @Override
    public List<User> findPagedWithConditions(int page, int size, String username, String realName,
                                              String phone, Long orgUnitId, Integer status) {
        int offset = (page - 1) * size;
        List<UserPO> pos = userMapper.findPagedWithConditions(offset, size, username, realName, phone, orgUnitId, status);
        if (pos.isEmpty()) {
            return new ArrayList<>();
        }

        // 一次性批量加载全部用户的角色，避免 N+1 查询
        List<Long> userIds = pos.stream().map(UserPO::getId).collect(Collectors.toList());
        Map<Long, List<Long>> roleIdsByUser = new HashMap<>();
        Map<Long, List<String>> roleNamesByUser = new HashMap<>();
        List<Map<String, Object>> rows = userMapper.findRolesByUserIds(userIds);
        for (Map<String, Object> row : rows) {
            Object uidObj = row.get("user_id");
            if (uidObj == null) continue;
            Long uid = ((Number) uidObj).longValue();
            Object roleIdObj = row.get("role_id");
            if (roleIdObj != null) {
                roleIdsByUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(((Number) roleIdObj).longValue());
            }
            Object roleNameObj = row.get("role_name");
            if (roleNameObj != null) {
                roleNamesByUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(roleNameObj.toString());
            }
        }

        return pos.stream().map(po -> {
            User user = toDomainWithOrgUnit(po);
            if (po.getId() != null) {
                user.assignRoles(roleIdsByUser.getOrDefault(po.getId(), new ArrayList<>()));
                user.setRoleNames(roleNamesByUser.getOrDefault(po.getId(), new ArrayList<>()));
            }
            return user;
        }).collect(Collectors.toList());
    }

    @Override
    public long countWithConditions(String username, String realName, String phone,
                                    Long orgUnitId, Integer status) {
        return userMapper.countWithConditions(username, realName, phone, orgUnitId, status);
    }

    @Override
    public List<User> findSimpleList(String keyword) {
        return userMapper.findSimpleList(keyword).stream()
                .map(this::toDomainWithOrgUnit)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.findAllPaged(0, 10000).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.selectBatchIds(ids).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private UserPO toPO(User domain) {
        UserPO po = new UserPO();
        po.setId(domain.getId());
        po.setUsername(domain.getUsername());
        po.setPassword(domain.getPassword());
        po.setRealName(domain.getRealName());
        po.setPhone(domain.getPhone());
        po.setEmail(domain.getEmail());
        po.setAvatar(domain.getAvatar());
        po.setEmployeeNo(domain.getEmployeeNo());
        po.setGender(domain.getGender());
        po.setBirthDate(domain.getBirthDate());
        po.setIdCard(domain.getIdCard());
        po.setPrimaryOrgUnitId(domain.getPrimaryOrgUnitId());
        po.setUserTypeCode(domain.getUserTypeCode());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setLastLoginTime(domain.getLastLoginTime());
        po.setLastLoginIp(domain.getLastLoginIp());
        po.setPasswordChangedAt(domain.getPasswordChangedAt());
        po.setWechatOpenid(domain.getWechatOpenid());
        po.setAllowMultipleDevices(domain.isAllowMultipleDevices() ? 1 : 0);
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private User toDomain(UserPO po) {
        return User.reconstruct(
                po.getId(),
                po.getUsername(),
                po.getPassword(),
                po.getRealName(),
                po.getPhone(),
                po.getEmail(),
                po.getAvatar(),
                po.getEmployeeNo(),
                po.getGender(),
                po.getBirthDate(),
                po.getIdCard(),
                po.getPrimaryOrgUnitId(),
                po.getUserTypeCode(),
                po.getStatus() != null ? UserStatus.fromCode(po.getStatus()) : null,
                po.getLastLoginTime(),
                po.getLastLoginIp(),
                po.getPasswordChangedAt(),
                po.getWechatOpenid(),
                po.getAllowMultipleDevices() != null && po.getAllowMultipleDevices() == 1,
                new ArrayList<>(), // Role IDs loaded separately
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    private User toDomainWithOrgUnitAndRoles(UserPO po) {
        User user = toDomainWithOrgUnit(po);
        if (po.getId() != null) {
            List<Long> roleIds = userMapper.findRoleIdsByUserId(po.getId());
            List<String> roleNames = userMapper.findRoleNamesByUserId(po.getId());
            user.assignRoles(roleIds);
            user.setRoleNames(roleNames);
        }
        return user;
    }

    private User toDomainWithOrgUnit(UserPO po) {
        User user = toDomain(po);
        // 设置组织单元信息（从JOIN查询获取）
        if (po.getOrgUnitId() != null) {
            user.setOrgUnitId(po.getOrgUnitId());
        }
        if (po.getOrgUnitName() != null) {
            user.setOrgUnitName(po.getOrgUnitName());
        }
        return user;
    }
}
