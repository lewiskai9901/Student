package com.school.management.application.organization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.shared.TypeTreeBuilder;
import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.domain.organization.model.entity.OrgCategory;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import com.school.management.domain.place.repository.UniversalPlaceTypeRepository;
import com.school.management.domain.user.repository.UserTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织类型应用服务
 */
@RequiredArgsConstructor
@Service
public class OrgUnitTypeApplicationService {

    private static final Set<String> ALLOWED_SCHEMA_FIELD_TYPES = Set.of(
            "text", "number", "date", "datetime", "boolean", "select", "multiselect", "textarea", "radio");

    private final OrgUnitTypeRepository orgUnitTypeRepository;
    private final UserTypeRepository userTypeRepository;
    private final UniversalPlaceTypeRepository placeTypeRepository;

    @Transactional
    public OrgType createOrgUnitType(CreateOrgUnitTypeCommand command) {
        if (orgUnitTypeRepository.existsByTypeCode(command.getTypeCode())) {
            throw new IllegalArgumentException("类型编码已存在: " + command.getTypeCode());
        }

        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            orgUnitTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        validateMetadataSchema(command.getMetadataSchema());
        validateCrossReferences(command.getDefaultUserTypeCodes(), command.getDefaultPlaceTypeCodes());

        // 如果未提供 features，使用 category 的默认值
        Map<String, Boolean> features = command.getFeatures();
        if (features == null && command.getCategory() != null) {
            try {
                OrgCategory cat = OrgCategory.valueOf(command.getCategory());
                features = cat.getDefaultFeatures();
            } catch (IllegalArgumentException ignored) {}
        }

        OrgType orgType = OrgType.builder()
                .typeCode(command.getTypeCode())
                .typeName(command.getTypeName())
                .category(command.getCategory())
                .parentTypeCode(command.getParentTypeCode())
                .icon(command.getIcon())
                .description(command.getDescription())
                .features(features)
                .metadataSchema(command.getMetadataSchema())
                .allowedChildTypeCodes(command.getAllowedChildTypeCodes())
                .maxDepth(command.getMaxDepth())
                .defaultUserTypeCodes(command.getDefaultUserTypeCodes())
                .defaultPlaceTypeCodes(command.getDefaultPlaceTypeCodes())
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(command.getSortOrder())
                .build();

        OrgType saved = orgUnitTypeRepository.save(orgType);
        syncChildParentTypeCodes(saved.getTypeCode(), saved.getAllowedChildTypeCodes());
        return saved;
    }

    @Transactional
    public OrgType updateOrgUnitType(Long id, UpdateOrgUnitTypeCommand command) {
        OrgType orgType = orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        validateMetadataSchema(command.getMetadataSchema());
        validateCrossReferences(command.getDefaultUserTypeCodes(), command.getDefaultPlaceTypeCodes());

        orgType.update(
                command.getTypeName() != null ? command.getTypeName() : orgType.getTypeName(),
                command.getDescription() != null ? command.getDescription() : orgType.getDescription(),
                command.getIcon() != null ? command.getIcon() : orgType.getIcon()
        );

        if (command.getCategory() != null || command.getFeatures() != null) {
            orgType.updateCategoryAndFeatures(
                    command.getCategory() != null ? command.getCategory() : orgType.getCategory(),
                    command.getFeatures() != null ? command.getFeatures() : orgType.getFeatures()
            );
        }

        if (command.getMetadataSchema() != null) {
            orgType.updateMetadataSchema(command.getMetadataSchema());
        }

        orgType.updateHierarchyConfig(
                command.getAllowedChildTypeCodes() != null ? command.getAllowedChildTypeCodes() : orgType.getAllowedChildTypeCodes(),
                command.getMaxDepth() != null ? command.getMaxDepth() : orgType.getMaxDepth()
        );

        orgType.updateCrossReferences(
                command.getDefaultUserTypeCodes() != null ? command.getDefaultUserTypeCodes() : orgType.getDefaultUserTypeCodes(),
                command.getDefaultPlaceTypeCodes() != null ? command.getDefaultPlaceTypeCodes() : orgType.getDefaultPlaceTypeCodes()
        );

        if (command.getSortOrder() != null) {
            orgType.updateSortOrder(command.getSortOrder());
        }

        OrgType saved = orgUnitTypeRepository.save(orgType);

        // Sync parentTypeCode on children when allowedChildTypeCodes changes
        if (command.getAllowedChildTypeCodes() != null) {
            syncChildParentTypeCodes(saved.getTypeCode(), saved.getAllowedChildTypeCodes());
        }

        return saved;
    }

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
     * 校验跨类型引用（默认用户类型 / 默认场所类型必须真实存在）
     */
    private void validateCrossReferences(List<String> defaultUserTypeCodes, List<String> defaultPlaceTypeCodes) {
        if (defaultUserTypeCodes != null && !defaultUserTypeCodes.isEmpty()) {
            for (String code : defaultUserTypeCodes) {
                if (code == null || code.isBlank()) continue;
                if (!userTypeRepository.existsByTypeCode(code)) {
                    throw new IllegalArgumentException("引用的用户类型不存在: " + code);
                }
            }
        }
        if (defaultPlaceTypeCodes != null && !defaultPlaceTypeCodes.isEmpty()) {
            for (String code : defaultPlaceTypeCodes) {
                if (code == null || code.isBlank()) continue;
                if (!placeTypeRepository.existsByTypeCode(code)) {
                    throw new IllegalArgumentException("引用的场所类型不存在: " + code);
                }
            }
        }
    }

