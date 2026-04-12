package com.school.management.application.place;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.shared.TypeTreeBuilder;
import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.domain.place.model.entity.UniversalPlaceType;
import com.school.management.domain.place.model.valueobject.BaseCategory;
import com.school.management.domain.place.repository.UniversalPlaceTypeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用场所类型应用服务（统一类型系统 Phase 3）
 */
@Service
@RequiredArgsConstructor
public class UniversalPlaceTypeApplicationService {

    private static final Set<String> ALLOWED_SCHEMA_FIELD_TYPES = Set.of(
            "text", "number", "date", "datetime", "boolean", "select", "multiselect", "textarea", "radio");

    private final UniversalPlaceTypeRepository placeTypeRepository;

    // ==================== Category 枚举查询 ====================

    /**
     * 获取所有内置分类（BaseCategory 枚举值 + defaultFeatures）
     */
    public List<PlaceCategoryDTO> getCategories() {
        List<PlaceCategoryDTO> list = new ArrayList<>();
        for (BaseCategory cat : BaseCategory.values()) {
            list.add(new PlaceCategoryDTO(
                    cat.name(),
                    cat.getLabel(),
                    cat.getDefaultFeatures(),
                    cat.getAllowedChildCategories(),
                    cat.isLeaf(),
                    cat.isRoot()
            ));
        }
        return list;
    }

    // ==================== 查询 ====================

    public List<UniversalPlaceType> getAllPlaceTypes() {
        return placeTypeRepository.findAll();
    }

    public List<UniversalPlaceType> getEnabledPlaceTypes() {
        return placeTypeRepository.findAllEnabled();
    }

    public List<UniversalPlaceType> getRootTypes() {
        return placeTypeRepository.findConcreteRootTypes();
    }

