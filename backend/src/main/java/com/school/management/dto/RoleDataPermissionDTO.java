package com.school.management.dto;

import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.DataScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色数据权限DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色数据权限配置")
public class RoleDataPermissionDTO {

    @Schema(description = "模块编码", example = "student")
    private String moduleCode;

    @Schema(description = "模块名称", example = "学生管理")
    private String moduleName;

    @Schema(description = "数据范围: 1=全部, 2=本部门, 3=本年级, 4=本班级, 5=仅本人")
    private Integer dataScope;

    @Schema(description = "数据范围名称", example = "本班级")
    private String dataScopeName;

    @Schema(description = "自定义部门ID列表(逗号分隔)")
    private String customDeptIds;

    @Schema(description = "自定义班级ID列表(逗号分隔)")
    private String customClassIds;

    /**
     * 模块定义
     * @deprecated 使用 {@link DataModule} 代替
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public enum Module {
        STUDENT("student", "学生管理"),
        CLASS("class", "班级管理"),
        DORMITORY("dormitory", "宿舍管理"),
        CHECK_RECORD("check_record", "检查记录"),
        APPEAL("appeal", "申诉管理"),
        EVALUATION("evaluation", "评价管理");

        private final String code;
        private final String name;

        Module(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public static String getNameByCode(String code) {
            // 先尝试从DDD DataModule获取
            try {
                return DataModule.fromCode(code).getName();
            } catch (IllegalArgumentException e) {
                // 回退到旧枚举
                for (Module m : values()) {
                    if (m.code.equals(code)) {
                        return m.name;
                    }
                }
                return code;
            }
        }
    }

    /**
     * 数据范围定义
     * @deprecated 使用 {@link DataScope#getDisplayName()} 代替
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static String getDataScopeName(Integer dataScope) {
        if (dataScope == null) return "未配置";
        try {
            return DataScope.fromCode(dataScope).getDisplayName();
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * V2 API: 模块权限配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "模块权限配置")
    public static class ModulePermission {
        @Schema(description = "模块代码", example = "student")
        private String moduleCode;

        @Schema(description = "数据范围代码", example = "all")
        private String scopeCode;

        @Schema(description = "自定义组织单元ID列表(当scope为custom时有效)")
        private List<Long> customOrgUnitIds;

        /**
         * 获取DataModule枚举
         */
        public DataModule getModule() {
            return DataModule.fromCode(moduleCode);
        }

        /**
         * 获取DataScope枚举
         */
        public DataScope getScope() {
            return DataScope.fromCode(scopeCode);
        }
    }

    /**
     * V2 API: 完整角色数据权限配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "角色数据权限完整配置")
    public static class RolePermissionConfig {
        @Schema(description = "角色ID")
        private Long roleId;

        @Schema(description = "角色名称")
        private String roleName;

        @Schema(description = "模块权限列表")
        private List<ModulePermission> modulePermissions;
    }
}
