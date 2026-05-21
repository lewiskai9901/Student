package com.school.management.infrastructure.extension.plugins.education.application.academic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.CreateCourseCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.UpdateCourseCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.query.CourseDTO;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.Course;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.repository.CourseRepository;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.academic.CoursePO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.academic.CoursePersistenceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CourseApplicationService 单测 — 课程 CRUD + 查询 + DTO 映射
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseApplicationService 测试")
class CourseApplicationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePersistenceMapper courseMapper;

    @InjectMocks
    private CourseApplicationService service;

    private CoursePO buildPO() {
        CoursePO po = new CoursePO();
        po.setId(100L);
        po.setCourseCode("CS101");
        po.setCourseName("计算机导论");
        po.setCourseNameEn("Intro to CS");
        po.setCourseCategory(1);
        po.setCourseType(1);
        po.setCourseNature(1);
        po.setCredits(new BigDecimal("3.0"));
        po.setTotalHours(48);
        po.setTheoryHours(32);
        po.setPracticeHours(16);
        po.setWeeklyHours(3);
        po.setAssessmentMethod(1);
        po.setOrgUnitId(9L);
        po.setDescription("desc");
        po.setStatus(1);
        po.setCreatedBy(99L);
        return po;
    }

    private Course buildCourse() {
        return Course.builder()
                .id(200L)
                .courseCode("CS202")
                .courseName("数据结构")
                .credits(new BigDecimal("4.0"))
                .orgUnitId(9L)
                .createdBy(99L)
                .build();
    }

    @Nested
    @DisplayName("getCourseList 分页查询")
    class GetCourseListTests {

        @Test
        @DisplayName("无过滤条件 — 返回映射后的 DTO 分页结果")
        void shouldListWithoutFilters() {
            CoursePO po = buildPO();
            Page<CoursePO> resultPage = new Page<>(1, 10);
            resultPage.setRecords(List.of(po));
            resultPage.setTotal(1);
            when(courseMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(resultPage);

            Page<CourseDTO> page = service.getCourseList(null, null, null, null, 1, 10);

            assertThat(page.getTotal()).isEqualTo(1);
            assertThat(page.getRecords()).hasSize(1);
            CourseDTO dto = page.getRecords().get(0);
            assertThat(dto.getId()).isEqualTo(100L);
            assertThat(dto.getCourseCode()).isEqualTo("CS101");
            assertThat(dto.getCourseName()).isEqualTo("计算机导论");
            assertThat(dto.getCredits()).isEqualByComparingTo("3.0");
        }

        @Test
        @DisplayName("带 keyword/category/type/status 全过滤 — selectPage 被调用")
        void shouldListWithAllFilters() {
            Page<CoursePO> resultPage = new Page<>(2, 5);
            resultPage.setRecords(List.of());
            resultPage.setTotal(0);
            when(courseMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(resultPage);

            Page<CourseDTO> page = service.getCourseList("CS", 1, 2, 1, 2, 5);

            assertThat(page.getRecords()).isEmpty();
            assertThat(page.getCurrent()).isEqualTo(2);
            verify(courseMapper).selectPage(any(IPage.class), any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("getAllCourses 全量查询")
    class GetAllCoursesTests {

        @Test
        @DisplayName("应返回全部课程映射成 DTO 列表")
        void shouldReturnAll() {
            when(courseMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(buildPO()));

            List<CourseDTO> result = service.getAllCourses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseCode()).isEqualTo("CS101");
        }

        @Test
        @DisplayName("无数据 — 返回空列表")
        void shouldReturnEmpty() {
            when(courseMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            assertThat(service.getAllCourses()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCourse / getCourseByCode 单条查询")
    class GetCourseTests {

        @Test
        @DisplayName("按 ID 查询存在 — 返回 DTO")
        void shouldGetById() {
            when(courseRepository.findById(200L)).thenReturn(Optional.of(buildCourse()));

            CourseDTO dto = service.getCourse(200L);

            assertThat(dto.getId()).isEqualTo(200L);
            assertThat(dto.getCourseName()).isEqualTo("数据结构");
        }

        @Test
        @DisplayName("按 ID 查询不存在 — 抛 IllegalArgumentException")
        void shouldThrowWhenIdNotFound() {
            when(courseRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCourse(404L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("课程不存在: 404");
        }

        @Test
        @DisplayName("按 code 查询存在 — 返回 DTO")
        void shouldGetByCode() {
            when(courseRepository.findByCourseCode("CS202"))
                    .thenReturn(Optional.of(buildCourse()));

            CourseDTO dto = service.getCourseByCode("CS202");

            assertThat(dto.getCourseCode()).isEqualTo("CS202");
        }

        @Test
        @DisplayName("按 code 查询不存在 — 抛 IllegalArgumentException")
        void shouldThrowWhenCodeNotFound() {
            when(courseRepository.findByCourseCode("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCourseByCode("NOPE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("课程不存在: NOPE");
        }
    }

    @Nested
    @DisplayName("createCourse 创建课程")
    class CreateCourseTests {

        private CreateCourseCommand cmd() {
            CreateCourseCommand c = new CreateCourseCommand();
            c.setCourseCode("CS303");
            c.setCourseName("操作系统");
            c.setCourseNameEn("OS");
            c.setCourseCategory(2);
            c.setCourseType(3);
            c.setCourseNature(1);
            c.setCredits(new BigDecimal("5.0"));
            c.setTotalHours(64);
            c.setTheoryHours(48);
            c.setPracticeHours(16);
            c.setWeeklyHours(4);
            c.setAssessmentMethod(1);
            c.setOrgUnitId(9L);
            c.setDescription("OS course");
            c.setStatus(1);
            c.setCreatedBy(99L);
            return c;
        }

        @Test
        @DisplayName("课程代码未占用 — 构建聚合根并保存, 返回 DTO")
        void shouldCreate() {
            when(courseRepository.existsByCourseCode("CS303")).thenReturn(false);
            ArgumentCaptor<Course> cap = ArgumentCaptor.forClass(Course.class);
            when(courseRepository.save(cap.capture())).thenAnswer(i -> i.getArgument(0));

            CourseDTO dto = service.createCourse(cmd());

            Course saved = cap.getValue();
            assertThat(saved.getCourseCode()).isEqualTo("CS303");
            assertThat(saved.getCourseName()).isEqualTo("操作系统");
            assertThat(saved.getCourseType()).isEqualTo(3);
            assertThat(saved.getCredits()).isEqualByComparingTo("5.0");
            assertThat(dto.getCourseCode()).isEqualTo("CS303");
            assertThat(dto.getCourseNameEn()).isEqualTo("OS");
        }

        @Test
        @DisplayName("课程代码已存在 — 抛异常且不保存")
        void shouldThrowWhenCodeExists() {
            when(courseRepository.existsByCourseCode("CS303")).thenReturn(true);

            assertThatThrownBy(() -> service.createCourse(cmd()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("课程代码已存在: CS303");

            verify(courseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCourse 更新课程")
    class UpdateCourseTests {

        private UpdateCourseCommand cmd(Integer status) {
            UpdateCourseCommand c = new UpdateCourseCommand();
            c.setCourseName("新课程名");
            c.setCredits(new BigDecimal("6.0"));
            c.setOrgUnitId(11L);
            c.setStatus(status);
            c.setUpdatedBy(88L);
            return c;
        }

        @Test
        @DisplayName("课程存在且带 status — 更新字段+状态后保存")
        void shouldUpdateWithStatus() {
            Course course = buildCourse();
            when(courseRepository.findById(200L)).thenReturn(Optional.of(course));
            when(courseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CourseDTO dto = service.updateCourse(200L, cmd(0));

            assertThat(dto.getCourseName()).isEqualTo("新课程名");
            assertThat(dto.getCredits()).isEqualByComparingTo("6.0");
            assertThat(dto.getStatus()).isEqualTo(0);
            verify(courseRepository).save(course);
        }

        @Test
        @DisplayName("status 为 null — 不调用 updateStatus, 保留原状态")
        void shouldUpdateWithoutStatus() {
            Course course = buildCourse();
            when(courseRepository.findById(200L)).thenReturn(Optional.of(course));
            when(courseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            CourseDTO dto = service.updateCourse(200L, cmd(null));

            assertThat(dto.getCourseName()).isEqualTo("新课程名");
            assertThat(dto.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("课程不存在 — 抛异常")
        void shouldThrowWhenNotFound() {
            when(courseRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCourse(404L, cmd(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("课程不存在: 404");
        }
    }

    @Nested
    @DisplayName("deleteCourse / updateCourseStatus")
    class DeleteAndStatusTests {

        @Test
        @DisplayName("deleteCourse 应委托仓储 deleteById")
        void shouldDelete() {
            service.deleteCourse(50L);
            verify(courseRepository).deleteById(50L);
        }

        @Test
        @DisplayName("updateCourseStatus 课程存在 — 改状态并保存")
        void shouldUpdateStatus() {
            Course course = buildCourse();
            when(courseRepository.findById(200L)).thenReturn(Optional.of(course));

            service.updateCourseStatus(200L, 0);

            assertThat(course.getStatus()).isEqualTo(0);
            verify(courseRepository).save(course);
        }

        @Test
        @DisplayName("updateCourseStatus 课程不存在 — 抛异常")
        void shouldThrowStatusNotFound() {
            when(courseRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCourseStatus(404L, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("课程不存在: 404");
        }
    }
}
