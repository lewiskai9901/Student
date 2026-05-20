package com.school.management.application.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.access.DataPermissionPolicyService;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataPermissionApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 DynamicModuleService / DataPermissionPolicyService / JdbcTemplate,
 * 验证模块分组组装 / 范围类型合并 / 角色权限读写 / 角色智能过滤的编排逻辑.
 *
 * 说明: TenantContextHolder 未设置 context 时 getTenantId() 兜底返回 1L, 测试中确定性.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataPermissionApplicationService 应用服务")
class DataPermissionApplicationServiceTest {

    @Mock DynamicModuleService dynamicModuleService;
    @Mock DataPermissionPolicyService dataPermissionPolicyService;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks DataPermissionApplicationService service;

    // ============================================================
    // helpers
    // ============================================================

    private DataModulePO module(String code, String name, String domainCode, String domainName,
                                String industry, Boolean pluginEnabled) {
        DataModulePO m = new DataModulePO();
        m.setId((long) code.hashCode());
        m.setTenantId(1L);
        m.setModuleCode(code);
        m.setModuleName(name);
        m.setDomainCode(domainCode);
        m.setDomainName(domainName);
        m.setIndustry(industry);
        m.setPluginEnabled(pluginEnabled);
        m.setEnabled(true);
        m.setSortOrder(0);
        return m;
    }

    // ============================================================
    @Nested
    @DisplayName("getAllModulesGroupedByDomain")
    class GroupedByDomainTests {

        @Test
        @DisplayName("按 domainCode 分组并排序, pluginEnabled 字段正确透传")
        void shouldGroupAndSort() {
            DataModulePO core = module("org_unit", "组织", "CORE", "通用核心", "CORE", true);
            DataModulePO edu = module("student", "学生", "education", "教育", "EDU", false);
            Map<String, List<DataModulePO>> grouped = new LinkedHashMap<>();
            grouped.put("education", List.of(edu));
            grouped.put("CORE", List.of(core));
            when(dynamicModuleService.listByDomain(1L, false)).thenReturn(grouped);

            List<DataPermissionApplicationService.DomainModulesDTO> result =
                    service.getAllModulesGroupedByDomain();

            assertThat(result).hasSize(2);
            // 排序后 CORE 在 education 前 (Comparator.comparing domainCode)
            assertThat(result.get(0).getDomainCode()).isEqualTo("CORE");
            assertThat(result.get(0).getDomainName()).isEqualTo("通用核心");
            assertThat(result.get(0).getModules()).hasSize(1);
            assertThat(result.get(0).getModules().get(0).getCode()).isEqualTo("org_unit");
            assertThat(result.get(0).getModules().get(0).getPluginEnabled()).isTrue();

            assertThat(result.get(1).getDomainCode()).isEqualTo("education");
            assertThat(result.get(1).getModules().get(0).getPluginEnabled()).isFalse();
        }

        @Test
        @DisplayName("pluginEnabled 为 null 时按启用处理")
        void shouldTreatNullPluginEnabledAsEnabled() {
            DataModulePO m = module("task", "任务", "CORE", "通用核心", "CORE", null);
            when(dynamicModuleService.listByDomain(1L, false))
                    .thenReturn(Map.of("CORE", List.of(m)));

            List<DataPermissionApplicationService.DomainModulesDTO> result =
                    service.getAllModulesGroupedByDomain();

            assertThat(result.get(0).getModules().get(0).getPluginEnabled()).isTrue();
        }

        @Test
        @DisplayName("domainName 缺失时回退为 domainCode")
        void shouldFallbackDomainNameToCode() {
            DataModulePO m = module("x", "X", "weird", null, "CUSTOM", true);
            when(dynamicModuleService.listByDomain(1L, false))
                    .thenReturn(Map.of("weird", List.of(m)));

            List<DataPermissionApplicationService.DomainModulesDTO> result =
                    service.getAllModulesGroupedByDomain();

            assertThat(result.get(0).getDomainName()).isEqualTo("weird");
        }

