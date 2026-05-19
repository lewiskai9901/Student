package com.school.management.infrastructure.extension.plugins.education.domain.calendar;

import com.school.management.infrastructure.extension.plugins.education.domain.calendar.model.aggregate.Semester;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.model.valueobject.SemesterStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.model.valueobject.SemesterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Semester 聚合根单元测试 — 验证创建、日期/编码校验、状态流转、当前学期切换、领域事件
 */
@DisplayName("Semester 聚合根测试")
class SemesterTest {

    private static final Long ACADEMIC_YEAR_ID = 1L;
    private static final LocalDate START = LocalDate.of(2024, 9, 1);
    private static final LocalDate END = LocalDate.of(2025, 1, 31);

    private Semester semester;

    @BeforeEach
    void setUp() {
        semester = Semester.create(
                ACADEMIC_YEAR_ID,
                "2024-2025 第一学期",
                "2024-2025-1",
                START, END,
                2024,
                SemesterType.FIRST
        );
    }

    @Nested
    @DisplayName("创建学期")
    class CreateSemesterTests {

        @Test
        @DisplayName("应成功创建学期")
        void shouldCreateSuccessfully() {
            assertThat(semester.getAcademicYearId()).isEqualTo(ACADEMIC_YEAR_ID);
            assertThat(semester.getSemesterName()).isEqualTo("2024-2025 第一学期");
            assertThat(semester.getSemesterCode()).isEqualTo("2024-2025-1");
            assertThat(semester.getStartDate()).isEqualTo(START);
            assertThat(semester.getEndDate()).isEqualTo(END);
            assertThat(semester.getStartYear()).isEqualTo(2024);
            assertThat(semester.getSemesterType()).isEqualTo(SemesterType.FIRST);
            assertThat(semester.getIsCurrent()).isFalse();
            assertThat(semester.getStatus()).isEqualTo(SemesterStatus.ACTIVE);
            assertThat(semester.getDomainEvents()).isNotEmpty();
        }

        @Test
        @DisplayName("开始日期为 null 抛异常")
        void shouldFailWhenStartDateNull() {
            assertThatThrownBy(() -> Semester.create(ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    null, END, 2024, SemesterType.FIRST))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("开始日期");
        }

        @Test
        @DisplayName("结束日期早于开始日期抛异常")
        void shouldFailWhenEndBeforeStart() {
            assertThatThrownBy(() -> Semester.create(ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    END, START, 2024, SemesterType.FIRST))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不能早于");
        }

        @Test
        @DisplayName("学期编码为空抛异常")
        void shouldFailWhenCodeEmpty() {
            assertThatThrownBy(() -> Semester.create(ACADEMIC_YEAR_ID, "n", "",
                    START, END, 2024, SemesterType.FIRST))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("编码不能为空");
        }

        @Test
        @DisplayName("学期编码格式错误抛异常")
        void shouldFailWhenCodeFormatInvalid() {
            assertThatThrownBy(() -> Semester.create(ACADEMIC_YEAR_ID, "n", "BAD-CODE",
                    START, END, 2024, SemesterType.FIRST))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("格式不正确");
        }

        @Test
        @DisplayName("generateCode 工具方法")
        void shouldGenerateCode() {
            assertThat(Semester.generateCode(2024, SemesterType.FIRST))
                    .isEqualTo("2024-2025-1");
            assertThat(Semester.generateCode(2024, SemesterType.SECOND))
                    .isEqualTo("2024-2025-2");
        }

