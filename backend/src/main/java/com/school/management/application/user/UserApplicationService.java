package com.school.management.application.user;

import com.school.management.application.user.command.CreateUserCommand;
import com.school.management.application.user.command.UpdateUserCommand;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.domain.user.repository.UserTypeRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用户应用服务
 *
 * 职责：
 * 1. 协调领域对象完成用例
 * 2. 事务边界管理
 * 3. 领域事件发布
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final RoleRepository roleRepository;
    private final AccessRelationRepository accessRelationRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;
    private final UniversalPlaceOccupantRepository occupantRepository;
    private final UniversalPlaceRepository placeRepository;

    @Value("${app.security.default-password:Pwd@123456}")
    private String defaultPassword;

    // ==================== 用户创建 ====================

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(CreateUserCommand command) {
        log.info("创建用户: {}", command.getUsername());

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new BusinessException("用户名已存在: " + command.getUsername());
        }

        // 加密密码
        String encodedPassword = passwordEncoder.encode(
                command.getPassword() != null ? command.getPassword() : defaultPassword
        );

        // 创建用户聚合
        User user = User.create(
                command.getUsername(),
                encodedPassword,
                command.getRealName(),
                command.getUserTypeCode() != null ? command.getUserTypeCode() : "TEACHER"
        );

        // 设置可选信息
        user.updateBasicInfo(
                command.getRealName(),
                command.getPhone(),
                command.getEmail(),
                command.getEmployeeNo(),
                command.getGender(),
                command.getBirthDate(),
                command.getIdCard()
        );

        // 设置主归属组织（持久化字段）
        if (command.getOrgUnitId() != null) {
            user.setPrimaryOrgUnitId(command.getOrgUnitId());
        }

        // 分配角色：优先使用命令中指定的角色，否则使用用户类型的默认角色
        List<Long> roleIds = command.getRoleIds();
        if ((roleIds == null || roleIds.isEmpty()) && command.getUserTypeCode() != null) {
            roleIds = resolveDefaultRoleIds(command.getUserTypeCode());
        }
        if (roleIds != null && !roleIds.isEmpty()) {
            user.assignRoles(roleIds);
        }

        // 保存用户
        user = userRepository.save(user);

        // 关联组织（写入 access_relations）
        if (command.getOrgUnitId() != null) {
            accessRelationRepository.save(AccessRelation.builder()
                    .resourceType("org_unit")
                    .resourceId(command.getOrgUnitId())
                    .relation("member")
                    .subjectType("user")
                    .subjectId(user.getId())
                    .accessLevel(1)
                    .metadata(java.util.Map.of("isPrimary", true, "relationType", "PRIMARY"))
                    .createdBy(command.getCreatedBy())
                    .build());
        }

        // 关联场所
        if (command.getPlaceId() != null) {
            accessRelationRepository.save(AccessRelation.builder()
                    .resourceType("place")
                    .resourceId(command.getPlaceId())
                    .relation("occupant")
                    .subjectType("user")
                    .subjectId(user.getId())
                    .accessLevel(1)
                    .metadata(java.util.Map.of("isPrimary", true, "relationType", "ASSIGNED"))
                    .createdBy(command.getCreatedBy())
                    .build());
        }

        // 发布领域事件
        publishEvents(user);

        log.info("用户创建成功: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 根据用户类型编码解析默认角色ID列表
     */
    private List<Long> resolveDefaultRoleIds(String userTypeCode) {
        Optional<UserType> utOpt = userTypeRepository.findByTypeCode(userTypeCode);
        if (utOpt.isEmpty()) return List.of();

        List<String> codes = utOpt.get().getDefaultRoleCodes();
        if (codes == null || codes.isEmpty()) return List.of();

        List<Long> ids = new ArrayList<>();
        for (String code : codes) {
            if (code != null && !code.trim().isEmpty()) {
                roleRepository.findByRoleCode(code.trim()).ifPresent(role -> ids.add(role.getId()));
            }
        }
        return ids;
    }

    // ==================== 用户更新 ====================

    /**
     * 更新用户
     */
    @Transactional
    public User updateUser(Long userId, UpdateUserCommand command) {
        log.info("更新用户: {}", userId);

        User user = getUserOrThrow(userId);

        // 更新基本信息
        user.updateBasicInfo(
                command.getRealName(),
                command.getPhone(),
                command.getEmail(),
                command.getEmployeeNo(),
                command.getGender(),
                command.getBirthDate(),
                command.getIdCard()
        );

        // 更新用户类型
        if (command.getUserTypeCode() != null) {
            user.changeUserType(command.getUserTypeCode());
        }

        // 更新主归属组织
        if (command.getOrgUnitId() != null) {
            // 查找现有主归属
            List<AccessRelation> existingOrgRels = accessRelationRepository
                    .findBySubjectAndResourceType("user", userId, "org_unit");
            AccessRelation existingPrimary = existingOrgRels.stream()
                    .filter(r -> r.getBooleanMeta("isPrimary"))
                    .findFirst().orElse(null);

            if (existingPrimary != null) {
                if (!command.getOrgUnitId().equals(existingPrimary.getResourceId())) {
                    accessRelationRepository.deleteById(existingPrimary.getId());
                    accessRelationRepository.save(AccessRelation.builder()
                            .resourceType("org_unit")
                            .resourceId(command.getOrgUnitId())
                            .relation("member")
                            .subjectType("user")
                            .subjectId(userId)
                            .accessLevel(1)
                            .metadata(java.util.Map.of("isPrimary", true, "relationType", "PRIMARY"))
                            .createdBy(command.getUpdatedBy())
                            .build());
                }
            } else {
                accessRelationRepository.save(AccessRelation.builder()
                        .resourceType("org_unit")
                        .resourceId(command.getOrgUnitId())
                        .relation("member")
                        .subjectType("user")
                        .subjectId(userId)
                        .accessLevel(1)
                        .metadata(java.util.Map.of("isPrimary", true, "relationType", "PRIMARY"))
                        .createdBy(command.getUpdatedBy())
                        .build());
            }
        }

        // 更新角色
        if (command.getRoleIds() != null) {
            user.assignRoles(command.getRoleIds());
        }

        // 保存
        user = userRepository.save(user);

        // 发布事件
        publishEvents(user);

        log.info("用户更新成功: {}", userId);
        return user;
    }

    // ==================== 状态管理 ====================

    /**
     * 启用用户
     */
    @Transactional
    public User enableUser(Long userId) {
        log.info("启用用户: {}", userId);

        User user = getUserOrThrow(userId);
        user.enable();
        user = userRepository.save(user);

        publishEvents(user);
        return user;
    }

    /**
     * 禁用用户
     */
    @Transactional
    public User disableUser(Long userId) {
        log.info("禁用用户: {}", userId);

        User user = getUserOrThrow(userId);
        user.disable();
        user = userRepository.save(user);

        publishEvents(user);
        return user;
    }

    // ==================== 密码管理 ====================

    /**
     * 重置密码
     */
    @Transactional
    public String resetPassword(Long userId) {
        log.info("重置用户密码: {}", userId);

        User user = getUserOrThrow(userId);

        String newPassword = defaultPassword;
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.resetPassword(encodedPassword);

        userRepository.save(user);
        publishEvents(user);

        return newPassword;
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改用户密码: {}", userId);

        User user = getUserOrThrow(userId);

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 设置新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);

        userRepository.save(user);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("删除用户: {}", userId);

        if (userRepository.findById(userId).isEmpty()) {
            throw new BusinessException("用户不存在: " + userId);
        }

        // 自动退房：清理该用户的所有在住记录
        autoCheckOutByUser(userId);

        userRepository.deleteById(userId);
    }

    /**
     * 批量删除用户
     */
    @Transactional
    public void deleteUsers(List<Long> userIds) {
        log.info("批量删除用户: {}", userIds);
        for (Long userId : userIds) {
            autoCheckOutByUser(userId);
        }
        userRepository.deleteByIds(userIds);
    }

    /**
     * 自动退房：退出指定用户的所有在住记录，并更新场所占用数
     */
    private void autoCheckOutByUser(Long userId) {
        List<UniversalPlaceOccupant> activeOccupancies = occupantRepository.findActiveByOccupantId(userId);
        if (activeOccupancies.isEmpty()) return;

        List<Long> ids = activeOccupancies.stream()
                .map(UniversalPlaceOccupant::getId)
                .collect(java.util.stream.Collectors.toList());
        occupantRepository.batchCheckOut(ids);

        // 更新每个场所的占用数
        activeOccupancies.stream()
                .map(UniversalPlaceOccupant::getPlaceId)
                .distinct()
                .forEach(placeId -> placeRepository.findById(placeId).ifPresent(place -> {
                    place.checkOut();
                    placeRepository.save(place);
                }));

        log.info("用户 {} 自动退房 {} 条记录", userId, ids.size());
    }

    // ==================== 查询操作 ====================

    /**
     * 根据ID获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 根据用户名获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据组织单元获取用户列表
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByOrgUnit(Long orgUnitId) {
        return userRepository.findByOrgUnitId(orgUnitId);
    }

    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsUsername(String username, Long excludeId) {
        if (excludeId != null) {
            return userRepository.existsByUsernameAndIdNot(username, excludeId);
        }
        return userRepository.existsByUsername(username);
    }

    /**
     * 分页查询用户（带条件）
     */
    @Transactional(readOnly = true)
    public List<User> getUsersPage(int page, int size, String username, String realName,
                                   String phone, Long orgUnitId, Integer status) {
        return userRepository.findPagedWithConditions(page, size, username, realName, phone, orgUnitId, status);
    }

    /**
     * 条件统计用户总数
     */
    @Transactional(readOnly = true)
    public long countUsers(String username, String realName, String phone, Long orgUnitId, Integer status) {
        return userRepository.countWithConditions(username, realName, phone, orgUnitId, status);
    }

    /**
     * 获取简单用户列表
     */
    @Transactional(readOnly = true)
    public List<User> getSimpleUserList(String keyword) {
        return userRepository.findSimpleList(keyword);
    }

    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllUsers();
    }

    // ==================== 角色分配 ====================

    /**
     * 分配角色
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        log.info("分配用户角色: userId={}, roleIds={}", userId, roleIds);

        User user = getUserOrThrow(userId);
        user.assignRoles(roleIds);
        userRepository.save(user);
    }

    /**
     * 获取用户角色ID列表
     */
    @Transactional(readOnly = true)
    public List<Long> getUserRoleIds(Long userId) {
        User user = getUserOrThrow(userId);
        return user.getRoleIds();
    }

    // ==================== 登录记录 ====================

    /**
     * 记录登录信息
     */
    @Transactional
    public void recordLogin(Long userId, String ip) {
        User user = getUserOrThrow(userId);
        user.recordLogin(ip);
        userRepository.save(user);
    }

    // ==================== 微信绑定 ====================

    /**
     * 绑定微信
     */
    @Transactional
    public void bindWechat(Long userId, String openid) {
        log.info("绑定微信: userId={}, openid={}", userId, openid);

        User user = getUserOrThrow(userId);
        user.bindWechat(openid);
        userRepository.save(user);
    }

    /**
     * 解绑微信
     */
    @Transactional
    public void unbindWechat(Long userId) {
        log.info("解绑微信: {}", userId);

        User user = getUserOrThrow(userId);
        user.unbindWechat();
        userRepository.save(user);
    }

    /**
     * 根据微信OpenID获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByWechatOpenid(String openid) {
        return userRepository.findByWechatOpenid(openid);
    }

    // ==================== Helper Methods ====================

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在: " + userId));
    }

    private void publishEvents(User user) {
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearDomainEvents();
    }
}
