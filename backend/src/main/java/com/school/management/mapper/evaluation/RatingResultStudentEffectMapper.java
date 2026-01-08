package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.RatingResultStudentEffect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级结果-学生影响Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface RatingResultStudentEffectMapper extends BaseMapper<RatingResultStudentEffect> {

    /**
     * 根据学生ID和日期范围查询评级影响
     */
    List<RatingResultStudentEffect> selectByStudentAndDateRange(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 根据学生ID和维度查询评级影响
     */
    List<RatingResultStudentEffect> selectByStudentAndDimension(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dimension") String dimension);

    /**
     * 根据评级结果ID查询学生影响
     */
    List<RatingResultStudentEffect> selectByRatingResultId(@Param("ratingResultId") Long ratingResultId);

    /**
     * 更新同步状态
     */
    int updateSyncStatus(@Param("id") Long id, @Param("evaluationPeriodId") Long evaluationPeriodId);
}
