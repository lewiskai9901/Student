package com.school.management.infrastructure.extension.plugins.core.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 最小入住人数策略 (core 内置 reference impl).
 *
 * 语义: AFTER_CHECKIN 后, 若场所当前入住人数低于 `policy.place.min-occupants` 配置阈值
 *       (application.yml, 默认 4), 发 WARN — 不阻断入住, 只提示可能需要拼人.
 *
 * 说明:
 *   - 只处理 AFTER_CHECKIN phase, entityType="place"
 *   - payload 是 CheckInCommand, 但本策略仅用来展示 Policy 契约, 不实际查 Place 仓储
 *     (查仓储会把本 policy 和 Place 领域耦合, 插件侧应自行注入需要的 repository)
 *   - 开关: policy.place.min-occupants=0 时该策略视为禁用 (不产 violation)
 *
 * 给插件开发者的模式参考 (@see plugin-extension-catalog.md):
 *   1. implements Policy&lt;T&gt; 声明支持的 payload 类型
 *   2. supports() 精确匹配 entityType + phase
 *   3. check() 返回违规列表, 空=通过
 *   4. 通过 @Component 注册, Spring 自动汇入 PolicyRegistry
 *   5. 可组合: 另注册 PluginPackage.contribute() → PolicyContribution 留审计痕迹
 */
@Slf4j
@Component
public class MinOccupantsPolicy implements Policy<Object> {

    private final int threshold;

    public MinOccupantsPolicy(@Value("${policy.place.min-occupants:4}") int threshold) {
        this.threshold = threshold;
        log.info("[MinOccupantsPolicy] threshold={}, disabled={}",
            threshold, threshold <= 0);
    }

    @Override public String code() { return "MIN_OCCUPANTS"; }
    @Override public String name() { return "最小入住人数警告"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return threshold > 0
            && "place".equals(ctx.entityType())
            && "AFTER_CHECKIN".equals(ctx.phase());
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        // 真实策略会从 payload 读 placeId, 查仓储得到 currentOccupancy, 这里示例性处理
        // 具体读法因项目而异, 本 reference impl 展示结构而不实际查表.
        // 如果 payload 是 Map 且含 currentOccupancy 字段, 按它判;
        // 否则返回空 (即放行, 无 violation).
        if (ctx.payload() instanceof Map<?, ?> m) {
            Object v = m.get("currentOccupancy");
            if (v instanceof Number n && n.intValue() < threshold) {
                return List.of(Violation.warn("MIN_OCCUPANTS",
                    "场所当前入住 " + n + " 人, 低于期望 " + threshold + " 人"));
            }
        }
        return List.of();
    }
}
