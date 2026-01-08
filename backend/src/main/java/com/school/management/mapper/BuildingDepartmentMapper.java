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
     * 根据楼宇ID查询部门列表
     *
     * @param buildingId 楼宇ID
     * @return 部门列表
     */
    @Select("SELECT bd.*, d.dept_name as departmentName " +
            "FROM building_departments bd " +
            "INNER JOIN departments d ON bd.department_id = d.id " +
            "WHERE bd.building_id = #{buildingId}")
    List<BuildingDepartment> selectByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据楼宇ID查询部门ID列表
     *
     * @param buildingId 楼宇ID
     * @return 部门ID列表
     */
    @Select("SELECT department_id FROM building_departments WHERE building_id = #{buildingId}")
    List<Long> selectDepartmentIdsByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据部门ID查询楼宇ID列表
     *
     * @param departmentId 部门ID
     * @return 楼宇ID列表
     */
    @Select("SELECT building_id FROM building_departments WHERE department_id = #{departmentId}")
    List<Long> selectBuildingIdsByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 检查楼宇是否关联了指定部门
     *
     * @param buildingId 楼宇ID
     * @param departmentId 部门ID
     * @return 是否关联
     */
    @Select("SELECT COUNT(*) > 0 FROM building_departments WHERE building_id = #{buildingId} AND department_id = #{departmentId}")
    boolean exists(@Param("buildingId") Long buildingId, @Param("departmentId") Long departmentId);
}
