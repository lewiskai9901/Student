package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.StudentEvaluationDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 学生综测明细Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface StudentEvaluationDetailMapper extends BaseMapper<StudentEvaluationDetail> {

    /**
     * 根据结果ID查询明细列表
     */
    List<StudentEvaluationDetail> selectByResultId(@Param("resultId") Long resultId);

    /**
     * 根据结果ID和维度查询明细
     */
    List<StudentEvaluationDetail> selectByResultIdAndDimension(@Param("resultId") Long resultId, @Param("dimension") String dimension);

    /**
     * 统计各维度得分汇总
     */
    List<Map<String, Object>> selectDimensionSummary(@Param("resultId") Long resultId);

    /**
     * 根据来源类型查询明细
     */
    List<StudentEvaluationDetail> selectBySourceType(@Param("resultId") Long resultId, @Param("sourceType") String sourceType);

    /**
     * 批量插入明细
     */
    void batchInsert(@Param("details") List<StudentEvaluationDetail> details);

    /**
     * 删除结果的所有明细
     */
    void deleteByResultId(@Param("resultId") Long resultId);

    /**
     * 根据结果ID和维度查询明细（返回Map）
     */
    List<Map<String, Object>> selectByResultAndDimension(@Param("resultId") Long resultId, @Param("dimension") String dimension);
}
