package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SensorReadingMapper extends BaseMapper<SensorReadingPO> {

    @Select("SELECT * FROM insp_sensor_readings WHERE sensor_id = #{sensorId} ORDER BY recorded_at DESC LIMIT #{limit}")
    List<SensorReadingPO> findBySensorId(@Param("sensorId") Long sensorId, @Param("limit") int limit);

    @Select("SELECT * FROM insp_sensor_readings WHERE sensor_id = #{sensorId} AND recorded_at BETWEEN #{from} AND #{to} ORDER BY recorded_at ASC")
    List<SensorReadingPO> findBySensorIdBetween(@Param("sensorId") Long sensorId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
}
