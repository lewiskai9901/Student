package com.school.management.service.record;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.record.*;
import com.school.management.entity.record.CheckRecordNew;

import java.util.List;

/**
 * 检查记录服务接口（重构版）
 *
 * @author system
 * @since 2.0.0
 */
public interface CheckRecordService {

    /**
     * 分页查询检查记录
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<CheckRecordDTO> queryPage(CheckRecordQueryDTO query);

    /**
     * 获取检查记录详情
     *
     * @param recordId 记录ID
     * @return 记录详情
     */
    CheckRecordDTO getById(Long recordId);

    /**
     * 获取检查记录详情（包含所有关联数据）
     *
     * @param recordId 记录ID
     * @return 完整记录详情
     */
    CheckRecordDTO getDetailById(Long recordId);

    /**
     * 根据日常检查ID获取记录
     *
     * @param dailyCheckId 日常检查ID
     * @return 记录详情
     */
    CheckRecordDTO getByDailyCheckId(Long dailyCheckId);

    /**
     * 从日常检查生成检查记录
     *
     * @param dailyCheckId 日常检查ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 生成的检查记录
     */
    CheckRecordNew generateFromDailyCheck(Long dailyCheckId, Long operatorId, String operatorName);

    /**
     * 归档检查记录
     *
     * @param recordId 记录ID
     */
    void archive(Long recordId);

    /**
     * 删除检查记录
     *
     * @param recordId 记录ID
     */
    void delete(Long recordId);

    /**
     * 重新计算检查记录统计
     *
     * @param recordId 记录ID
     * @param reason 重算原因
     */
    void recalculate(Long recordId, String reason);

    /**
     * 获取班级统计列表
     *
     * @param recordId 记录ID
     * @return 班级统计列表
     */
    List<CheckRecordClassStatsDTO> getClassStatsList(Long recordId);

    /**
     * 获取班级统计详情
     *
     * @param classStatId 班级统计ID
     * @return 班级统计详情
     */
    CheckRecordClassStatsDTO getClassStatsDetail(Long classStatId);

    /**
     * 按组织单元获取班级统计（原按院系获取）
     *
     * @param recordId 记录ID
     * @param orgUnitId 组织单元ID（原院系ID）
     * @return 班级统计列表
     */
    List<CheckRecordClassStatsDTO> getClassStatsByOrgUnit(Long recordId, Long orgUnitId);

    /**
     * 按年级获取班级统计
     *
     * @param recordId 记录ID
     * @param gradeId 年级ID
     * @return 班级统计列表
     */
    List<CheckRecordClassStatsDTO> getClassStatsByGrade(Long recordId, Long gradeId);

    /**
     * 获取扣分明细列表
     *
     * @param recordId 记录ID
     * @param classId 班级ID（可选）
     * @return 扣分明细列表
     */
    List<CheckRecordDeductionDTO> getDeductionList(Long recordId, Long classId);

    /**
     * 获取扣分明细详情
     *
     * @param deductionId 扣分明细ID
     * @return 扣分明细详情
     */
    CheckRecordDeductionDTO getDeductionById(Long deductionId);

    /**
     * 获取班级排名列表（含加权分数）
     *
     * @param recordId 记录ID
     * @param sortBy 排序方式：weighted=加权分数, original=原始分数
     * @param orgUnitId 组织单元ID筛选（原院系ID，可选）
     * @param gradeLevel 年级筛选（可选）
     * @return 排名后的班级统计列表
     */
    List<CheckRecordClassStatsDTO> getClassRankingWithWeight(Long recordId, String sortBy, Long orgUnitId, Integer gradeLevel);

    /**
     * 获取检查记录的加权配置详情
     *
     * @param recordId 记录ID
     * @return 加权配置详情
     */
    WeightConfigDetailDTO getWeightConfigDetail(Long recordId);

    /**
     * 获取检查记录的加权配置树形结构
     * 包含完整的3级继承关系：记录级 → 类别级 → 扣分项级
     *
     * @param recordId 记录ID
     * @return 加权配置树形结构
     */
    WeightConfigTreeDTO getWeightConfigTree(Long recordId);
}
