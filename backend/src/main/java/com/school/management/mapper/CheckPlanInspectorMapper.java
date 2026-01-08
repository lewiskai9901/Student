package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckPlanInspector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 检查计划打分人员Mapper
 */
@Mapper
public interface CheckPlanInspectorMapper extends BaseMapper<CheckPlanInspector> {

    /**
     * 查询计划的打分人员列表（包含用户信息）
     */
    List<CheckPlanInspector> selectByPlanIdWithUser(@Param("planId") Long planId);

    /**
     * 检查用户是否已是该计划的打分人员
     */
    int countByPlanIdAndUserId(@Param("planId") Long planId, @Param("userId") Long userId);
}
