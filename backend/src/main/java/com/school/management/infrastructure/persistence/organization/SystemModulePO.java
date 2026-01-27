package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统模块持久化对象
 */
@Data
@TableName("system_modules")
public class SystemModulePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 模块描述
     */
    private String moduleDesc;

    /**
     * 父模块编码
     */
    private String parentCode;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否启用 (映射到数据库的status字段)
     */
    @TableField("status")
    private Integer status;

    /**
     * 获取是否启用
     */
    public Boolean getIsEnabled() {
        return status != null && status == 1;
    }

    /**
     * 设置是否启用
     */
    public void setIsEnabled(Boolean enabled) {
        this.status = (enabled != null && enabled) ? 1 : 0;
    }

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