    public UniversalPlaceType getPlaceTypeById(Long id) {
        return placeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + id));
    }

    public UniversalPlaceType getPlaceTypeByCode(String typeCode) {
        return placeTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + typeCode));
    }

    /**
     * 获取类型树（基于 parentTypeCode 构建）
     */
    public List<TypeTreeNode<UniversalPlaceType>> getPlaceTypeTree() {
        return TypeTreeBuilder.buildTree(placeTypeRepository.findAll());
    }

    // ==================== 写操作 ====================

    @Transactional
    public UniversalPlaceType createPlaceType(CreatePlaceTypeCommand command) {
        if (command.getTypeCode() == null || command.getTypeCode().isBlank()) {
            command.setTypeCode(generateTypeCode());
        }

        if (placeTypeRepository.existsByTypeCode(command.getTypeCode())) {
            throw new IllegalArgumentException("类型编码已存在: " + command.getTypeCode());
        }

        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            placeTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        validateMetadataSchema(command.getMetadataSchema());
        validateSelfReference(command.getTypeCode(), command.getAllowedChildTypeCodes());

        // 如果未提供 features，使用 category 的默认值
        Map<String, Boolean> features = command.getFeatures();
        if (features == null && command.getCategory() != null) {
            try {
                BaseCategory cat = BaseCategory.valueOf(command.getCategory());
                features = cat.getDefaultFeatures();
            } catch (IllegalArgumentException ignored) {}
        }

        boolean isRootType = command.getParentTypeCode() == null || command.getParentTypeCode().isEmpty();

        UniversalPlaceType placeType = UniversalPlaceType.builder()
                .typeCode(command.getTypeCode())
                .typeName(command.getTypeName())
                .description(command.getDescription())
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .isSystem(false)
                .isEnabled(true)
                .isRootType(isRootType)
                .category(command.getCategory())
                .parentTypeCode(command.getParentTypeCode())
                .features(features)
                .metadataSchema(command.getMetadataSchema())
                .allowedChildTypeCodes(command.getAllowedChildTypeCodes())
                .maxDepth(command.getMaxDepth())
                .defaultUserTypeCodes(command.getDefaultUserTypeCodes())
                .defaultOrgTypeCodes(command.getDefaultOrgTypeCodes())
                .capacityUnit(command.getCapacityUnit())
                .defaultCapacity(command.getDefaultCapacity())
                .build();

        placeType.validate();
        return placeTypeRepository.save(placeType);
    }

    @Transactional
    public UniversalPlaceType updatePlaceType(Long id, UpdatePlaceTypeCommand command) {
        UniversalPlaceType placeType = getPlaceTypeById(id);

        if (placeType.isSystem()) {
            throw new IllegalStateException("系统预置类型不可修改");
        }

        validateMetadataSchema(command.getMetadataSchema());
        validateSelfReference(placeType.getTypeCode(), command.getAllowedChildTypeCodes());

        if (command.getTypeName() != null) {
            placeType.setTypeName(command.getTypeName());
        }
        if (command.getDescription() != null) {
            placeType.setDescription(command.getDescription());
        }
        if (command.getSortOrder() != null) {
            placeType.setSortOrder(command.getSortOrder());
        }
        if (command.getCategory() != null) {
            placeType.setCategory(command.getCategory());
        }
        if (command.getFeatures() != null) {
            placeType.setFeatures(command.getFeatures());
        }
        if (command.getMetadataSchema() != null) {
            placeType.setMetadataSchema(command.getMetadataSchema());
        }
        if (command.getAllowedChildTypeCodes() != null) {
            placeType.setAllowedChildTypeCodes(command.getAllowedChildTypeCodes());
        }
        if (command.getMaxDepth() != null) {
            placeType.setMaxDepth(command.getMaxDepth());
        }
        if (command.getDefaultUserTypeCodes() != null) {
            placeType.setDefaultUserTypeCodes(command.getDefaultUserTypeCodes());
        }
        if (command.getDefaultOrgTypeCodes() != null) {
            placeType.setDefaultOrgTypeCodes(command.getDefaultOrgTypeCodes());
        }
        if (command.getCapacityUnit() != null) {
            placeType.setCapacityUnit(command.getCapacityUnit());
        }
        if (command.getDefaultCapacity() != null) {
            placeType.setDefaultCapacity(command.getDefaultCapacity());
        }

        placeType.validate();
        return placeTypeRepository.save(placeType);
    }

    @Transactional
    public void deletePlaceType(Long id) {
        UniversalPlaceType placeType = getPlaceTypeById(id);

        if (placeType.isSystem()) {
            throw new IllegalStateException("系统预置类型不可删除");
        }

        if (placeTypeRepository.isTypeInUse(placeType.getTypeCode())) {
            throw new IllegalStateException("类型正在使用中，不可删除");
        }

        List<UniversalPlaceType> children = placeTypeRepository.findByParentTypeCode(placeType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalStateException("类型有子类型，请先删除子类型");
        }

        placeTypeRepository.deleteById(id);
    }

    @Transactional
    public UniversalPlaceType toggleStatus(Long id, boolean enabled) {
        UniversalPlaceType placeType = getPlaceTypeById(id);

        if (!enabled && placeTypeRepository.isTypeInUse(placeType.getTypeCode())) {
            throw new IllegalStateException("类型正在使用中，不可禁用");
        }

        if (enabled) {
            placeType.enable();
        } else {
            placeType.disable();
        }

        return placeTypeRepository.save(placeType);
    }

    private String generateTypeCode() {
        return "PLACE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
     * 校验 allowedChildTypeCodes 不能包含自身 typeCode
     */
    private void validateSelfReference(String typeCode, List<String> allowedChildTypeCodes) {
        if (allowedChildTypeCodes != null && typeCode != null && allowedChildTypeCodes.contains(typeCode)) {
            throw new IllegalArgumentException("allowedChildTypeCodes 不能包含自身 typeCode: " + typeCode);
        }
    }

    // ==================== 命令对象 ====================

    @Data
    public static class CreatePlaceTypeCommand {
        private String typeCode;
        private String typeName;
        private String category;
        private String parentTypeCode;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultOrgTypeCodes;
        private String capacityUnit;
        private Integer defaultCapacity;
        private Integer sortOrder = 0;
    }

    @Data
    public static class UpdatePlaceTypeCommand {
        private String typeName;
        private String category;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultOrgTypeCodes;
        private String capacityUnit;
        private Integer defaultCapacity;
        private Integer sortOrder;
    }

    // ==================== DTO ====================

    public static class PlaceCategoryDTO {
        private String code;
        private String label;
        private Map<String, Boolean> defaultFeatures;
        private List<String> allowedChildCategories;
        private boolean leaf;
        private boolean root;

        public PlaceCategoryDTO(String code, String label, Map<String, Boolean> defaultFeatures,
                                List<String> allowedChildCategories, boolean leaf, boolean root) {
            this.code = code;
            this.label = label;
            this.defaultFeatures = defaultFeatures;
            this.allowedChildCategories = allowedChildCategories;
            this.leaf = leaf;
            this.root = root;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
        public Map<String, Boolean> getDefaultFeatures() { return defaultFeatures; }
        public List<String> getAllowedChildCategories() { return allowedChildCategories; }
        public boolean isLeaf() { return leaf; }
        public boolean isRoot() { return root; }
    }
}
