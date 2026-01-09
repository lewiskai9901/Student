package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专业方向 Mapper (DDD infrastructure layer)
 */
@Mapper
public interface MajorDirectionPersistenceMapper extends BaseMapper<MajorDirectionPO> {

    @Select("SELECT * FROM major_directions WHERE major_id = #{majorId} AND deleted = 0 ORDER BY id")
    List<MajorDirectionPO> findByMajorId(@Param("majorId") Long majorId);

    @Select("SELECT * FROM major_directions WHERE direction_code = #{directionCode} AND deleted = 0")
    MajorDirectionPO findByDirectionCode(@Param("directionCode") String directionCode);

    @Select("SELECT * FROM major_directions WHERE status = 1 AND deleted = 0 ORDER BY id")
    List<MajorDirectionPO> findAllEnabled();

    @Select("SELECT COUNT(*) FROM major_directions WHERE major_id = #{majorId} AND deleted = 0")
    int countByMajorId(@Param("majorId") Long majorId);

    @Select("SELECT m.department_id FROM major_directions md " +
            "JOIN majors m ON md.major_id = m.id " +
            "WHERE md.id = #{directionId} AND md.deleted = 0 AND m.deleted = 0")
    Long findOrgUnitIdByDirectionId(@Param("directionId") Long directionId);
}
