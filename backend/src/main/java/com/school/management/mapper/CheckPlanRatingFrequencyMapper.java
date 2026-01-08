package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckPlanRatingFrequency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级频次统计Mapper
 */
@Mapper
public interface CheckPlanRatingFrequencyMapper extends BaseMapper<CheckPlanRatingFrequency> {

    /**
     * 获取班级在指定周期内的评级频次排名
     */
    @Select("""
        SELECT f.*,
               (SELECT COUNT(DISTINCT f2.class_id) + 1
                FROM check_plan_rating_frequency f2
                WHERE f2.rule_id = f.rule_id
                  AND f2.level_id = f.level_id
                  AND f2.period_type = f.period_type
                  AND f2.period_start = f.period_start
                  AND f2.frequency > f.frequency) as ranking
        FROM check_plan_rating_frequency f
        WHERE f.check_plan_id = #{checkPlanId}
          AND f.period_type = #{periodType}
          AND f.period_start = #{periodStart}
          AND f.period_end = #{periodEnd}
        ORDER BY f.level_id, f.frequency DESC
    """)
    List<CheckPlanRatingFrequency> getFrequencyRanking(
            @Param("checkPlanId") Long checkPlanId,
            @Param("periodType") String periodType,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    /**
     * 获取指定等级的TOP班级
     */
    @Select("""
        SELECT * FROM check_plan_rating_frequency
        WHERE rule_id = #{ruleId}
          AND level_id = #{levelId}
          AND period_type = #{periodType}
          AND period_start = #{periodStart}
        ORDER BY frequency DESC
        LIMIT #{limit}
    """)
    List<CheckPlanRatingFrequency> getTopClassesByLevel(
            @Param("ruleId") Long ruleId,
            @Param("levelId") Long levelId,
            @Param("periodType") String periodType,
            @Param("periodStart") LocalDate periodStart,
            @Param("limit") int limit
    );

    /**
     * 获取班级的评级频次历史
     */
    @Select("""
        SELECT * FROM check_plan_rating_frequency
        WHERE class_id = #{classId}
          AND rule_id = #{ruleId}
          AND period_type = #{periodType}
        ORDER BY period_start DESC
    """)
    List<CheckPlanRatingFrequency> getClassFrequencyHistory(
            @Param("classId") Long classId,
            @Param("ruleId") Long ruleId,
            @Param("periodType") String periodType
    );
}
