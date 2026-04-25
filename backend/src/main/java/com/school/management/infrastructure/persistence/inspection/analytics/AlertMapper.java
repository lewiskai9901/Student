package com.school.management.infrastructure.persistence.inspection.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertMapper extends BaseMapper<AlertPO> {

    @DataPermission(module = "inspection_alert", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_alerts WHERE status = #{status} AND deleted = 0 ORDER BY triggered_at DESC")
    List<AlertPO> findByStatus(@Param("status") String status);

    @DataPermission(module = "inspection_alert", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_alerts WHERE deleted = 0 ORDER BY triggered_at DESC LIMIT #{limit}")
    List<AlertPO> findRecent(@Param("limit") int limit);

    @DataPermission(module = "inspection_alert", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT COUNT(*) FROM insp_alerts WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);
}
