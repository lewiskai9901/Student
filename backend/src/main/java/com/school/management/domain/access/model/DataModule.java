package com.school.management.domain.access.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据模块枚举 (V5)
 * 定义系统中可配置数据权限的业务模块
 */
public enum DataModule {

    // ==================== Organization Domain ====================
    STUDENT("student", "学生信息", "organization", 101),
    SCHOOL_CLASS("school_class", "班级管理", "organization", 102),
    ORG_UNIT("org_unit", "组织架构", "organization", 103),

    // ==================== Space Domain ====================
    DORMITORY_BUILDING("dormitory_building", "楼栋管理", "space", 201),
    DORMITORY_ROOM("dormitory_room", "房间管理", "space", 202),
    DORMITORY_CHECKIN("dormitory_checkin", "入住管理", "space", 203),

    // ==================== Inspection Domain ====================
    INSPECTION_TEMPLATE("inspection_template", "检查模板", "inspection", 301),
    INSPECTION_PROJECT("inspection_project", "检查项目", "inspection", 302),
    INSPECTION_TASK("inspection_task", "检查任务", "inspection", 303),
    INSPECTION_RECORD("inspection_record", "检查记录", "inspection", 304),
    INSPECTION_PERSONAL("inspection_personal", "个人检查记录", "inspection", 305),
    INSPECTION_APPEAL("inspection_appeal", "申诉管理", "inspection", 306),
    INSPECTION_SUMMARY("inspection_summary", "检查汇总", "inspection", 307),
    INSPECTION_CORRECTIVE("inspection_corrective", "整改管理", "inspection", 308),

    // ==================== Access Domain ====================
    SYSTEM_USER("system_user", "用户管理", "access", 401),
    SYSTEM_ROLE("system_role", "角色管理", "access", 402);

    private final String code;
    private final String name;
    private final String domain;
    private final int sortOrder;

    DataModule(String code, String name, String domain, int sortOrder) {
        this.code = code;
        this.name = name;
        this.domain = domain;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static DataModule fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataModule module : values()) {
            if (module.code.equals(code)) {
                return module;
            }
        }
        return null;
    }

    /**
     * 获取所有领域列表
     */
    public static List<String> getAllDomains() {
        return Arrays.stream(values())
                .map(DataModule::getDomain)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 按领域分组获取模块
     */
    public static Map<String, List<DataModule>> groupByDomain() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(DataModule::getDomain));
    }

    /**
     * 获取指定领域的所有模块
     */
    public static List<DataModule> getByDomain(String domain) {
        return Arrays.stream(values())
                .filter(m -> m.domain.equals(domain))
                .sorted((a, b) -> Integer.compare(a.sortOrder, b.sortOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取领域显示名称
     */
    public static String getDomainDisplayName(String domain) {
        return switch (domain) {
            case "organization" -> "组织管理";
            case "space" -> "场所管理";
            case "inspection" -> "量化检查";
            case "access" -> "权限管理";
            default -> domain;
        };
    }
}
