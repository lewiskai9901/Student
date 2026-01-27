package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis mapper for class inspection records.
 */
@Mapper
public interface ClassInspectionRecordMapper extends BaseMapper<ClassInspectionRecordPO> {

    @Select("SELECT * FROM class_inspection_records WHERE session_id = #{sessionId} AND deleted = 0")
    List<ClassInspectionRecordPO> findBySessionId(@Param("sessionId") Long sessionId);

    @Select("SELECT * FROM class_inspection_records WHERE session_id = #{sessionId} AND class_id = #{classId} AND deleted = 0")
    ClassInspectionRecordPO findBySessionIdAndClassId(
        @Param("sessionId") Long sessionId,
        @Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM class_inspection_records WHERE session_id = #{sessionId} AND deleted = 0")
    int countBySessionId(@Param("sessionId") Long sessionId);

    @Select("SELECT r.* FROM class_inspection_records r " +
            "INNER JOIN inspection_sessions s ON r.session_id = s.id " +
            "WHERE r.class_id = #{classId} " +
            "AND s.inspection_date BETWEEN #{start} AND #{end} " +
            "AND r.deleted = 0 AND s.deleted = 0 " +
            "ORDER BY s.inspection_date DESC")
    List<ClassInspectionRecordPO> findByClassIdAndDateRange(
        @Param("classId") Long classId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end);

    @Select("SELECT * FROM class_inspection_records WHERE class_id = #{classId} AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<ClassInspectionRecordPO> findByClassIdOrderByCreatedAtDesc(
        @Param("classId") Long classId,
        @Param("limit") int limit);
}
