package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InspEvidenceMapper extends BaseMapper<InspEvidencePO> {

    /**
     * 按提交记录查询证据列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_evidences WHERE submission_id = #{submissionId} AND deleted = 0 ORDER BY created_at")
    List<InspEvidencePO> findBySubmissionId(@Param("submissionId") Long submissionId);

    /**
     * 按明细查询证据列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_evidences WHERE detail_id = #{detailId} AND deleted = 0 ORDER BY created_at")
    List<InspEvidencePO> findByDetailId(@Param("detailId") Long detailId);
}
