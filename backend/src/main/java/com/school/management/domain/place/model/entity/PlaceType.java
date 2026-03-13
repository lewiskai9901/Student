package com.school.management.domain.space.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 场所类型领域实体
 * 定义场所的类型（简化版）
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceType implements Entity<Long> {

    private Long id;

    /**
     * 类型编码（唯一标识，自动生成）
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 父类型编码
     */
    private String parentTypeCode;

    /**
     * 层级顺序
     */
    private Integer levelOrder;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否系统预置
     */
    private boolean isSystem;

    /**
     * 是否启用
     */
    private boolean isEnabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 设置自动生成的类型编码
     */
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * 更新基本信息
     */
    public void update(String typeName, String description, String icon) {
        this.typeName = typeName;
        this.description = description;
        this.icon = icon;
    }

    /**
     * 更新排序号
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 启用
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * 是否顶级类型
     */
    public boolean isTopLevel() {
        return parentTypeCode == null || parentTypeCode.isEmpty();
    }
}
