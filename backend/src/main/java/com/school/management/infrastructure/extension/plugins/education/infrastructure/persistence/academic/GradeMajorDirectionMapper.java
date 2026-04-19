package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 年级-专业方向关联 Mapper
 */
@Mapper
public interface GradeMajorDirectionMapper extends BaseMapper<GradeMajorDirectionPO> {

    @Select("SELECT * FROM grade_major_directions WHERE grade_id = #{gradeId} AND deleted = 0 ORDER BY id")
    List<GradeMajorDirectionPO> findByGradeId(@Param("gradeId") Long gradeId);

    @Select("SELECT gmd.*, " +
            "md.direction_name, md.direction_code, md.level, md.years, " +
            "md.is_segmented, md.phase1_level, md.phase1_years, md.phase2_level, md.phase2_years, " +
            "m.org_unit_id, m.major_code, " +
            "g.enrollment_year " +
            "FROM grade_major_directions gmd " +
            "LEFT JOIN major_directions md ON gmd.major_direction_id = md.id AND md.deleted = 0 " +
            "LEFT JOIN majors m ON gmd.major_id = m.id AND m.deleted = 0 " +
            "LEFT JOIN grades g ON gmd.grade_id = g.id AND g.deleted = 0 " +
            "WHERE gmd.grade_id = #{gradeId} AND gmd.deleted = 0 ORDER BY gmd.id")
    List<GradeMajorDirectionPO> findByGradeIdWithJoin(@Param("gradeId") Long gradeId);

    @Select("SELECT * FROM grade_major_directions WHERE major_direction_id = #{majorDirectionId} AND deleted = 0")
    List<GradeMajorDirectionPO> findByMajorDirectionId(@Param("majorDirectionId") Long majorDirectionId);

    @Select("SELECT * FROM grade_major_directions " +
            "WHERE grade_id = #{gradeId} AND major_direction_id = #{majorDirectionId} AND deleted = 0")
    GradeMajorDirectionPO findByGradeIdAndMajorDirectionId(
            @Param("gradeId") Long gradeId,
            @Param("majorDirectionId") Long majorDirectionId);

    @Select("SELECT COUNT(*) FROM grade_major_directions " +
            "WHERE grade_id = #{gradeId} AND major_direction_id = #{majorDirectionId} AND deleted = 0")
    int countByGradeIdAndMajorDirectionId(
            @Param("gradeId") Long gradeId,
            @Param("majorDirectionId") Long majorDirectionId);

    @Select("SELECT COUNT(*) FROM grade_major_directions WHERE grade_id = #{gradeId} AND deleted = 0")
    int countByGradeId(@Param("gradeId") Long gradeId);

    @Select("SELECT COUNT(*) FROM grade_major_directions WHERE major_direction_id = #{majorDirectionId} AND deleted = 0")
    int countByMajorDirectionId(@Param("majorDirectionId") Long majorDirectionId);

    @Delete("UPDATE grade_major_directions SET deleted = 1 WHERE grade_id = #{gradeId} AND deleted = 0")
    void softDeleteByGradeId(@Param("gradeId") Long gradeId);

    @Delete("UPDATE grade_major_directions SET deleted = 1 WHERE major_direction_id = #{majorDirectionId} AND deleted = 0")
    void softDeleteByMajorDirectionId(@Param("majorDirectionId") Long majorDirectionId);
}
