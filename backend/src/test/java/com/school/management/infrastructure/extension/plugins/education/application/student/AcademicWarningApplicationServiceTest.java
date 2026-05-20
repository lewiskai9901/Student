package com.school.management.infrastructure.extension.plugins.education.application.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.school.management.infrastructure.access.OrgScopeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AcademicWarningApplicationService 单测.
 *
 * 用 Mockito 隔离 JdbcTemplate, 用真实 ObjectMapper (无 IO, 确定性).
 * 覆盖规则 CRUD / 扫描 (3 种规则类型 + 异常分支) / 预警处理 / 统计.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AcademicWarningApplicationService 应用服务")
class AcademicWarningApplicationServiceTest {

    @Mock JdbcTemplate jdbc;

    @Mock OrgScopeHelper orgScopeHelper;

    ObjectMapper objectMapper = new ObjectMapper();

    AcademicWarningApplicationService service;

    @BeforeEach
    void setUp() {
        // 数据权限关闭 / super admin 等价: orgScopeClause 返回 "" (无收窄), isOrgAllowed 放行.
        lenient().when(orgScopeHelper.orgScopeClause(anyString())).thenReturn("");
        lenient().when(orgScopeHelper.isOrgAllowed(any())).thenReturn(true);
        service = new AcademicWarningApplicationService(jdbc, objectMapper, orgScopeHelper);
    }

