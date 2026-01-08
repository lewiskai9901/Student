package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 楼宇 Mapper 接口 (DDD架构)
 */
@Mapper
public interface AssetBuildingMapper extends BaseMapper<BuildingPO> {

    @Select("SELECT * FROM buildings WHERE building_no = #{buildingNo} AND deleted = 0")
    BuildingPO selectByBuildingNo(@Param("buildingNo") String buildingNo);

    @Select("SELECT * FROM buildings WHERE building_type = #{buildingType} AND deleted = 0")
    List<BuildingPO> selectByType(@Param("buildingType") Integer buildingType);

    @Select("SELECT * FROM buildings WHERE status = 1 AND deleted = 0")
    List<BuildingPO> selectAllActive();

    @Select("SELECT * FROM buildings WHERE building_type = 2 AND status = 1 AND deleted = 0")
    List<BuildingPO> selectDormitoryBuildings();

    @Select("SELECT COUNT(*) FROM buildings WHERE building_no = #{buildingNo} AND deleted = 0")
    long countByBuildingNo(@Param("buildingNo") String buildingNo);
}
