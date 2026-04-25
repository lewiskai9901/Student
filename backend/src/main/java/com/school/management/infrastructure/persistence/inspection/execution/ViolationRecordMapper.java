package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ViolationRecordMapper extends BaseMapper<ViolationRecordPO> {

    @DataPermission(module = "inspection_violation", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_violation_records WHERE submission_id = #{submissionId} AND deleted = 0 ORDER BY occurred_at DESC")
    List<ViolationRecordPO> findBySubmissionId(@Param("submissionId") Long submissionId);

    @DataPermission(module = "inspection_violation", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_violation_records WHERE submission_detail_id = #{submissionDetailId} AND deleted = 0 ORDER BY occurred_at DESC")
    List<ViolationRecordPO> findBySubmissionDetailId(@Param("submissionDetailId") Long submissionDetailId);

    @DataPermission(module = "inspection_violation", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_violation_records WHERE user_id = #{userId} AND deleted = 0 ORDER BY occurred_at DESC")
    List<ViolationRecordPO> findByUserId(@Param("userId") Long userId);
}
