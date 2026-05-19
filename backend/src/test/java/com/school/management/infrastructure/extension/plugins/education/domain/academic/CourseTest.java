package com.school.management.infrastructure.extension.plugins.education.domain.academic;

import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Course 聚合根单元测试 - 验证创建、更新、状态、字段默认值等业务规则
 */
@DisplayName("Course 聚合根测试")
class CourseTest {

    private static final Long CREATOR_ID = 1L;

    private Course course;

    @BeforeEach
    void setUp() {
        course = Course.create("CS101", "数据结构", CREATOR_ID);
    }

    @Nested
    @DisplayName("创建课程")
    class CreateCourseTests {

        @Test
        @DisplayName("应成功创建课程并赋默认值")
        void shouldCreateWithDefaults() {
            assertThat(course.getCourseCode()).isEqualTo("CS101");
            assertThat(course.getCourseName()).isEqualTo("数据结构");
            assertThat(course.getCourseCategory()).isEqualTo(1);
            assertThat(course.getCourseType()).isEqualTo(1);
            assertThat(course.getCourseNature()).isEqualTo(1);
            assertThat(course.getCredits()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(course.getTotalHours()).isEqualTo(0);
            assertThat(course.getTheoryHours()).isEqualTo(0);
            assertThat(course.getPracticeHours()).isEqualTo(0);
            assertThat(course.getWeeklyHours()).isEqualTo(2);
            assertThat(course.getAssessmentMethod()).isEqualTo(1);
            assertThat(course.getStatus()).isEqualTo(1);
            assertThat(course.getCreatedBy()).isEqualTo(CREATOR_ID);
            assertThat(course.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("courseCode 为 null 抛 NPE")
        void shouldFailWhenCodeNull() {
            assertThatThrownBy(() -> Course.create(null, "数据结构", CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("courseName 为 null 抛 NPE")
        void shouldFailWhenNameNull() {
            assertThatThrownBy(() -> Course.create("CS101", null, CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("courseCode 为空字符串抛 IllegalArgumentException")
        void shouldFailWhenCodeBlank() {
            assertThatThrownBy(() -> Course.create("   ", "数据结构", CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Course code");
        }

        @Test
        @DisplayName("courseName 为空字符串抛 IllegalArgumentException")
        void shouldFailWhenNameBlank() {
            assertThatThrownBy(() -> Course.create("CS101", "   ", CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Course name");
        }

        @Test
        @DisplayName("courseCode 长度超过 50 抛异常")
        void shouldFailWhenCodeTooLong() {
            String longCode = "X".repeat(51);
            assertThatThrownBy(() -> Course.create(longCode, "数据结构", CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed 50");
        }

        @Test
        @DisplayName("Builder 可设定所有字段")
        void shouldBuildFully() {
            Course built = Course.builder()
                    .id(1L)
                    .courseCode("CS999")
                    .courseName("操作系统")
                    .courseNameEn("Operating Systems")
                    .courseCategory(2)
                    .courseType(3)
                    .courseNature(2)
                    .credits(new BigDecimal("4.5"))
                    .totalHours(72)
                    .theoryHours(48)
                    .practiceHours(24)
                    .weeklyHours(4)
                    .assessmentMethod(4)
                    .orgUnitId(10L)
                    .description("desc")
                    .status(0)
                    .createdBy(99L)
                    .build();

            assertThat(built.getId()).isEqualTo(1L);
            assertThat(built.getCourseNameEn()).isEqualTo("Operating Systems");
            assertThat(built.getCourseCategory()).isEqualTo(2);
            assertThat(built.getCredits()).isEqualByComparingTo("4.5");
            assertThat(built.getTotalHours()).isEqualTo(72);
            assertThat(built.getStatus()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("更新课程")
    class UpdateCourseTests {

        @Test
        @DisplayName("应能更新非空字段")
        void shouldUpdateNonNullFields() {
            course.update("数据结构(进阶)", "Data Structures Advanced",
                    2, 3, 2, new BigDecimal("3"), 64, 32, 32, 4, 1,
                    20L, "新描述", 99L);

            assertThat(course.getCourseName()).isEqualTo("数据结构(进阶)");
            assertThat(course.getCourseNameEn()).isEqualTo("Data Structures Advanced");
            assertThat(course.getCourseCategory()).isEqualTo(2);
            assertThat(course.getCourseType()).isEqualTo(3);
            assertThat(course.getCourseNature()).isEqualTo(2);
            assertThat(course.getCredits()).isEqualByComparingTo("3");
            assertThat(course.getTotalHours()).isEqualTo(64);
            assertThat(course.getOrgUnitId()).isEqualTo(20L);
            assertThat(course.getDescription()).isEqualTo("新描述");
            assertThat(course.getUpdatedBy()).isEqualTo(99L);
        }

        @Test
        @DisplayName("update 传 null 字段时保留原值")
        void shouldKeepOldValuesWhenNullPassed() {
            String oldName = course.getCourseName();
            course.update(null, null, null, null, null, null, null, null, null, null, null,
                    null, null, 99L);
            assertThat(course.getCourseName()).isEqualTo(oldName);
            assertThat(course.getUpdatedBy()).isEqualTo(99L);
        }

        @Test
        @DisplayName("update 时 orgUnitId 允许置空")
        void shouldAllowNullOrgUnitIdOnUpdate() {
            course = Course.builder().courseCode("X").courseName("Y").orgUnitId(10L).build();
            assertThat(course.getOrgUnitId()).isEqualTo(10L);

            course.update(null, null, null, null, null, null, null, null, null, null, null,
                    null, null, 1L);
            assertThat(course.getOrgUnitId()).isNull();
        }

        @Test
        @DisplayName("updateStatus 改状态")
        void shouldUpdateStatus() {
            course.updateStatus(0);
            assertThat(course.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("updateStatus 传 null 抛 NPE")
        void shouldFailWhenUpdateStatusNull() {
            assertThatThrownBy(() -> course.updateStatus(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
