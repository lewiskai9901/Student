package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_logs")
public class OperationLog extends BaseEntity {

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 操作模块
     */
    private String operationModule;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作名称
     */
    private String operationName;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;
}
