package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 考试安排Mapper
 */
@Mapper
public interface ExamArrangementMapper extends BaseMapper<ExamArrangementPO> {

    @Delete("DELETE FROM exam_arrangements WHERE batch_id = #{batchId}")
    void deleteByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT ea.* FROM exam_arrangements ea " +
            "INNER JOIN exam_batches eb ON ea.batch_id = eb.id " +
            "WHERE eb.semester_id = #{semesterId} AND ea.course_id = #{courseId} " +
            "ORDER BY ea.exam_date")
    List<ExamArrangementPO> findByCourseId(@Param("semesterId") Long semesterId, @Param("courseId") Long courseId);

    @Select("SELECT DISTINCT ea.* FROM exam_arrangements ea " +
            "INNER JOIN exam_batches eb ON ea.batch_id = eb.id " +
            "INNER JOIN exam_rooms er ON ea.id = er.arrangement_id " +
            "WHERE eb.semester_id = #{semesterId} AND er.classroom_id = #{classroomId} " +
            "ORDER BY ea.exam_date")
    List<ExamArrangementPO> findByClassroomId(@Param("semesterId") Long semesterId, @Param("classroomId") Long classroomId);
}
