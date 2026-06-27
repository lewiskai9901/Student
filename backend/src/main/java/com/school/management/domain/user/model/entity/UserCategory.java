package com.school.management.domain.user.model.entity;

import java.util.Map;

/**
 * 用户类型分类枚举 (成员 / 职工 / 管理员 / 外部人员)。
 *
 * <p>当前仅作"分类标签 + 分组"用 —— 原 defaultFeatures(requiresOrg/requiresPlace) 已删:
 * 它们从不写入类型 features 列, validateRequiresOrg 永久 no-op (归属真相源已是 access_relations member),
 * 属死代码。如将来要"成员必须有归属组织"之类规则, 应显式接线(注册时合并进 features)并加测试, 而非留空壳。
 */
public enum UserCategory {

    ADMIN("管理员", Map.of()),
    STAFF("职工", Map.of()),
    MEMBER("成员", Map.of()),
    EXTERNAL("外部人员", Map.of());

    private final String label;
    private final Map<String, Boolean> defaultFeatures;

    UserCategory(String label, Map<String, Boolean> defaultFeatures) {
        this.label = label;
        this.defaultFeatures = defaultFeatures;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, Boolean> getDefaultFeatures() {
        return defaultFeatures;
    }
}
