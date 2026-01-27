package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.StudentEvaluationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 学生综测结果Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface StudentEvaluationResultMapper extends BaseMapper<StudentEvaluationResult> {

    /**
     * 分页查询综测结果
     */
    Page<Map<String, Object>> selectResultPage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 根据周期和学生查询结果
     */
    StudentEvaluationResult selectByPeriodAndStudent(@Param("periodId") Long periodId, @Param("studentId") Long studentId);

    /**
     * 查询学生的综测历史
     */
    List<Map<String, Object>> selectStudentHistory(@Param("studentId") Long studentId);

    /**
     * 查询班级综测结果列表
     */
    List<StudentEvaluationResult> selectByPeriodAndClass(@Param("periodId") Long periodId, @Param("classId") Long classId);

    /**
     * 计算班级排名
     */
    void calculateClassRank(@Param("periodId") Long periodId, @Param("classId") Long classId);

    /**
     * 计算年级排名
     */
    void calculateGradeRank(@Param("periodId") Long periodId, @Param("gradeId") Long gradeId);

    /**
     * 计算组织单元排名
     */
    void calculateOrgUnitRank(@Param("periodId") Long periodId, @Param("orgUnitId") Long orgUnitId);

    /**
     * 查询综测结果详情(含明细统计)
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 批量更新状态
     */
    void batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 统计班级综测数据
     */
    Map<String, Object> selectClassStatistics(@Param("periodId") Long periodId, @Param("classId") Long classId);

    /**
     * 统计年级综测数据
     */
    Map<String, Object> selectGradeStatistics(@Param("periodId") Long periodId, @Param("gradeId") Long gradeId);

    /**
     * 根据学生和周期查询
     */
    StudentEvaluationResult selectByStudentAndPeriod(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    /**
     * 查询班级综测排名
     */
    List<Map<String, Object>> selectClassRanking(@Param("periodId") Long periodId, @Param("classId") Long classId);

    /**
     * 查询年级综测排名
     */
    List<Map<String, Object>> selectGradeRanking(@Param("periodId") Long periodId, @Param("gradeId") Long gradeId, @Param("limit") Integer limit);

    /**
     * 统计周期综测数据
     */
    Map<String, Object> selectPeriodStatistics(@Param("periodId") Long periodId);

    /**
     * 导出综测结果
     */
    List<Map<String, Object>> selectForExport(@Param("periodId") Long periodId, @Param("classId") Long classId);
}
