package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for class scores.
 */
@Mapper
public interface ClassScoreMapper extends BaseMapper<ClassScorePO> {

    @Select("SELECT * FROM class_scores WHERE record_id = #{recordId} AND deleted = 0 ORDER BY class_name")
    List<ClassScorePO> findByRecordId(@Param("recordId") Long recordId);

    @Select("SELECT * FROM class_scores WHERE record_id = #{recordId} AND class_id = #{classId} AND deleted = 0")
    ClassScorePO findByRecordIdAndClassId(
        @Param("recordId") Long recordId,
        @Param("classId") Long classId);

    @Select("SELECT * FROM class_scores WHERE class_id = #{classId} AND deleted = 0 ORDER BY created_at DESC")
    List<ClassScorePO> findByClassId(@Param("classId") Long classId);
}
