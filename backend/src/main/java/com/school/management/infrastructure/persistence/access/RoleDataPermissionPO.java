package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色数据权限配置持久化对象
 */
@Data
@TableName("role_data_permissions")
public class RoleDataPermissionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private String moduleCode;

    private Integer dataScope;

    private String customDeptIds;

    private String customClassIds;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
