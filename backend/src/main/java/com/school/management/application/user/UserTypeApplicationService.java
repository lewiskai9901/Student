package com.school.management.application.user;

import com.school.management.application.shared.TypeTreeBuilder;
import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.domain.user.model.entity.UserCategory;
import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserTypeRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户类型应用服务（统一类型系统 Phase 2）
 */
@RequiredArgsConstructor
@Service
public class UserTypeApplicationService {

    private final UserTypeRepository userTypeRepository;

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

    // ==================== 内部方法 ====================

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
