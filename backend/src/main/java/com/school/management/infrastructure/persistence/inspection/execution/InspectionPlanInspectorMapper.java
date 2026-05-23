package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspectionPlanInspectorMapper extends BaseMapper<InspectionPlanInspectorPO> {

    /** 加载某调度组的全部检查员 user_id, 按插入序. */
    @Select("SELECT user_id FROM insp_plan_inspectors WHERE plan_id = #{planId} AND deleted = 0 ORDER BY created_at, user_id")
    List<Long> findUserIdsByPlanId(@Param("planId") Long planId);

    /** 批量加载: 列表页 N+1 消除. 返回 (plan_id, user_id) 元组, 调用方分组. */
    @Select("<script>"
            + "SELECT plan_id, user_id FROM insp_plan_inspectors"
            + " WHERE deleted = 0 AND plan_id IN"
            + " <foreach item='id' collection='planIds' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY plan_id, created_at, user_id"
            + "</script>")
    List<PlanInspectorRow> findByPlanIds(@Param("planIds") List<Long> planIds);

    /** 物理删某调度组的全部关系 (重建用). */
    @Delete("DELETE FROM insp_plan_inspectors WHERE plan_id = #{planId}")
    int hardDeleteByPlanId(@Param("planId") Long planId);

    /** 项目移除某 inspector 时, 级联清除该 user 在该项目所有调度组的关系. */
    @Delete("DELETE pi FROM insp_plan_inspectors pi "
          + "JOIN insp_inspection_plans p ON p.id = pi.plan_id "
          + "WHERE p.project_id = #{projectId} AND pi.user_id = #{userId}")
    int cascadeRemoveByProjectUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @lombok.Data
    class PlanInspectorRow {
        private Long planId;
        private Long userId;
    }
}
