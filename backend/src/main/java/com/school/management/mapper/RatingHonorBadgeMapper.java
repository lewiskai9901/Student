package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.RatingHonorBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 荣誉徽章Mapper
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Mapper
public interface RatingHonorBadgeMapper extends BaseMapper<RatingHonorBadge> {

    /**
     * 获取检查计划的所有徽章（通过规则关联）
     */
    @Select("""
        SELECT DISTINCT b.*
        FROM rating_honor_badge b
        INNER JOIN check_plan_rating_rules r ON b.rule_id = r.id
        WHERE r.check_plan_id = #{checkPlanId}
          AND b.deleted = 0
        ORDER BY b.badge_level, b.created_at DESC
    """)
    List<RatingHonorBadge> selectByCheckPlanId(@Param("checkPlanId") Long checkPlanId);

    /**
     * 获取启用的自动授予徽章
     */
    @Select("""
        SELECT * FROM rating_honor_badge
        WHERE enabled = 1
          AND auto_grant = 1
          AND deleted = 0
        ORDER BY created_at
    """)
    List<RatingHonorBadge> selectEnabledAutoGrantBadges();
}
