package com.school.management.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 班级检查统计Mapper（重构版）
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
@Repository("newCheckRecordClassStatsMapper")
public interface CheckRecordClassStatsMapper extends BaseMapper<CheckRecordClassStatsNew> {

    /**
     * 根据检查记录ID查询所有班级统计
     *
     * @param recordId 检查记录ID
     * @return 班级统计列表
     */
    List<CheckRecordClassStatsNew> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据检查记录ID和班级ID查询
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 班级统计
     */
    CheckRecordClassStatsNew selectByRecordAndClass(
            @Param("recordId") Long recordId,
            @Param("classId") Long classId
    );

    /**
     * 按院系分组查询班级统计
     *
     * @param recordId 检查记录ID
     * @param departmentId 院系ID
     * @return 班级统计列表
     */
    List<CheckRecordClassStatsNew> selectByDepartment(
            @Param("recordId") Long recordId,
            @Param("departmentId") Long departmentId
    );

    /**
     * 按年级分组查询班级统计
     *
     * @param recordId 检查记录ID
     * @param gradeId 年级ID
     * @return 班级统计列表
     */
    List<CheckRecordClassStatsNew> selectByGrade(
            @Param("recordId") Long recordId,
            @Param("gradeId") Long gradeId
    );

    /**
     * 查询扣分排名（按总分降序）
     *
     * @param recordId 检查记录ID
     * @param limit 限制数量
     * @return 排名列表
     */
    List<CheckRecordClassStatsNew> selectTopDeductions(
            @Param("recordId") Long recordId,
            @Param("limit") Integer limit
    );

    /**
     * 更新班级排名
     *
     * @param recordId 检查记录ID
     * @return 更新行数
     */
    int updateRankings(@Param("recordId") Long recordId);

    /**
     * 更新班级统计的申诉相关字段
     *
     * @param classStatId 班级统计ID
     * @return 更新行数
     */
    int updateAppealStats(@Param("classStatId") Long classStatId);

    /**
     * 重新计算班级统计分数
     *
     * @param classStatId 班级统计ID
     * @return 更新行数
     */
    int recalculateScore(@Param("classStatId") Long classStatId);

    /**
     * 批量插入班级统计
     *
     * @param list 班级统计列表
     * @return 插入行数
     */
    int batchInsert(@Param("list") List<CheckRecordClassStatsNew> list);

    /**
     * 获取检查记录的平均分
     *
     * @param recordId 检查记录ID
     * @return 平均分
     */
    BigDecimal selectAvgScore(@Param("recordId") Long recordId);

    /**
     * 根据班级ID查询历史统计（用于趋势分析）
     *
     * @param classId 班级ID
     * @param limit 限制数量
     * @return 历史统计列表
     */
    List<CheckRecordClassStatsNew> selectHistoryByClass(
            @Param("classId") Long classId,
            @Param("limit") Integer limit
    );

    /**
     * 统计年级的平均扣分(按班级)
     * 用于年级统计功能
     *
     * @param gradeId 年级ID
     * @return 平均扣分
     */
    @Select("SELECT COALESCE(AVG(total_score), 0) FROM check_record_class_stats_new WHERE grade_id = #{gradeId} AND deleted = 0")
    BigDecimal avgDeductionByGrade(@Param("gradeId") Long gradeId);
}
