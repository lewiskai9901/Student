package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专业 Mapper (DDD infrastructure layer)
 */
@Mapper
public interface MajorPersistenceMapper extends BaseMapper<MajorPO> {

    @Select("SELECT * FROM majors WHERE major_code = #{majorCode} AND deleted = 0")
    MajorPO findByMajorCode(@Param("majorCode") String majorCode);

    @Select("SELECT * FROM majors WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY sort_order, id")
    List<MajorPO> findByDepartmentId(@Param("orgUnitId") Long orgUnitId);

    @Select("SELECT * FROM majors WHERE status = 1 AND deleted = 0 ORDER BY sort_order, id")
    List<MajorPO> findAllEnabled();

    @Select("SELECT COUNT(*) FROM majors WHERE org_unit_id = #{orgUnitId} AND deleted = 0")
    int countByDepartmentId(@Param("orgUnitId") Long orgUnitId);

    @Select("SELECT COUNT(*) FROM majors WHERE major_code = #{majorCode} AND deleted = 0")
    int countByMajorCode(@Param("majorCode") String majorCode);
}
