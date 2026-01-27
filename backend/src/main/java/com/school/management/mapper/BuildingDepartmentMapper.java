package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.BuildingDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 楼宇部门关联Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface BuildingDepartmentMapper extends BaseMapper<BuildingDepartment> {

    /**
     * 根据楼宇ID查询组织单元列表
     *
     * @param buildingId 楼宇ID
     * @return 组织单元列表
     */
    @Select("SELECT bd.*, o.unit_name as orgUnitName " +
            "FROM building_departments bd " +
            "INNER JOIN org_units o ON bd.org_unit_id = o.id " +
            "WHERE bd.building_id = #{buildingId}")
    List<BuildingDepartment> selectByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据楼宇ID查询组织单元ID列表
     *
     * @param buildingId 楼宇ID
     * @return 组织单元ID列表
     */
    @Select("SELECT org_unit_id FROM building_departments WHERE building_id = #{buildingId}")
    List<Long> selectOrgUnitIdsByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据组织单元ID查询楼宇ID列表
     *
     * @param orgUnitId 组织单元ID
     * @return 楼宇ID列表
     */
    @Select("SELECT building_id FROM building_departments WHERE org_unit_id = #{orgUnitId}")
    List<Long> selectBuildingIdsByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 检查楼宇是否关联了指定组织单元
     *
     * @param buildingId 楼宇ID
     * @param orgUnitId 组织单元ID
     * @return 是否关联
     */
    @Select("SELECT COUNT(*) > 0 FROM building_departments WHERE building_id = #{buildingId} AND org_unit_id = #{orgUnitId}")
    boolean exists(@Param("buildingId") Long buildingId, @Param("orgUnitId") Long orgUnitId);
}
