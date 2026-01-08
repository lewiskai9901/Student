package com.school.management.interfaces.rest.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.task.TaskApplicationService;
import com.school.management.domain.task.model.aggregate.Task;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task V2 Controller集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("任务V2 API测试")
class TaskControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskApplicationService taskService;

    private Task testTask;

    @BeforeEach
    void setUp() {
        testTask = Task.create(
            "TASK-001",
            "测试任务",
            "任务描述",
            TaskPriority.NORMAL,
            1L,
            "管理员",
            10L,
            LocalDateTime.now().plusDays(7),
            Arrays.asList(100L),
            1
        );
    }

    @Nested
    @DisplayName("GET /api/v2/tasks")
    class ListTasksTest {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("获取任务列表")
        void shouldListTasks() throws Exception {
            when(taskService.findAllTasks()).thenReturn(List.of(testTask));

            mockMvc.perform(get("/api/v2/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

            verify(taskService).findAllTasks();
        }

        @Test
        @DisplayName("未认证返回401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v2/tasks"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v2/tasks/{id}")
    class GetTaskByIdTest {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("根据ID获取任务")
        void shouldGetTaskById() throws Exception {
            when(taskService.findTaskById(1L)).thenReturn(Optional.of(testTask));

            mockMvc.perform(get("/api/v2/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("测试任务"));

            verify(taskService).findTaskById(1L);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("任务不存在返回404")
        void shouldReturn404WhenNotFound() throws Exception {
            when(taskService.findTaskById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v2/tasks/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/tasks")
    class CreateTaskTest {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("创建任务")
        void shouldCreateTask() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("新任务");
            request.setDescription("任务描述");
            request.setPriority("NORMAL");
            request.setDeadline(LocalDateTime.now().plusDays(7));
            request.setAssigneeIds(Arrays.asList(100L));

            when(taskService.createTask(any())).thenReturn(testTask);

            mockMvc.perform(post("/api/v2/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(taskService).createTask(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("缺少必填字段返回400")
        void shouldReturn400WhenMissingRequiredFields() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest();
            // 缺少title等必填字段

            mockMvc.perform(post("/api/v2/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/tasks/{id}/accept")
    class AcceptTaskTest {

        @Test
        @WithMockUser(username = "100", roles = "USER")
        @DisplayName("接受任务")
        void shouldAcceptTask() throws Exception {
            doNothing().when(taskService).acceptTask(anyLong(), anyLong());

            mockMvc.perform(post("/api/v2/tasks/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(taskService).acceptTask(eq(1L), anyLong());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/tasks/{id}/submit")
    class SubmitTaskTest {

        @Test
        @WithMockUser(username = "100", roles = "USER")
        @DisplayName("提交任务")
        void shouldSubmitTask() throws Exception {
            SubmitTaskRequest request = new SubmitTaskRequest();
            request.setContent("已完成工作");
            request.setAttachmentIds(Arrays.asList(1L, 2L));

            doNothing().when(taskService).submitTask(any());

            mockMvc.perform(post("/api/v2/tasks/1/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(taskService).submitTask(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/tasks/{id}/approve")
    class ApproveTaskTest {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("审批通过任务")
        void shouldApproveTask() throws Exception {
            ApproveRequest request = new ApproveRequest();
            request.setComment("审批通过");

            doNothing().when(taskService).approveTask(any());

            mockMvc.perform(post("/api/v2/tasks/1/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(taskService).approveTask(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/tasks/{id}/reject")
    class RejectTaskTest {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("驳回任务")
        void shouldRejectTask() throws Exception {
            RejectRequest request = new RejectRequest();
            request.setReason("数据有误，请修改");

            doNothing().when(taskService).rejectTask(any());

            mockMvc.perform(post("/api/v2/tasks/1/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(taskService).rejectTask(any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v2/tasks/{id}")
    class CancelTaskTest {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("取消任务")
        void shouldCancelTask() throws Exception {
            doNothing().when(taskService).cancelTask(anyLong(), anyString(), anyLong());

            mockMvc.perform(delete("/api/v2/tasks/1")
                    .param("reason", "不再需要"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(taskService).cancelTask(eq(1L), eq("不再需要"), anyLong());
        }
    }
}
