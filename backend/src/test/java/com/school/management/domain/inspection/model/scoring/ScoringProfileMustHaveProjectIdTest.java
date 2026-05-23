package com.school.management.domain.inspection.model.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回退守护 — ScoringProfile 项目-owned 重构 (2026-05-23).
 *
 * <p>评分配置统一由项目持有: `ScoringProfile.projectId` 必有,跨项目不共享。
 * 跨项目复用通过显式「克隆现有项目」实现 (POST /inspection/projects/{id}/clone)。
 *
 * <p>守护:
 * - 领域聚合根必须有 `projectId` 字段
 * - 防止后续有人移除该字段而恢复"全局共享 ScoringProfile"的旧模型
 */
@DisplayName("ScoringProfile 防回退 — 必须持有 projectId")
class ScoringProfileMustHaveProjectIdTest {

    @Test
    @DisplayName("ScoringProfile 含 projectId 字段且类型为 Long")
    void scoringProfileHasProjectIdField() {
        Field[] fields = ScoringProfile.class.getDeclaredFields();
        Field projectIdField = Arrays.stream(fields)
                .filter(f -> f.getName().equals("projectId"))
                .findFirst()
                .orElse(null);

        assertThat(projectIdField)
                .as("ScoringProfile 必须持有 projectId 字段 — 项目-owned 重构要求每个评分方案归属一个项目, "
                        + "跨项目复用通过克隆项目实现, 不再全局共享。"
                        + "若需移除此字段, 请先废止该架构决策。")
                .isNotNull();

        assertThat(projectIdField.getType())
                .as("ScoringProfile.projectId 类型必须是 Long")
                .isEqualTo(Long.class);
    }

    @Test
    @DisplayName("ScoringProfile 不含 templateId 字段 — 评分配置与模板解耦")
    void scoringProfileDoesNotReferenceTemplate() {
        long templateIdCount = Arrays.stream(ScoringProfile.class.getDeclaredFields())
                .filter(f -> f.getName().equals("templateId"))
                .count();
        assertThat(templateIdCount)
                .as("ScoringProfile 不得持有 templateId — 模板=结构, 项目=规则, 二者解耦")
                .isZero();
    }
}
