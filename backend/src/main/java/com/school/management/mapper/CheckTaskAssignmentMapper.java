package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.CheckTaskAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 检查任务分配Mapper
 */
@Mapper
public interface CheckTaskAssignmentMapper extends BaseMapper<CheckTaskAssignment> {

    /**
     * 分页查询用户的检查任务列表
     */
    IPage<CheckTaskAssignment> selectPageByUserId(
            Page<CheckTaskAssignment> page,
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("planId") Long planId
    );

    /**
     * 查询日常检查的所有任务分配
     */
    List<CheckTaskAssignment> selectByDailyCheckId(@Param("dailyCheckId") Long dailyCheckId);

    /**
     * 查询用户在某个日常检查中的任务
     */
    CheckTaskAssignment selectByDailyCheckIdAndUserId(
            @Param("dailyCheckId") Long dailyCheckId,
            @Param("userId") Long userId
    );

    /**
     * 统计用户的待处理任务数量
     */
    int countPendingByUserId(@Param("userId") Long userId);
}
