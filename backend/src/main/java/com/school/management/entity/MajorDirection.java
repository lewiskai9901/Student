package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业方向实体类
 * 同一专业的不同培养层次
 */
@Data
@TableName("major_directions")
public class MajorDirection {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 方向名称(如:3+2高级工班)
     */
    private String directionName;

    /**
     * 方向编码(如:CN_3P2_ADV)
     */
    private String directionCode;

    /**
     * 层次(中级工/高级工/预备技师/技师)
     */
    @TableField("level")
    private String level;

    /**
     * 学制(年)
     */
    private Integer years;

    /**
     * 是否分段注册(0=否,1=是)
     */
    private Integer isSegmented;

    /**
     * 第一阶段层次
     */
    private String phase1Level;

    /**
     * 第一阶段学制(年)
     */
    private Integer phase1Years;

    /**
     * 第二阶段层次
     */
    private String phase2Level;

    /**
     * 第二阶段学制(年)
     */
    private Integer phase2Years;

    /**
     * 备注
     */
    private String remarks;

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
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 删除标志(0=未删除,1=已删除)
     */
    @TableLogic
    private Integer deleted;

    /**
     * 专业名称(非数据库字段)
     */
    @TableField(exist = false)
    private String majorName;

    /**
     * 获取显示用的层次描述
     */
    public String getLevelDisplay() {
        if (isSegmented != null && isSegmented == 1) {
            return phase1Level + " → " + phase2Level;
        }
        return level;
    }

    /**
     * 获取显示用的学制描述
     */
    public String getYearsDisplay() {
        if (isSegmented != null && isSegmented == 1) {
            return phase1Years + "+" + phase2Years + "年";
        }
        return years + "年";
    }
}
