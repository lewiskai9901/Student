package com.school.management.infrastructure.extension.plugins.education.domain.student.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 班级聚合根单元测试
 * 测试班级的创建、状态流转、教师分配、人数管理等功能
 */
@DisplayName("班级聚合根测试")
class SchoolClassTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long ORG_UNIT_ID = 10L;
    private static final Long MAJOR_DIRECTION_ID = 20L;
    private static final Long TEACHER_ID = 100L;
    private static final String TEACHER_NAME = "张老师";

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = createValidSchoolClass();
    }

    private SchoolClass createValidSchoolClass() {
        return SchoolClass.create(
            "CLASS2024001",
            "2024级软件1班",
            ORG_UNIT_ID,
            2024,
            MAJOR_DIRECTION_ID,
            CREATOR_ID
        );
    }

    @Nested
    @DisplayName("创建班级测试")
    class CreateSchoolClassTest {

        @Test
        @DisplayName("成功创建班级")
        void shouldCreateSchoolClassSuccessfully() {
            assertNotNull(schoolClass);
            assertEquals("CLASS2024001", schoolClass.getClassCode());
            assertEquals("2024级软件1班", schoolClass.getClassName());
            assertEquals(ORG_UNIT_ID, schoolClass.getOrgUnitId());
            assertEquals(2024, schoolClass.getEnrollmentYear());
            assertEquals(MAJOR_DIRECTION_ID, schoolClass.getMajorDirectionId());
            assertEquals(ClassStatus.PREPARING, schoolClass.getStatus());
            assertEquals(CREATOR_ID, schoolClass.getCreatedBy());
            assertEquals(0, schoolClass.getCurrentSize());
            assertNotNull(schoolClass.getCreatedAt());
            assertFalse(schoolClass.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("创建班级时缺少班级编码应抛出异常")
        void shouldFailWhenClassCodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                SchoolClass.create(null, "2024级软件1班", ORG_UNIT_ID, 2024, MAJOR_DIRECTION_ID, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建班级时缺少班级名称应抛出异常")
        void shouldFailWhenClassNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                SchoolClass.create("CLASS2024001", null, ORG_UNIT_ID, 2024, MAJOR_DIRECTION_ID, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建班级时缺少组织单元ID应抛出异常")
        void shouldFailWhenOrgUnitIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                SchoolClass.create("CLASS2024001", "2024级软件1班", null, 2024, MAJOR_DIRECTION_ID, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建班级时入学年份无效应抛出异常")
        void shouldFailWhenEnrollmentYearInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                SchoolClass.create("CLASS2024001", "2024级软件1班", ORG_UNIT_ID, 1990, MAJOR_DIRECTION_ID, CREATOR_ID)
            );
        }
    }

    @Nested
    @DisplayName("班级状态流转测试")
    class ClassStatusTransitionTest {

        @Test
        @DisplayName("激活筹备中的班级")
        void shouldActivatePreparingClass() {
            schoolClass.activate(CREATOR_ID);

            assertEquals(ClassStatus.ACTIVE, schoolClass.getStatus());
            assertFalse(schoolClass.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("非筹备中状态不能激活")
        void shouldFailToActivateNonPreparingClass() {
            schoolClass.activate(CREATOR_ID);

            assertThrows(IllegalStateException.class, () ->
                schoolClass.activate(CREATOR_ID)
            );
        }

        @Test
        @DisplayName("激活的班级可以毕业")
        void shouldGraduateActiveClass() {
            schoolClass.activate(CREATOR_ID);
            schoolClass.clearDomainEvents();

            schoolClass.graduate(CREATOR_ID);

            assertEquals(ClassStatus.GRADUATED, schoolClass.getStatus());
            assertFalse(schoolClass.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("非激活状态不能毕业")
        void shouldFailToGraduateNonActiveClass() {
            assertThrows(IllegalStateException.class, () ->
                schoolClass.graduate(CREATOR_ID)
            );
        }

        @Test
        @DisplayName("撤销班级")
        void shouldDissolveClass() {
            schoolClass.dissolve(CREATOR_ID);

            assertEquals(ClassStatus.DISSOLVED, schoolClass.getStatus());
        }

        @Test
        @DisplayName("已毕业班级不能撤销")
        void shouldFailToDissolveGraduatedClass() {
            schoolClass.activate(CREATOR_ID);
            schoolClass.graduate(CREATOR_ID);

            assertThrows(IllegalStateException.class, () ->
                schoolClass.dissolve(CREATOR_ID)
            );
        }

        @Test
        @DisplayName("已撤销班级不能再次撤销")
        void shouldFailToDissolveAlreadyDissolvedClass() {
            schoolClass.dissolve(CREATOR_ID);

            assertThrows(IllegalStateException.class, () ->
                schoolClass.dissolve(CREATOR_ID)
            );
        }
    }

    @Nested
    @DisplayName("教师分配测试")
    class TeacherAssignmentTest {

        @Test
        @DisplayName("分配班主任")
        void shouldAssignHeadTeacher() {
            schoolClass.assignHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);

            assertTrue(schoolClass.getCurrentHeadTeacher().isPresent());
            assertEquals(TEACHER_ID, schoolClass.getCurrentHeadTeacher().get().getTeacherId());
            assertEquals(TEACHER_NAME, schoolClass.getCurrentHeadTeacher().get().getTeacherName());
            assertFalse(schoolClass.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("更换班主任时结束原班主任任职")
        void shouldEndPreviousHeadTeacherWhenAssigningNew() {
            schoolClass.assignHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);
            Long newTeacherId = 200L;
            String newTeacherName = "李老师";

            schoolClass.assignHeadTeacher(newTeacherId, newTeacherName, CREATOR_ID);

            assertTrue(schoolClass.getCurrentHeadTeacher().isPresent());
            assertEquals(newTeacherId, schoolClass.getCurrentHeadTeacher().get().getTeacherId());
        }

        @Test
        @DisplayName("分配副班主任")
        void shouldAssignDeputyHeadTeacher() {
            schoolClass.assignDeputyHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);

            assertEquals(1, schoolClass.getCurrentDeputyHeadTeachers().size());
            assertEquals(TEACHER_ID, schoolClass.getCurrentDeputyHeadTeachers().get(0).getTeacherId());
        }

        @Test
        @DisplayName("可以有多个副班主任")
        void shouldAllowMultipleDeputyHeadTeachers() {
            schoolClass.assignDeputyHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);
            schoolClass.assignDeputyHeadTeacher(200L, "李老师", CREATOR_ID);

            assertEquals(2, schoolClass.getCurrentDeputyHeadTeachers().size());
        }

        @Test
        @DisplayName("结束教师任职")
        void shouldEndTeacherAssignment() {
            schoolClass.assignHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);

            schoolClass.endTeacherAssignment(TEACHER_ID, TeacherAssignment.TeacherRole.HEAD_TEACHER);

            assertFalse(schoolClass.getCurrentHeadTeacher().isPresent());
        }

        @Test
        @DisplayName("获取所有当前任职教师")
        void shouldGetAllCurrentTeachers() {
            schoolClass.assignHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);
            schoolClass.assignDeputyHeadTeacher(200L, "李老师", CREATOR_ID);

            assertEquals(2, schoolClass.getCurrentTeachers().size());
        }

        @Test
        @DisplayName("毕业时结束所有任职")
        void shouldEndAllAssignmentsWhenGraduate() {
            schoolClass.assignHeadTeacher(TEACHER_ID, TEACHER_NAME, CREATOR_ID);
            schoolClass.assignDeputyHeadTeacher(200L, "李老师", CREATOR_ID);
            schoolClass.activate(CREATOR_ID);

            schoolClass.graduate(CREATOR_ID);

            assertTrue(schoolClass.getCurrentTeachers().isEmpty());
        }
    }

    @Nested
    @DisplayName("学生人数管理测试")
    class StudentSizeManagementTest {

        @Test
        @DisplayName("更新学生人数")
        void shouldUpdateCurrentSize() {
            schoolClass.updateCurrentSize(30);

            assertEquals(30, schoolClass.getCurrentSize());
        }

        @Test
        @DisplayName("学生人数不能为负数")
        void shouldFailWhenSizeIsNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                schoolClass.updateCurrentSize(-1)
            );
        }

        @Test
        @DisplayName("增加学生")
        void shouldIncrementSize() {
            schoolClass.incrementSize();

            assertEquals(1, schoolClass.getCurrentSize());
        }

        @Test
        @DisplayName("减少学生")
        void shouldDecrementSize() {
            schoolClass.updateCurrentSize(10);
            schoolClass.decrementSize();

            assertEquals(9, schoolClass.getCurrentSize());
        }

        @Test
        @DisplayName("人数为0时减少不变")
        void shouldNotDecrementBelowZero() {
            schoolClass.decrementSize();

            assertEquals(0, schoolClass.getCurrentSize());
        }

        @Test
        @DisplayName("检查是否满员")
        void shouldCheckIfFull() {
            schoolClass.updateCurrentSize(50); // 默认standardSize是50

            assertTrue(schoolClass.isFull());
        }

        @Test
        @DisplayName("计算空余名额")
        void shouldCalculateAvailableSlots() {
            schoolClass.updateCurrentSize(30);

            assertEquals(20, schoolClass.getAvailableSlots());
        }
    }

    @Nested
    @DisplayName("其他业务方法测试")
    class OtherBusinessMethodsTest {

        @Test
        @DisplayName("计算预计毕业年份")
        void shouldCalculateExpectedGraduationYear() {
            // 默认学制3年
            assertEquals(2027, schoolClass.getExpectedGraduationYear());
        }

        @Test
        @DisplayName("更新班级基本信息")
        void shouldUpdateInfo() {
            schoolClass.updateInfo("2024级软件技术1班", "软技1", 45, 1, CREATOR_ID);

            assertEquals("2024级软件技术1班", schoolClass.getClassName());
            assertEquals("软技1", schoolClass.getShortName());
            assertEquals(45, schoolClass.getStandardSize());
            assertEquals(1, schoolClass.getSortOrder());
        }

        @Test
        @DisplayName("更新时空名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithBlank() {
            schoolClass.updateInfo("", null, null, null, CREATOR_ID);

            assertEquals("2024级软件1班", schoolClass.getClassName());
        }
    }

    @Nested
    @DisplayName("Builder测试")
    class BuilderTest {

        @Test
        @DisplayName("使用Builder构建完整对象")
        void shouldBuildSchoolClassWithAllFields() {
            SchoolClass built = SchoolClass.builder()
                .id(1L)
                .classCode("CLASS2024001")
                .className("2024级软件1班")
                .shortName("软1")
                .orgUnitId(ORG_UNIT_ID)
                .enrollmentYear(2024)
                .gradeLevel(1)
                .majorDirectionId(MAJOR_DIRECTION_ID)
                .schoolingYears(4)
                .standardSize(40)
                .currentSize(35)
                .status(ClassStatus.ACTIVE)
                .sortOrder(1)
                .createdBy(CREATOR_ID)
                .build();

            assertEquals(1L, built.getId());
            assertEquals("软1", built.getShortName());
            assertEquals(4, built.getSchoolingYears());
            assertEquals(40, built.getStandardSize());
            assertEquals(35, built.getCurrentSize());
            assertEquals(ClassStatus.ACTIVE, built.getStatus());
        }
    }
}
