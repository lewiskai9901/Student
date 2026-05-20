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
 * <p>M3.3 修复 5 个 education plugin 聚合根 (Course / CurriculumPlan / Major /
 * Cohort / SchoolClass). N1 (2026-05-20) 清零剩余 21 个历史违规
 * (access / inspection×16 / organization / place / schedule). baseline 已空 —
 * 全部 AggregateRoot 子类现在都必须无 shadow id, 无任何豁免.
 */
class ArchUnitNoShadowIdInAggregateTest {

    /**
     * 历史违规 baseline — 已全部清零 (N1, 2026-05-20). 必须保持为空:
     * 新增 shadow id 一律失败, 不允许往这里加豁免.
     */
    private static final Set<String> SHADOW_ID_BASELINE = Set.of();

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
