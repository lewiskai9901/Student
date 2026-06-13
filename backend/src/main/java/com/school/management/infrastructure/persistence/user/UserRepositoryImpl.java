package com.school.management.infrastructure.persistence.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public UserRepositoryImpl(UserDomainMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
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

        // 保存角色关联 — 差异更新, 不再全量删+裸插。
        // 旧实现 deleteUserRoles + insertUserRole 会把 UserRoleController 设置的
        // scope_type/scope_id/expires_at/granted_by 整表抹平 (任何 updateUser 哪怕只改手机号都触发,
        // 因 findById 已把 roleIds 装进聚合)。改为只删"不再需要"的、只插"新增"的, 未变角色保留其 scope 行。
        if (user.getId() != null && user.getRoleIds() != null) {
            java.util.Set<Long> existing = new java.util.HashSet<>(userMapper.findAllRoleIdsByUserId(user.getId()));
            java.util.Set<Long> desired = new java.util.LinkedHashSet<>(user.getRoleIds());
            for (Long roleId : existing) {
                if (!desired.contains(roleId)) {
                    userMapper.deleteUserRole(user.getId(), roleId);
                }
            }
            for (Long roleId : desired) {
                if (!existing.contains(roleId)) {
                    userMapper.insertUserRole(user.getId(), roleId);
                }
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
    public List<User> findByOrgUnit(Long orgUnitId, boolean includeChildren, String keyword) {
        if (orgUnitId == null) {
            return new ArrayList<>();
        }
        return userMapper.findByOrgUnitSubtree(orgUnitId, includeChildren, keyword).stream()
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
    public List<User> findByUserTypeCode(String userTypeCode) {
        if (userTypeCode == null || userTypeCode.isBlank()) {
            return new ArrayList<>();
        }
        return userMapper.findByUserTypeCode(userTypeCode).stream()
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
        po.setGender(domain.getGender());
        po.setBirthDate(domain.getBirthDate());
        po.setIdCard(domain.getIdCard());
        po.setUserTypeCode(domain.getUserTypeCode());
        po.setAttributes(attributesToJson(domain.getAttributes()));
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
        User user = User.reconstruct(
                po.getId(),
                po.getUsername(),
                po.getPassword(),
                po.getRealName(),
                po.getPhone(),
                po.getEmail(),
                po.getAvatar(),
                po.getGender(),
                po.getBirthDate(),
                po.getIdCard(),
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
        user.setAttributes(attributesFromJson(po.getAttributes()));
        return user;
    }

    /** 扩展属性 Map → JSON 字符串 (写库); null/空 → null */
    private String attributesToJson(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON 字符串 → 扩展属性 Map (读库); null/解析失败 → null */
    private Map<String, Object> attributesFromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
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
