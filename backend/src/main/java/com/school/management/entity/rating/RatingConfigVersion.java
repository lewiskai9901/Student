package com.school.management.entity.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评级配置版本
 *
 * @author System
 * @since 4.4.0
 */
@Data
@TableName("rating_config_version")
public class RatingConfigVersion {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 配置快照JSON
     */
    private String configSnapshot;

    /**
     * 变更说明
     */
    private String changeDescription;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