        @Test
        @DisplayName("includeDisabled=true 时传给底层 service")
        void shouldPassIncludeDisabledFlag() {
            when(dynamicModuleService.listByDomain(1L, true)).thenReturn(Collections.emptyMap());

            List<DataPermissionApplicationService.DomainModulesDTO> result =
                    service.getAllModulesGroupedByDomain(true);

            assertThat(result).isEmpty();
            verify(dynamicModuleService).listByDomain(1L, true);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getAllScopeTypes")
    class ScopeTypesTests {

        @Test
        @DisplayName("core 5 种按 level 倒序 + 插件维度拼接在后")
        void shouldCombineCoreAndDynamic() {
            DataPermissionApplicationService.ScopeTypeDTO plugin =
                    new DataPermissionApplicationService.ScopeTypeDTO(
                            "BY_MAJOR", "按专业", "专业维度", "PLUGIN:education");
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenReturn(List.of(plugin));

            List<DataPermissionApplicationService.ScopeTypeDTO> result = service.getAllScopeTypes();

            // 5 hardcoded + 1 plugin
            assertThat(result).hasSize(6);
            // 倒序: ALL(100) 先于 SELF(20)
            assertThat(result.get(0).getCode()).isEqualTo("ALL");
            assertThat(result.get(0).getSource()).isEqualTo("CORE");
            assertThat(result.get(4).getCode()).isEqualTo("SELF");
            assertThat(result.get(5).getCode()).isEqualTo("BY_MAJOR");
            assertThat(result.get(5).getSource()).isEqualTo("PLUGIN:education");
        }

        @Test
        @DisplayName("data_scope_dims 查询失败时只返 core 5 种, 不抛异常")
        void shouldFallbackToCoreWhenQueryFails() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                    .thenThrow(new RuntimeException("table not found"));

            List<DataPermissionApplicationService.ScopeTypeDTO> result = service.getAllScopeTypes();

            assertThat(result).hasSize(5);
            assertThat(result).allMatch(s -> "CORE".equals(s.getSource()));
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getRoleDataPermissions")
    class GetRolePermissionsTests {

        @Test
        @DisplayName("已配置模块返回真实 scopeCode + scopeItems")
        void shouldReturnConfiguredPermission() {
            DataScopeItem item = DataScopeItem.builder()
                    .scopeId(7L).scopeName("一年级").itemTypeCode("org_unit").includeChildren(true)
                    .build();
            RoleDataPermission perm = RoleDataPermission.builder()
                    .roleId(9L).moduleCode("student").scopeCode("CUSTOM")
                    .scopeItems(List.of(item)).build();
            when(dataPermissionPolicyService.getRolePermissions(1L, 9L)).thenReturn(List.of(perm));
            when(dynamicModuleService.listModules(1L))
                    .thenReturn(List.of(module("student", "学生", "education", "教育", "EDU", true)));

            List<DataPermissionApplicationService.RoleModulePermissionDTO> result =
                    service.getRoleDataPermissions(9L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModuleCode()).isEqualTo("student");
            assertThat(result.get(0).getScopeCode()).isEqualTo("CUSTOM");
            assertThat(result.get(0).getScopeItems()).hasSize(1);
            assertThat(result.get(0).getScopeItems().get(0).getScopeId()).isEqualTo(7L);
            assertThat(result.get(0).getScopeItems().get(0).getIncludeChildren()).isTrue();
        }

        @Test
        @DisplayName("未配置模块回退为 SELF + 空 scopeItems")
        void shouldDefaultToSelfWhenNoPermission() {
            when(dataPermissionPolicyService.getRolePermissions(1L, 9L))
                    .thenReturn(Collections.emptyList());
            when(dynamicModuleService.listModules(1L))
                    .thenReturn(List.of(module("org_unit", "组织", "CORE", "核心", "CORE", true)));

            List<DataPermissionApplicationService.RoleModulePermissionDTO> result =
                    service.getRoleDataPermissions(9L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScopeCode()).isEqualTo(DataScope.SELF.getCode());
            assertThat(result.get(0).getScopeItems()).isEmpty();
        }

        @Test
        @DisplayName("已配置模块但 scopeItems 为 null 时返回空列表")
        void shouldHandleNullScopeItems() {
            RoleDataPermission perm = RoleDataPermission.builder()
                    .roleId(9L).moduleCode("student").scopeCode("ALL").scopeItems(null).build();
            when(dataPermissionPolicyService.getRolePermissions(1L, 9L)).thenReturn(List.of(perm));
            when(dynamicModuleService.listModules(1L))
                    .thenReturn(List.of(module("student", "学生", "education", "教育", "EDU", true)));

            List<DataPermissionApplicationService.RoleModulePermissionDTO> result =
                    service.getRoleDataPermissions(9L);

            assertThat(result.get(0).getScopeItems()).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("saveRoleDataPermissions")
    class SaveRolePermissionsTests {

        @Test
        @DisplayName("allowed_scopes 内的 scope 通过校验并保存")
        void shouldSaveWhenScopeAllowed() {
            DataModulePO m = module("student", "学生", "education", "教育", "EDU", true);
            m.setAllowedScopes(Arrays.asList("ALL", "SELF"));
            when(dynamicModuleService.listModules(1L, true)).thenReturn(List.of(m));

            DataPermissionApplicationService.SavePermissionCommand cmd =
                    new DataPermissionApplicationService.SavePermissionCommand(
                            "student", "ALL", null);

            service.saveRoleDataPermissions(9L, List.of(cmd));

            ArgumentCaptor<List<RoleDataPermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(dataPermissionPolicyService).saveRolePermissions(eq(1L), eq(9L), captor.capture());
            List<RoleDataPermission> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getRoleId()).isEqualTo(9L);
            assertThat(saved.get(0).getModuleCode()).isEqualTo("student");
            assertThat(saved.get(0).getScopeCode()).isEqualTo("ALL");
        }

        @Test
        @DisplayName("scope 不在 allowed_scopes 内时抛 BusinessException")
        void shouldRejectWhenScopeNotAllowed() {
            DataModulePO m = module("student", "学生信息", "education", "教育", "EDU", true);
            m.setAllowedScopes(Arrays.asList("ALL", "SELF"));
            when(dynamicModuleService.listModules(1L, true)).thenReturn(List.of(m));

            DataPermissionApplicationService.SavePermissionCommand cmd =
                    new DataPermissionApplicationService.SavePermissionCommand(
                            "student", "DEPARTMENT", null);

            assertThatThrownBy(() -> service.saveRoleDataPermissions(9L, List.of(cmd)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("学生信息")
                    .hasMessageContaining("DEPARTMENT");
            verify(dataPermissionPolicyService, never())
                    .saveRolePermissions(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("模块未配置 allowed_scopes 时宽松放行")
        void shouldSkipValidationWhenNoAllowedScopes() {
            DataModulePO m = module("student", "学生", "education", "教育", "EDU", true);
            m.setAllowedScopes(null);
            when(dynamicModuleService.listModules(1L, true)).thenReturn(List.of(m));

            DataPermissionApplicationService.SavePermissionCommand cmd =
                    new DataPermissionApplicationService.SavePermissionCommand(
                            "student", "DEPARTMENT", null);

            service.saveRoleDataPermissions(9L, List.of(cmd));

            verify(dataPermissionPolicyService).saveRolePermissions(eq(1L), eq(9L), any());
        }

        @Test
        @DisplayName("CUSTOM scope 携带 scopeItems 被映射进 RoleDataPermission")
        void shouldMapCustomScopeItems() {
            when(dynamicModuleService.listModules(1L, true)).thenReturn(Collections.emptyList());

            DataPermissionApplicationService.ScopeItemDTO itemDto =
                    new DataPermissionApplicationService.ScopeItemDTO(
                            5L, "二班", "org_unit", true);
            DataPermissionApplicationService.SavePermissionCommand cmd =
                    new DataPermissionApplicationService.SavePermissionCommand(
                            "student", "CUSTOM", List.of(itemDto));

            service.saveRoleDataPermissions(9L, List.of(cmd));

            ArgumentCaptor<List<RoleDataPermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(dataPermissionPolicyService).saveRolePermissions(eq(1L), eq(9L), captor.capture());
            RoleDataPermission p = captor.getValue().get(0);
            assertThat(p.getScopeCode()).isEqualTo("CUSTOM");
            assertThat(p.getScopeItems()).hasSize(1);
            assertThat(p.getScopeItems().get(0).getScopeId()).isEqualTo(5L);
            assertThat(p.getScopeItems().get(0).getItemTypeCode()).isEqualTo("org_unit");
            assertThat(p.getScopeItems().get(0).getIncludeChildren()).isTrue();
        }

        @Test
        @DisplayName("CUSTOM scopeItem includeChildren 为 null 时默认为 false")
        void shouldDefaultIncludeChildrenToFalse() {
            when(dynamicModuleService.listModules(1L, true)).thenReturn(Collections.emptyList());

            DataPermissionApplicationService.ScopeItemDTO itemDto =
                    new DataPermissionApplicationService.ScopeItemDTO(
                            5L, "二班", "org_unit", null);
            DataPermissionApplicationService.SavePermissionCommand cmd =
                    new DataPermissionApplicationService.SavePermissionCommand(
                            "student", "CUSTOM", List.of(itemDto));

            service.saveRoleDataPermissions(9L, List.of(cmd));

            ArgumentCaptor<List<RoleDataPermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(dataPermissionPolicyService).saveRolePermissions(eq(1L), eq(9L), captor.capture());
            assertThat(captor.getValue().get(0).getScopeItems().get(0).getIncludeChildren()).isFalse();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getAllModules — 平铺 map 格式")
    class GetAllModulesFlatTests {

        @Test
        @DisplayName("组装 code/name/domain/industry/pluginEnabled 字段")
        void shouldFlattenModules() {
            DataModulePO m = module("org_unit", "组织", "CORE", "核心", "CORE", true);
            when(dynamicModuleService.listByDomain(1L, false))
                    .thenReturn(Map.of("CORE", List.of(m)));

            Map<String, List<Map<String, String>>> result = service.getAllModules();

            assertThat(result).containsKey("CORE");
            Map<String, String> entry = result.get("CORE").get(0);
            assertThat(entry.get("code")).isEqualTo("org_unit");
            assertThat(entry.get("name")).isEqualTo("组织");
            assertThat(entry.get("domain")).isEqualTo("CORE");
            assertThat(entry.get("industry")).isEqualTo("CORE");
            assertThat(entry.get("pluginEnabled")).isEqualTo("true");
        }

        @Test
        @DisplayName("industry 为 null 时回退为 CUSTOM")
        void shouldFallbackIndustryToCustom() {
            DataModulePO m = module("x", "X", "CORE", "核心", null, true);
            when(dynamicModuleService.listByDomain(1L, false))
                    .thenReturn(Map.of("CORE", List.of(m)));

            Map<String, List<Map<String, String>>> result = service.getAllModules();

            assertThat(result.get("CORE").get(0).get("industry")).isEqualTo("CUSTOM");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getAllScopes")
    class GetAllScopesTests {

        @Test
        @DisplayName("返回 5 种 core scope, 按 level 倒序, 含 level 字段")
        void shouldReturnSortedScopes() {
            List<Map<String, String>> result = service.getAllScopes();

            assertThat(result).hasSize(5);
            assertThat(result.get(0).get("code")).isEqualTo("ALL");
            assertThat(result.get(0).get("level")).isEqualTo("100");
            assertThat(result.get(4).get("code")).isEqualTo("SELF");
            assertThat(result.get(4).get("level")).isEqualTo("20");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getModulesForRole — 角色智能过滤")
    class GetModulesForRoleTests {

        @Test
        @DisplayName("roleId 为 null: 兼容路径返全部, filtered=false")
        void shouldReturnAllWhenRoleIdNull() {
            when(dynamicModuleService.listModules(1L, false))
                    .thenReturn(List.of(module("org_unit", "组织", "CORE", "核心", "CORE", true)));

            Map<String, Object> result = service.getModulesForRole(null, false);

            assertThat(result.get("relevant")).asList().hasSize(1);
            assertThat(result.get("advanced")).asList().isEmpty();
            assertThat(((Map<?, ?>) result.get("meta")).get("filtered")).isEqualTo(false);
        }

        @Test
        @DisplayName("角色不存在: 返全部, meta 标注角色不存在")
        void shouldReturnAllWhenRoleNotFound() {
            when(dynamicModuleService.listModules(1L, false))
                    .thenReturn(List.of(module("org_unit", "组织", "CORE", "核心", "CORE", true)));
            when(jdbcTemplate.queryForMap(anyString(), eq(99L)))
                    .thenThrow(new RuntimeException("not found"));

            Map<String, Object> result = service.getModulesForRole(99L, false);

            assertThat(result.get("relevant")).asList().hasSize(1);
            assertThat(((Map<?, ?>) result.get("meta")).get("filtered")).isEqualTo(false);
            assertThat(((Map<?, ?>) result.get("meta")).get("filterRule").toString())
                    .contains("角色不存在");
        }

        @Test
        @DisplayName("SUPER_ADMIN 豁免: 全部进 relevant, advanced 空")
        void shouldExemptSuperAdmin() {
            when(dynamicModuleService.listModules(1L, false)).thenReturn(List.of(
                    module("org_unit", "组织", "CORE", "核心", "CORE", true),
                    module("student", "学生", "education", "教育", "EDU", true)));
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("id", 1L);
            role.put("role_code", "SUPER_ADMIN");
            role.put("role_name", "超管");
            role.put("role_type", "SYSTEM");
            role.put("industry", null);
            when(jdbcTemplate.queryForMap(anyString(), eq(1L))).thenReturn(role);

            Map<String, Object> result = service.getModulesForRole(1L, false);

            assertThat(result.get("relevant")).asList().hasSize(2);
            assertThat(result.get("advanced")).asList().isEmpty();
            Map<?, ?> meta = (Map<?, ?>) result.get("meta");
            assertThat(meta.get("filtered")).isEqualTo(false);
            assertThat(meta.get("roleIndustry")).isEqualTo("CORE");
        }

        @Test
        @DisplayName("EDU 行业角色: CARE 模块被分到 advanced 并附跨行业理由")
        void shouldFilterCrossIndustryModules() {
            when(dynamicModuleService.listModules(1L, false)).thenReturn(List.of(
                    module("org_unit", "组织", "CORE", "核心", "CORE", true),
                    module("student", "学生", "education", "教育", "EDU", true),
                    module("patient", "病患", "healthcare", "医疗", "CARE", true)));
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("role_code", "EDU_TEACHER");
            role.put("role_name", "教师");
            role.put("role_type", "BUSINESS");
            role.put("industry", "EDU");
            when(jdbcTemplate.queryForMap(anyString(), eq(2L))).thenReturn(role);
            // 无功能权限 → hasPerms=false, 仅按 industry 过滤
            when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(2L)))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> result = service.getModulesForRole(2L, false);

            // CORE + EDU 进 relevant, CARE 进 advanced
            assertThat(result.get("relevant")).asList().hasSize(2);
            List<?> advanced = (List<?>) result.get("advanced");
            assertThat(advanced).hasSize(1);
            Map<?, ?> advModule = (Map<?, ?>) advanced.get(0);
            assertThat(advModule.get("moduleCode")).isEqualTo("patient");
            assertThat(advModule.get("reason").toString()).contains("跨行业");
            Map<?, ?> meta = (Map<?, ?>) result.get("meta");
            assertThat(meta.get("filtered")).isEqualTo(true);
            assertThat(meta.get("totalRelevant")).isEqualTo(2);
            assertThat(meta.get("totalAdvanced")).isEqualTo(1);
        }

        @Test
        @DisplayName("有功能权限的角色: 非 CORE 模块无前缀匹配 → advanced (角色无相关功能权限)")
        void shouldFilterByPermissionPrefix() {
            when(dynamicModuleService.listModules(1L, false)).thenReturn(List.of(
                    module("org_unit", "组织", "CORE", "核心", "CORE", true),
                    module("student", "学生", "education", "教育", "EDU", true),
                    module("teacher", "教师", "education", "教育", "EDU", true)));
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("role_code", "EDU_ROLE");
            role.put("role_name", "角色");
            role.put("role_type", "BUSINESS");
            role.put("industry", "EDU");
            when(jdbcTemplate.queryForMap(anyString(), eq(3L))).thenReturn(role);
            // 只有 student 前缀权限 → teacher 无匹配
            when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(3L)))
                    .thenReturn(List.of("student:info:view"));

            Map<String, Object> result = service.getModulesForRole(3L, false);

            // CORE + student 进 relevant; teacher 进 advanced
            assertThat(result.get("relevant")).asList().hasSize(2);
            List<?> advanced = (List<?>) result.get("advanced");
            assertThat(advanced).hasSize(1);
            assertThat(((Map<?, ?>) advanced.get(0)).get("moduleCode")).isEqualTo("teacher");
            assertThat(((Map<?, ?>) advanced.get(0)).get("reason").toString())
                    .contains("无 teacher 相关功能权限");
        }

        @Test
        @DisplayName("CUSTOM 行业角色: 所有行业模块均允许")
        void shouldAllowAllIndustriesForCustomRole() {
            when(dynamicModuleService.listModules(1L, false)).thenReturn(List.of(
                    module("student", "学生", "education", "教育", "EDU", true),
                    module("patient", "病患", "healthcare", "医疗", "CARE", true)));
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("role_code", "CUSTOM_ROLE");
            role.put("role_name", "自定义");
            role.put("role_type", "BUSINESS");
            role.put("industry", "CUSTOM");
            when(jdbcTemplate.queryForMap(anyString(), eq(4L))).thenReturn(role);
            when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(4L)))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> result = service.getModulesForRole(4L, false);

            assertThat(result.get("relevant")).asList().hasSize(2);
            assertThat(result.get("advanced")).asList().isEmpty();
        }

        @Test
        @DisplayName("查询权限码失败时不抛异常, hasPerms=false 走 industry 过滤")
        void shouldToleratePermissionQueryFailure() {
            when(dynamicModuleService.listModules(1L, false))
                    .thenReturn(List.of(module("student", "学生", "education", "教育", "EDU", true)));
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("role_code", "EDU_ROLE");
            role.put("role_name", "角色");
            role.put("role_type", "BUSINESS");
            role.put("industry", "EDU");
            when(jdbcTemplate.queryForMap(anyString(), eq(5L))).thenReturn(role);
            when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(5L)))
                    .thenThrow(new RuntimeException("perm query failed"));

            Map<String, Object> result = service.getModulesForRole(5L, false);

            assertThat(result.get("relevant")).asList().hasSize(1);
            Map<?, ?> meta = (Map<?, ?>) result.get("meta");
            assertThat(meta.get("filterRule").toString()).contains("未绑权限");
        }
    }
}
