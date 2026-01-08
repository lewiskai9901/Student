package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.StudentPunishment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 学生处分Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface StudentPunishmentMapper extends BaseMapper<StudentPunishment> {

    /**
     * 根据学生ID查询有效处分
     */
    List<StudentPunishment> selectValidByStudentId(@Param("studentId") Long studentId);

    /**
     * 根据学生ID和日期查询有效处分
     */
    List<StudentPunishment> selectValidByStudentIdAndDate(
            @Param("studentId") Long studentId,
            @Param("date") LocalDate date);

    /**
     * 查询学生在某日期之前的有效处分
     */
    List<StudentPunishment> selectValidBefore(
            @Param("studentId") Long studentId,
            @Param("beforeDate") LocalDate beforeDate);
}
