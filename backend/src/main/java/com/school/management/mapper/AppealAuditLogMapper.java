package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.AppealAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 申诉审计日志Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface AppealAuditLogMapper extends BaseMapper<AppealAuditLog> {

    /**
     * 查询申诉的所有审计日志
     *
     * @param appealId 申诉ID
     * @return 审计日志列表
     */
    List<AppealAuditLog> selectByAppealId(@Param("appealId") Long appealId);

    /**
     * 查询指定操作类型的日志
     *
     * @param appealId 申诉ID
     * @param actionType 操作类型
     * @return 审计日志列表
     */
    List<AppealAuditLog> selectByAppealAndAction(
            @Param("appealId") Long appealId,
            @Param("actionType") Integer actionType
    );

    /**
     * 批量插入审计日志
     *
     * @param logs 审计日志列表
     * @return 插入行数
     */
    int batchInsert(@Param("logs") List<AppealAuditLog> logs);
}
