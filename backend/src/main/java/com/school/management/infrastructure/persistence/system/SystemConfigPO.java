package com.school.management.infrastructure.persistence.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置持久化对象
 */
@Data
@TableName("system_configs")
public class SystemConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 值类型: string/number/boolean/json
     */
    private String configType;

    /**
     * 配置分组: system/business/ui
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
     * 是否系统内置: 0否 1是
     */
    private Integer isSystem;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
