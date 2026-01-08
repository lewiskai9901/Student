package com.school.management.infrastructure.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.audit.AuditLog;
import com.school.management.infrastructure.audit.AuditLogService;
import com.school.management.infrastructure.audit.AuditLogPO;
import com.school.management.infrastructure.audit.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void log(AuditLog auditLog) {
        try {
            // 填充请求上下文信息
            enrichWithRequestContext(auditLog);

            // 填充用户信息
            enrichWithUserContext(auditLog);

            // 转换并保存
            AuditLogPO po = toPO(auditLog);
            auditLogMapper.insert(po);

            log.debug("Audit log saved: {} {} {}",
                      auditLog.getAction(), auditLog.getResourceType(), auditLog.getResourceId());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    @Override
    public void logCreate(String resourceType, String resourceId, String resourceName,
                          Object afterData, String description) {
        AuditLog auditLog = AuditLog.builder()
                .action(AuditLog.AuditAction.CREATE)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .afterData(toJson(afterData))
                .description(description)
                .result(AuditLog.AuditResult.SUCCESS)
                .occurredAt(LocalDateTime.now())
                .build();
        log(auditLog);
    }

    @Override
    public void logUpdate(String resourceType, String resourceId, String resourceName,
                          Object beforeData, Object afterData, String description) {
        AuditLog auditLog = AuditLog.builder()
                .action(AuditLog.AuditAction.UPDATE)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .beforeData(toJson(beforeData))
                .afterData(toJson(afterData))
                .description(description)
                .result(AuditLog.AuditResult.SUCCESS)
                .occurredAt(LocalDateTime.now())
                .build();
        log(auditLog);
    }

    @Override
    public void logDelete(String resourceType, String resourceId, String resourceName,
                          Object beforeData, String description) {
        AuditLog auditLog = AuditLog.builder()
                .action(AuditLog.AuditAction.DELETE)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .beforeData(toJson(beforeData))
                .description(description)
                .result(AuditLog.AuditResult.SUCCESS)
                .occurredAt(LocalDateTime.now())
                .build();
        log(auditLog);
    }

    @Override
    public void logRead(String resourceType, String resourceId, String resourceName) {
        AuditLog auditLog = AuditLog.builder()
                .action(AuditLog.AuditAction.READ)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .result(AuditLog.AuditResult.SUCCESS)
                .occurredAt(LocalDateTime.now())
                .build();
        log(auditLog);
    }

    @Override
    public void logLogin(Long userId, String username, boolean success, String errorMessage) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(AuditLog.AuditAction.LOGIN)
                .resourceType("USER")
                .resourceId(userId != null ? userId.toString() : null)
                .resourceName(username)
                .result(success ? AuditLog.AuditResult.SUCCESS : AuditLog.AuditResult.FAILURE)
                .errorMessage(errorMessage)
                .occurredAt(LocalDateTime.now())
                .build();
        log(auditLog);
    }

    @Override
    public void logLogout(Long userId, String username) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(AuditLog.AuditAction.LOGOUT)
                .resourceType("USER")
                .resourceId(userId.toString())
                .resourceName(username)
                .result(AuditLog.AuditResult.SUCCESS)
                .occurredAt(LocalDateTime.now())
                .build();
        log(auditLog);
    }

    @Override
    public List<AuditLog> query(AuditLogQuery query) {
        LambdaQueryWrapper<AuditLogPO> wrapper = buildQueryWrapper(query);

        Page<AuditLogPO> page = new Page<>(
                query.getPageNum() != null ? query.getPageNum() : 1,
                query.getPageSize() != null ? query.getPageSize() : 20
        );

        return auditLogMapper.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> countByAction(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(AuditLogPO::getOccurredAt, startTime)
               .le(AuditLogPO::getOccurredAt, endTime);

        List<AuditLogPO> logs = auditLogMapper.selectList(wrapper);

        return logs.stream()
                .collect(Collectors.groupingBy(
                        AuditLogPO::getAction,
                        Collectors.counting()
                ));
    }

    @Override
    public List<AuditLog> getRecentByUser(Long userId, int limit) {
        LambdaQueryWrapper<AuditLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLogPO::getUserId, userId)
               .orderByDesc(AuditLogPO::getOccurredAt)
               .last("LIMIT " + limit);

        return auditLogMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> getResourceHistory(String resourceType, String resourceId) {
        LambdaQueryWrapper<AuditLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLogPO::getResourceType, resourceType)
               .eq(AuditLogPO::getResourceId, resourceId)
               .orderByDesc(AuditLogPO::getOccurredAt);

        return auditLogMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private void enrichWithRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUri(request.getRequestURI());
                auditLog.setRequestMethod(request.getMethod());
            }
        } catch (Exception e) {
            log.debug("Cannot get request context", e);
        }
    }

    private void enrichWithUserContext(AuditLog auditLog) {
        if (auditLog.getUserId() == null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() != null) {
                    // 从认证信息获取用户ID和用户名
                    auditLog.setUsername(auth.getName());
                    // 如果 Principal 是自定义 UserDetails，可以获取更多信息
                }
            } catch (Exception e) {
                log.debug("Cannot get user context", e);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个IP（可能有多个代理）
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return obj.toString();
        }
    }

    private LambdaQueryWrapper<AuditLogPO> buildQueryWrapper(AuditLogQuery query) {
        LambdaQueryWrapper<AuditLogPO> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(AuditLogPO::getUserId, query.getUserId());
        }
        if (query.getUsername() != null) {
            wrapper.like(AuditLogPO::getUsername, query.getUsername());
        }
        if (query.getAction() != null) {
            wrapper.eq(AuditLogPO::getAction, query.getAction().name());
        }
        if (query.getResourceType() != null) {
            wrapper.eq(AuditLogPO::getResourceType, query.getResourceType());
        }
        if (query.getResourceId() != null) {
            wrapper.eq(AuditLogPO::getResourceId, query.getResourceId());
        }
        if (query.getResult() != null) {
            wrapper.eq(AuditLogPO::getResult, query.getResult().name());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(AuditLogPO::getOccurredAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(AuditLogPO::getOccurredAt, query.getEndTime());
        }
        if (query.getIpAddress() != null) {
            wrapper.eq(AuditLogPO::getIpAddress, query.getIpAddress());
        }

        wrapper.orderByDesc(AuditLogPO::getOccurredAt);

        return wrapper;
    }

    private AuditLogPO toPO(AuditLog domain) {
        AuditLogPO po = new AuditLogPO();
        po.setUserId(domain.getUserId());
        po.setUsername(domain.getUsername());
        po.setAction(domain.getAction() != null ? domain.getAction().name() : null);
        po.setResourceType(domain.getResourceType());
        po.setResourceId(domain.getResourceId());
        po.setResourceName(domain.getResourceName());
        po.setDescription(domain.getDescription());
        po.setBeforeData(domain.getBeforeData());
        po.setAfterData(domain.getAfterData());
        po.setIpAddress(domain.getIpAddress());
        po.setUserAgent(domain.getUserAgent());
        po.setRequestUri(domain.getRequestUri());
        po.setRequestMethod(domain.getRequestMethod());
        po.setResult(domain.getResult() != null ? domain.getResult().name() : null);
        po.setErrorMessage(domain.getErrorMessage());
        po.setOccurredAt(domain.getOccurredAt());
        po.setMetadata(toJson(domain.getMetadata()));
        return po;
    }

    private AuditLog toDomain(AuditLogPO po) {
        return AuditLog.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .username(po.getUsername())
                .action(po.getAction() != null ? AuditLog.AuditAction.valueOf(po.getAction()) : null)
                .resourceType(po.getResourceType())
                .resourceId(po.getResourceId())
                .resourceName(po.getResourceName())
                .description(po.getDescription())
                .beforeData(po.getBeforeData())
                .afterData(po.getAfterData())
                .ipAddress(po.getIpAddress())
                .userAgent(po.getUserAgent())
                .requestUri(po.getRequestUri())
                .requestMethod(po.getRequestMethod())
                .result(po.getResult() != null ? AuditLog.AuditResult.valueOf(po.getResult()) : null)
                .errorMessage(po.getErrorMessage())
                .occurredAt(po.getOccurredAt())
                .build();
    }
}
