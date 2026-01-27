package com.school.management.mapper.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.teaching.AcademicEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 校历事件Mapper
 */
@Mapper
public interface AcademicEventMapper extends BaseMapper<AcademicEvent> {

    /**
     * 根据学年ID查询事件列表
     */
    List<AcademicEvent> selectByYearId(@Param("yearId") Long yearId);

    /**
     * 根据学期ID查询事件列表
     */
    List<AcademicEvent> selectBySemesterId(@Param("semesterId") Long semesterId);
}
