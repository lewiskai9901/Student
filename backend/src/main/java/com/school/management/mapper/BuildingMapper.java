package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Building;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 楼宇Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {

    /**
     * 分页查询楼宇列表
     *
     * @param page 分页对象
     * @param buildingType 楼宇类型
     * @param buildingNo 楼号
     * @param buildingName 楼宇名称
     * @param status 状态
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT b.*, " +
            "CASE b.building_type " +
            "  WHEN 1 THEN (SELECT COUNT(*) FROM classrooms c WHERE c.building_id = b.id AND c.deleted = 0) " +
            "  WHEN 2 THEN (SELECT COUNT(*) FROM dormitories d WHERE d.building_id = b.id AND d.deleted = 0) " +
            "  ELSE 0 END as roomCount, " +
            "CASE b.building_type " +
            "  WHEN 1 THEN '教学楼' " +
            "  WHEN 2 THEN '宿舍楼' " +
            "  WHEN 3 THEN '办公楼' " +
            "  ELSE '未知' END as buildingTypeName, " +
            "(SELECT GROUP_CONCAT(DISTINCT d.dept_name) FROM building_departments bd " +
            "  JOIN departments d ON bd.department_id = d.id " +
            "  WHERE bd.building_id = b.id) as departmentNames " +
            "FROM buildings b " +
            "WHERE b.deleted = 0 " +
            "<if test='buildingType != null'> AND b.building_type = #{buildingType} </if>" +
            "<if test='buildingNo != null and buildingNo != \"\"'> AND b.building_no LIKE CONCAT('%', #{buildingNo}, '%') </if>" +
            "<if test='buildingName != null and buildingName != \"\"'> AND b.building_name LIKE CONCAT('%', #{buildingName}, '%') </if>" +
            "<if test='status != null'> AND b.status = #{status} </if>" +
            "ORDER BY b.building_no ASC, b.created_at DESC" +
            "</script>")
    IPage<Building> selectBuildingPage(
            Page<Building> page,
            @Param("buildingType") Integer buildingType,
            @Param("buildingNo") String buildingNo,
            @Param("buildingName") String buildingName,
            @Param("status") Integer status
    );

    /**
     * 检查楼号是否存在
     *
     * @param buildingNo 楼号
     * @param excludeId 排除的ID
     * @return 数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM buildings " +
            "WHERE building_no = #{buildingNo} AND deleted = 0 " +
            "<if test='excludeId != null'> AND id != #{excludeId} </if>" +
            "</script>")
    int countByBuildingNo(@Param("buildingNo") String buildingNo, @Param("excludeId") Long excludeId);
}
