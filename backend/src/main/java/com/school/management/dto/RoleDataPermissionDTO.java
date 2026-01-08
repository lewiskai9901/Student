package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     */
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
            for (Module m : values()) {
                if (m.code.equals(code)) {
                    return m.name;
                }
            }
            return code;
        }
    }

    /**
     * 数据范围定义
     */
    public static String getDataScopeName(Integer dataScope) {
        if (dataScope == null) return "未配置";
        switch (dataScope) {
            case 1: return "全部数据";
            case 2: return "本部门";
            case 3: return "本年级";
            case 4: return "本班级";
            case 5: return "仅本人";
            default: return "未知";
        }
    }
}
