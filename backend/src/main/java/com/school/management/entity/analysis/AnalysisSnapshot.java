package com.school.management.entity.analysis;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析结果快照实体
 */
@Data
@TableName(value = "stat_analysis_snapshots", autoResultMap = true)
public class AnalysisSnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分析配置ID
     */
    private Long configId;

    /**
     * 快照名称
     */
    private String snapshotName;

    /**
     * 快照描述
     */
    private String snapshotDesc;

    /**
     * 纳入统计的记录ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> recordIds;

    /**
     * 涉及的班级ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> classIds;

    /**
     * 数据起始日期
     */
    private LocalDate dateRangeStart;

    /**
     * 数据截止日期
     */
    private LocalDate dateRangeEnd;

    /**
     * 检查记录数
     */
    private Integer recordCount;

    /**
     * 涉及班级数
     */
    private Integer classCount;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 平均扣分
     */
    private BigDecimal avgScore;

    /**
     * 概览数据JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> overviewData;

    /**
     * 各指标计算结果JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metricsData;

    /**
     * 完整结果数据JSON (用于快照还原)
     */
    private String resultData;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 生成者ID
     */
    private Long generatedBy;

    /**
     * 生成者姓名
     */
    private String generatedByName;

    /**
     * 是否自动生成
     */
    private Boolean isAuto;

    /**
     * 版本号
     */
    private Integer version;
}
