package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.OperationLogQueryRequest;
import com.school.management.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface OperationLogService {

    /**
     * 保存操作日志
     *
     * @param operationLog 操作日志
     */
    void saveLog(OperationLog operationLog);

    /**
     * 分页查询操作日志
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<OperationLog> queryPage(OperationLogQueryRequest request);

    /**
     * 根据ID获取操作日志
     *
     * @param id 日志ID
     * @return 操作日志
     */
    OperationLog getById(Long id);

    /**
     * 删除操作日志
     *
     * @param id 日志ID
     */
    void deleteById(Long id);

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     */
    void deleteBatch(List<Long> ids);

    /**
     * 清空操作日志
     *
     * @param beforeDate 清空指定日期之前的日志
     */
    void clearLogs(LocalDateTime beforeDate);
}
