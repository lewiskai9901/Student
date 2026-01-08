package com.school.management.application.task;

import com.school.management.application.task.query.TaskDTO;
import com.school.management.domain.task.model.aggregate.Task;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import com.school.management.domain.task.repository.TaskRepository;
import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TaskQueryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskQueryService tests")
class TaskQueryServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new TaskQueryService(taskRepository);
    }

    @Nested
    @DisplayName("Get task by ID tests")
    class GetByIdTest {

        @Test
        @DisplayName("Successfully get task by ID")
        void shouldGetTaskById() {
            Task task = createTask(1L, "TASK-001", "Test Task");
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            TaskDTO result = queryService.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("TASK-001", result.getTaskCode());
            assertEquals("Test Task", result.getTitle());

            verify(taskRepository).findById(1L);
        }

        @Test
        @DisplayName("Throw exception when task not found")
        void shouldThrowExceptionWhenNotFound() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () ->
                    queryService.getById(999L)
            );
        }
    }

    @Nested
    @DisplayName("Get task by code tests")
    class GetByTaskCodeTest {

        @Test
        @DisplayName("Successfully get task by code")
        void shouldGetTaskByCode() {
            Task task = createTask(1L, "TASK-001", "Test Task");
            when(taskRepository.findByTaskCode("TASK-001")).thenReturn(Optional.of(task));

            TaskDTO result = queryService.getByTaskCode("TASK-001");

            assertNotNull(result);
            assertEquals("TASK-001", result.getTaskCode());

            verify(taskRepository).findByTaskCode("TASK-001");
        }

        @Test
        @DisplayName("Throw exception when task code not found")
        void shouldThrowExceptionWhenCodeNotFound() {
            when(taskRepository.findByTaskCode("INVALID")).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () ->
                    queryService.getByTaskCode("INVALID")
            );
        }
    }

    @Nested
    @DisplayName("Find tasks by assigner tests")
    class FindByAssignerTest {

        @Test
        @DisplayName("Find tasks by assigner ID")
        void shouldFindTasksByAssignerId() {
            List<Task> tasks = Arrays.asList(
                    createTask(1L, "TASK-001", "Task 1"),
                    createTask(2L, "TASK-002", "Task 2")
            );
            when(taskRepository.findByAssignerId(100L)).thenReturn(tasks);

            List<TaskDTO> result = queryService.findByAssignerId(100L);

            assertEquals(2, result.size());
            verify(taskRepository).findByAssignerId(100L);
        }

        @Test
        @DisplayName("Return empty list when no tasks found")
        void shouldReturnEmptyListWhenNoTasks() {
            when(taskRepository.findByAssignerId(100L)).thenReturn(Collections.emptyList());

            List<TaskDTO> result = queryService.findByAssignerId(100L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find pending tasks tests")
    class FindPendingTasksTest {

        @Test
        @DisplayName("Find pending tasks for user")
        void shouldFindPendingTasksForUser() {
            List<Task> tasks = Arrays.asList(
                    createTask(1L, "TASK-001", "Pending Task 1"),
                    createTask(2L, "TASK-002", "Pending Task 2")
            );
            when(taskRepository.findPendingByAssigneeId(100L)).thenReturn(tasks);

            List<TaskDTO> result = queryService.findPendingTasksForUser(100L);

            assertEquals(2, result.size());
            verify(taskRepository).findPendingByAssigneeId(100L);
        }
    }

    @Nested
    @DisplayName("Statistics tests")
    class StatisticsTest {

        @Test
        @DisplayName("Get task statistics")
        void shouldGetStatistics() {
            when(taskRepository.countByStatus(TaskStatus.PENDING)).thenReturn(5L);
            when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(3L);
            when(taskRepository.countByStatus(TaskStatus.SUBMITTED)).thenReturn(2L);
            when(taskRepository.countByStatus(TaskStatus.COMPLETED)).thenReturn(10L);
            when(taskRepository.countByStatus(TaskStatus.REJECTED)).thenReturn(1L);

            TaskQueryService.TaskStatistics stats = queryService.getStatistics();

            assertEquals(5L, stats.pending());
            assertEquals(3L, stats.inProgress());
            assertEquals(2L, stats.submitted());
            assertEquals(10L, stats.completed());
            assertEquals(1L, stats.rejected());
            assertEquals(21L, stats.total());
        }
    }

    @Nested
    @DisplayName("Exists tests")
    class ExistsTest {

        @Test
        @DisplayName("Check if task exists by ID")
        void shouldCheckExistsById() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(createTask(1L, "TASK-001", "Task")));
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertTrue(queryService.existsById(1L));
            assertFalse(queryService.existsById(999L));
        }

        @Test
        @DisplayName("Check if task code exists")
        void shouldCheckExistsByTaskCode() {
            when(taskRepository.existsByTaskCode("TASK-001")).thenReturn(true);
            when(taskRepository.existsByTaskCode("INVALID")).thenReturn(false);

            assertTrue(queryService.existsByTaskCode("TASK-001"));
            assertFalse(queryService.existsByTaskCode("INVALID"));
        }
    }

    private Task createTask(Long id, String taskCode, String title) {
        return Task.reconstruct(
                id,
                taskCode,
                title,
                "Description",
                TaskPriority.NORMAL,
                1L,
                "Assigner",
                1,
                1L,
                LocalDateTime.now().plusDays(7),
                TaskStatus.PENDING,
                Arrays.asList(100L),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
