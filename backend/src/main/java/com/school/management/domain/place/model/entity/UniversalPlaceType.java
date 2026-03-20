package com.school.management.domain.place.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.school.management.domain.place.model.valueobject.BaseCategory;
import com.school.management.domain.shared.ConfigurableType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用场所类型实体（统一类型系统 Phase 3）
 * 支持完全配置化的场所类型定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalPlaceType implements ConfigurableType {

    private Long id;

    // ==================== 基础信息 ====================

    private String typeCode;
    private String typeName;
    private String description;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private boolean isSystem = false;

    @Builder.Default
    private boolean isEnabled = true;

    // ==================== 统一类型字段 ====================

    /**
     * 分类编码 (SITE/BUILDING/FLOOR/ROOM/AREA/POINT)
     * 基础分类自身此字段为null，具体类型引用基础分类
     */
    private String category;

    /**
     * 父类型编码 (string-based hierarchy, consistent with OrgType/UserType)
     */
    private String parentTypeCode;

    /**
     * 行为特征 JSON {hasCapacity, bookable, assignable, occupiable, ...}
     */
    private Map<String, Boolean> features;

    /**
     * 扩展属性 JSON Schema (动态字段定义)
     */
    private String metadataSchema;

    /**
     * 允许的子类型编码列表
     */
    @Builder.Default
    private List<String> allowedChildTypeCodes = new ArrayList<>();

    /**
     * 最大层级深度限制
     */
    private Integer maxDepth;

    /**
     * 关联用户类型编码列表
     */
    private List<String> defaultUserTypeCodes;

    /**
     * 关联组织类型编码列表
     */
    private List<String> defaultOrgTypeCodes;

    // ==================== 场所特有字段 ====================

    /**
     * 是否可作为根节点
     */
    @Builder.Default
    private boolean isRootType = false;

    /**
     * 容量单位（人/床位/工位/平方米）
     */
    private String capacityUnit;

    /**
     * 默认容量
     */
    private Integer defaultCapacity;

    // ==================== 业务方法 ====================

    public boolean isBaseCategory() {
        return category == null && isSystem;
    }

    public boolean isConcreteType() {
        return category != null;
    }

    public boolean isLeafType() {
        if (category != null) {
            try {
                BaseCategory bc = BaseCategory.valueOf(category);
                return bc.isLeaf();
            } catch (IllegalArgumentException e) {
                return true;
            }
        }
        return allowedChildTypeCodes == null || allowedChildTypeCodes.isEmpty();
    }

    public boolean isHasCapacity() {
        return hasFeature("hasCapacity");
    }

    public boolean isBookable() {
        return hasFeature("bookable");
    }

    public boolean isAssignable() {
        return hasFeature("assignable");
    }

    public boolean isOccupiable() {
        return hasFeature("occupiable");
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void validate() {
        if (typeCode == null || typeCode.isBlank()) {
            throw new IllegalArgumentException("类型编码不能为空");
        }
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("类型名称不能为空");
        }
        if (isHasCapacity() && (capacityUnit == null || capacityUnit.isBlank())) {
            throw new IllegalArgumentException("有容量的类型必须指定容量单位");
        }
        if (isOccupiable() && !isHasCapacity()) {
            throw new IllegalArgumentException("可占用的类型必须有容量概念");
        }
    }

    // ==================== JSON property aliases ====================

    @JsonProperty("system")
    public boolean isSystem() {
        return isSystem;
    }

    @JsonProperty("enabled")
    public boolean isEnabled() {
        return isEnabled;
    }

    @JsonProperty("rootType")
    public boolean isRootType() {
        return isRootType;
    }
}
