package com.school.management.casbin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.casbin.entity.PermissionAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限审计日志Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface PermissionAuditLogMapper extends BaseMapper<PermissionAuditLog> {

    /**
     * 查询指定时间范围内的日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<PermissionAuditLog> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定目标的日志
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 日志列表
     */
    List<PermissionAuditLog> selectByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId);
}
