package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 消息域插件 SPI — 一个业务域(宿舍/成绩/考试...)的触发点 + 事件类型 + 默认触发器整包声明.
 *
 * 启动时由 {@link MessagingRegistrar} 扫描,UPSERT 到:
 *   - trigger_points (触发点)
 *   - entity_event_types (事件类型,业务语义分类)
 *   - event_triggers (默认触发器映射,is_plugin_default=1,admin 可覆盖)
 *
 * 典型实现:
 * <pre>
 *   &#64;Component
 *   public class DormitoryMessagingPlugin implements MessagingDomainPlugin {
 *       public String getDomainCode() { return "dormitory"; }
 *       public String getDomainName() { return "宿舍"; }
 *       public List&lt;TriggerPointDef&gt; triggerPoints() {
 *           return List.of(new TriggerPointDef(DORM_CHECKIN, "宿舍入住", ...));
 *       }
 *       ...
 *   }
 * </pre>
 *
 * @deprecated since 1.1.0 — 用 {@link PluginPackage#contribute()} 返回
 *   {@link Contribution.EventDomainContribution} (一域打包触发点/事件类型/默认触发器) 替代.
 *   旧 API 仍被 {@link MessagingRegistrar} 扫描, 运行时等价, 现有实现无需立即迁移.
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public interface MessagingDomainPlugin {

    /** 业务域码,如 "dormitory" / "grade" / "inspection" */
    String getDomainCode();

    /** 业务域显示名 */
    String getDomainName();

    /** 触发点声明(代码 fire 用) */
    default List<TriggerPointDef> triggerPoints() { return List.of(); }

    /** 事件类型声明(业务语义分类,给消息订阅/UI 分类用) */
    default List<EventTypeDef> eventTypes() { return List.of(); }

    /** 默认触发器(触发点 → 事件类型的开箱即用映射,admin 可覆盖) */
    default List<DefaultTriggerDef> defaultTriggers() { return List.of(); }

    // ────────── Record 定义 ──────────

    /**
     * 触发点定义 (对应 trigger_points 表一行).
     * @param pointCode      如 "DORM_CHECKIN"
     * @param pointName      中文名 "宿舍入住"
     * @param contextSchema  期望的 context 字段 schema,如 {"occupantId":"Long","placeId":"Long"}
     * @param description    业务描述
     */
    record TriggerPointDef(
        String pointCode,
        String pointName,
        Map<String, String> contextSchema,
        String description
    ) {
        public static TriggerPointDef of(String code, String name, Map<String, String> schema) {
            return new TriggerPointDef(code, name, schema, null);
        }
        public TriggerPointDef withDescription(String desc) {
            return new TriggerPointDef(pointCode, pointName, contextSchema, desc);
        }
    }

    /**
     * 事件类型定义 (对应 entity_event_types 表一行).
     * @param typeCode            如 "DORM_CHECKIN_EVT"
     * @param typeName            中文名 "入住登记"
     * @param categoryCode        "PERSONNEL"/"ACADEMIC"/"DISCIPLINE"/"AWARD"/"TEACHING"/"PLACE"
     * @param categoryPolarity    "POSITIVE"/"NEGATIVE"/"NEUTRAL"
     * @param icon                图标标识
     * @param color               颜色 hex
     * @param applicableSubjects  适用的 subject_type 集合 [USER/ORG_UNIT/PLACE]
     */
    record EventTypeDef(
        String typeCode,
        String typeName,
        String categoryCode,
        String categoryName,
        String categoryPolarity,
        String icon,
        String color,
        List<String> applicableSubjects,
        String description
    ) {}

    /**
     * 默认触发器定义 (对应 event_triggers 表一行).
     * 把触发点默认映射到事件类型,admin 可在 UI 里覆盖.
     *
     * @param pointCode        触发点码
     * @param eventTypeCode    事件类型码
     * @param subjectsExpr     JSON 表达式,如 [{"type":"USER","id":"{{occupantId}}"}]
     */
    record DefaultTriggerDef(
        String pointCode,
        String eventTypeCode,
        Object subjectsExpr
    ) {}
}
