package com.school.management.domain.inspection.model.v7.execution;

/**
 * 人数来源枚举
 */
public enum PopulationSource {
    AUTO,    // 自动从 org_units/school_classes 表拉取
    MANUAL,  // 检查员执行时手动输入
    FIXED    // 项目级别统一配置固定值
}
