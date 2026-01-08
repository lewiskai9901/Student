package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.InspectorPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 打分人员权限配置Mapper
 */
@Mapper
public interface InspectorPermissionMapper extends BaseMapper<InspectorPermission> {

    /**
     * 根据打分人员ID查询权限配置
     */
    List<InspectorPermission> selectByInspectorId(@Param("inspectorId") Long inspectorId);

    /**
     * 根据计划ID和用户ID查询权限配置
     */
    List<InspectorPermission> selectByPlanIdAndUserId(@Param("planId") Long planId, @Param("userId") Long userId);

    /**
     * 删除打分人员的所有权限配置
     */
    int deleteByInspectorId(@Param("inspectorId") Long inspectorId);

    /**
     * 根据计划ID查询所有打分人员的权限配置
     */
    List<InspectorPermission> selectByPlanId(@Param("planId") Long planId);
}
