package com.school.management.application.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.access.AccessApplicationService;
import com.school.management.application.shared.TypeTreeBuilder;
import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.entity.UserCategory;
import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.domain.user.repository.UserTypeRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户类型应用服务（统一类型系统 Phase 2）
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserTypeApplicationService {

    private static final Set<String> ALLOWED_SCHEMA_FIELD_TYPES = Set.of(
            "text", "number", "date", "datetime", "boolean", "select", "multiselect", "textarea", "radio");

    private final UserTypeRepository userTypeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AccessApplicationService accessApplicationService;

    // ==================== 查询 ====================

    public List<UserType> getAllUserTypes() {
        return userTypeRepository.findAll();
    }

    public List<UserType> getEnabledUserTypes() {
        return userTypeRepository.findAllEnabled();
    }

    public List<TypeTreeNode<UserType>> getUserTypeTree() {
        return TypeTreeBuilder.buildTree(userTypeRepository.findAll());
    }

    public UserType getUserTypeById(Long id) {
        return userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));
    }

    public UserType getUserTypeByCode(String typeCode) {
        return userTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + typeCode));
    }

    public List<UserCategoryDTO> getCategories() {
        return Arrays.stream(UserCategory.values())
                .map(c -> new UserCategoryDTO(c.name(), c.getLabel(), c.getDefaultFeatures()))
                .toList();
    }

    // ==================== 写操作 ====================

    @Transactional
    public UserType createUserType(CreateUserTypeCommand command) {
        // If typeCode is provided and non-blank, use it. Otherwise generate.
        String typeCode = command.getTypeCode();
        if (typeCode == null || typeCode.isBlank()) {
            typeCode = generateTypeCode();
        } else if (userTypeRepository.existsByTypeCode(typeCode)) {
            throw new IllegalArgumentException("类型编码已存在: " + typeCode);
        }

        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            userTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        validateMetadataSchema(command.getMetadataSchema());
        validateSelfReference(typeCode, command.getAllowedChildTypeCodes());

        // 如果未提供 features，从 category 默认值填充
        Map<String, Boolean> features = command.getFeatures();
        if ((features == null || features.isEmpty()) && command.getCategory() != null) {
            try {
                UserCategory cat = UserCategory.valueOf(command.getCategory());
                features = new HashMap<>(cat.getDefaultFeatures());
            } catch (IllegalArgumentException ignored) {}
        }

        UserType userType = UserType.builder()
                .typeCode(typeCode)
                .typeName(command.getTypeName())
                .category(command.getCategory())
                .parentTypeCode(command.getParentTypeCode())
                .icon(command.getIcon())
                .description(command.getDescription())
                .features(features)
                .metadataSchema(command.getMetadataSchema())
                .allowedChildTypeCodes(command.getAllowedChildTypeCodes())
                .maxDepth(command.getMaxDepth())
                .defaultRoleCodes(command.getDefaultRoleCodes())
                .defaultOrgTypeCodes(command.getDefaultOrgTypeCodes())
                .defaultPlaceTypeCodes(command.getDefaultPlaceTypeCodes())
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(command.getSortOrder())
                .build();

        return userTypeRepository.save(userType);
    }

    @Transactional
    public UserType updateUserType(Long id, UpdateUserTypeCommand command) {
        UserType userType = userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));

        validateMetadataSchema(command.getMetadataSchema());
        validateSelfReference(userType.getTypeCode(), command.getAllowedChildTypeCodes());

        // 用 builder 重建（因为 category/features 等字段需要整体替换）
        UserType updated = UserType.builder()
                .id(userType.getId())
                .typeCode(userType.getTypeCode())
                .typeName(command.getTypeName() != null ? command.getTypeName() : userType.getTypeName())
                .category(command.getCategory() != null ? command.getCategory() : userType.getCategory())
                .parentTypeCode(userType.getParentTypeCode())
                .icon(command.getIcon() != null ? command.getIcon() : userType.getIcon())
                .description(command.getDescription() != null ? command.getDescription() : userType.getDescription())
                .features(command.getFeatures() != null ? command.getFeatures() : userType.getFeatures())
                .metadataSchema(command.getMetadataSchema() != null ? command.getMetadataSchema() : userType.getMetadataSchema())
                .allowedChildTypeCodes(command.getAllowedChildTypeCodes() != null ? command.getAllowedChildTypeCodes() : userType.getAllowedChildTypeCodes())
                .maxDepth(command.getMaxDepth() != null ? command.getMaxDepth() : userType.getMaxDepth())
                .defaultRoleCodes(command.getDefaultRoleCodes() != null ? command.getDefaultRoleCodes() : userType.getDefaultRoleCodes())
                .defaultOrgTypeCodes(command.getDefaultOrgTypeCodes() != null ? command.getDefaultOrgTypeCodes() : userType.getDefaultOrgTypeCodes())
                .defaultPlaceTypeCodes(command.getDefaultPlaceTypeCodes() != null ? command.getDefaultPlaceTypeCodes() : userType.getDefaultPlaceTypeCodes())
                .isSystem(userType.isSystem())
                .isEnabled(userType.isEnabled())
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : userType.getSortOrder())
                .build();

        return userTypeRepository.save(updated);
    }

    @Transactional
    public void deleteUserType(Long id) {
        UserType userType = userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));

        if (userType.isSystem()) {
            throw new IllegalArgumentException("系统预置类型不能删除");
        }
        if (userTypeRepository.isTypeInUse(userType.getTypeCode())) {
            throw new IllegalArgumentException("该类型已被用户使用，无法删除");
        }
        List<UserType> children = userTypeRepository.findByParentTypeCode(userType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该类型下存在子类型，无法删除");
        }

        userTypeRepository.deleteById(id);
    }

    @Transactional
    public UserType toggleStatus(Long id, boolean enabled) {
        UserType userType = userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));
        if (enabled) { userType.enable(); } else { userType.disable(); }
        return userTypeRepository.save(userType);
    }

    /**
     * Phase 2.6 — UserType.defaultRoleCodes 变更后的回溯同步。
     *
     * 对已有该类型的所有用户：
     *   - 若用户目前没有任何角色 → 按 defaultRoleCodes 为其分配
     *   - 若用户已有任意角色 → 跳过（避免覆盖人工配置，保持幂等）
     *
     * 返回统计：扫描用户数 / 新补齐用户数 / 跳过用户数 / 失败用户数。
     *
     * 仅补齐"零角色"用户，不做"diff 补齐"（例如用户已有一个管理员角色，
     * 即使 defaultRoleCodes 里还有其它角色也不会再加）—— 这是一个保守选择，
     * 避免把管理员显式移除过的默认角色又加回来。
     */
    @Transactional
    public SyncDefaultRolesResult syncDefaultRolesToUsers(String typeCode) {
        UserType type = userTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + typeCode));

        List<String> defaultCodes = type.getDefaultRoleCodeList();
        List<User> users = userRepository.findByUserTypeCode(typeCode);

        int scanned = users.size();
        int filled = 0;
        int skipped = 0;
        int failed = 0;

        if (defaultCodes.isEmpty()) {
            return new SyncDefaultRolesResult(scanned, 0, scanned, 0);
        }

        for (User user : users) {
            Long userId = user.getId();
            if (userId == null) {
                continue;
            }
            if (!userRoleRepository.findByUserId(userId).isEmpty()) {
                skipped++;
                continue;
            }
            boolean anyAssigned = false;
            for (String roleCode : defaultCodes) {
                if (roleCode == null || roleCode.isBlank()) continue;
                try {
                    var roleOpt = roleRepository.findByRoleCode(roleCode.trim());
                    if (roleOpt.isEmpty()) {
                        log.warn("[UserType同步] 角色编码不存在: {}", roleCode);
                        continue;
                    }
                    accessApplicationService.assignRoleToUser(userId, roleOpt.get().getId(), null);
                    anyAssigned = true;
                } catch (IllegalArgumentException e) {
                    log.debug("[UserType同步] 用户 {} 角色 {} 分配跳过: {}", userId, roleCode, e.getMessage());
                } catch (RuntimeException e) {
                    log.warn("[UserType同步] 用户 {} 角色 {} 分配失败: {}", userId, roleCode, e.getMessage());
                    failed++;
                }
            }
            if (anyAssigned) {
                filled++;
            } else {
                skipped++;
            }
        }
        return new SyncDefaultRolesResult(scanned, filled, skipped, failed);
    }

    // ==================== 内部方法 ====================

    /**
     * 校验 metadataSchema 结构
     */
    private void validateMetadataSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return;
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode node = om.readTree(schemaJson);

            if (!node.has("fields")) {
                throw new IllegalArgumentException("metadataSchema 必须包含 fields 数组");
            }
            JsonNode fields = node.get("fields");
            if (!fields.isArray()) {
                throw new IllegalArgumentException("fields 必须是数组");
            }

            for (JsonNode field : fields) {
                if (!field.has("key") || !field.has("label") || !field.has("type")) {
                    throw new IllegalArgumentException("每个字段必须有 key、label、type");
                }
                String key = field.get("key").asText();
                if (!key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    throw new IllegalArgumentException("字段 key 只能包含字母、数字、下划线，且不能以数字开头: " + key);
                }
                String type = field.get("type").asText();
                if (!ALLOWED_SCHEMA_FIELD_TYPES.contains(type)) {
                    throw new IllegalArgumentException("不支持的字段类型: " + type);
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("metadataSchema JSON 格式错误: " + e.getMessage());
        }
    }

    /**
     * 校验 allowedChildTypeCodes 不能包含自身 typeCode
     */
    private void validateSelfReference(String typeCode, List<String> allowedChildTypeCodes) {
        if (allowedChildTypeCodes != null && typeCode != null && allowedChildTypeCodes.contains(typeCode)) {
            throw new IllegalArgumentException("allowedChildTypeCodes 不能包含自身 typeCode: " + typeCode);
        }
    }

    private String generateTypeCode() {
        String code;
        do {
            code = "UT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userTypeRepository.existsByTypeCode(code));
        return code;
    }

    // ==================== DTO ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCategoryDTO {
        private String code;
        private String label;
        private Map<String, Boolean> defaultFeatures;
    }

    @Data
    @AllArgsConstructor
    public static class SyncDefaultRolesResult {
        /** 该类型下的用户总数 */
        private int scannedUsers;
        /** 新补齐默认角色的用户数 */
        private int filledUsers;
        /** 已有角色或无需处理而跳过的用户数 */
        private int skippedUsers;
        /** 角色分配过程中发生异常的用户数 */
        private int failedUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserTypeCommand {
        private String typeCode;
        private String typeName;
        private String category;
        private String parentTypeCode;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultRoleCodes;
        private List<String> defaultOrgTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private Integer sortOrder = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserTypeCommand {
        private String typeName;
        private String category;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultRoleCodes;
        private List<String> defaultOrgTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private Integer sortOrder;
    }

}