    /**
     * Sync parentTypeCode on child types based on allowedChildTypeCodes.
     * When a type declares allowedChildTypeCodes, those child types get their parentTypeCode set.
     */
    private void syncChildParentTypeCodes(String parentCode, List<String> childCodes) {
        if (childCodes == null || childCodes.isEmpty()) return;
        for (String childCode : childCodes) {
            orgUnitTypeRepository.findByTypeCode(childCode).ifPresent(child -> {
                String currentParent = child.getParentTypeCode();
                if (currentParent == null || currentParent.isEmpty() || !currentParent.equals(parentCode)) {
                    child.setParentTypeCode(parentCode);
                    orgUnitTypeRepository.save(child);
                }
            });
        }
    }

    @Transactional
    public void deleteOrgUnitType(Long id) {
        OrgType orgType = orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        if (orgUnitTypeRepository.isTypeInUse(orgType.getTypeCode())) {
            throw new IllegalArgumentException("该类型已被组织单元使用，无法删除");
        }

        List<OrgType> children = orgUnitTypeRepository.findByParentTypeCode(orgType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该类型下存在子类型，无法删除");
        }

        orgUnitTypeRepository.deleteById(id);
    }

    @Transactional
    public OrgType toggleStatus(Long id, boolean enabled) {
        OrgType orgType = orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        if (enabled) {
            orgType.enable();
        } else {
            orgType.disable();
        }

        return orgUnitTypeRepository.save(orgType);
    }

    public List<OrgType> getAllOrgUnitTypes() {
        return orgUnitTypeRepository.findAll();
    }

    public List<OrgType> getEnabledOrgUnitTypes() {
        return orgUnitTypeRepository.findAllEnabled();
    }

    public List<TypeTreeNode<OrgType>> getOrgUnitTypeTree() {
        return TypeTreeBuilder.buildTree(orgUnitTypeRepository.findAll());
    }

    /**
     * 获取可检查的组织类型（通过 features.inspectionTarget 查询）
     */
    public List<OrgType> getInspectableTypes() {
        return orgUnitTypeRepository.findByFeature("inspectionTarget");
    }

    /**
     * 获取所有 category 枚举值
     */
    public List<OrgCategoryDTO> getCategories() {
        List<OrgCategoryDTO> list = new ArrayList<>();
        for (OrgCategory cat : OrgCategory.values()) {
            list.add(new OrgCategoryDTO(cat.name(), cat.getLabel(), cat.getDefaultFeatures()));
        }
        return list;
    }

    public OrgType getOrgUnitTypeById(Long id) {
        return orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));
    }

    public OrgType getOrgUnitTypeByCode(String typeCode) {
        return orgUnitTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + typeCode));
    }

    // ==================== 命令对象 ====================

    public static class CreateOrgUnitTypeCommand {
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
        private List<String> defaultUserTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private Integer sortOrder = 0;

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getParentTypeCode() { return parentTypeCode; }
        public void setParentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Boolean> getFeatures() { return features; }
        public void setFeatures(Map<String, Boolean> features) { this.features = features; }
        public String getMetadataSchema() { return metadataSchema; }
        public void setMetadataSchema(String metadataSchema) { this.metadataSchema = metadataSchema; }
        public List<String> getAllowedChildTypeCodes() { return allowedChildTypeCodes; }
        public void setAllowedChildTypeCodes(List<String> allowedChildTypeCodes) { this.allowedChildTypeCodes = allowedChildTypeCodes; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public List<String> getDefaultUserTypeCodes() { return defaultUserTypeCodes; }
        public void setDefaultUserTypeCodes(List<String> defaultUserTypeCodes) { this.defaultUserTypeCodes = defaultUserTypeCodes; }
        public List<String> getDefaultPlaceTypeCodes() { return defaultPlaceTypeCodes; }
        public void setDefaultPlaceTypeCodes(List<String> defaultPlaceTypeCodes) { this.defaultPlaceTypeCodes = defaultPlaceTypeCodes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateOrgUnitTypeCommand {
        private String typeName;
        private String category;
        private String icon;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultPlaceTypeCodes;
        private Integer sortOrder;

        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Boolean> getFeatures() { return features; }
        public void setFeatures(Map<String, Boolean> features) { this.features = features; }
        public String getMetadataSchema() { return metadataSchema; }
        public void setMetadataSchema(String metadataSchema) { this.metadataSchema = metadataSchema; }
        public List<String> getAllowedChildTypeCodes() { return allowedChildTypeCodes; }
        public void setAllowedChildTypeCodes(List<String> allowedChildTypeCodes) { this.allowedChildTypeCodes = allowedChildTypeCodes; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public List<String> getDefaultUserTypeCodes() { return defaultUserTypeCodes; }
        public void setDefaultUserTypeCodes(List<String> defaultUserTypeCodes) { this.defaultUserTypeCodes = defaultUserTypeCodes; }
        public List<String> getDefaultPlaceTypeCodes() { return defaultPlaceTypeCodes; }
        public void setDefaultPlaceTypeCodes(List<String> defaultPlaceTypeCodes) { this.defaultPlaceTypeCodes = defaultPlaceTypeCodes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    // ==================== DTO ====================

    public static class OrgCategoryDTO {
        private String code;
        private String label;
        private Map<String, Boolean> defaultFeatures;

        public OrgCategoryDTO(String code, String label, Map<String, Boolean> defaultFeatures) {
            this.code = code;
            this.label = label;
            this.defaultFeatures = defaultFeatures;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
        public Map<String, Boolean> getDefaultFeatures() { return defaultFeatures; }
    }
}
