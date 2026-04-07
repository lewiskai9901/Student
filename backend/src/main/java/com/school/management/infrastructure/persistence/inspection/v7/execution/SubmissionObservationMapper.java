package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SubmissionObservationMapper extends BaseMapper<SubmissionObservationPO> {

    @Select("SELECT * FROM insp_submission_observations WHERE submission_id = #{submissionId} AND deleted = 0 ORDER BY id")
    List<SubmissionObservationPO> findBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("SELECT * FROM insp_submission_observations WHERE submission_id = #{submissionId} AND is_negative = 1 AND deleted = 0 ORDER BY id")
    List<SubmissionObservationPO> findNegativeBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("<script>" +
            "SELECT * FROM insp_submission_observations WHERE deleted = 0" +
            "<if test='projectId != null'> AND project_id = #{projectId}</if>" +
            "<if test='subjectType != null'> AND subject_type = #{subjectType}</if>" +
            "<if test='severity != null'> AND severity = #{severity}</if>" +
            "<if test='isNegative != null'> AND is_negative = #{isNegative}</if>" +
            "<if test='linkedEventTypeCode != null'> AND linked_event_type_code = #{linkedEventTypeCode}</if>" +
            " ORDER BY observed_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<SubmissionObservationPO> findFiltered(
            @Param("projectId") Long projectId,
            @Param("subjectType") String subjectType,
            @Param("severity") String severity,
            @Param("isNegative") Integer isNegative,
            @Param("linkedEventTypeCode") String linkedEventTypeCode,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM insp_submission_observations WHERE deleted = 0" +
            "<if test='projectId != null'> AND project_id = #{projectId}</if>" +
            "<if test='subjectType != null'> AND subject_type = #{subjectType}</if>" +
            "<if test='severity != null'> AND severity = #{severity}</if>" +
            "<if test='isNegative != null'> AND is_negative = #{isNegative}</if>" +
            "<if test='linkedEventTypeCode != null'> AND linked_event_type_code = #{linkedEventTypeCode}</if>" +
            "</script>")
    long countFiltered(
            @Param("projectId") Long projectId,
            @Param("subjectType") String subjectType,
            @Param("severity") String severity,
            @Param("isNegative") Integer isNegative,
            @Param("linkedEventTypeCode") String linkedEventTypeCode);
}
