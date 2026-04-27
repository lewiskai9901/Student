package com.school.management.infrastructure.persistence.inspection.appeal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspAppealMapper extends BaseMapper<InspAppealPO> {

    @Select("SELECT * FROM inspection_appeals WHERE appeal_code = #{appealCode} AND deleted = 0")
    InspAppealPO findByAppealCode(@Param("appealCode") String appealCode);

    /** 按提交人查 — 申诉人查自己的申诉, 不必走数据权限 */
    @Select("SELECT * FROM inspection_appeals WHERE submitter_user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<InspAppealPO> findBySubmitterUserId(@Param("userId") Long userId);

    /** 按状态查 (审核员看待审清单) — 走数据权限 */
    @DataPermission(module = "inspection_appeal", orgUnitField = "org_unit_id", creatorField = "submitter_user_id")
    @Select("SELECT * FROM inspection_appeals WHERE status = #{status} AND deleted = 0 ORDER BY created_at")
    List<InspAppealPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM inspection_appeals WHERE submission_detail_id = #{detailId} AND deleted = 0")
    List<InspAppealPO> findBySubmissionDetailId(@Param("detailId") Long detailId);

    @DataPermission(module = "inspection_appeal", orgUnitField = "org_unit_id", creatorField = "submitter_user_id")
    @Select("SELECT * FROM inspection_appeals WHERE project_id = #{projectId} AND deleted = 0 ORDER BY created_at DESC")
    List<InspAppealPO> findByProjectId(@Param("projectId") Long projectId);
}
