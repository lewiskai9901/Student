package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.SchedulingConstraintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConstraintApplicationService 单测 — 排课约束 CRUD + 时间矩阵计算
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConstraintApplicationService 测试")
class ConstraintApplicationServiceTest {

    @Mock
    private SchedulingConstraintRepository constraintRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ConstraintApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ConstraintApplicationService(constraintRepo, objectMapper);
    }

    private SchedulingConstraint constraint(String name, ConstraintType type,
                                            String paramsJson, boolean enabled) {
        SchedulingConstraint c = SchedulingConstraint.create(
                1L, name, ConstraintLevel.GLOBAL, null, null, 9L,
                type, true, 50, paramsJson, null, 99L);
        if (!enabled) {
            c.disable();
        }
        return c;
    }

    @Nested
    @DisplayName("list 查询分支")
    class ListTests {

        @Test
        @DisplayName("level + targetId 同时存在 — 走 LevelAndTargetId 查询")
        void shouldQueryByLevelAndTarget() {
            List<SchedulingConstraint> expected = List.of(
                    constraint("c", ConstraintType.TIME_FORBIDDEN, "{}", true));
            when(constraintRepo.findBySemesterIdAndLevelAndTargetId(
                    1L, ConstraintLevel.TEACHER, 7L)).thenReturn(expected);

            List<SchedulingConstraint> result = service.list(1L, 2, 7L);

            assertThat(result).isSameAs(expected);
            verify(constraintRepo).findBySemesterIdAndLevelAndTargetId(1L, ConstraintLevel.TEACHER, 7L);
        }

        @Test
        @DisplayName("仅 level 存在 — 走 ByLevel 查询")
        void shouldQueryByLevelOnly() {
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of());

            service.list(1L, 1, null);

            verify(constraintRepo).findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL);
            verify(constraintRepo, never()).findBySemesterId(any());
        }

        @Test
        @DisplayName("level 与 targetId 都为空 — 走 BySemesterId 查询")
        void shouldQueryBySemesterOnly() {
            when(constraintRepo.findBySemesterId(1L)).thenReturn(List.of());

            service.list(1L, null, null);

            verify(constraintRepo).findBySemesterId(1L);
        }

        @Test
        @DisplayName("只有 targetId 没有 level — 仍走 BySemesterId")
        void shouldQueryBySemesterWhenLevelNull() {
            when(constraintRepo.findBySemesterId(1L)).thenReturn(List.of());

            service.list(1L, null, 7L);

            verify(constraintRepo).findBySemesterId(1L);
        }
    }

    @Nested
    @DisplayName("create 创建约束")
    class CreateTests {

        @Test
        @DisplayName("应组装聚合根并保存 — 完整字段")
        void shouldCreateWithAllFields() {
            ArgumentCaptor<SchedulingConstraint> cap = ArgumentCaptor.forClass(SchedulingConstraint.class);
            when(constraintRepo.save(cap.capture())).thenAnswer(i -> i.getArgument(0));

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            data.put("constraintName", "周一不排课");
            data.put("constraintLevel", 1);
            data.put("targetId", null);
            data.put("targetName", "全局");
            data.put("orgUnitId", 9L);
            data.put("constraintType", "TIME_FORBIDDEN");
            data.put("isHard", false);
            data.put("priority", 80);
            data.put("params", Map.of("days", List.of(1)));
            data.put("effectiveWeeks", "1-18");

            SchedulingConstraint saved = service.create(data, 99L);

            SchedulingConstraint c = cap.getValue();
            assertThat(c.getSemesterId()).isEqualTo(1L);
            assertThat(c.getConstraintName()).isEqualTo("周一不排课");
            assertThat(c.getConstraintLevel()).isEqualTo(ConstraintLevel.GLOBAL);
            assertThat(c.getConstraintType()).isEqualTo(ConstraintType.TIME_FORBIDDEN);
            assertThat(c.getIsHard()).isFalse();
            assertThat(c.getPriority()).isEqualTo(80);
            assertThat(c.getCreatedBy()).isEqualTo(99L);
            assertThat(c.getParams()).contains("days");
            assertThat(saved).isSameAs(c);
        }

        @Test
        @DisplayName("isHard / priority 缺省时取默认值 true / 50")
        void shouldApplyDefaults() {
            ArgumentCaptor<SchedulingConstraint> cap = ArgumentCaptor.forClass(SchedulingConstraint.class);
            when(constraintRepo.save(cap.capture())).thenAnswer(i -> i.getArgument(0));

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            data.put("constraintName", "X");
            data.put("constraintLevel", 2);
            data.put("targetId", 7L);
            data.put("orgUnitId", 9L);
            data.put("constraintType", "MAX_DAILY");

            service.create(data, 99L);

            assertThat(cap.getValue().getIsHard()).isTrue();
            assertThat(cap.getValue().getPriority()).isEqualTo(50);
        }

        @Test
        @DisplayName("params 已是字符串 — 直接透传不再序列化")
        void shouldPassThroughStringParams() {
            ArgumentCaptor<SchedulingConstraint> cap = ArgumentCaptor.forClass(SchedulingConstraint.class);
            when(constraintRepo.save(cap.capture())).thenAnswer(i -> i.getArgument(0));

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            data.put("constraintName", "X");
            data.put("constraintLevel", 1);
            data.put("orgUnitId", 9L);
            data.put("constraintType", "TIME_FIXED");
            data.put("params", "{\"raw\":true}");

            service.create(data, 99L);

            assertThat(cap.getValue().getParams()).isEqualTo("{\"raw\":true}");
        }

        @Test
        @DisplayName("非法 constraintType 字面量 — 抛 IllegalArgumentException")
        void shouldThrowOnBadType() {
            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            data.put("constraintName", "X");
            data.put("constraintLevel", 1);
            data.put("orgUnitId", 9L);
            data.put("constraintType", "NOT_A_TYPE");

            assertThatThrownBy(() -> service.create(data, 99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("update 更新约束")
    class UpdateTests {

        @Test
        @DisplayName("约束存在 — 应更新字段并保存")
        void shouldUpdate() {
            SchedulingConstraint existing = constraint("旧名", ConstraintType.TIME_FORBIDDEN, "{}", true);
            when(constraintRepo.findById(5L)).thenReturn(Optional.of(existing));
            when(constraintRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            Map<String, Object> data = new HashMap<>();
            data.put("constraintName", "新名");
            data.put("isHard", false);
            data.put("priority", 30);
            data.put("params", Map.of("k", "v"));
            data.put("effectiveWeeks", "2-10");

            SchedulingConstraint result = service.update(5L, data);

            assertThat(result.getConstraintName()).isEqualTo("新名");
            assertThat(result.getIsHard()).isFalse();
            assertThat(result.getPriority()).isEqualTo(30);
            assertThat(result.getEffectiveWeeks()).isEqualTo("2-10");
            assertThat(result.getParams()).contains("\"k\"");
        }

        @Test
        @DisplayName("约束不存在 — 抛 IllegalArgumentException")
        void shouldThrowWhenNotFound() {
            when(constraintRepo.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404L, new HashMap<>()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("约束不存在: 404");
        }
    }

    @Nested
    @DisplayName("delete / enable / disable")
    class StateTests {

        @Test
        @DisplayName("delete 应委托仓储 deleteById")
        void shouldDelete() {
            service.delete(8L);
            verify(constraintRepo).deleteById(8L);
        }

        @Test
        @DisplayName("enable 应把约束置为启用并保存")
        void shouldEnable() {
            SchedulingConstraint c = constraint("X", ConstraintType.MAX_DAILY, "{}", false);
            when(constraintRepo.findById(3L)).thenReturn(Optional.of(c));

            service.enable(3L);

            assertThat(c.getEnabled()).isTrue();
            verify(constraintRepo).save(c);
        }

        @Test
        @DisplayName("enable 约束不存在 — 抛异常")
        void shouldThrowEnableNotFound() {
            when(constraintRepo.findById(3L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.enable(3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("约束不存在");
        }

        @Test
        @DisplayName("disable 应把约束置为停用并保存")
        void shouldDisable() {
            SchedulingConstraint c = constraint("X", ConstraintType.MAX_DAILY, "{}", true);
            when(constraintRepo.findById(3L)).thenReturn(Optional.of(c));

            service.disable(3L);

            assertThat(c.getEnabled()).isFalse();
            verify(constraintRepo).save(c);
        }

        @Test
        @DisplayName("disable 约束不存在 — 抛异常")
        void shouldThrowDisableNotFound() {
            when(constraintRepo.findById(3L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.disable(3L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getTimeMatrix 时间矩阵")
    class TimeMatrixTests {

        @Test
        @DisplayName("无约束 — 7x10 全部 available")
        void shouldReturnAllAvailable() {
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of());

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix).hasSize(7);
            assertThat(matrix.get(0)).hasSize(10);
            Map<String, Object> slot = matrix.get(0).get(0);
            assertThat(slot.get("day")).isEqualTo(1);
            assertThat(slot.get("period")).isEqualTo(1);
            assertThat(slot.get("status")).isEqualTo("available");
            assertThat((List<?>) slot.get("reasons")).isEmpty();
        }

        @Test
        @DisplayName("全局 TIME_FORBIDDEN 约束 — 命中槽位标记 forbidden")
        void shouldMarkForbidden() {
            SchedulingConstraint c = constraint("周一全天禁排", ConstraintType.TIME_FORBIDDEN,
                    "{\"days\":[1],\"periods\":[]}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            // day=1 row -> all periods forbidden
            for (Map<String, Object> slot : matrix.get(0)) {
                assertThat(slot.get("status")).isEqualTo("forbidden");
                assertThat((List<String>) slot.get("reasons")).contains("周一全天禁排");
            }
            // day=2 row -> available
            assertThat(matrix.get(1).get(0).get("status")).isEqualTo("available");
        }

        @Test
        @DisplayName("禁用的约束被跳过 — 不影响矩阵")
        void shouldSkipDisabledConstraint() {
            SchedulingConstraint c = constraint("禁用约束", ConstraintType.TIME_FORBIDDEN,
                    "{\"days\":[1],\"periods\":[]}", false);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix.get(0).get(0).get("status")).isEqualTo("available");
        }

        @Test
        @DisplayName("TIME_PREFERRED 约束 — 命中槽位标记 preferred")
        void shouldMarkPreferred() {
            SchedulingConstraint c = constraint("上午优先", ConstraintType.TIME_PREFERRED,
                    "{\"days\":[],\"periods\":[1,2]}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix.get(0).get(0).get("status")).isEqualTo("preferred");
            assertThat(matrix.get(0).get(2).get("status")).isEqualTo("available");
        }

        @Test
        @DisplayName("TIME_AVOIDED 约束 — 命中槽位标记 avoided")
        void shouldMarkAvoided() {
            SchedulingConstraint c = constraint("尽量回避", ConstraintType.TIME_AVOIDED,
                    "{\"days\":[3],\"periods\":[5]}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix.get(2).get(4).get("status")).isEqualTo("avoided");
        }

        @Test
        @DisplayName("forbidden 优先级最高 — 覆盖 preferred")
        void forbiddenOverridesPreferred() {
            SchedulingConstraint pref = constraint("偏好", ConstraintType.TIME_PREFERRED,
                    "{\"days\":[1],\"periods\":[1]}", true);
            SchedulingConstraint forb = constraint("禁排", ConstraintType.TIME_FORBIDDEN,
                    "{\"days\":[1],\"periods\":[1]}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(Arrays.asList(pref, forb));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            Map<String, Object> slot = matrix.get(0).get(0);
            assertThat(slot.get("status")).isEqualTo("forbidden");
            assertThat((List<?>) slot.get("reasons")).hasSize(2);
        }

        @Test
        @DisplayName("TIME_FIXED 约束 — 固定日+课节标记 preferred")
        void shouldMarkFixedAsPreferred() {
            SchedulingConstraint c = constraint("固定排课", ConstraintType.TIME_FIXED,
                    "{\"day\":2,\"periods\":[3]}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix.get(1).get(2).get("status")).isEqualTo("preferred");
            assertThat(matrix.get(0).get(2).get("status")).isEqualTo("available");
        }

        @Test
        @DisplayName("非时间类约束 (MAX_DAILY) — 不影响任何槽位")
        void otherTypesDoNotAffectSlots() {
            SchedulingConstraint c = constraint("每日上限", ConstraintType.MAX_DAILY,
                    "{\"max\":4}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix.get(0).get(0).get("status")).isEqualTo("available");
        }

        @Test
        @DisplayName("非法 JSON params — evaluateConstraintForSlot 吞异常返回 available")
        void shouldSkipInvalidJsonParams() {
            SchedulingConstraint c = constraint("坏参数", ConstraintType.TIME_FORBIDDEN,
                    "not-json", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(List.of(c));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, null, null);

            assertThat(matrix.get(0).get(0).get("status")).isEqualTo("available");
        }

        @Test
        @DisplayName("level + targetId 存在 — 同时查询目标专属约束")
        void shouldAggregateTargetConstraints() {
            SchedulingConstraint targetC = constraint("教师专属禁排", ConstraintType.TIME_FORBIDDEN,
                    "{\"days\":[5],\"periods\":[]}", true);
            when(constraintRepo.findBySemesterIdAndLevel(1L, ConstraintLevel.GLOBAL))
                    .thenReturn(new ArrayList<>());
            when(constraintRepo.findBySemesterIdAndLevelAndTargetId(
                    1L, ConstraintLevel.TEACHER, 7L)).thenReturn(List.of(targetC));

            List<List<Map<String, Object>>> matrix = service.getTimeMatrix(1L, 2, 7L);

            // day=5 row forbidden
            assertThat(matrix.get(4).get(0).get("status")).isEqualTo("forbidden");
            verify(constraintRepo).findBySemesterIdAndLevelAndTargetId(1L, ConstraintLevel.TEACHER, 7L);
        }
    }
}
