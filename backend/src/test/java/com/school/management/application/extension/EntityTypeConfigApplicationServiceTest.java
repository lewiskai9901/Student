package com.school.management.application.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EntityTypeConfigApplicationService 单测 — 验证统一实体类型配置 CRUD /
 * 子类型解析 / 覆写字段维护 / 字段恢复 / 自定义字段管理的 SQL 与分支行为。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityTypeConfigApplicationService 测试")
class EntityTypeConfigApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private ApplicationContext appCtx;

    @InjectMocks
    private EntityTypeConfigApplicationService service;

    // ==================== list ====================

    @Nested
    @DisplayName("list 查询类型配置")
    class ListTests {

        @Test
        @DisplayName("非管理员视角附加启用过滤")
        void listNonAdminAddsEnabledFilter() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.list("USER", null, false);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).queryForList(sql.capture(), params.capture());
            assertThat(sql.getValue()).contains("is_enabled = 1 AND plugin_enabled = 1");
            assertThat(sql.getValue()).doesNotContain("category = ?");
            assertThat(params.getValue()).containsExactly("USER");
        }

        @Test
        @DisplayName("管理员视角不加启用过滤")
        void listAdminNoEnabledFilter() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.list("PLACE", null, true);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sql.capture(), any(Object[].class));
            assertThat(sql.getValue()).doesNotContain("is_enabled = 1 AND plugin_enabled = 1");
        }

        @Test
        @DisplayName("传 category 时附加 category 过滤并加入参数")
        void listWithCategory() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.list("USER", "STUDENT", true);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).queryForList(sql.capture(), params.capture());
            assertThat(sql.getValue()).contains("AND category = ?");
            assertThat(params.getValue()).containsExactly("USER", "STUDENT");
        }
    }

    // ==================== detail ====================

    @Nested
    @DisplayName("detail 查询单个类型")
    class DetailTests {

        @Test
        @DisplayName("detail 按 entityType + typeCode 查询单行")
        void detail() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", 1L);
            when(jdbc.queryForMap(anyString(), eq("USER"), eq("STUDENT"))).thenReturn(row);

            Map<String, Object> result = service.detail("USER", "STUDENT");

            assertThat(result).isSameAs(row);
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForMap(sql.capture(), eq("USER"), eq("STUDENT"));
            assertThat(sql.getValue()).contains("entity_type = ? AND type_code = ?");
        }
    }

    // ==================== allowedChildren ====================

    @Nested
    @DisplayName("allowedChildren 子类型解析")
    class AllowedChildrenTests {

        @Test
        @DisplayName("父类型查询异常时返回空列表")
        void parentQueryFailsReturnsEmpty() {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq("USER"), eq("ROOT")))
                    .thenThrow(new EmptyResultDataAccessException(1));

            assertThat(service.allowedChildren("USER", "ROOT")).isEmpty();
        }

        @Test
        @DisplayName("子类型 JSON 为 null 字面量时返回空列表")
        void nullJsonReturnsEmpty() {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq("USER"), eq("ROOT")))
                    .thenReturn("null");

            assertThat(service.allowedChildren("USER", "ROOT")).isEmpty();
        }

        @Test
        @DisplayName("子类型 JSON 为 [] 时返回空列表")
        void emptyArrayJsonReturnsEmpty() {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq("USER"), eq("ROOT")))
                    .thenReturn("[]");

            assertThat(service.allowedChildren("USER", "ROOT")).isEmpty();
        }

        @Test
        @DisplayName("子类型 JSON 解析失败返回空列表")
        void invalidJsonReturnsEmpty() {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq("USER"), eq("ROOT")))
                    .thenReturn("not-a-json");

            assertThat(service.allowedChildren("USER", "ROOT")).isEmpty();
        }

        @Test
        @DisplayName("子类型 JSON 有效时按 IN 子句查询子配置")
        void validJsonQueriesChildren() {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq("USER"), eq("ROOT")))
                    .thenReturn("[\"A\",\"B\"]");
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.singletonList(new HashMap<>()));

            List<Map<String, Object>> result = service.allowedChildren("USER", "ROOT");

            assertThat(result).hasSize(1);
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).queryForList(sql.capture(), params.capture());
            assertThat(sql.getValue()).contains("type_code IN (?,?)");
            assertThat(params.getValue()).containsExactly("USER", "A", "B");
        }
    }

    // ==================== create ====================

    @Nested
    @DisplayName("create 创建自定义类型")
    class CreateTests {

        @Test
        @DisplayName("create 组装 INSERT 并带 CUSTOM industry")
        void create() throws Exception {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "USER");
            data.put("typeCode", "X");
            data.put("typeName", "X类型");
            data.put("category", "STUDENT");
            data.put("parentTypeCode", null);

            service.create(data);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(sql.capture(), params.capture());
            assertThat(sql.getValue()).contains("INSERT INTO entity_type_configs");
            assertThat(sql.getValue()).contains("'CUSTOM'");
            Object[] p = params.getValue();
            assertThat(p[0]).isEqualTo("USER");
            assertThat(p[1]).isEqualTo("X");
            assertThat(p[2]).isEqualTo("X类型");
            // allowedChildTypeCodes 默认空数组 JSON
            assertThat(p[5]).isEqualTo("[]");
            // metadataSchema 默认值
            assertThat(p[6]).isEqualTo("{\"fields\":[]}");
            assertThat(p[7]).isEqualTo("{}");
        }

        @Test
        @DisplayName("非法 entityType 抛 IllegalArgumentException, 不落库")
        void invalidEntityTypeThrows() {
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "GARBAGE");
            data.put("typeCode", "X");
            data.put("typeName", "X类型");

            assertThatThrownBy(() -> service.create(data))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("USER / ORG_UNIT / PLACE");
            verifyNoLandingInsert();
        }

        @Test
        @DisplayName("空 typeCode 抛 IllegalArgumentException")
        void blankTypeCodeThrows() {
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "USER");
            data.put("typeCode", "  ");
            data.put("typeName", "X类型");

            assertThatThrownBy(() -> service.create(data))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("typeCode 不能为空");
            verifyNoLandingInsert();
        }

        @Test
        @DisplayName("空 typeName 抛 IllegalArgumentException")
        void blankTypeNameThrows() {
            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "USER");
            data.put("typeCode", "X");
            data.put("typeName", "");

            assertThatThrownBy(() -> service.create(data))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("typeName 不能为空");
            verifyNoLandingInsert();
        }

        @Test
        @DisplayName("typeCode 已存在抛 IllegalArgumentException (唯一性预检, 不裸抛 SQLException)")
        void duplicateTypeCodeThrows() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("USER"), eq("DUP")))
                    .thenReturn(1);

            Map<String, Object> data = new HashMap<>();
            data.put("entityType", "USER");
            data.put("typeCode", "DUP");
            data.put("typeName", "重复类型");

            assertThatThrownBy(() -> service.create(data))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("类型编码已存在: DUP");
            verifyNoLandingInsert();
        }

        private void verifyNoLandingInsert() {
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }
    }

    // ==================== update ====================

    @Nested
    @DisplayName("update 更新类型配置")
    class UpdateTests {

        @Test
        @DisplayName("非插件类型更新: 动态 SET 只含传入字段, 不碰 overridden_fields")
        void updateNonPlugin() throws Exception {
            // 7204104c partial-update 语义: SET 按 data.containsKey 动态拼列 (防未传字段被清空);
            // overridden_fields 是插件类型的覆写追踪, 非插件类型的 UPDATE 完全不含该列。
            Map<String, Object> current = new HashMap<>();
            current.put("is_plugin_registered", 0);
            current.put("type_name", "旧名");
            current.put("category", "STUDENT");
            current.put("ui_config", null);
            current.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(1L))).thenReturn(current);
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> data = new HashMap<>();
            data.put("typeName", "新名");
            data.put("category", "STUDENT");

            service.update(1L, data);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(sql.capture(), params.capture());
            assertThat(sql.getValue())
                    .contains("UPDATE entity_type_configs SET ")
                    .contains("type_name=?")
                    .contains("category=?")
                    .doesNotContain("overridden_fields")
                    // 未传入的字段不得进 SET (partial-update 防清空)
                    .doesNotContain("ui_config")
                    .doesNotContain("parent_type_code");
            // params = [typeName, category, id]
            assertThat(params.getValue()).containsExactly("新名", "STUDENT", 1L);
        }

        @Test
        @DisplayName("插件类型 typeName 变化时 overridden_fields 含 typeName")
        void updatePluginTracksOverride() throws Exception {
            Map<String, Object> current = new HashMap<>();
            current.put("is_plugin_registered", 1);
            current.put("type_name", "旧名");
            current.put("category", "STUDENT");
            current.put("ui_config", null);
            current.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(2L))).thenReturn(current);
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> data = new HashMap<>();
            data.put("typeName", "新名");
            // 不带 category, 避免触发 features 子集校验路径

            service.update(2L, data);

            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(anyString(), params.capture());
            Object[] p = params.getValue();
            // overridden_fields JSON 含 typeName
            assertThat(String.valueOf(p[p.length - 2])).contains("typeName");
        }

        @Test
        @DisplayName("插件类型字段无变化时 overridden_fields 为空数组 JSON")
        void updatePluginNoChange() throws Exception {
            Map<String, Object> current = new HashMap<>();
            current.put("is_plugin_registered", 1);
            current.put("type_name", "同名");
            current.put("category", "STUDENT");
            current.put("ui_config", null);
            current.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(3L))).thenReturn(current);
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> data = new HashMap<>();
            data.put("typeName", "同名");

            service.update(3L, data);

            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(anyString(), params.capture());
            Object[] p = params.getValue();
            assertThat(p[p.length - 2]).isEqualTo("[]");
        }

        @Test
        @DisplayName("features 子集校验: 翻开 category 默认关闭的能力被拒 (payload 省略 category 时回退当前行 category)")
        void featuresSubsetUsesCurrentCategoryFallback() {
            // current.category = ADMIN (requiresOrg 默认 false); payload 只带 features 不带 category。
            // 校验须回退当前行 category, 否则 effectiveCategory 为空被绕过 → 翻开 requiresOrg 会漏判。
            Map<String, Object> current = new HashMap<>();
            current.put("is_plugin_registered", 0);
            current.put("type_name", "管理员");
            current.put("category", "ADMIN");
            current.put("ui_config", null);
            current.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(9L))).thenReturn(current);
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(9L))).thenReturn("USER");

            Map<String, Object> data = new HashMap<>();
            data.put("features", Map.of("requiresOrg", true)); // ADMIN 默认 false

            assertThatThrownBy(() -> service.update(9L, data))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requiresOrg");
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("features 子集校验: category 默认开启的能力可保持开启")
        void featuresSubsetAllowsCategoryEnabled() throws Exception {
            // current.category = STAFF (requiresOrg 默认 true); 保持 requiresOrg=true 合法。
            Map<String, Object> current = new HashMap<>();
            current.put("is_plugin_registered", 0);
            current.put("type_name", "职工");
            current.put("category", "STAFF");
            current.put("ui_config", null);
            current.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(10L))).thenReturn(current);
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(10L))).thenReturn("USER");
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> data = new HashMap<>();
            data.put("features", Map.of("requiresOrg", true));

            service.update(10L, data); // 不抛
            verify(jdbc).update(anyString(), any(Object[].class));
        }
    }

    // ==================== resetField ====================

    @Nested
    @DisplayName("resetField 字段恢复默认")
    class ResetFieldTests {

        @Test
        @DisplayName("非插件类型返回错误消息")
        void nonPluginReturnsMessage() throws Exception {
            Map<String, Object> row = new HashMap<>();
            row.put("is_plugin_registered", 0);
            row.put("plugin_class", null);
            row.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(1L))).thenReturn(row);

            String msg = service.resetField(1L, "typeName");

            assertThat(msg).isEqualTo("非插件类型无需恢复");
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("插件类型但无 plugin_class 返回错误消息")
        void pluginWithoutClassReturnsMessage() throws Exception {
            Map<String, Object> row = new HashMap<>();
            row.put("is_plugin_registered", 1);
            row.put("plugin_class", null);
            row.put("overridden_fields", null);
            when(jdbc.queryForMap(anyString(), eq(2L))).thenReturn(row);

            String msg = service.resetField(2L, "typeName");

            assertThat(msg).isEqualTo("无插件类, 无法恢复默认值");
        }

        @Test
        @DisplayName("plugin_class 无法解析时返回字段未声明消息")
        void unresolvablePluginClassReturnsMessage() throws Exception {
            Map<String, Object> row = new HashMap<>();
            row.put("is_plugin_registered", 1);
            row.put("plugin_class", "com.nonexistent.FakePlugin");
            row.put("overridden_fields", "[\"typeName\"]");
            when(jdbc.queryForMap(anyString(), eq(3L))).thenReturn(row);

            String msg = service.resetField(3L, "typeName");

            // resolvePluginOriginal 反射失败 -> 返回 null -> 该分支错误消息
            assertThat(msg).contains("未提供字段 [typeName] 的声明");
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete 删除类型配置 (插件保护 + 在用保护)")
    class DeleteTests {

        private Map<String, Object> row(int isPlugin, String entityType, String typeCode) {
            Map<String, Object> r = new HashMap<>();
            r.put("is_plugin_registered", isPlugin);
            r.put("entity_type", entityType);
            r.put("type_code", typeCode);
            return r;
        }

        @Test
        @DisplayName("插件注册类型不能删除")
        void pluginCannotDelete() {
            when(jdbc.queryForMap(anyString(), eq(1L))).thenReturn(row(1, "USER", "STUDENT"));

            String msg = service.delete(1L);

            assertThat(msg).isEqualTo("插件注册的类型不能删除");
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("自定义类型仍有实体在用时拒绝删除 (防孤儿化)")
        void customTypeInUseBlocked() {
            when(jdbc.queryForMap(anyString(), eq(2L))).thenReturn(row(0, "PLACE", "MY_ROOM"));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq("MY_ROOM"))).thenReturn(3L);

            String msg = service.delete(2L);

            assertThat(msg).contains("正在被 3 个实体使用");
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("自定义类型无实体在用时逻辑删除成功返回 null")
        void customTypeNotInUseDeleted() {
            when(jdbc.queryForMap(anyString(), eq(3L))).thenReturn(row(0, "USER", "MY_ROLE"));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq("MY_ROLE"))).thenReturn(0L);
            when(jdbc.update(anyString(), eq(3L))).thenReturn(1);

            String msg = service.delete(3L);

            assertThat(msg).isNull();
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sql.capture(), eq(3L));
            assertThat(sql.getValue()).contains("deleted=1");
        }

        @Test
        @DisplayName("countEntitiesUsingType 按 entityType 路由到正确宿主表")
        void countRoutesToCorrectTable() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq("X"))).thenReturn(5L);

            assertThat(service.countEntitiesUsingType("USER", "X")).isEqualTo(5L);
            assertThat(service.countEntitiesUsingType("ORG_UNIT", "X")).isEqualTo(5L);
            assertThat(service.countEntitiesUsingType("PLACE", "X")).isEqualTo(5L);
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(jdbc, org.mockito.Mockito.times(3)).queryForObject(sql.capture(), eq(Long.class), eq("X"));
            assertThat(sql.getAllValues().get(0)).contains("FROM users").contains("user_type_code");
            assertThat(sql.getAllValues().get(1)).contains("FROM org_units").contains("type_code");
            assertThat(sql.getAllValues().get(2)).contains("FROM places").contains("type_code");
        }

        @Test
        @DisplayName("countEntitiesUsingType 未知 entityType / 空 typeCode → 0, 不查表")
        void countUnknownReturnsZero() {
            assertThat(service.countEntitiesUsingType("GARBAGE", "X")).isZero();
            assertThat(service.countEntitiesUsingType("USER", null)).isZero();
            assertThat(service.countEntitiesUsingType(null, "X")).isZero();
            verify(jdbc, never()).queryForObject(anyString(), eq(Long.class), any(Object[].class));
        }
    }

    // ==================== addCustomField ====================

    @Nested
    @DisplayName("addCustomField 追加自定义字段")
    class AddCustomFieldTests {

        @Test
        @DisplayName("key 重复时返回错误消息且不更新")
        void duplicateKeyReturnsMessage() throws Exception {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(1L)))
                    .thenReturn("{\"fields\":[{\"key\":\"age\"}]}");

            Map<String, Object> field = new HashMap<>();
            field.put("key", "age");

            String msg = service.addCustomField(1L, field, "age");

            assertThat(msg).isEqualTo("字段 key 已存在: age");
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("新 key 追加字段并标记 system=false")
        void newKeyAppendsField() throws Exception {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(2L)))
                    .thenReturn("{\"fields\":[]}");
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> field = new HashMap<>();
            field.put("key", "nickname");

            String msg = service.addCustomField(2L, field, "nickname");

            assertThat(msg).isNull();
            assertThat(field.get("system")).isEqualTo(false);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(sql.capture(), params.capture());
            assertThat(sql.getValue()).contains("SET metadata_schema = ?");
            assertThat(String.valueOf(params.getValue()[0])).contains("nickname");
        }

        @Test
        @DisplayName("schema 缺 fields 键时按空列表处理仍可追加")
        void schemaWithoutFieldsKey() throws Exception {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(3L)))
                    .thenReturn("{}");
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> field = new HashMap<>();
            field.put("key", "addr");

            assertThat(service.addCustomField(3L, field, "addr")).isNull();
            verify(jdbc).update(anyString(), any(Object[].class));
        }
    }

    // ==================== removeCustomField ====================

    @Nested
    @DisplayName("removeCustomField 移除自定义字段")
    class RemoveCustomFieldTests {

        @Test
        @DisplayName("仅移除 system=false 字段, 保留系统字段")
        void removesOnlyNonSystemField() throws Exception {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(1L)))
                    .thenReturn("{\"fields\":[" +
                            "{\"key\":\"age\",\"system\":true}," +
                            "{\"key\":\"nickname\",\"system\":false}]}");
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.removeCustomField(1L, "nickname");

            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(anyString(), params.capture());
            String written = String.valueOf(params.getValue()[0]);
            assertThat(written).contains("age");
            assertThat(written).doesNotContain("nickname");
        }

        @Test
        @DisplayName("尝试移除系统字段时不删除")
        void doesNotRemoveSystemField() throws Exception {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq(2L)))
                    .thenReturn("{\"fields\":[{\"key\":\"age\",\"system\":true}]}");
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.removeCustomField(2L, "age");

            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(anyString(), params.capture());
            assertThat(String.valueOf(params.getValue()[0])).contains("age");
        }
    }

    // ==================== OVERRIDABLE_FIELDS 常量 ====================

    @Nested
    @DisplayName("常量")
    class ConstantTests {

        @Test
        @DisplayName("OVERRIDABLE_FIELDS = 6 个可覆写字段 (7204104c M4 对齐: +features/parentTypeCode/allowedChildTypeCodes)")
        void overridableFields() {
            assertThat(EntityTypeConfigApplicationService.OVERRIDABLE_FIELDS)
                    .containsExactlyInAnyOrder("typeName", "category", "uiConfig",
                            "features", "parentTypeCode", "allowedChildTypeCodes");
        }
    }
}
