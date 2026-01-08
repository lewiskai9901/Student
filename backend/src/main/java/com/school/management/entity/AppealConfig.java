package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 申诉配置实体类
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appeal_configs")
public class AppealConfig extends BaseEntity {

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置编码
     */
    private String configCode;

    /**
     * 申诉期限(天)
     */
    private Integer appealDeadlineDays;

    /**
     * 期限模式(FIXED=固定天数,UNTIL_DATE=截止日期)
     */
    private String appealDeadlineMode;

    /**
     * 每次检查最多申诉数(NULL=不限制)
     */
    private Integer maxAppealsPerCheck;

    /**
     * 每个班级最多申诉数(NULL=不限制)
     */
    private Integer maxAppealsPerClass;

    /**
     * 是否允许撤销(1=允许,0=不允许)
     */
    private Integer allowWithdraw;

    /**
     * 撤销期限(小时)
     */
    private Integer withdrawDeadlineHours;

    /**
     * 是否启用公示期
     */
    private Integer enablePublicity;

    /**
     * 公示天数
     */
    private Integer publicityDays;

    /**
     * 公示查看模式(FULL=完全公开,LIMITED=限制查看,NONE=不公开)
     */
    private String publicityViewMode;

    /**
     * 是否必须提供证据
     */
    private Integer requireEvidence;

    /**
     * 最少证据数量
     */
    private Integer minEvidenceCount;

    /**
     * 最多证据数量
     */
    private Integer maxEvidenceCount;

    /**
     * 申诉通过后自动重算排名
     */
    private Integer autoRecalculate;

    /**
     * 是否默认配置
     */
    private Integer isDefault;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 配置说明
     */
    private String description;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 审批流配置列表
     */
    @TableField(exist = false)
    private java.util.List<AppealApprovalConfig> approvalConfigs;
}
