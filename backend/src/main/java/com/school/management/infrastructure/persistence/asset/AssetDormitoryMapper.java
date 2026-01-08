package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 宿舍 Mapper 接口 (DDD架构)
 */
@Mapper
public interface AssetDormitoryMapper extends BaseMapper<DormitoryPO> {

    @Select("SELECT * FROM dormitories WHERE dormitory_no = #{dormitoryNo} AND deleted = 0")
    DormitoryPO selectByDormitoryNo(@Param("dormitoryNo") String dormitoryNo);

    @Select("SELECT * FROM dormitories WHERE building_id = #{buildingId} AND deleted = 0")
    List<DormitoryPO> selectByBuildingId(@Param("buildingId") Long buildingId);

    @Select("SELECT * FROM dormitories WHERE building_id = #{buildingId} AND floor_number = #{floorNumber} AND deleted = 0")
    List<DormitoryPO> selectByBuildingIdAndFloor(@Param("buildingId") Long buildingId, @Param("floorNumber") Integer floorNumber);

    @Select("SELECT * FROM dormitories WHERE department_id = #{departmentId} AND deleted = 0")
    List<DormitoryPO> selectByDepartmentId(@Param("departmentId") Long departmentId);

    @Select("SELECT * FROM dormitories WHERE building_id = #{buildingId} AND gender_type = #{genderType} " +
            "AND status = 1 AND bed_count > occupied_beds AND deleted = 0")
    List<DormitoryPO> selectAvailable(@Param("buildingId") Long buildingId, @Param("genderType") Integer genderType);

    @Select("SELECT COUNT(*) FROM dormitories WHERE dormitory_no = #{dormitoryNo} AND deleted = 0")
    long countByDormitoryNo(@Param("dormitoryNo") String dormitoryNo);

    @Select("SELECT COUNT(*) FROM dormitories WHERE building_id = #{buildingId} AND deleted = 0")
    long countByBuildingId(@Param("buildingId") Long buildingId);

    @Select("SELECT COUNT(*) FROM dormitories WHERE department_id = #{departmentId} AND deleted = 0")
    long countByDepartmentId(@Param("departmentId") Long departmentId);
}
