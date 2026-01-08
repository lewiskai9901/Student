package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for appeals.
 */
@Mapper
public interface AppealMapper extends BaseMapper<AppealPO> {

    @Select("SELECT * FROM appeals WHERE appeal_code = #{appealCode} AND deleted = 0")
    AppealPO findByAppealCode(@Param("appealCode") String appealCode);

    @Select("SELECT * FROM appeals WHERE status = #{status} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM appeals WHERE class_id = #{classId} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM appeals WHERE inspection_record_id = #{recordId} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByInspectionRecordId(@Param("recordId") Long recordId);

    @Select("SELECT * FROM appeals WHERE applicant_id = #{applicantId} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByApplicantId(@Param("applicantId") Long applicantId);

    @Select("SELECT * FROM appeals WHERE level1_reviewer_id = #{reviewerId} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByLevel1ReviewerId(@Param("reviewerId") Long reviewerId);

    @Select("SELECT * FROM appeals WHERE level2_reviewer_id = #{reviewerId} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByLevel2ReviewerId(@Param("reviewerId") Long reviewerId);

    @Select("SELECT COUNT(*) > 0 FROM appeals WHERE deduction_detail_id = #{deductionDetailId} AND status NOT IN ('REJECTED', 'WITHDRAWN', 'EFFECTIVE') AND deleted = 0")
    boolean existsByDeductionDetailId(@Param("deductionDetailId") Long deductionDetailId);

    @Select("SELECT COUNT(*) FROM appeals WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);

    @Select("SELECT * FROM appeals WHERE applied_at BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY applied_at DESC")
    List<AppealPO> findByAppliedDateRange(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
