package com.school.management.service;

import com.school.management.dto.ClassWeightResultDTO;
import com.school.management.entity.ClassWeightConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 班级加权服务接口
 * 核心业务:班级人数加权计算引擎
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
public interface ClassWeightService {

    /**
     * 计算加权后的分数
     *
     * @param classId 班级ID
     * @param originalScore 原始分数
     * @param checkDate 检查日期
     * @param recordId 检查记录ID(可选,用于关联快照)
     * @return 加权计算结果
     */
    ClassWeightResultDTO calculateWeightedScore(
            Long classId,
            BigDecimal originalScore,
            LocalDate checkDate,
            Long recordId
    );

    /**
     * 批量计算班级的加权分数
     *
     * @param classIds 班级ID列表
     * @param originalScore 原始分数
     * @param checkDate 检查日期
     * @param recordId 检查记录ID
     * @return 加权计算结果列表
     */
    List<ClassWeightResultDTO> batchCalculateWeightedScore(
            List<Long> classIds,
            BigDecimal originalScore,
            LocalDate checkDate,
            Long recordId
    );

    /**
     * 获取班级的权重系数
     *
     * @param classId 班级ID
     * @param checkDate 检查日期
     * @return 权重系数
     */
    BigDecimal getWeightFactor(Long classId, LocalDate checkDate);

    /**
     * 获取标准班级人数
     * 优先级: 锁定的标准人数 > 实时计算的平均值
     *
     * @param classId 班级ID
     * @param semesterId 学期ID
     * @param checkDate 检查日期
     * @return 标准人数
     */
    Integer getStandardSize(Long classId, Long semesterId, LocalDate checkDate);

    /**
     * 获取标准班级人数(支持TARGET_AVERAGE模式)
     * 优先级: 锁定的标准人数 > 目标班级平均人数 > 实时计算的平均值
     *
     * @param classId 班级ID
     * @param semesterId 学期ID
     * @param checkDate 检查日期
     * @param recordId 检查记录ID(用于TARGET_AVERAGE模式计算目标班级平均人数)
     * @return 标准人数
     */
    Integer getStandardSize(Long classId, Long semesterId, LocalDate checkDate, Long recordId);

    /**
     * 获取班级的实际人数
     * 优先使用快照,没有快照则使用实时人数
     *
     * @param classId 班级ID
     * @param checkDate 检查日期
     * @param recordId 检查记录ID
     * @return 实际人数
     */
    Integer getActualClassSize(Long classId, LocalDate checkDate, Long recordId);

    /**
     * 获取班级的加权配置
     * 按优先级查找: 班级专属 > 年级 > 部门 > 全局
     *
     * @param classId 班级ID
     * @param currentDate 当前日期
     * @return 加权配置
     */
    ClassWeightConfig getWeightConfigForClass(Long classId, LocalDate currentDate);

    /**
     * 检查是否应该应用加权
     *
     * @param classId 班级ID
     * @param recordId 检查记录ID
     * @param categoryId 类别ID
     * @return 是否应用加权
     */
    boolean shouldApplyWeight(Long classId, Long recordId, Long categoryId);

    /**
     * 重新计算班级统计数据(应用加权后)
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 是否成功
     */
    boolean recalculateClassStats(Long recordId, Long classId);

    /**
     * 计算分段加权的权重系数
     * 根据班级人数所在区间返回对应的权重系数
     *
     * @param actualSize 实际班级人数
     * @param segmentRules 分段规则JSON
     * @return 权重系数
     */
    BigDecimal calculateSegmentWeight(Integer actualSize, String segmentRules);

    /**
     * 应用权重上下限
     *
     * @param weightFactor 原始权重系数
     * @param config 加权配置
     * @return 限制后的权重系数
     */
    BigDecimal applyWeightLimit(BigDecimal weightFactor, ClassWeightConfig config);

    /**
     * 创建加权计算结果DTO
     *
     * @param classId 班级ID
     * @param originalScore 原始分数
     * @param weightFactor 权重系数
     * @param config 加权配置
     * @param actualSize 实际人数
     * @param standardSize 标准人数
     * @return 计算结果DTO
     */
    ClassWeightResultDTO buildWeightResult(
            Long classId,
            BigDecimal originalScore,
            BigDecimal weightFactor,
            ClassWeightConfig config,
            Integer actualSize,
            Integer standardSize
    );
}
