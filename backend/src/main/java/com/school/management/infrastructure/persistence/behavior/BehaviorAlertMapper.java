package com.school.management.infrastructure.persistence.behavior;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BehaviorAlertMapper extends BaseMapper<BehaviorAlertPO> {

    @Select("SELECT * FROM student_behavior_alerts WHERE student_id = #{studentId} AND deleted = 0 ORDER BY created_at DESC")
    List<BehaviorAlertPO> findByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT * FROM student_behavior_alerts WHERE class_id = #{classId} AND is_handled = 0 AND deleted = 0 ORDER BY created_at DESC")
    List<BehaviorAlertPO> findUnhandledByClassId(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM student_behavior_alerts WHERE class_id = #{classId} AND is_handled = 0 AND deleted = 0")
    long countUnhandledByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM student_behavior_alerts WHERE class_id = #{classId} AND deleted = 0 ORDER BY created_at DESC")
    List<BehaviorAlertPO> findByClassId(@Param("classId") Long classId);
}
