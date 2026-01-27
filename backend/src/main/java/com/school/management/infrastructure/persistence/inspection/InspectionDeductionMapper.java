package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for inspection deductions.
 */
@Mapper
public interface InspectionDeductionMapper extends BaseMapper<InspectionDeductionPO> {

    @Select("SELECT * FROM inspection_deductions WHERE class_record_id = #{classRecordId} AND deleted = 0")
    List<InspectionDeductionPO> findByClassRecordId(@Param("classRecordId") Long classRecordId);

    @Select("SELECT * FROM inspection_deductions WHERE session_id = #{sessionId} AND deleted = 0")
    List<InspectionDeductionPO> findBySessionId(@Param("sessionId") Long sessionId);
}
