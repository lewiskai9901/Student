package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SemesterMapper extends BaseMapper<SemesterPO> {

    @Select("SELECT * FROM semesters WHERE semester_code = #{code} AND deleted = 0")
    SemesterPO findBySemesterCode(@Param("code") String code);

    @Select("SELECT * FROM semesters WHERE is_current = 1 AND deleted = 0 LIMIT 1")
    SemesterPO findCurrentSemester();

    @Select("SELECT COUNT(1) > 0 FROM semesters WHERE semester_code = #{code} AND deleted = 0")
    boolean existsBySemesterCode(@Param("code") String code);

    @Select("SELECT COUNT(1) > 0 FROM semesters WHERE semester_code = #{code} AND id != #{excludeId} AND deleted = 0")
    boolean existsBySemesterCodeAndIdNot(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Select("SELECT * FROM semesters WHERE start_date >= #{start} AND start_date <= #{end} AND deleted = 0 ORDER BY start_date DESC")
    List<SemesterPO> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT * FROM semesters WHERE start_year = #{year} AND deleted = 0 ORDER BY semester_type")
    List<SemesterPO> findByStartYear(@Param("year") Integer year);

    @Select("SELECT * FROM semesters WHERE status = 1 AND deleted = 0 ORDER BY start_date DESC")
    List<SemesterPO> findAllActive();

    @Select("SELECT * FROM semesters WHERE deleted = 0 ORDER BY start_date DESC")
    List<SemesterPO> findAllOrderByStartDateDesc();

    @Select("SELECT * FROM semesters WHERE deleted = 0 ORDER BY start_date DESC LIMIT #{offset}, #{size}")
    List<SemesterPO> findAllPaged(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM semesters WHERE deleted = 0")
    long countAll();

    @Update("UPDATE semesters SET is_current = 0, updated_at = NOW() WHERE is_current = 1 AND deleted = 0")
    void clearAllCurrentFlags();
}
