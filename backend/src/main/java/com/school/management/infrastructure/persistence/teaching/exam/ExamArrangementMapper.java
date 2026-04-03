package com.school.management.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamArrangementMapper extends BaseMapper<ExamArrangementPO> {

    @Select("SELECT a.id, a.batch_id AS batchId, a.course_id AS courseId, " +
            "a.exam_date AS examDate, a.start_time AS startTime, a.end_time AS endTime, " +
            "a.duration, a.exam_form AS examForm, a.total_students AS totalStudents, " +
            "a.remark, a.status, a.created_at AS createdAt, " +
            "c.course_name AS courseName " +
            "FROM exam_arrangements a " +
            "LEFT JOIN courses c ON a.course_id = c.id " +
            "WHERE a.batch_id = #{batchId} ORDER BY a.exam_date, a.start_time")
    List<Map<String, Object>> listWithCourseName(@Param("batchId") Long batchId);
}
