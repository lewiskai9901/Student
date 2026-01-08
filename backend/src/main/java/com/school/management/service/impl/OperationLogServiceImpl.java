package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.OperationLogQueryRequest;
import com.school.management.entity.OperationLog;
import com.school.management.mapper.OperationLogMapper;
import com.school.management.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    @Async
    public void saveLog(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    @Override
    public IPage<OperationLog> queryPage(OperationLogQueryRequest request) {
        request.validatePagination();

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 用户名模糊查询
        if (StringUtils.hasText(request.getUsername())) {
            wrapper.like(OperationLog::getUsername, request.getUsername());
        }

        // 真实姓名模糊查询
        if (StringUtils.hasText(request.getRealName())) {
            wrapper.like(OperationLog::getRealName, request.getRealName());
        }

        // 操作模块精确查询
        if (StringUtils.hasText(request.getOperationModule())) {
            wrapper.eq(OperationLog::getOperationModule, request.getOperationModule());
        }

        // 操作类型精确查询
        if (StringUtils.hasText(request.getOperationType())) {
            wrapper.eq(OperationLog::getOperationType, request.getOperationType());
        }

        // 操作名称模糊查询
        if (StringUtils.hasText(request.getOperationName())) {
            wrapper.like(OperationLog::getOperationName, request.getOperationName());
        }

        // IP地址精确查询
        if (StringUtils.hasText(request.getIpAddress())) {
            wrapper.eq(OperationLog::getIpAddress, request.getIpAddress());
        }

        // 响应状态码精确查询
        if (request.getResponseStatus() != null) {
            wrapper.eq(OperationLog::getResponseStatus, request.getResponseStatus());
        }

        // 创建时间范围查询
        if (StringUtils.hasText(request.getCreatedAtStart())) {
            wrapper.ge(OperationLog::getCreatedAt, request.getCreatedAtStart());
        }
        if (StringUtils.hasText(request.getCreatedAtEnd())) {
            wrapper.le(OperationLog::getCreatedAt, request.getCreatedAtEnd());
        }

        // 排序
        if ("asc".equalsIgnoreCase(request.getSortDir())) {
            wrapper.orderByAsc(OperationLog::getCreatedAt);
        } else {
            wrapper.orderByDesc(OperationLog::getCreatedAt);
        }

        Page<OperationLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        return operationLogMapper.selectPage(page, wrapper);
    }

    @Override
    public OperationLog getById(Long id) {
        return operationLogMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        operationLogMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        operationLogMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearLogs(LocalDateTime beforeDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(OperationLog::getCreatedAt, beforeDate);
        operationLogMapper.delete(wrapper);
        log.info("清空{}之前的操作日志", beforeDate);
    }
}
