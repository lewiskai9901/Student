package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 特性定义贡献点 —— 每个行业(含核心)声明<b>自己拥有</b>的特性, 由 {@link FeatureRegistry} 汇总。
 *
 * <p>体现"特性归属 + 依赖方向": 核心({@code CoreFeatureProvider})拥有通用特性(canLogin/isStaff…),
 * 行业插件拥有自己的特性(教育: isLearner/canTeach…)。类型只能使用 CORE 特性 + 本行业特性;
 * 跨行业引用或未登记特性会在 {@code PluginRegistrar} 启动校验时 fail-fast。
 */
public interface FeatureDefProvider {

    /** 归属行业码 (CORE / EDU / ...)。 */
    String industry();

    /** 本行业拥有的特性定义。 */
    List<FeatureDef> features();
}
