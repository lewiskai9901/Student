package com.school.management.domain.access.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色自定义数据范围实体
 * 用于存储角色对特定模块的自定义组织单元访问权限
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleCustomScope {

    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 模块代码
     */
    private String moduleCode;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    public RoleCustomScope(Long roleId, String moduleCode, Long orgUnitId) {
        this.roleId = roleId;
        this.moduleCode = moduleCode;
        this.orgUnitId = orgUnitId;
    }

    /**
     * 获取DataModule枚举
     */
    public DataModule getModule() {
        return DataModule.fromCode(moduleCode);
    }
}
