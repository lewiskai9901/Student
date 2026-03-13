package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IoTSensorMapper extends BaseMapper<IoTSensorPO> {

    @Select("SELECT * FROM insp_iot_sensors WHERE sensor_code = #{sensorCode} AND deleted = 0 LIMIT 1")
    IoTSensorPO findBySensorCode(@Param("sensorCode") String sensorCode);

    @Select("SELECT * FROM insp_iot_sensors WHERE is_active = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<IoTSensorPO> findActive();

    @Select("SELECT * FROM insp_iot_sensors WHERE place_id = #{placeId} AND deleted = 0 ORDER BY created_at DESC")
    List<IoTSensorPO> findByPlaceId(@Param("placeId") Long placeId);
}
