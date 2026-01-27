package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis mapper for inspection sessions.
 */
@Mapper
public interface InspectionSessionMapper extends BaseMapper<InspectionSessionPO> {

    @Select("SELECT * FROM inspection_sessions WHERE session_code = #{sessionCode} AND deleted = 0")
    InspectionSessionPO findBySessionCode(@Param("sessionCode") String sessionCode);

    @Select("SELECT * FROM inspection_sessions WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionSessionPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM inspection_sessions WHERE inspection_date = #{date} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionSessionPO> findByInspectionDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM inspection_sessions WHERE inspection_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY inspection_date DESC")
    List<InspectionSessionPO> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM inspection_sessions WHERE inspector_id = #{inspectorId} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionSessionPO> findByInspectorId(@Param("inspectorId") Long inspectorId);

    @Select("SELECT * FROM inspection_sessions WHERE inspection_level = #{level} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionSessionPO> findByInspectionLevel(@Param("level") String level);

    @Select("SELECT * FROM inspection_sessions WHERE status = 'PUBLISHED' AND inspection_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY inspection_date DESC")
    List<InspectionSessionPO> findPublishedByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
