package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.EvaluationPeriod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 综测评定周期Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface EvaluationPeriodMapper extends BaseMapper<EvaluationPeriod> {

    /**
     * 分页查询综测周期
     */
    Page<Map<String, Object>> selectPeriodPage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 获取当前进行中的周期
     */
    EvaluationPeriod selectCurrentPeriod();

    /**
     * 根据学期ID查询周期
     */
    EvaluationPeriod selectBySemesterId(@Param("semesterId") Long semesterId);

    /**
     * 根据周期编码查询
     */
    EvaluationPeriod selectByCode(@Param("code") String code);

    /**
     * 查询周期详情(含学期信息)
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 根据学年查询周期列表
     */
    List<EvaluationPeriod> selectByAcademicYear(@Param("academicYear") String academicYear);
}
