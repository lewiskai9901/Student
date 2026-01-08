package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.EvaluationSemester;

import java.util.List;
import java.util.Map;

/**
 * 学期服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface SemesterService extends IService<EvaluationSemester> {

    /**
     * 分页查询学期
     */
    Page<Map<String, Object>> pageSemesters(Page<?> page, Map<String, Object> query);

    /**
     * 创建学期
     */
    Long createSemester(EvaluationSemester semester);

    /**
     * 更新学期
     */
    void updateSemester(EvaluationSemester semester);

    /**
     * 删除学期
     */
    void deleteSemester(Long id);

    /**
     * 获取学期详情
     */
    Map<String, Object> getSemesterDetail(Long id);

    /**
     * 获取当前学期
     */
    EvaluationSemester getCurrentSemester();

    /**
     * 根据学年获取学期列表
     */
    List<EvaluationSemester> getByAcademicYear(String academicYear);

    /**
     * 获取所有学年列表
     */
    List<String> getAllAcademicYears();

    /**
     * 设置为当前学期
     */
    void setAsCurrent(Long id);

    /**
     * 检查学期编码是否存在
     */
    boolean existsByCode(String semesterCode, Long excludeId);
}
