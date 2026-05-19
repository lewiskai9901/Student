package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.SchedulingStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.TaskStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.TaskTeacher;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.TeachingTask;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.TaskTeacherRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.TeachingTaskRepository;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.task.TeachingTaskMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeachingTaskApplicationService 单测 — 验证 create/update/status/teacher 分配编排
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeachingTaskApplicationService 测试")
class TeachingTaskApplicationServiceTest {

    @Mock
    private TeachingTaskRepository taskRepo;

    @Mock
    private TaskTeacherRepository teacherRepo;

    @Mock
    private TeachingTaskMapper taskMapper;

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private TeachingTaskApplicationService service;

    private TeachingTask sampleTask(TaskStatus status) {
        return TeachingTask.reconstruct(
                1L, "TT-1", 1L, 2L, 3L,
                30, 4, 64, 1, 16,
                "ROOM_LECTURE", 2, 1,
                SchedulingStatus.UNSCHEDULED, status,
                "", 9L);
    }

    @Nested
    @DisplayName("更新状态")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新现有任务的状态")
        void shouldUpdateStatus() {
            TeachingTask task = sampleTask(TaskStatus.PENDING);
            when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
            when(taskRepo.save(task)).thenReturn(task);

            service.updateStatus(1L, TaskStatus.SCHEDULED.getCode());

            assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.SCHEDULED);
            verify(taskRepo).save(task);
        }

        @Test
        @DisplayName("任务不存在抛异常")
        void shouldFailWhenTaskNotFound() {
            when(taskRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(999L, TaskStatus.SCHEDULED.getCode()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("教学任务不存在");
            verify(taskRepo, never()).save(any());
        }

        @Test
        @DisplayName("非法 statusCode 抛异常")
        void shouldFailWhenStatusInvalid() {
            TeachingTask task = sampleTask(TaskStatus.PENDING);
            when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> service.updateStatus(1L, 9999))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("更新任务")
    class UpdateTests {

        @Test
        @DisplayName("应根据 data 更新字段")
        void shouldUpdate() {
            TeachingTask task = sampleTask(TaskStatus.PENDING);
            when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

            Map<String, Object> data = new HashMap<>();
            data.put("courseId", 22L);
            data.put("orgUnitId", 33L);
            data.put("studentCount", 50);
            data.put("weeklyHours", 6);
            data.put("totalHours", 96);
            data.put("startWeek", 2);
            data.put("endWeek", 17);
            data.put("roomTypeRequired", "ROOM_LAB");
            data.put("consecutivePeriods", 3);
            data.put("courseNature", 2);
            data.put("remark", "new");

            service.update(1L, data);

            assertThat(task.getCourseId()).isEqualTo(22L);
            assertThat(task.getStudentCount()).isEqualTo(50);
            assertThat(task.getRoomTypeRequired()).isEqualTo("ROOM_LAB");
            verify(taskRepo).save(task);
        }

        @Test
        @DisplayName("不存在的任务抛异常")
        void shouldFailWhenNotFound() {
            when(taskRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(999L, new HashMap<>()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("分配教师")
    class AssignTeachersTests {

        @Test
        @DisplayName("分配多名教师并自动推进状态")
        void shouldAssignTeachersAndAdvanceStatus() {
            TeachingTask task = sampleTask(TaskStatus.PENDING);
            when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

            Map<String, Object> t1 = new HashMap<>();
            t1.put("teacherId", 100L);
            t1.put("role", 1);
            t1.put("weeklyHours", 2);
            Map<String, Object> t2 = new HashMap<>();
            t2.put("teacherId", 200L);
            t2.put("role", 2);
            t2.put("weeklyHours", 2);

            service.assignTeachers(1L, List.of(t1, t2));

            verify(teacherRepo).deleteByTaskId(1L);
            verify(teacherRepo, times(2)).save(any(TaskTeacher.class));
            // 状态从 PENDING 推进到 TEACHER_ASSIGNED
            assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.TEACHER_ASSIGNED);
            verify(taskRepo).save(task);
        }

        @Test
        @DisplayName("已分配状态再分配空列表 — 回退到 PENDING")
        void shouldRevertToPendingWhenClearTeachers() {
            TeachingTask task = sampleTask(TaskStatus.TEACHER_ASSIGNED);
            when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

            service.assignTeachers(1L, List.of());

            verify(teacherRepo).deleteByTaskId(1L);
            verify(teacherRepo, never()).save(any(TaskTeacher.class));
            assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.PENDING);
        }

        @Test
        @DisplayName("removeTeacher 应只删除指定关联")
        void shouldRemoveSingleTeacher() {
            service.removeTeacher(1L, 100L);
            verify(teacherRepo).deleteByTaskIdAndTeacherId(1L, 100L);
        }
    }

    @Nested
    @DisplayName("删除任务")
    class DeleteTests {

        @Test
        @DisplayName("应调用 repo.deleteById")
        void shouldDelete() {
            service.delete(1L);
            verify(taskRepo).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("查询任务")
    class QueryTests {

        @Test
        @DisplayName("getById — 任务不存在抛异常")
        void shouldFailGetByIdNotFound() {
            when(taskRepo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("教学任务不存在");
        }

        @Test
        @DisplayName("list — 包含 records 与 total 字段")
        void shouldReturnPagedList() {
            when(taskRepo.countByFilter(eq(1L), any())).thenReturn(0L);
            when(taskRepo.findByFilter(eq(1L), any(), eq(0), eq(20)))
                    .thenReturn(List.of());

            Map<String, Object> result = service.list(1L, null, 1, 20);

            assertThat(result).containsKey("records");
            assertThat(result).containsEntry("total", 0L);
        }
    }
}
