package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface PositionMapper extends BaseMapper<PositionPO> {
    @Select("SELECT * FROM positions WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY sort_order")
    List<PositionPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    @Select("SELECT * FROM positions WHERE position_code = #{code} AND deleted = 0")
    PositionPO findByPositionCode(@Param("code") String code);

    @Select("SELECT * FROM positions WHERE reports_to_id = #{reportsToId} AND deleted = 0 ORDER BY sort_order")
    List<PositionPO> findByReportsToId(@Param("reportsToId") Long reportsToId);

    /**
     * 逻辑删除某组织下所有岗位
     */
    @Update("UPDATE positions SET deleted = id, updated_at = NOW() WHERE org_unit_id = #{orgUnitId} AND deleted = 0")
    int softDeleteByOrgUnitId(@Param("orgUnitId") Long orgUnitId);
}