    private Map<String, Object> ruleRow(Object id, String type, int level, String paramsJson) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("rule_type", type);
        m.put("rule_name", "规则-" + id);
        m.put("warning_level", level);
        m.put("condition_params", paramsJson);
        return m;
    }

    // ============================================================
    @Nested
    @DisplayName("listRules — 查询规则列表")
    class ListRulesTests {

        @Test
        @DisplayName("解析 conditionParams JSON 字符串为对象")
        void shouldParseConditionParams() {
            Map<String, Object> rule = new HashMap<>();
            rule.put("id", 1L);
            rule.put("conditionParams", "{\"minFailCount\":3}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));

            List<Map<String, Object>> result = service.listRules();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("conditionParams")).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) result.get(0).get("conditionParams");
            assertThat(parsed).containsEntry("minFailCount", 3);
        }

        @Test
        @DisplayName("非法 JSON 的 conditionParams 保持原字符串不报错")
        void shouldLeaveInvalidJsonAsString() {
            Map<String, Object> rule = new HashMap<>();
            rule.put("conditionParams", "not-json");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));

            List<Map<String, Object>> result = service.listRules();
            assertThat(result.get(0).get("conditionParams")).isEqualTo("not-json");
        }

        @Test
        @DisplayName("空结果返回空列表")
        void shouldReturnEmptyList() {
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>());
            assertThat(service.listRules()).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("createRule — 创建规则")
    class CreateRuleTests {

        @Test
        @DisplayName("conditionParams 为 Map 时序列化为 JSON 并 INSERT, 返回新 id")
        void shouldCreateWithMapParams() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ruleName", "挂科预警");
            data.put("ruleType", "GRADE_FAIL");
            data.put("warningLevel", 2);
            data.put("conditionParams", Map.of("minFailCount", 3));
            data.put("applicableGrades", "2024");
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(77L);

            Long id = service.createRule(data);

            assertThat(id).isEqualTo(77L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    eq("挂科预警"), eq("GRADE_FAIL"), eq(2),
                    eq("{\"minFailCount\":3}"), eq("2024"), any());
            assertThat(sqlCap.getValue()).contains("INSERT INTO academic_warning_rules");
        }

        @Test
        @DisplayName("conditionParams 已是字符串时直接使用")
        void shouldCreateWithStringParams() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ruleName", "出勤预警");
            data.put("ruleType", "ATTENDANCE_LOW");
            data.put("warningLevel", 1);
            data.put("conditionParams", "{\"minAttendanceRate\":85}");
            data.put("applicableGrades", null);
            when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);

            Long id = service.createRule(data);

            assertThat(id).isEqualTo(5L);
            verify(jdbc).update(anyString(), eq("出勤预警"), eq("ATTENDANCE_LOW"), eq(1),
                    eq("{\"minAttendanceRate\":85}"), any(), any());
        }

        @Test
        @DisplayName("conditionParams 不可序列化时返回 null, 不执行 INSERT")
        void shouldReturnNullWhenParamsUnserializable() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ruleName", "x");
            data.put("ruleType", "GRADE_FAIL");
            data.put("warningLevel", 1);
            data.put("conditionParams", new Object() {
                // self-referential object Jackson cannot serialize
                @SuppressWarnings("unused")
                Object getSelf() { throw new RuntimeException("not serializable"); }
            });
            data.put("applicableGrades", null);

            Long id = service.createRule(data);

            assertThat(id).isNull();
            verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(), any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updateRule — 更新规则")
    class UpdateRuleTests {

        @Test
        @DisplayName("成功更新返回 true 并执行 UPDATE")
        void shouldUpdate() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ruleName", "改名");
            data.put("ruleType", "CREDIT_SHORT");
            data.put("warningLevel", 3);
            data.put("conditionParams", Map.of("expectedCredits", 30));
            data.put("applicableGrades", "2023");

            boolean ok = service.updateRule(9L, data);

            assertThat(ok).isTrue();
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    eq("改名"), eq("CREDIT_SHORT"), eq(3),
                    eq("{\"expectedCredits\":30}"), eq("2023"), eq(9L));
            assertThat(sqlCap.getValue()).contains("UPDATE academic_warning_rules SET");
        }

        @Test
        @DisplayName("conditionParams 不可序列化时返回 false 不执行 UPDATE")
        void shouldReturnFalseWhenParamsUnserializable() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ruleName", "x");
            data.put("ruleType", "GRADE_FAIL");
            data.put("warningLevel", 1);
            data.put("conditionParams", new Object() {
                @SuppressWarnings("unused")
                Object getSelf() { throw new RuntimeException("bad"); }
            });
            data.put("applicableGrades", null);

            boolean ok = service.updateRule(9L, data);

            assertThat(ok).isFalse();
            verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(), any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("deleteRule / toggleRule")
    class DeleteToggleTests {

        @Test
        @DisplayName("deleteRule 执行逻辑删除 SQL")
        void shouldSoftDelete() {
            service.deleteRule(3L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(3L));
            assertThat(sqlCap.getValue()).contains("SET deleted = 1");
        }

        @Test
        @DisplayName("toggleRule 翻转 enabled 标志")
        void shouldToggleEnabled() {
            service.toggleRule(3L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(3L));
            assertThat(sqlCap.getValue()).contains("enabled = 1 - enabled");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("scanWarnings — 扫描预警")
    class ScanWarningsTests {

        private Map<String, Object> flaggedStudent(long studentId) {
            Map<String, Object> s = new HashMap<>();
            s.put("student_id", studentId);
            s.put("student_no", "S" + studentId);
            s.put("student_name", "学生" + studentId);
            s.put("org_unit_id", 10L);
            s.put("class_name", "一班");
            s.put("fail_count", 3L);
            s.put("failed_courses", "数学、英语");
            s.put("rate", 65.0);
            s.put("earned_credits", 12L);
            return s;
        }

        @Test
        @DisplayName("无启用规则时 totalWarnings=0, rulesScanned=0")
        void shouldReturnZeroWhenNoRules() {
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>());

            Map<String, Object> result = service.scanWarnings(1L);

            assertThat(result.get("totalWarnings")).isEqualTo(0);
            assertThat(result.get("rulesScanned")).isEqualTo(0);
        }

        @Test
        @DisplayName("GRADE_FAIL 规则: 命中学生时插入预警并计数")
        void shouldScanGradeFail() {
            Map<String, Object> rule = ruleRow(1L, "GRADE_FAIL", 2, "{\"minFailCount\":2}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            when(jdbc.queryForList(anyString(), eq(1L), eq(2)))
                    .thenReturn(new ArrayList<>(List.of(flaggedStudent(100L))));
            // insertWarning duplicate check -> 0 means not existing
            when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            Map<String, Object> result = service.scanWarnings(1L);

            assertThat(result.get("totalWarnings")).isEqualTo(1);
            assertThat(result.get("rulesScanned")).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
            assertThat(sqlCap.getValue()).contains("INSERT INTO academic_warnings");
        }

        @Test
        @DisplayName("ATTENDANCE_LOW 规则: 命中学生计数")
        void shouldScanAttendanceLow() {
            Map<String, Object> rule = ruleRow(2L, "ATTENDANCE_LOW", 1, "{\"minAttendanceRate\":80}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            when(jdbc.queryForList(anyString(), eq(1L), eq(80)))
                    .thenReturn(new ArrayList<>(List.of(flaggedStudent(200L))));
            when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            Map<String, Object> result = service.scanWarnings(1L);
            assertThat(result.get("totalWarnings")).isEqualTo(1);
        }

        @Test
        @DisplayName("CREDIT_SHORT 规则: 命中学生计数")
        void shouldScanCreditShort() {
            Map<String, Object> rule = ruleRow(3L, "CREDIT_SHORT", 3, "{\"expectedCredits\":30,\"actualCreditsBelow\":20}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            when(jdbc.queryForList(anyString(), eq(1L), eq(20)))
                    .thenReturn(new ArrayList<>(List.of(flaggedStudent(300L))));
            when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            Map<String, Object> result = service.scanWarnings(1L);
            assertThat(result.get("totalWarnings")).isEqualTo(1);
        }

        @Test
        @DisplayName("已存在未关闭预警时不重复插入")
        void shouldSkipDuplicateWarning() {
            Map<String, Object> rule = ruleRow(1L, "GRADE_FAIL", 2, "{\"minFailCount\":2}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            when(jdbc.queryForList(anyString(), eq(1L), eq(2)))
                    .thenReturn(new ArrayList<>(List.of(flaggedStudent(100L))));
            // duplicate check returns 1 -> already exists
            when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(1);

            Map<String, Object> result = service.scanWarnings(1L);

            assertThat(result.get("totalWarnings")).isEqualTo(1); // counter still increments
            verify(jdbc, never()).update(anyString(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("非法 condition_params 的规则被跳过, 不计入扫描")
        void shouldSkipRuleWithBadParams() {
            Map<String, Object> rule = ruleRow(1L, "GRADE_FAIL", 2, "{bad json");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));

            Map<String, Object> result = service.scanWarnings(1L);

            assertThat(result.get("totalWarnings")).isEqualTo(0);
            verify(jdbc, never()).queryForList(anyString(), any(Class.class), any());
        }

        @Test
        @DisplayName("未知规则类型被忽略, totalWarnings 保持 0")
        void shouldIgnoreUnknownRuleType() {
            Map<String, Object> rule = ruleRow(1L, "MYSTERY", 2, "{}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));

            Map<String, Object> result = service.scanWarnings(1L);
            assertThat(result.get("totalWarnings")).isEqualTo(0);
            assertThat(result.get("rulesScanned")).isEqualTo(1);
        }

        @Test
        @DisplayName("condition_params 为 null 时按 {} 处理并用默认阈值")
        void shouldHandleNullConditionParams() {
            Map<String, Object> rule = ruleRow(1L, "GRADE_FAIL", 2, null);
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            // default minFailCount = 2
            when(jdbc.queryForList(anyString(), eq(1L), eq(2)))
                    .thenReturn(new ArrayList<>());

            Map<String, Object> result = service.scanWarnings(1L);
            assertThat(result.get("totalWarnings")).isEqualTo(0);
            verify(jdbc).queryForList(anyString(), eq(1L), eq(2));
        }
    }

    // ============================================================
    @Nested
    @DisplayName("previewScan — 预览扫描")
    class PreviewScanTests {

        @Test
        @DisplayName("GRADE_FAIL 预览: 给命中学生附加 warningType/level/ruleName/description")
        void shouldPreviewGradeFail() {
            Map<String, Object> rule = ruleRow(1L, "GRADE_FAIL", 2, "{\"minFailCount\":2}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            Map<String, Object> student = new HashMap<>();
            student.put("studentId", 100L);
            student.put("failCount", 3L);
            student.put("failedCourses", "数学");
            when(jdbc.queryForList(anyString(), eq(1L), eq(2)))
                    .thenReturn(new ArrayList<>(List.of(student)));

            List<Map<String, Object>> result = service.previewScan(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("warningType")).isEqualTo("GRADE_FAIL");
            assertThat(result.get(0).get("warningLevel")).isEqualTo(2);
            assertThat(result.get(0).get("ruleName")).isEqualTo("规则-1");
            assertThat((String) result.get(0).get("description")).contains("挂科");
        }

        @Test
        @DisplayName("ATTENDANCE_LOW 预览: 附加描述")
        void shouldPreviewAttendanceLow() {
            Map<String, Object> rule = ruleRow(2L, "ATTENDANCE_LOW", 1, "{\"minAttendanceRate\":90}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            Map<String, Object> student = new HashMap<>();
            student.put("studentId", 200L);
            student.put("rate", 75.0);
            when(jdbc.queryForList(anyString(), eq(1L), eq(90)))
                    .thenReturn(new ArrayList<>(List.of(student)));

            List<Map<String, Object>> result = service.previewScan(1L);
            assertThat(result.get(0).get("warningType")).isEqualTo("ATTENDANCE_LOW");
            assertThat((String) result.get(0).get("description")).contains("出勤率");
        }

        @Test
        @DisplayName("CREDIT_SHORT 预览: 附加描述")
        void shouldPreviewCreditShort() {
            Map<String, Object> rule = ruleRow(3L, "CREDIT_SHORT", 3, "{\"expectedCredits\":30,\"actualCreditsBelow\":20}");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));
            Map<String, Object> student = new HashMap<>();
            student.put("studentId", 300L);
            student.put("earnedCredits", 10L);
            when(jdbc.queryForList(anyString(), eq(1L), eq(20)))
                    .thenReturn(new ArrayList<>(List.of(student)));

            List<Map<String, Object>> result = service.previewScan(1L);
            assertThat(result.get(0).get("warningType")).isEqualTo("CREDIT_SHORT");
            assertThat((String) result.get(0).get("description")).contains("学分");
        }

        @Test
        @DisplayName("非法 condition_params 的规则在预览中被跳过")
        void shouldSkipBadParamsInPreview() {
            Map<String, Object> rule = ruleRow(1L, "GRADE_FAIL", 2, "{bad");
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>(List.of(rule)));

            assertThat(service.previewScan(1L)).isEmpty();
        }

        @Test
        @DisplayName("无规则时返回空列表")
        void shouldReturnEmptyPreview() {
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>());
            assertThat(service.previewScan(1L)).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("listWarnings — 分页查询预警")
    class ListWarningsTests {

        @Test
        @DisplayName("无过滤条件: SQL 只含 1=1, 分页参数为 size/offset")
        void shouldListWithoutFilters() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
            when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());

            Map<String, Object> result = service.listWarnings(null, null, null, null, null, null, 1, 20);

            assertThat(result.get("total")).isEqualTo(0L);
            assertThat(result.get("records")).isInstanceOf(List.class);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("WHERE 1=1");
            assertThat(sql).contains("LIMIT ? OFFSET ?");
            assertThat(sql).doesNotContain("warning_level = ?");
        }

        @Test
        @DisplayName("全部过滤条件都拼进 WHERE 子句")
        void shouldListWithAllFilters() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);
            when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());

            service.listWarnings(2, 1, 10L, 100L, "GRADE_FAIL", 9L, 2, 10);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("warning_level = ?");
            assertThat(sql).contains("status = ?");
            assertThat(sql).contains("org_unit_id = ?");
            assertThat(sql).contains("student_id = ?");
            assertThat(sql).contains("warning_type = ?");
            assertThat(sql).contains("semester_id = ?");
        }

        @Test
        @DisplayName("warningType 为空字符串时不加 warning_type 过滤")
        void shouldIgnoreBlankWarningType() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
            when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());

            service.listWarnings(null, null, null, null, "", null, 1, 10);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).doesNotContain("warning_type = ?");
        }

        @Test
        @DisplayName("记录中的 detail JSON 字段被解析为对象")
        void shouldParseDetailField() {
            Map<String, Object> rec = new HashMap<>();
            rec.put("detail", "{\"failCount\":2}");
            when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(new ArrayList<>(List.of(rec)));

            Map<String, Object> result = service.listWarnings(null, null, null, null, null, null, 1, 10);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
            assertThat(records.get(0).get("detail")).isInstanceOf(Map.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getWarningDetail / studentWarningHistory")
    class WarningDetailTests {

        @Test
        @DisplayName("getWarningDetail 解析 detail JSON 字段")
        void shouldGetDetailAndParse() {
            Map<String, Object> warning = new HashMap<>();
            warning.put("id", 5L);
            warning.put("detail", "{\"rate\":70}");
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(warning);

            Map<String, Object> result = service.getWarningDetail(5L);

            assertThat(result.get("detail")).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("getWarningDetail detail 非 JSON 时保持原样")
        void shouldKeepNonJsonDetail() {
            Map<String, Object> warning = new HashMap<>();
            warning.put("detail", "plain text");
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(warning);

            Map<String, Object> result = service.getWarningDetail(5L);
            assertThat(result.get("detail")).isEqualTo("plain text");
        }

        @Test
        @DisplayName("studentWarningHistory 委托 jdbc 按学生倒序查询")
        void shouldQueryStudentHistory() {
            when(jdbc.queryForList(anyString(), eq(100L))).thenReturn(new ArrayList<>());
            service.studentWarningHistory(100L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), eq(100L));
            assertThat(sqlCap.getValue()).contains("WHERE student_id = ?");
            assertThat(sqlCap.getValue()).contains("ORDER BY created_at DESC");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("confirmWarning / interveneWarning / dismissWarning")
    class HandleWarningTests {

        @Test
        @DisplayName("confirmWarning: status=1 且仅对 status=0 生效")
        void shouldConfirm() {
            service.confirmWarning(5L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), any(), eq(5L));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("SET status = 1");
            assertThat(sql).contains("AND status = 0");
        }

        @Test
        @DisplayName("interveneWarning: status=2, 携带备注, 仅对 status IN (0,1) 生效")
        void shouldIntervene() {
            service.interveneWarning(5L, "已约谈");
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), eq("已约谈"), any(), eq(5L));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("SET status = 2");
            assertThat(sql).contains("status IN (0,1)");
        }

        @Test
        @DisplayName("dismissWarning: status=3, 携带备注")
        void shouldDismiss() {
            service.dismissWarning(5L, "误报");
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), eq("误报"), any(), eq(5L));
            assertThat(sqlCap.getValue()).contains("SET status = 3");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("statistics — 统计")
    class StatisticsTests {

        @Test
        @DisplayName("不带 semesterId: SQL 无 WHERE 子句, 组装四项统计")
        void shouldStatisticsWithoutSemester() {
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>());
            when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(42L);

            Map<String, Object> result = service.statistics(null);

            assertThat(result.get("totalWarnings")).isEqualTo(42L);
            assertThat(result).containsKeys("byLevel", "byType", "byStatus");
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForObject(sqlCap.capture(), eq(Long.class));
            assertThat(sqlCap.getValue()).doesNotContain("semester_id");
        }

        @Test
        @DisplayName("带 semesterId: SQL 拼接 WHERE semester_id 过滤")
        void shouldStatisticsWithSemester() {
            when(jdbc.queryForList(anyString())).thenReturn(new ArrayList<>());
            when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(7L);

            Map<String, Object> result = service.statistics(9L);

            assertThat(result.get("totalWarnings")).isEqualTo(7L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForObject(sqlCap.capture(), eq(Long.class));
            assertThat(sqlCap.getValue()).contains("semester_id = 9");
        }
    }
}
