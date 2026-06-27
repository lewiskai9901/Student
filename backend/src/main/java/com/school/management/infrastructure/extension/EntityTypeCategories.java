package com.school.management.infrastructure.extension;

import com.school.management.domain.organization.model.entity.OrgCategory;
import com.school.management.domain.place.model.valueobject.BaseCategory;
import com.school.management.domain.user.model.entity.UserCategory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 实体类型"分类"(category) 的合法值校验。
 *
 * <p>分类是封闭枚举 ({@link UserCategory}/{@link OrgCategory}/{@link BaseCategory}), 它驱动
 * 默认特性、特性白名单校验等行为。历史上写入路径不校验 → 非法分类(如 PLACE 的 "SPACE")会静默落库,
 * 且 categoryDefaults 的 valueOf 落 catch 返 null → 悄悄关掉该类型的特性校验 (footgun)。
 *
 * <p>本工具集中校验 category ∈ 对应实体的枚举, 供注册期守护 + 创建/更新写入校验复用。
 * null/空 视为合法 (= 未归类, 该类型不套用分类级行为)。
 */
public final class EntityTypeCategories {

    private EntityTypeCategories() {}

    /** category 是否对该实体合法 (null/空 = 合法的"未归类")。 */
    public static boolean isValid(String entityType, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            switch (norm(entityType)) {
                case "USER":     UserCategory.valueOf(category); return true;
                case "PLACE":    BaseCategory.valueOf(category); return true;
                case "ORG_UNIT": OrgCategory.valueOf(category);  return true;
                default:         return true;   // 未知实体不拦 (无对应枚举可校验)
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 该实体所有合法分类值 (报错提示用), 如 "ADMIN/STAFF/MEMBER/EXTERNAL"。 */
    public static String validValues(String entityType) {
        switch (norm(entityType)) {
            case "USER":     return names(UserCategory.values());
            case "PLACE":    return names(BaseCategory.values());
            case "ORG_UNIT": return names(OrgCategory.values());
            default:         return "";
        }
    }

    private static String norm(String entityType) {
        return entityType == null ? "" : entityType.toUpperCase();
    }

    private static <E extends Enum<E>> String names(E[] vals) {
        return Arrays.stream(vals).map(Enum::name).collect(Collectors.joining("/"));
    }
}
