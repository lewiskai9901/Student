package com.school.management.domain.organization.model.entity;

/**
 * 组织类型内置分类枚举
 * 定义组织类型的本质角色，业务代码应基于 category 而非 typeCode 做判断
 */
public enum OrgCategory {

    /** 根组织 - 整个组织树的顶层 */
    ROOT("根组织"),

    /** 分支机构 - 独立数据权限边界（如分校/事业部） */
    BRANCH("分支机构"),

    /** 职能部门 - 承载业务功能（如教务处/行政部） */
    FUNCTIONAL("职能部门"),

    /** 成员组 - 直接管理成员（如班级/小组） */
    GROUP("成员组"),

    /** 容器 - 纯分组用途（如年级/学部） */
    CONTAINER("容器");

    private final String label;

    OrgCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 该分类的默认 features
     */
    public java.util.Map<String, Boolean> getDefaultFeatures() {
        java.util.Map<String, Boolean> features = new java.util.LinkedHashMap<>();
        switch (this) {
            case ROOT:
                features.put("dataPermissionBoundary", true);
                features.put("inspectionTarget", false);
                features.put("memberManagement", false);
                features.put("attendance", false);
                features.put("scheduling", false);
                break;
            case BRANCH:
                features.put("dataPermissionBoundary", true);
                features.put("inspectionTarget", true);
                features.put("memberManagement", false);
                features.put("attendance", false);
                features.put("scheduling", false);
                break;
            case FUNCTIONAL:
                features.put("dataPermissionBoundary", false);
                features.put("inspectionTarget", true);
                features.put("memberManagement", true);
                features.put("attendance", false);
                features.put("scheduling", false);
                break;
            case GROUP:
                features.put("dataPermissionBoundary", false);
                features.put("inspectionTarget", true);
                features.put("memberManagement", true);
                features.put("attendance", true);
                features.put("scheduling", true);
                break;
            case CONTAINER:
                features.put("dataPermissionBoundary", false);
                features.put("inspectionTarget", false);
                features.put("memberManagement", false);
                features.put("attendance", false);
                features.put("scheduling", false);
                break;
        }
        return features;
    }
}
