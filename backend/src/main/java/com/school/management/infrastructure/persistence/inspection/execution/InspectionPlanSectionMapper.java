package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspectionPlanSectionMapper extends BaseMapper<InspectionPlanSectionPO> {

    @Select("SELECT section_id FROM insp_plan_sections WHERE plan_id = #{planId} AND deleted = 0 ORDER BY created_at, section_id")
    List<Long> findSectionIdsByPlanId(@Param("planId") Long planId);

    @Select("<script>"
            + "SELECT plan_id, section_id FROM insp_plan_sections"
            + " WHERE deleted = 0 AND plan_id IN"
            + " <foreach item='id' collection='planIds' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY plan_id, created_at, section_id"
            + "</script>")
    List<PlanSectionRow> findByPlanIds(@Param("planIds") List<Long> planIds);

    @Delete("DELETE FROM insp_plan_sections WHERE plan_id = #{planId}")
    int hardDeleteByPlanId(@Param("planId") Long planId);

    /** 删除某 section 时, 从所有调度组的关系表里清除. */
    @Delete("DELETE FROM insp_plan_sections WHERE section_id = #{sectionId}")
    int cascadeRemoveBySectionId(@Param("sectionId") Long sectionId);

    @lombok.Data
    class PlanSectionRow {
        private Long planId;
        private Long sectionId;
    }
}
