package com.school.management.casbin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限审计日志实体
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("permission_audit_log")
public class PermissionAuditLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型: GRANT/REVOKE/MODIFY
     */
    private String actionType;

    /**
     * 目标类型: ROLE/USER/SCOPE/POLICY
     */
    private String targetType;

    /**
     * 目标ID
     */
    private String targetId;

    /**
     * 变更前的值
     */
    private String oldValue;

    /**
     * 变更后的值
     */
    private String newValue;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;
}
