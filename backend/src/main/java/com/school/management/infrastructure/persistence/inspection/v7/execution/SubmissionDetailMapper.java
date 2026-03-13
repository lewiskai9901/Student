package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SubmissionDetailMapper extends BaseMapper<SubmissionDetailPO> {

    @Select("SELECT * FROM insp_submission_details WHERE submission_id = #{submissionId} AND deleted = 0 ORDER BY id")
    List<SubmissionDetailPO> findBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("SELECT * FROM insp_submission_details WHERE submission_id = #{submissionId} AND is_flagged = 1 AND deleted = 0")
    List<SubmissionDetailPO> findFlaggedBySubmissionId(@Param("submissionId") Long submissionId);
}
