package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.StudentEvaluationResult;

import java.util.List;
import java.util.Map;

/**
 * 综测结果服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface EvaluationResultService extends IService<StudentEvaluationResult> {

    /**
     * 分页查询综测结果
     */
    Page<Map<String, Object>> pageResults(Page<?> page, Map<String, Object> query);

    /**
     * 获取综测结果详情
     */
    Map<String, Object> getResultDetail(Long id);

    /**
     * 获取学生综测结果
     */
    Map<String, Object> getStudentResult(Long studentId, Long periodId);

    /**
     * 获取班级综测排名
     */
    List<Map<String, Object>> getClassRanking(Long periodId, Long classId);

    /**
     * 获取年级综测排名
     */
    List<Map<String, Object>> getGradeRanking(Long periodId, Long gradeId, Integer limit);

    /**
     * 获取综测统计
     */
    Map<String, Object> getStatistics(Long periodId, Long classId, Long gradeId);

    /**
     * 导出综测结果
     */
    List<Map<String, Object>> exportResults(Long periodId, Long classId);

    /**
     * 获取维度分数明细
     */
    Map<String, Object> getDimensionDetail(Long resultId, String dimension);

    /**
     * 保存或更新综测结果
     */
    void saveOrUpdateResult(StudentEvaluationResult result);

    /**
     * 根据学生和周期查询结果
     */
    StudentEvaluationResult getByStudentAndPeriod(Long studentId, Long periodId);
}
