package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统配置实体类
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_configs")
public class SystemConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置键(唯一)
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 值类型(string/number/boolean/json)
     */
    private String configType;

    /**
     * 配置分组(system/business/ui等)
     */
    private String configGroup;

    /**
     * 配置标签(中文名称)
     */
    private String configLabel;

    /**
     * 配置描述
     */
    private String configDesc;

    /**
     * 是否系统内置(0-否,1-是,系统内置不可删除)
     */
    private Integer isSystem;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态(1-启用,0-禁用)
     */
    private Integer status;
}
