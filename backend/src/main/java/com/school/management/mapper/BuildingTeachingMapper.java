package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.BuildingTeaching;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 教学楼扩展Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface BuildingTeachingMapper extends BaseMapper<BuildingTeaching> {

    /**
     * 根据楼宇ID查询教学楼信息
     *
     * @param buildingId 楼宇ID
     * @return 教学楼信息
     */
    @Select("SELECT bt.*, b.building_name " +
            "FROM building_teachings bt " +
            "INNER JOIN buildings b ON bt.building_id = b.id " +
            "WHERE bt.building_id = #{buildingId}")
    BuildingTeaching selectByBuildingId(@Param("buildingId") Long buildingId);
}
