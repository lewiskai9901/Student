package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.domain.access.model.DataScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色数据权限配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role_data_permissions")
public class RoleDataPermission extends BaseEntity {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 模块编码: student, class, dormitory, check_record, appeal, evaluation
     */
    private String moduleCode;

    /**
     * 数据范围: 1=全部, 2=本部门, 3=本年级, 4=本班级, 5=仅本人
     */
    private Integer dataScope;

    /**
     * 自定义部门ID列表(逗号分隔)
     */
    private String customDeptIds;

    /**
     * 自定义班级ID列表(逗号分隔)
     */
    private String customClassIds;

    /**
     * 获取数据范围枚举
     */
    public DataScope getDataScopeEnum() {
        return DataScope.fromCode(this.dataScope);
    }
}
