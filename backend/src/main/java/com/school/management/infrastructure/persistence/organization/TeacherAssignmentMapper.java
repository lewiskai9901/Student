package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教师任职记录 Mapper 接口
 */
@Mapper
public interface TeacherAssignmentMapper extends BaseMapper<TeacherAssignmentPO> {

    /**
     * 根据班级ID查找所有任职记录
     */
    @Select("SELECT * FROM teacher_assignments " +
            "WHERE class_id = #{classId} " +
            "AND deleted = 0 " +
            "ORDER BY start_date DESC")
    List<TeacherAssignmentPO> findByClassId(@Param("classId") Long classId);

    /**
     * 根据班级ID查找当前任职记录
     */
    @Select("SELECT * FROM teacher_assignments " +
            "WHERE class_id = #{classId} " +
            "AND is_current = 1 " +
            "AND deleted = 0")
    List<TeacherAssignmentPO> findCurrentByClassId(@Param("classId") Long classId);

    /**
     * 根据教师ID查找所有任职记录
     */
    @Select("SELECT * FROM teacher_assignments " +
            "WHERE teacher_id = #{teacherId} " +
            "AND deleted = 0 " +
            "ORDER BY start_date DESC")
    List<TeacherAssignmentPO> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 根据教师ID查找当前任职记录
     */
    @Select("SELECT * FROM teacher_assignments " +
            "WHERE teacher_id = #{teacherId} " +
            "AND is_current = 1 " +
            "AND deleted = 0")
    List<TeacherAssignmentPO> findCurrentByTeacherId(@Param("teacherId") Long teacherId);
}
