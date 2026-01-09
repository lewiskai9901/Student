package com.school.management.infrastructure.persistence.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评价配置 Mapper (DDD infrastructure layer)
 */
@Mapper
public interface RatingConfigPersistenceMapper extends BaseMapper<RatingConfigPO> {

    /**
     * 根据检查计划ID查找配置
     */
    @Select("SELECT * FROM rating_configs WHERE check_plan_id = #{checkPlanId} AND deleted = 0")
    List<RatingConfigPO> findByCheckPlanId(@Param("checkPlanId") Long checkPlanId);

    /**
     * 根据检查计划ID和周期类型查找启用的配置
     */
    @Select("SELECT * FROM rating_configs WHERE check_plan_id = #{checkPlanId} " +
            "AND period_type = #{periodType} AND enabled = 1 AND deleted = 0")
    List<RatingConfigPO> findEnabledByCheckPlanAndPeriodType(
            @Param("checkPlanId") Long checkPlanId,
            @Param("periodType") String periodType);

    /**
     * 查找所有启用的配置
     */
    @Select("SELECT * FROM rating_configs WHERE enabled = 1 AND deleted = 0")
    List<RatingConfigPO> findAllEnabled();

    /**
     * 检查ID是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM rating_configs WHERE id = #{id} AND deleted = 0")
    boolean existsById(@Param("id") Long id);
}
