package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for checklist responses.
 */
@Mapper
public interface ChecklistResponseMapper extends BaseMapper<ChecklistResponsePO> {

    @Select("SELECT * FROM checklist_responses WHERE class_record_id = #{classRecordId}")
    List<ChecklistResponsePO> findByClassRecordId(@Param("classRecordId") Long classRecordId);

    @Select("SELECT * FROM checklist_responses WHERE session_id = #{sessionId}")
    List<ChecklistResponsePO> findBySessionId(@Param("sessionId") Long sessionId);
}
