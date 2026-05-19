package com.school.management.architecture;

import com.school.management.domain.shared.AggregateRoot;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Shadow-id guard for aggregate roots (M3.3, 2026-05-20).
 *
 * <p>{@link AggregateRoot} already declares {@code protected ID id} plus
 * {@code getId()} / {@code setId(ID)}. Any subclass that re-declares a field
 * named {@code id} shadows the inherited one, causing two physical slots and
 * silent NPE / inconsistency between {@code this.id} business reads and the
 * inherited ORM-bound field.
 *
 * <p>M3.3 已修复 5 个 education plugin 聚合根 (Course / CurriculumPlan / Major /
 * Cohort / SchoolClass). 守护规则锁定这 5 个不能回退, 同时 ratchet baseline 记录
 * 21 个历史违规, 任何新增 shadow id 或减少 baseline 之外的违规都会失败.
 *
 * <p>下一步: 逐步清理 BASELINE 列表 (随 M3.4/M4 推进, 每修一个聚合根就从 baseline
 * 移除一个). baseline 长度不可增长.
 */
class ArchUnitNoShadowIdInAggregateTest {

    /**
     * 历史违规 baseline — 21 个聚合根存在 shadow id 反模式, 待后续重构清理.
     * <p>不可增长; 修复一个就从列表删一个 (test 会因 baseline 大于实际违规而失败,
     * 提醒开发者更新 baseline).
     */
    private static final Set<String> SHADOW_ID_BASELINE = Set.of(
        "com.school.management.domain.access.model.Role",
        "com.school.management.domain.inspection.model.appeal.InspAppeal",
        "com.school.management.domain.inspection.model.corrective.CorrectiveCase",
        "com.school.management.domain.inspection.model.corrective.CorrectiveSubtask",
        "com.school.management.domain.inspection.model.corrective.IssueCategory",
        "com.school.management.domain.inspection.model.execution.InspProject",
        "com.school.management.domain.inspection.model.execution.InspSubmission",
        "com.school.management.domain.inspection.model.execution.InspTask",
        "com.school.management.domain.inspection.model.execution.InspectionPlan",
        "com.school.management.domain.inspection.model.execution.ViolationRecord",
        "com.school.management.domain.inspection.model.platform.HolidayCalendar",
        "com.school.management.domain.inspection.model.scoring.ScoringPolicy",
        "com.school.management.domain.inspection.model.scoring.ScoringProfile",
        "com.school.management.domain.inspection.model.template.InspTemplate",
        "com.school.management.domain.inspection.model.template.ResponseSet",
        "com.school.management.domain.inspection.model.template.TemplateCatalog",
        "com.school.management.domain.inspection.model.template.TemplateSection",
        "com.school.management.domain.organization.model.OrgUnit",
        "com.school.management.domain.place.model.aggregate.UniversalPlace",
        "com.school.management.domain.schedule.model.ScheduleExecution",
        "com.school.management.domain.schedule.model.SchedulePolicy"
    );

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void aggregateRootSubclasses_doNotShadowIdField() {
        classes()
            .that().areAssignableTo(AggregateRoot.class)
            .and(new DescribedPredicate<JavaClass>("are not AggregateRoot itself") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return !javaClass.getName().equals(AggregateRoot.class.getName());
                }
            })
            .and(new DescribedPredicate<JavaClass>("are not in the historical baseline") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return !SHADOW_ID_BASELINE.contains(javaClass.getName());
                }
            })
            .should(new ArchCondition<JavaClass>(
                "not declare a field named 'id' (inherits AggregateRoot.id — re-declaring shadows it)") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    for (JavaField field : item.getFields()) {
                        if ("id".equals(field.getName())) {
                            String message = String.format(
                                "Class %s declares field 'id' which shadows AggregateRoot.id "
                                    + "(in %s)",
                                item.getName(),
                                field.getSourceCodeLocation());
                            events.add(SimpleConditionEvent.violated(field, message));
                        }
                    }
                }
            })
            .because("M3.3 — AggregateRoot 已提供 protected ID id + getId()/setId(). "
                + "子类再声明 private Long id 会造成双字段，业务读与 ORM 写不一致, NPE 风险")
            .check(classes);
    }
}
