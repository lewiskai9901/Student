package com.school.management.infrastructure.persistence.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量化类型字典持久化对象
 * 用于存储独立的检查类别定义（不依赖模板）
 */
@Data
@TableName("quantification_dict_categories")
public class QuantificationDictCategoryPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 类别编码 */
    private String categoryCode;

    /** 类别名称 */
    private String categoryName;

    /** 描述 */
    private String description;

    /** 状态: 1=启用, 0=禁用 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
