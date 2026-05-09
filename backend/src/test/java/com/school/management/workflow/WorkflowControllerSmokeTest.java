package com.school.management.workflow;

import com.school.management.interfaces.rest.workflow.ProcessDefinitionController;
import com.school.management.interfaces.rest.workflow.ProcessInstanceController;
import com.school.management.interfaces.rest.workflow.TaskController;
import com.school.management.interfaces.rest.workflow.WorkflowHistoryController;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Phase 2 — 验证 4 个 workflow controller 可以用 mock service 实例化.
 * 不启动 Spring context, 仅验证编译和构造器签名.
 */
class WorkflowControllerSmokeTest {

    @Test
    void controllers_canBeInstantiated() {
        assertThat(new ProcessDefinitionController(mock(RepositoryService.class))).isNotNull();
        assertThat(new ProcessInstanceController(mock(RuntimeService.class))).isNotNull();
        assertThat(new TaskController(mock(TaskService.class))).isNotNull();
        assertThat(new WorkflowHistoryController(mock(HistoryService.class))).isNotNull();
    }
}
