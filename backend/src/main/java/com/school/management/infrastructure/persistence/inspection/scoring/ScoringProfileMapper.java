package com.school.management.infrastructure.persistence.inspection.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoringProfileMapper extends BaseMapper<ScoringProfilePO> {

    /**
     * 旧的"按 sectionId 全局查"语义 — 项目-owned 后存在多条同 sectionId 跨项目并存,
     * 仅供迁移期间 / 旧调用兼容. 新代码请使用 {@link #findByProjectIdAndSectionId}.
     * @deprecated 项目-owned 重构后语义已不唯一.
     */
    @Deprecated
    @Select("SELECT * FROM insp_scoring_profiles WHERE section_id = #{sectionId} AND deleted = 0 LIMIT 1")
    ScoringProfilePO findBySectionId(@Param("sectionId") Long sectionId);

    /** 列出某项目下所有评分方案. */
    @Select("SELECT * FROM insp_scoring_profiles WHERE project_id = #{projectId} AND deleted = 0")
    List<ScoringProfilePO> findByProjectId(@Param("projectId") Long projectId);

    /** 按 (项目, 分区) 唯一定位评分方案. */
    @Select("SELECT * FROM insp_scoring_profiles WHERE project_id = #{projectId} AND section_id = #{sectionId} AND deleted = 0 LIMIT 1")
    ScoringProfilePO findByProjectIdAndSectionId(@Param("projectId") Long projectId,
                                                  @Param("sectionId") Long sectionId);

    /** 物理删除某项目下所有 profile (供项目级联删除使用). */
    @org.apache.ibatis.annotations.Delete(
            "DELETE FROM insp_scoring_profiles WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") Long projectId);
}
