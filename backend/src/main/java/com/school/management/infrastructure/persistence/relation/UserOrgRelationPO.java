package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户-组织关系持久化对象
 */
@Data
@TableName("user_org_relations")
public class UserOrgRelationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 关系类型
     */
    private String relationType;

    /**
     * 职务名称
     */
    private String positionTitle;

    /**
     * 职务级别
     */
    private Integer positionLevel;

    /**
     * 是否主归属
     */
    private Boolean isPrimary;

    /**
     * 是否领导
     */
    private Boolean isLeader;

    /**
     * 是否有管理权限
     */
    private Boolean canManage;

    /**
     * 是否有审批权限
     */
    private Boolean canApprove;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 权重比例
     */
    private BigDecimal weightRatio;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private Long createdBy;

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
