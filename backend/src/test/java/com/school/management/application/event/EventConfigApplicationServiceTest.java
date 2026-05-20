package com.school.management.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.PluginEnabledGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EventConfigApplicationService 应用服务单测.
 *
 * 该服务是 3 张配置表 (event_triggers / entity_event_types / trigger_points)
 * 的 jdbc CRUD. 测试通过 mock JdbcTemplate, 验证:
 *  - 动态 SQL 条件拼接 (filter 参数有无)
 *  - INSERT/UPDATE 参数顺序与默认值兜底
 *  - 系统预置实体的锁定逻辑
 *  - snake_case → camelCase 结果转换
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventConfigApplicationService 事件配置应用服务")
class EventConfigApplicationServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock PluginEnabledGuard pluginEnabledGuard;
    @Mock TriggerService triggerService;

    ObjectMapper objectMapper = new ObjectMapper();

    EventConfigApplicationService service;

    private EventConfigApplicationService newService() {
        return new EventConfigApplicationService(
                jdbcTemplate, objectMapper, pluginEnabledGuard, triggerService);
    }

    {
        // service 用真 ObjectMapper, 不可 @InjectMocks (会注入 mock ObjectMapper)
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    // ============================================================
    @Nested
    @DisplayName("event_triggers — listTriggers")
    class ListTriggersTests {

        @Test
        @DisplayName("无 filter: SQL 不含额外条件, 结果 snake→camel 转换")
        void shouldListWithoutFilter() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of(row("trigger_point_name", "点A", "module_code", "M1")));

            List<Map<String, Object>> result = service.listTriggers(null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).containsKey("triggerPointName").containsKey("moduleCode");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), paramCaptor.capture());
            assertThat(sqlCaptor.getValue())
                    .doesNotContain("trigger_point_code = ?")
                    .doesNotContain("event_type_code = ?");
            assertThat(paramCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("带 pointCode + eventType: SQL 追加两个条件且参数按序")
        void shouldListWithBothFilters() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listTriggers("PC-1", "ET-1");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), paramCaptor.capture());
            assertThat(sqlCaptor.getValue())
                    .contains("t.trigger_point_code = ?")
                    .contains("t.event_type_code = ?");
            assertThat(paramCaptor.getValue()).containsExactly("PC-1", "ET-1");
        }

        @Test
        @DisplayName("空白 pointCode 视为无 filter")
        void blankFilterIgnored() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listTriggers("   ", "");

            ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).queryForList(anyString(), paramCaptor.capture());
            assertThat(paramCaptor.getValue()).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("event_triggers — getTriggerById")
    class GetTriggerByIdTests {

        @Test
        @DisplayName("命中: 返回 camelCase 单行")
        void shouldReturnRow() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), eq(5L)))
                    .thenReturn(List.of(row("trigger_point_name", "点A")));
            Map<String, Object> result = service.getTriggerById(5L);
            assertThat(result).containsEntry("triggerPointName", "点A");
        }

        @Test
        @DisplayName("无命中: 返回 null")
        void shouldReturnNullWhenEmpty() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of());
            assertThat(service.getTriggerById(5L)).isNull();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("event_triggers — create/update/delete/enable/disable")
    class TriggerWriteTests {

        @Test
        @DisplayName("createTrigger: 默认值兜底 (eventTypeMode=FIXED, isEnabled=1, sortOrder=0)")
        void shouldCreateWithDefaults() {
            service = newService();
            Map<String, Object> body = row("name", "触发器A", "triggerPointCode", "PC-1");

            service.createTrigger(body);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO event_triggers"), captor.capture());
            Object[] params = captor.getValue();
            assertThat(params[0]).isEqualTo("触发器A");
            assertThat(params[1]).isEqualTo("PC-1");
            assertThat(params[3]).isEqualTo("FIXED");   // eventTypeMode 默认
            assertThat(params[8]).isEqualTo(1);          // isEnabled 默认
            assertThat(params[9]).isEqualTo(0);          // sortOrder 默认
        }

        @Test
        @DisplayName("createTrigger: conditionJson 为 Map 时序列化为 JSON 字符串")
        void shouldSerializeConditionJsonMap() {
            service = newService();
            Map<String, Object> body = row(
                    "name", "T", "conditionJson", Map.of("k", "v"));

            service.createTrigger(body);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(anyString(), captor.capture());
            assertThat(captor.getValue()[2]).isEqualTo("{\"k\":\"v\"}");
        }

        @Test
        @DisplayName("createTrigger: conditionJson 为 String 时原样传递")
        void shouldPassThroughConditionJsonString() {
            service = newService();
            Map<String, Object> body = row("name", "T", "conditionJson", "{\"raw\":1}");

            service.createTrigger(body);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(anyString(), captor.capture());
            assertThat(captor.getValue()[2]).isEqualTo("{\"raw\":1}");
        }

        @Test
        @DisplayName("updateTrigger: 先过 pluginEnabledGuard 再 UPDATE")
        void shouldUpdateAfterGuard() {
            service = newService();
            service.updateTrigger(9L, row("name", "改名"));

            verify(pluginEnabledGuard).check("event_triggers", 9L);
            verify(jdbcTemplate).update(contains("UPDATE event_triggers SET"),
                    any(Object[].class));
        }

        @Test
        @DisplayName("deleteTrigger: guard + 逻辑删除")
        void shouldSoftDelete() {
            service = newService();
            service.deleteTrigger(9L);
            verify(pluginEnabledGuard).check("event_triggers", 9L);
            verify(jdbcTemplate).update(contains("SET deleted = 1"), eq(9L));
        }

        @Test
        @DisplayName("enableTrigger: guard + is_enabled=1")
        void shouldEnable() {
            service = newService();
            service.enableTrigger(9L);
            verify(pluginEnabledGuard).check("event_triggers", 9L);
            verify(jdbcTemplate).update(contains("is_enabled = 1"), eq(9L));
        }

        @Test
        @DisplayName("disableTrigger: guard + is_enabled=0")
        void shouldDisable() {
            service = newService();
            service.disableTrigger(9L);
            verify(pluginEnabledGuard).check("event_triggers", 9L);
            verify(jdbcTemplate).update(contains("is_enabled = 0"), eq(9L));
        }

        @Test
        @DisplayName("testTrigger: 委托 triggerService.testFire 并转 camelCase")
        void shouldTestTrigger() {
            service = newService();
            when(triggerService.testFire(eq("PC-1"), any()))
                    .thenReturn(List.of(row("trigger_id", 1L)));

            List<Map<String, Object>> result = service.testTrigger("PC-1", Map.of("x", 1));

            assertThat(result.get(0)).containsKey("triggerId");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("entity_event_types — listEventTypesGrouped / listEventCategories")
    class EventTypeQueryTests {

        @Test
        @DisplayName("非 admin: SQL 含 plugin_enabled=1 过滤")
        void shouldFilterPluginEnabledForNonAdmin() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listEventTypesGrouped(null, false);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
            assertThat(sqlCaptor.getValue()).contains("plugin_enabled = 1");
        }

        @Test
        @DisplayName("admin (includeDisabled=true): SQL 不含 plugin_enabled 过滤")
        void shouldNotFilterForAdmin() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listEventTypesGrouped(null, true);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
            assertThat(sqlCaptor.getValue()).doesNotContain("plugin_enabled = 1");
        }

        @Test
        @DisplayName("按 category 分组: 同分类聚合, 携带分类元数据")
        void shouldGroupByCategory() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of(
                            row("category_code", "CAT1", "category_name", "分类1",
                                    "category_polarity", "POSITIVE", "type_code", "T1"),
                            row("category_code", "CAT1", "category_name", "分类1",
                                    "category_polarity", "POSITIVE", "type_code", "T2"),
                            row("category_code", "CAT2", "category_name", "分类2",
                                    "category_polarity", "NEGATIVE", "type_code", "T3")));

            List<Map<String, Object>> result = service.listEventTypesGrouped(null, false);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("categoryCode")).isEqualTo("CAT1");
            assertThat(result.get(0).get("categoryName")).isEqualTo("分类1");
            assertThat(result.get(0).get("categoryPolarity")).isEqualTo("POSITIVE");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cat1Types =
                    (List<Map<String, Object>>) result.get(0).get("types");
            assertThat(cat1Types).hasSize(2);
            assertThat(result.get(1).get("categoryCode")).isEqualTo("CAT2");
        }

        @Test
        @DisplayName("带 category 过滤: SQL 追加条件, 参数传 category")
        void shouldFilterByCategory() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listEventTypesGrouped("CAT9", false);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), paramCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("category_code = ?");
            assertThat(paramCaptor.getValue()).containsExactly("CAT9");
        }

        @Test
        @DisplayName("listEventCategories: GROUP BY 查询 + camelCase")
        void shouldListCategories() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("GROUP BY category_code")))
                    .thenReturn(List.of(row("category_code", "CAT1", "type_count", 3L)));

            List<Map<String, Object>> result = service.listEventCategories();

            assertThat(result.get(0)).containsKey("categoryCode").containsKey("typeCount");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("entity_event_types — createEventType")
    class CreateEventTypeTests {

        @Test
        @DisplayName("同分类已存在: 沿用已有 polarity 覆盖入参")
        void shouldReuseExistingPolarity() {
            service = newService();
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("CAT1")))
                    .thenReturn("NEGATIVE");
            Map<String, Object> body = row(
                    "categoryCode", "CAT1", "categoryPolarity", "POSITIVE",
                    "typeCode", "T1", "typeName", "类型1");

            service.createEventType(body);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO entity_event_types"),
                    captor.capture());
            // 第 3 个参数是 categoryPolarity
            assertThat(captor.getValue()[2]).isEqualTo("NEGATIVE");
        }

        @Test
        @DisplayName("同分类首次创建 (查询抛异常): 用入参 polarity")
        void shouldUseInputPolarityOnFirstCreate() {
            service = newService();
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("CATNEW")))
                    .thenThrow(new RuntimeException("no rows"));
            Map<String, Object> body = row(
                    "categoryCode", "CATNEW", "categoryPolarity", "POSITIVE",
                    "typeCode", "T1", "typeName", "类型1");

            service.createEventType(body);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(anyString(), captor.capture());
            assertThat(captor.getValue()[2]).isEqualTo("POSITIVE");
        }

        @Test
        @DisplayName("无 categoryCode: 跳过 polarity 查询, polarity 默认 NEUTRAL")
        void shouldDefaultNeutralWhenNoCategory() {
            service = newService();
            Map<String, Object> body = row("typeCode", "T1", "typeName", "类型1");

            service.createEventType(body);

            verify(jdbcTemplate, never()).queryForObject(anyString(), eq(String.class), any());
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(anyString(), captor.capture());
            assertThat(captor.getValue()[2]).isEqualTo("NEUTRAL");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("entity_event_types — updateEventType")
    class UpdateEventTypeTests {

        @Test
        @DisplayName("实体不存在: 返回 false, 不执行 UPDATE")
        void shouldReturnFalseWhenMissing() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("SELECT is_system"), eq(5L)))
                    .thenReturn(List.of());

            boolean updated = service.updateEventType(5L, row("typeName", "改名"));

            assertThat(updated).isFalse();
            verify(jdbcTemplate, never()).update(contains("UPDATE entity_event_types"),
                    any(Object[].class));
        }

        @Test
        @DisplayName("普通类型: 用入参更新 typeName/icon/color")
        void shouldUpdateNonSystemType() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("SELECT is_system"), eq(5L)))
                    .thenReturn(List.of(row("is_system", 0, "type_name", "旧名",
                            "icon", "旧icon", "color", "红", "applicable_subjects", "S")));

            boolean updated = service.updateEventType(5L,
                    row("typeName", "新名", "icon", "新icon", "color", "蓝",
                            "applicableSubjects", "S2"));

            assertThat(updated).isTrue();
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("UPDATE entity_event_types"), captor.capture());
            Object[] p = captor.getValue();
            // params: categoryCode, categoryName, polarity, typeName, icon, color, applicableSubjects...
            assertThat(p[3]).isEqualTo("新名");
            assertThat(p[4]).isEqualTo("新icon");
            assertThat(p[5]).isEqualTo("蓝");
        }

        @Test
        @DisplayName("系统预置类型 (is_system=1): 核心字段锁定保留旧值")
        void shouldLockSystemTypeCoreFields() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("SELECT is_system"), eq(5L)))
                    .thenReturn(List.of(row("is_system", 1, "type_name", "锁定名",
                            "icon", "锁定icon", "color", "锁定色", "applicable_subjects", "锁定S")));

            boolean updated = service.updateEventType(5L,
                    row("typeName", "试图改", "icon", "试图改", "color", "试图改",
                            "applicableSubjects", "试图改"));

            assertThat(updated).isTrue();
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("UPDATE entity_event_types"), captor.capture());
            Object[] p = captor.getValue();
            assertThat(p[3]).isEqualTo("锁定名");
            assertThat(p[4]).isEqualTo("锁定icon");
            assertThat(p[5]).isEqualTo("锁定色");
        }

        @Test
        @DisplayName("调用前过 pluginEnabledGuard")
        void shouldCheckGuard() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of());
            service.updateEventType(5L, row());
            verify(pluginEnabledGuard).check("entity_event_types", 5L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("entity_event_types — deleteEventType")
    class DeleteEventTypeTests {

        @Test
        @DisplayName("普通类型: 逻辑删除, 返回 null")
        void shouldDeleteNonSystem() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("SELECT is_system"), eq(5L)))
                    .thenReturn(List.of(row("is_system", 0)));

            String result = service.deleteEventType(5L);

            assertThat(result).isNull();
            verify(jdbcTemplate).update(contains("SET deleted = 1"), eq(5L));
        }

        @Test
        @DisplayName("系统预置类型: 返回错误信息, 不删除")
        void shouldRejectDeleteSystem() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("SELECT is_system"), eq(5L)))
                    .thenReturn(List.of(row("is_system", 1)));

            String result = service.deleteEventType(5L);

            assertThat(result).contains("系统预置类型不允许删除");
            verify(jdbcTemplate, never()).update(contains("SET deleted = 1"), any(Object[].class));
        }

        @Test
        @DisplayName("实体不存在: 直接逻辑删除返回 null")
        void shouldDeleteWhenRowMissing() {
            service = newService();
            when(jdbcTemplate.queryForList(contains("SELECT is_system"), eq(5L)))
                    .thenReturn(List.of());

            String result = service.deleteEventType(5L);

            assertThat(result).isNull();
            verify(jdbcTemplate).update(contains("SET deleted = 1"), eq(5L));
        }
    }

    // ============================================================
    @Nested
    @DisplayName("entity_event_types — createEventCategory")
    class CreateEventCategoryTests {

        @Test
        @DisplayName("categoryCode 为 null: 返回校验错误")
        void shouldRejectNullCode() {
            service = newService();
            String result = service.createEventCategory(null, "分类名", "POSITIVE");
            assertThat(result).contains("不能为空");
            verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("categoryName 为 null: 返回校验错误")
        void shouldRejectNullName() {
            service = newService();
            String result = service.createEventCategory("CAT", null, "POSITIVE");
            assertThat(result).contains("不能为空");
        }

        @Test
        @DisplayName("分类编码已存在: 返回冲突错误")
        void shouldRejectDuplicateCode() {
            service = newService();
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("CAT")))
                    .thenReturn(2);

            String result = service.createEventCategory("CAT", "分类名", "POSITIVE");

            assertThat(result).contains("分类编码已存在");
            verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("正常创建: 插入 placeholder type, 返回 null")
        void shouldCreateCategory() {
            service = newService();
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("CAT")))
                    .thenReturn(0);

            String result = service.createEventCategory("CAT", "分类名", "NEGATIVE");

            assertThat(result).isNull();
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO entity_event_types"),
                    captor.capture());
            Object[] p = captor.getValue();
            assertThat(p[0]).isEqualTo("CAT");
            assertThat(p[1]).isEqualTo("分类名");
            assertThat(p[2]).isEqualTo("NEGATIVE");
            assertThat(p[3]).isEqualTo("CAT_PLACEHOLDER");
        }

        @Test
        @DisplayName("polarity 为 null: 兜底 NEUTRAL")
        void shouldDefaultPolarityNeutral() {
            service = newService();
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("CAT")))
                    .thenReturn(0);

            service.createEventCategory("CAT", "分类名", null);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(anyString(), captor.capture());
            assertThat(captor.getValue()[2]).isEqualTo("NEUTRAL");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("trigger_points — CRUD")
    class TriggerPointTests {

        @Test
        @DisplayName("listTriggerPoints 无 module: 不追加条件")
        void shouldListWithoutModule() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listTriggerPoints(null);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), paramCaptor.capture());
            assertThat(sqlCaptor.getValue()).doesNotContain("module_code = ?");
            assertThat(paramCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("listTriggerPoints 带 module: 参数化追加条件 (防注入)")
        void shouldListWithModule() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            service.listTriggerPoints("M1");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), paramCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("module_code = ?");
            assertThat(paramCaptor.getValue()).containsExactly("M1");
        }

        @Test
        @DisplayName("getTriggerPointById 命中: 返回 camelCase")
        void shouldGetPoint() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), eq(7L)))
                    .thenReturn(List.of(row("point_code", "PC", "module_code", "M")));
            assertThat(service.getTriggerPointById(7L)).containsKey("pointCode");
        }

        @Test
        @DisplayName("getTriggerPointById 无命中: 返回 null")
        void shouldGetPointNull() {
            service = newService();
            when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of());
            assertThat(service.getTriggerPointById(7L)).isNull();
        }

        @Test
        @DisplayName("createTriggerPoint: contextSchema 非 null 转字符串")
        void shouldCreatePoint() {
            service = newService();
            Map<String, Object> body = row(
                    "moduleCode", "M1", "moduleName", "模块1",
                    "pointCode", "PC1", "pointName", "点1",
                    "contextSchema", Map.of("k", "v"));

            service.createTriggerPoint(body);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO trigger_points"), captor.capture());
            Object[] p = captor.getValue();
            assertThat(p[0]).isEqualTo("M1");
            assertThat(p[2]).isEqualTo("PC1");
            assertThat(p[5]).isNotNull();   // contextSchema toString
            assertThat(p[6]).isEqualTo(1);  // isEnabled 默认
            assertThat(p[7]).isEqualTo(0);  // sortOrder 默认
        }

        @Test
        @DisplayName("createTriggerPoint: contextSchema 为 null 传 null")
        void shouldCreatePointNullSchema() {
            service = newService();
            service.createTriggerPoint(row("moduleCode", "M1", "pointCode", "PC1"));

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(anyString(), captor.capture());
            assertThat(captor.getValue()[5]).isNull();
        }

        @Test
        @DisplayName("updateTriggerPoint: guard + UPDATE")
        void shouldUpdatePoint() {
            service = newService();
            service.updateTriggerPoint(7L, row("moduleCode", "M2"));
            verify(pluginEnabledGuard).check("trigger_points", 7L);
            verify(jdbcTemplate).update(contains("UPDATE trigger_points SET"),
                    any(Object[].class));
        }

        @Test
        @DisplayName("deleteTriggerPoint: guard + 逻辑删除")
        void shouldDeletePoint() {
            service = newService();
            service.deleteTriggerPoint(7L);
            verify(pluginEnabledGuard).check("trigger_points", 7L);
            verify(jdbcTemplate).update(contains("SET deleted = 1"), eq(7L));
        }

        @Test
        @DisplayName("enableTriggerPoint: guard + is_enabled=1")
        void shouldEnablePoint() {
            service = newService();
            service.enableTriggerPoint(7L);
            verify(pluginEnabledGuard).check("trigger_points", 7L);
            verify(jdbcTemplate).update(contains("is_enabled = 1"), eq(7L));
        }

        @Test
        @DisplayName("disableTriggerPoint: guard + is_enabled=0")
        void shouldDisablePoint() {
            service = newService();
            service.disableTriggerPoint(7L);
            verify(pluginEnabledGuard).check("trigger_points", 7L);
            verify(jdbcTemplate).update(contains("is_enabled = 0"), eq(7L));
        }

        @Test
        @DisplayName("写操作各调用 guard 一次")
        void writeOpsCheckGuardOnce() {
            service = newService();
            service.enableTriggerPoint(7L);
            service.disableTriggerPoint(7L);
            verify(pluginEnabledGuard, times(2)).check(eq("trigger_points"), eq(7L));
        }
    }
}
