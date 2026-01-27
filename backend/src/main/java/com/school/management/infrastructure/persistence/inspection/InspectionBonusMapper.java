package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for inspection bonuses.
 */
@Mapper
public interface InspectionBonusMapper extends BaseMapper<InspectionBonusPO> {

    @Select("SELECT * FROM inspection_bonuses WHERE class_record_id = #{classRecordId} AND deleted = 0")
    List<InspectionBonusPO> findByClassRecordId(@Param("classRecordId") Long classRecordId);

    @Select("SELECT * FROM inspection_bonuses WHERE session_id = #{sessionId} AND deleted = 0")
    List<InspectionBonusPO> findBySessionId(@Param("sessionId") Long sessionId);
}
