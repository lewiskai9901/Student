package com.school.management.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.util.SecurityUtils;
import com.school.management.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.school.management.annotation.OperationLog;
import com.school.management.service.OperationLogService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志AOP切面
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 定义切入点: 所有带@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.school.management.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知: 记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取@OperationLog注解
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 创建操作日志实体
        com.school.management.entity.OperationLog operationLog = new com.school.management.entity.OperationLog();
        operationLog.setOperationModule(annotation.module());
        operationLog.setOperationType(annotation.type());
        operationLog.setOperationName(annotation.name());

        // 获取当前用户信息
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    operationLog.setUserId(userDetails.getUserId());
                    operationLog.setUsername(userDetails.getUsername());
                    operationLog.setRealName(userDetails.getRealName());
                } else if (principal instanceof String) {
                    operationLog.setUsername((String) principal);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败", e);
        }

        // 获取请求信息
        try {
            HttpServletRequest request = WebUtils.getRequest();
            if (request != null) {
                operationLog.setRequestMethod(request.getMethod());
                operationLog.setRequestUrl(request.getRequestURI());
                operationLog.setIpAddress(WebUtils.getIpAddress(request));

                String userAgent = request.getHeader("User-Agent");
                operationLog.setUserAgent(userAgent);
                operationLog.setBrowser(WebUtils.getBrowser(userAgent));
                operationLog.setOs(WebUtils.getOS(userAgent));

                // 记录请求参数
                if (annotation.recordParams()) {
                    try {
                        Map<String, String[]> paramMap = request.getParameterMap();
                        if (!paramMap.isEmpty()) {
                            // 转换为简单格式
                            Map<String, Object> params = new HashMap<>();
                            paramMap.forEach((key, value) -> {
                                if (value.length == 1) {
                                    params.put(key, value[0]);
                                } else {
                                    params.put(key, value);
                                }
                            });

                            // 过滤敏感参数
                            params.remove("password");
                            params.remove("oldPassword");
                            params.remove("newPassword");

                            String paramsJson = objectMapper.writeValueAsString(params);
                            // 限制参数长度
                            if (paramsJson.length() > 2000) {
                                paramsJson = paramsJson.substring(0, 2000) + "...";
                            }
                            operationLog.setRequestParams(paramsJson);
                        }
                    } catch (Exception e) {
                        log.warn("记录请求参数失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败", e);
        }

        // 执行目标方法
        Object result = null;
        Integer responseStatus = 200;
        try {
            result = joinPoint.proceed();
            responseStatus = 200;
        } catch (Exception e) {
            responseStatus = 500;
            operationLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 记录响应信息
            long endTime = System.currentTimeMillis();
            operationLog.setResponseTime((int) (endTime - startTime));
            operationLog.setResponseStatus(responseStatus);

            // 异步保存日志
            try {
                operationLogService.saveLog(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }
}
