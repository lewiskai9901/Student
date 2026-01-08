package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.DormitoryBuildingManager;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 宿舍楼管理员关联Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DormitoryBuildingManagerMapper extends BaseMapper<DormitoryBuildingManager> {

    /**
     * 根据楼宇ID查询管理员列表
     *
     * @param buildingId 楼宇ID
     * @return 管理员列表
     */
    @Select("SELECT dbm.*, u.real_name as userName, u.employee_no as employeeNo " +
            "FROM dormitory_building_managers dbm " +
            "INNER JOIN users u ON dbm.user_id = u.id " +
            "WHERE dbm.building_id = #{buildingId}")
    List<DormitoryBuildingManager> selectByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据用户ID查询管理的楼宇ID列表
     *
     * @param userId 用户ID
     * @return 楼宇ID列表
     */
    @Select("SELECT building_id FROM dormitory_building_managers WHERE user_id = #{userId}")
    List<Long> selectBuildingIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据楼宇ID统计管理员数量
     *
     * @param buildingId 楼宇ID
     * @return 管理员数量
     */
    @Select("SELECT COUNT(*) FROM dormitory_building_managers WHERE building_id = #{buildingId}")
    long countByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 检查用户是否为指定楼宇的管理员
     *
     * @param buildingId 楼宇ID
     * @param userId 用户ID
     * @return 是否为管理员
     */
    @Select("SELECT COUNT(*) > 0 FROM dormitory_building_managers WHERE building_id = #{buildingId} AND user_id = #{userId}")
    boolean isManager(@Param("buildingId") Long buildingId, @Param("userId") Long userId);
}
