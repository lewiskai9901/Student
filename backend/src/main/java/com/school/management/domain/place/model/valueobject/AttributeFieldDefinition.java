package com.school.management.domain.place.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 属性字段定义值对象
 * 描述场所类型的一个动态属性字段的元信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeFieldDefinition {

    /** 字段标识 */
    private String key;

    /** 显示名 */
    private String label;

    /** 字段类型: string | number | select | textarea | boolean | date */
    private String type;

    /** 是否必填 */
    @Builder.Default
    private boolean required = false;

    /** 占位提示 */
    private String placeholder;

    /** 默认值 */
    private Object defaultValue;

    // --- string / textarea ---
    private Integer maxLength;
    private String pattern;

    // --- number ---
    private Number min;
    private Number max;
    private Number step;
    private Integer precision;

    // --- textarea ---
    private Integer rows;

    // --- select ---
    @Builder.Default
    private boolean multiple = false;
    private List<SelectOption> options;

    // --- date ---
    private String format;

    // --- 显示控制 ---
    /** 是否在侧边栏树节点显示 */
    @Builder.Default
    private boolean showInTree = false;

    /** 是否在详情面板显示 */
    @Builder.Default
    private boolean showInDetail = true;

    /** 排序号 */
    @Builder.Default
    private int sortOrder = 0;

    /** 是否系统内置属性（内置属性 key/type 不可改、不可删除） */
    @Builder.Default
    private boolean builtIn = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectOption {
        private String value;
        private String label;
    }
}
