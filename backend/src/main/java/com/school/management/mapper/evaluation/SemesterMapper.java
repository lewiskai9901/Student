package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.EvaluationSemester;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 学期Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
@Repository("evaluationSemesterMapper")
public interface SemesterMapper extends BaseMapper<EvaluationSemester> {

    /**
     * 分页查询学期
     */
    Page<Map<String, Object>> selectSemesterPage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 获取当前学期
     */
    EvaluationSemester selectCurrent();

    /**
     * 根据学期编码查询
     */
    EvaluationSemester selectByCode(@Param("code") String code);

    /**
     * 根据学年查询学期列表
     */
    List<EvaluationSemester> selectByAcademicYear(@Param("academicYear") String academicYear);

    /**
     * 获取所有学年列表
     */
    List<String> selectAllAcademicYears();

    /**
     * 清除当前学期标记
     */
    void clearCurrentFlag();

    /**
     * 查询学期详情
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 统计学期关联数据
     */
    int countRelatedData(@Param("semesterId") Long semesterId);
}
