package com.school.management.infrastructure.extension.plugins.education.scope;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 行为守护: BY_GRADE / BY_MAJOR 维度反查"我能看到哪些 student"时, 必须经
 * {@code access_relations} 的 member 关系 (subject=学生 user_id, resource=班级 org_unit)
 * 反查学生档案, 而**不得**读 {@code user_student.grade_id} / {@code user_student.major_id}
 * —— 这两个行业列已在 V20260531 从 user_student 上 DROP。
 *
 * <p>背景: 学生不是 org_unit, 且 user_student 不再有任何班级/年级/专业归属列;
 * 学生↔班级唯一来源是 access_relations member tuple。年级/专业不直接含学生, 经班级:
 * <ul>
 *   <li>BY_GRADE: 年级 org → tree_path 子树展开到子班级 org → member 学生</li>
 *   <li>BY_MAJOR: major → classes.major_id 反查班级 org_unit_id → member 学生</li>
 * </ul>
 * (BY_CLASS 的 {@link ClassDataScopeResolver} 早已是此口径, 这两个当初漏改。)
 *
 * <p>测试在单元层捕获 resolver 实际发给 JdbcTemplate 的"student 反查 SQL", 断言其
 * 走 access_relations 且不含已 DROP 的行业列 —— 无需真 DB, 确定性强, 防再次 drift。
 */
class EduStudentScopeMembershipTest {

    /** 捕获所有发给 jdbc.queryForList(String, Class, Object...) 的 SQL, 返回查 user_student 的那条。 */
    private String captureStudentSql(JdbcTemplate jdbc, Runnable invokeResolve) {
        invokeResolve.run();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbc, org.mockito.Mockito.atLeastOnce())
            .queryForList(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        return sqlCaptor.getAllValues().stream()
            .filter(sql -> sql.toLowerCase().contains("user_student"))
            .reduce((a, b) -> a + "\n---\n" + b)
            .orElse("");
    }

    @Test
    void byGrade_studentLookup_usesAccessRelationsMember_notDroppedGradeId() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // findUserGradeOrgUnitIds() 返回非空, 使 resolve 进入 student 反查分支
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(List.of(9001L));

        GradeDataScopeResolver resolver = new GradeDataScopeResolver(jdbc);
        String studentSql = captureStudentSql(jdbc, () -> resolver.resolve(42L, "student"));

        assertThat(studentSql)
            .as("BY_GRADE 反查学生必须走 access_relations member 模型")
            .contains("access_relations");
        assertThat(studentSql)
            .as("user_student(别名 s) 已无 grade_id 列, 不得读 (classes.grade_id 另算)")
            .doesNotContain("s.grade_id");
    }

    @Test
    void byClass_headTeacherPath_resolvesClassOwnOrgId_notParentGrade() {
        // classes 是 org_units 上的 VIEW: c.id = 班级自身 org_unit id (学生 member 挂它),
        // c.org_unit_id = 父节点(年级)。班主任经 attributes.headTeacher 持久化 (= classes.teacher_id),
        // 此 fallback 路径必须解析到班级自身 org (id), 否则 member join 到年级 → 找不到学生。
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(List.of()); // 两个来源查询都返回空即可, 只检视发出的 classes SQL 字符串

        ClassDataScopeResolver resolver = new ClassDataScopeResolver(jdbc);
        resolver.resolve(42L, "student");

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbc, org.mockito.Mockito.atLeastOnce())
            .queryForList(cap.capture(), eq(Long.class), any(Object[].class));
        String classesSql = cap.getAllValues().stream()
            .filter(s -> s.contains("classes") && s.contains("teacher_id"))
            .findFirst().orElse("");

        assertThat(classesSql)
            .as("班主任(headTeacher)路径必须取班级自身 org id (classes.id)")
            .contains("SELECT id FROM classes");
        assertThat(classesSql)
            .as("不得取 classes.org_unit_id (=父年级, member join 找不到学生)")
            .doesNotContain("org_unit_id");
    }

    @Test
    void byMajor_studentLookup_usesAccessRelationsMember_notDroppedMajorId() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // findUserMajorIds(): 各 queryForList 返回非空 major id, teacherCount(queryForObject) 返回 >0
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(List.of(7001L));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
            .thenReturn(1);

        MajorDataScopeResolver resolver = new MajorDataScopeResolver(jdbc);
        String studentSql = captureStudentSql(jdbc, () -> resolver.resolve(42L, "student"));

        assertThat(studentSql)
            .as("BY_MAJOR 反查学生必须走 access_relations member 模型")
            .contains("access_relations");
        assertThat(studentSql)
            .as("user_student(别名 s) 已无 major_id 列, 不得读 (classes.major_id 另算)")
            .doesNotContain("s.major_id");
    }
}
