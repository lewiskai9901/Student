package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 消息推送目标模式解析 SPI — 插件可贡献新的 target_mode.
 *
 * core 内置 4 种 (BY_SUBJECT/BY_ROLE/BY_RELATION/BY_FEATURE) 各为独立 @Component.
 * 行业可加如 BY_DEPARTMENT / BY_WARD, 通过 PluginPackage.contribute() 返回
 * TargetModeResolverContribution 声明, 或直接 @Component.
 *
 * 实现契约:
 *   - modeCode() 全局唯一 (core 4 种已占, 插件不可重名)
 *   - resolve(config, event) 返回 user id 列表 (允许空; 必须非 null)
 *   - 不要在 resolve 内抛异常 — 捕获并记 log, 返空避免一条规则挂掉全部派发
 */
public interface TargetModeResolver {

    /** 模式码, 全局唯一 */
    String modeCode();

    /** 人类可读名 (用于前端下拉) */
    String displayName();

    /**
     * 解析规则到 user id 列表.
     * @param config 规则 config_json (e.g. {"roleCode": "TEACHER"} 或 {"relation": "viewer", "resourceType": "place", "resourceId": 123})
     * @param event  触发事件 (EntityEvent, 含 subjectId/payload 等); 预览场景可能为空 map
     * @return 目标 user ids, 不含重复
     */
    List<Long> resolve(Map<String, Object> config, Map<String, Object> event);

    /**
     * 是否支持静态预览 (无事件上下文时估算命中用户).
     * BY_SUBJECT / BY_RELATION 依赖事件主体, 应 override 返回 false;
     * BY_ROLE / BY_FEATURE 仅依赖 config, 默认 true.
     */
    default boolean supportsPreview() { return true; }
}
