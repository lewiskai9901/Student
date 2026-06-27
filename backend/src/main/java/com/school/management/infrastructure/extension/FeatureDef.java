package com.school.management.infrastructure.extension;

/**
 * 实体类型"特性"(feature) 的登记定义 —— 特性词汇表的单一权威项。
 *
 * <p>特性是挂在实体类型上的布尔能力标签 (entity_type_configs.features), 核心靠 {@code hasFeature(code)}
 * 统一判断行为而不认具体类型。此前词汇表无治理 (散落 magic string + 仅 javadoc 约定), 现集中登记:
 * 每个特性必须由某 {@link FeatureDefProvider} 声明 (code + 中文名 + 用处说明 + 归属行业), 否则启动期 fail-fast。
 *
 * @param code  特性 key (如 canLogin / isLearner)
 * @param label 中文名 (如 可登录系统)
 * @param description 用处 / 效果说明
 */
public record FeatureDef(String code, String label, String description) {}
