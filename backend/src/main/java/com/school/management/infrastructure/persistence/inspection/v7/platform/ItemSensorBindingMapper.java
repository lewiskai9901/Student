package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemSensorBindingMapper extends BaseMapper<ItemSensorBindingPO> {

    @Select("SELECT * FROM insp_item_sensor_bindings WHERE template_item_id = #{templateItemId} AND deleted = 0")
    List<ItemSensorBindingPO> findByTemplateItemId(@Param("templateItemId") Long templateItemId);

    @Select("SELECT * FROM insp_item_sensor_bindings WHERE sensor_id = #{sensorId} AND deleted = 0")
    List<ItemSensorBindingPO> findBySensorId(@Param("sensorId") Long sensorId);
}
