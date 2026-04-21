package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Track M3 守护: {@code MessagingDomainPlugin} 是 @Deprecated 旧 SPI.
 *
 * 新 messaging 功能必须走 {@link com.school.management.infrastructure.extension.Contribution}
 * 模式 (TriggerPointContribution / EventTypeContribution / EventDomainContribution).
 * 本测试守护实现数 ≤ 基线, 防未来渐进迁移过程中反向新增旧 SPI 实现.
 *
 * <h3>基线推进</h3>
 * M3 后基线 = 12 (原 14 - 已迁 2: PatientAdmissionMessagingPlugin + DormitoryMessagingPlugin).
 * 当其他 11 个 plugin 随业务迭代渐进迁移时, 把基线逐步降到 0, 最终删 interface.
 */
class NoNewMessagingDomainPluginTest {

    /**
     * 允许存在的 {@code MessagingDomainPlugin} 实现基线.
     *
     * Track M3 建立时值 = 12 (PatientAdmission + Dormitory 已迁至 Contribution 后剩余数量).
     * 只能单调下降, 不得上升.
     */
    private static final int BASELINE_COUNT = 12;

    @Test
    @DisplayName("MessagingDomainPlugin 实现数 <= 基线 — 新 messaging 功能必须用 Contribution SPI")
    void messagingDomainPluginImplementationsStayAtBaseline() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages("com.school.management");

        long count = classes.stream()
            .filter(c -> c.getInterfaces().stream()
                .anyMatch(i -> i.getName().equals(
                    "com.school.management.infrastructure.extension.MessagingDomainPlugin")))
            .count();

        assertThat(count)
            .as("MessagingDomainPlugin (@Deprecated) 实现数必须单调下降. " +
                "新增 messaging 功能请用 PluginPackage.contribute() 返回 " +
                "TriggerPointContribution / EventTypeContribution / EventDomainContribution.")
            .isLessThanOrEqualTo(BASELINE_COUNT);
    }
}
