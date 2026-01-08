package com.school.management.entity.record;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 班级检查统计表实体（重构版）
 * 用于存储班级级别的聚合快照数据
 *
 * @author system
 * @since 2.0.0
 */
@Data
@TableName("check_record_class_stats_new")
@Alias("RecordCheckRecordClassStatsNew")
public class CheckRecordClassStatsNew {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查记录ID
     */
    private Long recordId;

    // ==================== 班级信息快照 ====================

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称（快照）
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称（快照）
     */
    private String gradeName;

    /**
     * 院系ID
     */
    private Long departmentId;

    /**
     * 院系名称（快照）
     */
    private String departmentName;

    // ==================== 班主任信息快照 ====================

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任姓名（快照）
     */
    private String teacherName;

    /**
     * 班主任电话（快照）
     */
    private String teacherPhone;

    // ==================== 班级规模快照 ====================

    /**
     * 班级人数（快照）
     */
    private Integer classSize;

    // ==================== 扣分统计 ====================

    /**
     * 扣分项数量
     */
    private Integer deductionCount;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    // ==================== 分类扣分统计 ====================

    /**
     * 卫生类扣分
     */
    private BigDecimal hygieneScore;

    /**
     * 纪律类扣分
     */
    private BigDecimal disciplineScore;

    /**
     * 考勤类扣分
     */
    private BigDecimal attendanceScore;

    /**
     * 宿舍类扣分
     */
    private BigDecimal dormitoryScore;

    /**
     * 其他类扣分
     */
    private BigDecimal otherScore;

    // ==================== 排名 ====================

    /**
     * 全校排名
     */
    private Integer overallRanking;

    /**
     * 年级排名
     */
    private Integer gradeRanking;

    /**
     * 院系排名
     */
    private Integer departmentRanking;

    // ==================== 评级 ====================

    /**
     * 评分等级：优秀/良好/一般/较差
     */
    private String scoreLevel;

    // ==================== 申诉统计 ====================

    /**
     * 申诉数量
     */
    private Integer appealCount;

    /**
     * 通过的申诉
     */
    private Integer appealApproved;

    /**
     * 待处理申诉
     */
    private Integer appealPending;

    // ==================== 对比分析 ====================

    /**
     * 与平均分差值
     */
    private BigDecimal vsAvgDiff;

    /**
     * 与上次检查差值
     */
    private BigDecimal vsLastDiff;

    /**
     * 趋势：UP/DOWN/STABLE
     */
    private String trend;

    // ==================== 时间戳 ====================

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 评级常量 ====================

    public static final String LEVEL_EXCELLENT = "优秀";
    public static final String LEVEL_GOOD = "良好";
    public static final String LEVEL_AVERAGE = "一般";
    public static final String LEVEL_POOR = "较差";

    public static final String TREND_UP = "UP";
    public static final String TREND_DOWN = "DOWN";
    public static final String TREND_STABLE = "STABLE";
}
