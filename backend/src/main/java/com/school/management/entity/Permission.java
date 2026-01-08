package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 权限实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permissions")
public class Permission extends BaseEntity {

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限描述
     */
    private String permissionDesc;

    /**
     * 资源类型: 1菜单 2按钮 3接口
     */
    private Integer resourceType;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 前端路由路径
     */
    private String path;

    /**
     * 前端组件路径
     */
    private String component;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 子权限列表(非数据库字段)
     */
    @TableField(exist = false)
    private List<Permission> children;
}