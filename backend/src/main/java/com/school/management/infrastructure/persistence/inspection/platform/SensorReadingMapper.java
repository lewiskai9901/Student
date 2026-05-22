package com.school.management.infrastructure.persistence.inspection.platform;

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

    /**
     * 时间范围查询 — 强制 LIMIT 10000 安全上限, 防止宽时间窗返回海量行打爆内存.
     * 传感器读数高频写入, 单传感器一天可达数千行; 调用方需自行收窄时间窗.
     */
    @Select("SELECT * FROM insp_sensor_readings WHERE sensor_id = #{sensorId} AND recorded_at BETWEEN #{from} AND #{to} ORDER BY recorded_at ASC LIMIT 10000")
    List<SensorReadingPO> findBySensorIdBetween(@Param("sensorId") Long sensorId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
}
