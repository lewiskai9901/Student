package com.school.management.infrastructure.persistence.behavior;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BehaviorRecordMapper extends BaseMapper<BehaviorRecordPO> {

    @Select("SELECT * FROM student_behavior_records WHERE student_id = #{studentId} AND deleted = 0 ORDER BY recorded_at DESC")
    List<BehaviorRecordPO> findByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT * FROM student_behavior_records WHERE class_id = #{classId} AND deleted = 0 ORDER BY recorded_at DESC")
    List<BehaviorRecordPO> findByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM student_behavior_records WHERE class_id = #{classId} AND recorded_at BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY recorded_at DESC")
    List<BehaviorRecordPO> findByClassIdAndDateRange(@Param("classId") Long classId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM student_behavior_records WHERE student_id = #{studentId} AND behavior_type = #{type} AND deleted = 0")
    long countByStudentIdAndType(@Param("studentId") Long studentId, @Param("type") String type);

    @Select("SELECT * FROM student_behavior_records WHERE student_id = #{studentId} AND recorded_at BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY recorded_at DESC")
    List<BehaviorRecordPO> findByStudentIdAndDateRange(@Param("studentId") Long studentId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
}
