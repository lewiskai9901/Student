package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GradeSchemeMapper extends BaseMapper<GradeSchemePO> {

    @Select("SELECT * FROM insp_grade_schemes WHERE (tenant_id = #{tenantId} OR is_system = 1) AND deleted = 0 ORDER BY is_system DESC, id")
    List<GradeSchemePO> findByTenantIdOrSystem(@Param("tenantId") Long tenantId);
}
