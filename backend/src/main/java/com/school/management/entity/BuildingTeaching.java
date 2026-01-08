package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学楼扩展实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("building_teachings")
public class BuildingTeaching {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 楼宇ID
     */
    private Long buildingId;

    /**
     * 教学类型: 1-综合教学楼, 2-实验楼, 3-专业楼
     */
    private Integer teachingType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 楼宇名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 教学类型名称 (非数据库字段)
     */
    @TableField(exist = false)
    private String teachingTypeName;

    /**
     * 获取教学类型名称
     */
    public String getTeachingTypeName() {
        if (teachingTypeName != null) {
            return teachingTypeName;
        }
        if (teachingType == null) {
            return "";
        }
        switch (teachingType) {
            case 1:
                return "综合教学楼";
            case 2:
                return "实验楼";
            case 3:
                return "专业楼";
            default:
                return "未知";
        }
    }
}
