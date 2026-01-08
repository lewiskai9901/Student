package com.school.management.infrastructure.persistence.semester;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 学期领域 Mapper
 */
@Mapper
public interface SemesterDomainMapper extends BaseMapper<SemesterPO> {

    /**
     * 根据学期编码查找学期
     */
    @Select("SELECT * FROM semesters WHERE semester_code = #{semesterCode} AND deleted = 0")
    SemesterPO findBySemesterCode(@Param("semesterCode") String semesterCode);

    /**
     * 获取当前学期
     */
    @Select("SELECT * FROM semesters WHERE is_current = 1 AND deleted = 0 LIMIT 1")
    SemesterPO findCurrentSemester();

    /**
     * 检查学期编码是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM semesters WHERE semester_code = #{semesterCode} AND deleted = 0")
    boolean existsBySemesterCode(@Param("semesterCode") String semesterCode);

    /**
     * 检查学期编码是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(1) > 0 FROM semesters WHERE semester_code = #{semesterCode} AND id != #{excludeId} AND deleted = 0")
    boolean existsBySemesterCodeAndIdNot(@Param("semesterCode") String semesterCode, @Param("excludeId") Long excludeId);

    /**
     * 根据日期范围查找学期
     */
    @Select("SELECT * FROM semesters WHERE start_date >= #{startDate} AND end_date <= #{endDate} AND deleted = 0")
    List<SemesterPO> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 根据年份查找学期
     */
    @Select("SELECT * FROM semesters WHERE start_year = #{startYear} AND deleted = 0 ORDER BY semester_type")
    List<SemesterPO> findByStartYear(@Param("startYear") Integer startYear);

    /**
     * 获取所有正常状态的学期
     */
    @Select("SELECT * FROM semesters WHERE status = 1 AND deleted = 0 ORDER BY start_date DESC")
    List<SemesterPO> findAllActive();

    /**
     * 获取所有学期（按开始日期降序）
     */
    @Select("SELECT * FROM semesters WHERE deleted = 0 ORDER BY start_date DESC")
    List<SemesterPO> findAllOrderByStartDateDesc();

    /**
     * 分页查询学期
     */
    @Select("SELECT * FROM semesters WHERE deleted = 0 ORDER BY start_date DESC LIMIT #{offset}, #{size}")
    List<SemesterPO> findAllPaged(@Param("offset") int offset, @Param("size") int size);

    /**
     * 统计学期总数
     */
    @Select("SELECT COUNT(1) FROM semesters WHERE deleted = 0")
    long countAll();

    /**
     * 取消所有当前学期标识
     */
    @Update("UPDATE semesters SET is_current = 0, updated_at = NOW() WHERE is_current = 1 AND deleted = 0")
    void clearAllCurrentFlags();
}
