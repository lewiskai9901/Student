package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日常检查实体(增强版 V1.0.6)
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_checks")
public class DailyCheck extends BaseEntity {

    /**
     * 所属检查计划ID
     */
    private Long planId;

    /**
     * 检查日期
     */
    private LocalDate checkDate;

    /**
     * 检查名称
     */
    private String checkName;

    /**
     * 使用的模板ID
     */
    private Long templateId;

    /**
     * 检查类型 1日常检查 2专项检查
     */
    private Integer checkType;

    /**
     * 总轮次数
     */
    private Integer totalRounds;

    /**
     * 轮次名称数组 JSON格式 如["早检","午检","晚检"]
     */
    private String roundNames;

    /**
     * 状态 0未开始 1进行中 2已提交 3已发布
     */
    private Integer status;

    /**
     * 检查说明
     */
    private String description;

    /**
     * 排除的目标列表(JSON格式)
     * 存储被排除的班级ID、名称和排除原因
     */
    private String excludedTargets;

    /**
     * 检查人ID
     */
    private Long checkerId;

    /**
     * 检查人姓名
     */
    private String checkerName;

    /**
     * 创建人ID
     * 重写BaseEntity中的字段,在此表中该字段实际存在
     */
    @TableField(value = "created_by", exist = true)
    private Long createdBy;

    /**
     * 加权方案ID(可选,关联class_weight_configs表)
     */
    private Long weightConfigId;

    /**
     * 是否启用加权(0=否,1=是)
     * 用于控制本次检查是否应用加权方案
     */
    private Integer enableWeight;

    /**
     * 自定义标准人数(可选)
     * 当设置此值时,将覆盖加权方案中配置的标准人数,仅对本次检查生效
     */
    private Integer customStandardSize;

    /**
     * 加权配置快照(JSON格式)
     * 当选择加权方案时,保存方案的完整快照,确保后续配置修改不影响本次检查
     * 快照内容包括: weightMode, standardSizeMode, standardSize, minWeight, maxWeight等所有配置
     */
    private String weightConfigSnapshot;

    /**
     * 乐观锁版本号
     * 用于防止并发修改冲突，每次更新时自动递增
     */
    @Version
    private Integer version;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 加权方案名称(关联查询)
     */
    @TableField(exist = false)
    private String weightConfigName;

    /**
     * 加权模式描述(关联查询)
     */
    @TableField(exist = false)
    private String weightModeDesc;
}