        @Test
        @DisplayName("generateCode 参数为 null 抛异常")
        void shouldFailGenerateCodeWithNull() {
            assertThatThrownBy(() -> Semester.generateCode(null, SemesterType.FIRST))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("状态流转")
    class StatusTransitionTests {

        @Test
        @DisplayName("setAsCurrent — ACTIVE 学期可设为当前")
        void shouldSetAsCurrentWhenActive() {
            // 需要 ID 才能 register event;直接通过 reconstruct 设置
            Semester withId = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    false, SemesterStatus.ACTIVE,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            withId.setAsCurrent();

            assertThat(withId.getIsCurrent()).isTrue();
            assertThat(withId.getDomainEvents()).isNotEmpty();
        }

        @Test
        @DisplayName("已结束的学期不能设为当前")
        void shouldFailSetCurrentWhenEnded() {
            Semester ended = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    false, SemesterStatus.ENDED,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            assertThatThrownBy(ended::setAsCurrent)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("正常状态");
        }

        @Test
        @DisplayName("unsetAsCurrent — 取消当前标记")
        void shouldUnsetAsCurrent() {
            Semester withId = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    true, SemesterStatus.ACTIVE,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            withId.unsetAsCurrent();
            assertThat(withId.getIsCurrent()).isFalse();
        }

        @Test
        @DisplayName("end — ACTIVE 学期可以结束")
        void shouldEndSemester() {
            Semester withId = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    false, SemesterStatus.ACTIVE,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            withId.end();

            assertThat(withId.getStatus()).isEqualTo(SemesterStatus.ENDED);
            assertThat(withId.getDomainEvents()).isNotEmpty();
        }

        @Test
        @DisplayName("已结束的学期不能再次结束")
        void shouldFailEndingTwice() {
            Semester ended = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    false, SemesterStatus.ENDED,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            assertThatThrownBy(ended::end)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已经结束");
        }

        @Test
        @DisplayName("当前学期不能直接结束")
        void shouldFailEndingCurrent() {
            Semester current = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    true, SemesterStatus.ACTIVE,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            assertThatThrownBy(current::end)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不能结束当前学期");
        }

        @Test
        @DisplayName("reactivate — 结束后可重新激活")
        void shouldReactivate() {
            Semester ended = Semester.reconstruct(
                    100L, ACADEMIC_YEAR_ID, "n", "2024-2025-1",
                    START, END, 2024, SemesterType.FIRST,
                    false, SemesterStatus.ENDED,
                    LocalDateTime.now(), LocalDateTime.now(), 1L, null);

            ended.reactivate();

            assertThat(ended.getStatus()).isEqualTo(SemesterStatus.ACTIVE);
        }

        @Test
        @DisplayName("已激活的学期不能再次激活")
        void shouldFailReactivatingActive() {
            assertThatThrownBy(() -> semester.reactivate())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("日期工具方法")
    class DateUtilTests {

        @Test
        @DisplayName("containsDate — 区间内")
        void shouldContainDate() {
            assertThat(semester.containsDate(LocalDate.of(2024, 10, 15))).isTrue();
            assertThat(semester.containsDate(START)).isTrue();
            assertThat(semester.containsDate(END)).isTrue();
        }

        @Test
        @DisplayName("containsDate — 区间外")
        void shouldNotContainOutOfRangeDate() {
            assertThat(semester.containsDate(LocalDate.of(2024, 8, 31))).isFalse();
            assertThat(semester.containsDate(LocalDate.of(2025, 2, 1))).isFalse();
        }

        @Test
        @DisplayName("containsDate — null 返回 false")
        void shouldHandleNullDate() {
            assertThat(semester.containsDate(null)).isFalse();
        }

        @Test
        @DisplayName("getDurationDays — 跨度天数包含两端")
        void shouldCalculateDurationDays() {
            long expected = java.time.temporal.ChronoUnit.DAYS.between(START, END) + 1;
            assertThat(semester.getDurationDays()).isEqualTo(expected);
        }

        @Test
        @DisplayName("updateBasicInfo — 修改名称和日期")
        void shouldUpdateBasicInfo() {
            LocalDate newEnd = LocalDate.of(2025, 2, 15);
            semester.updateBasicInfo("新名称", START, newEnd);

            assertThat(semester.getSemesterName()).isEqualTo("新名称");
            assertThat(semester.getEndDate()).isEqualTo(newEnd);
        }

        @Test
        @DisplayName("updateBasicInfo 日期错误抛异常")
        void shouldFailUpdateWhenDateInvalid() {
            assertThatThrownBy(() -> semester.updateBasicInfo("名", END, START))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
