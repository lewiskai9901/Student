package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 班级检查统计响应DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordClassStatsDTO {

    private Long id;

    /**
     * 检查记录ID
     */
    private Long recordId;

    // ==================== 班级信息 ====================

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

    // ==================== 班主任信息 ====================

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任姓名
     */
    private String teacherName;

    /**
     * 班主任电话
     */
    private String teacherPhone;

    // ==================== 班级规模 ====================

    /**
     * 班级人数
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
     * 组织单元排名
     */
    private Integer orgUnitRanking;

    /**
     * 评分等级
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

    // ==================== 加权信息 ====================

    /**
     * 是否启用加权
     */
    private Boolean weightEnabled;

    /**
     * 加权系数
     */
    private BigDecimal weightFactor;

    /**
     * 标准人数
     */
    private Integer standardSize;

    /**
     * 加权模式: STANDARD(标准人数/班级人数) 或 PER_CAPITA(班级人数/标准人数)
     */
    private String weightMode;

    /**
     * 加权模式描述
     */
    private String weightModeDesc;

    /**
     * 加权后总扣分
     */
    private BigDecimal weightedTotalScore;

    /**
     * 卫生类加权扣分
     */
    private BigDecimal weightedHygieneScore;

    /**
     * 纪律类加权扣分
     */
    private BigDecimal weightedDisciplineScore;

    /**
     * 考勤类加权扣分
     */
    private BigDecimal weightedAttendanceScore;

    /**
     * 宿舍类加权扣分
     */
    private BigDecimal weightedDormitoryScore;

    /**
     * 其他类加权扣分
     */
    private BigDecimal weightedOtherScore;

    /**
     * 加权计算详情
     */
    private String weightCalculationDetails;

    /**
     * 是否启用多配置加权
     */
    private Boolean multiConfigEnabled;

    /**
     * 多配置加权详情列表
     */
    private List<MultiWeightConfigInfo> multiWeightConfigs;

    /**
     * 多配置加权信息内部类
     */
    @Data
    public static class MultiWeightConfigInfo {
        /**
         * 配置ID
         */
        private Long configId;

        /**
         * 配置名称
         */
        private String configName;

        /**
         * 颜色代码
         */
        private String colorCode;

        /**
         * 颜色名称
         */
        private String colorName;

        /**
         * 是否默认配置
         */
        private Boolean isDefault;

        /**
         * 应用的分类ID列表
         */
        private List<Long> applyCategoryIds;

        /**
         * 应用的分类名称列表
         */
        private List<String> applyCategoryNames;

        /**
         * 加权模式
         */
        private String weightMode;

        /**
         * 加权模式描述
         */
        private String weightModeDesc;

        /**
         * 标准人数模式
         */
        private String standardSizeMode;

        /**
         * 标准人数获取方式描述
         */
        private String standardSizeModeDesc;

        /**
         * 计算得到的标准人数
         */
        private Integer standardSize;

        /**
         * 加权系数
         */
        private BigDecimal weightFactor;

        /**
         * 系数下限
         */
        private BigDecimal minWeight;

        /**
         * 系数上限
         */
        private BigDecimal maxWeight;

        /**
         * 原始扣分（该配置应用的分类总扣分）
         */
        private BigDecimal originalScore;

        /**
         * 加权后扣分
         */
        private BigDecimal weightedScore;
    }

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

    // ==================== 排名（加权后） ====================

    /**
     * 加权后全校排名
     */
    private Integer weightedOverallRanking;

    /**
     * 加权后年级排名
     */
    private Integer weightedGradeRanking;

    /**
     * 加权后组织单元排名
     */
    private Integer weightedOrgUnitRanking;

    // ==================== 关联数据 ====================

    /**
     * 类别统计列表
     */
    private List<CheckRecordCategoryStatsDTO> categoryStats;

    /**
     * 扣分明细列表
     */
    private List<CheckRecordDeductionDTO> deductions;
}
