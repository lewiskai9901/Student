package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis mapper for inspection records.
 */
@Mapper
public interface InspectionRecordMapper extends BaseMapper<InspectionRecordPO> {

    @Select("SELECT * FROM inspection_records WHERE record_code = #{recordCode} AND deleted = 0")
    InspectionRecordPO findByRecordCode(@Param("recordCode") String recordCode);

    @Select("SELECT * FROM inspection_records WHERE status = #{status} AND deleted = 0")
    List<InspectionRecordPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM inspection_records WHERE inspection_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY inspection_date DESC")
    List<InspectionRecordPO> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM inspection_records WHERE template_id = #{templateId} AND deleted = 0 ORDER BY inspection_date DESC")
    List<InspectionRecordPO> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM inspection_records WHERE round_id = #{roundId} AND deleted = 0")
    List<InspectionRecordPO> findByRoundId(@Param("roundId") Long roundId);

    @Select("SELECT * FROM inspection_records WHERE inspector_id = #{inspectorId} AND deleted = 0 ORDER BY inspection_date DESC")
    List<InspectionRecordPO> findByInspectorId(@Param("inspectorId") Long inspectorId);

    @Select("SELECT * FROM inspection_records WHERE inspection_date = #{date} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionRecordPO> findByInspectionDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM inspection_records WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);
}
