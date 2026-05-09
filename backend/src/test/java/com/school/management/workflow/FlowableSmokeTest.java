package com.school.management.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 引擎 smoke test — 验证 Phase 1 目标:
 * <ol>
 *   <li>ProcessEngine 启动成功</li>
 *   <li>classpath:/processes/hello-world.bpmn20.xml 自动部署</li>
 *   <li>启动实例 + 设 assignee variable</li>
 *   <li>查询 user task + 完成</li>
 *   <li>流程结束(无活跃实例)</li>
 * </ol>
 *
 * <p><b>Slice 测试策略</b>: 本项目应用上下文紧耦合 MySQL (AccessRelationService.bootImpliedCache
 * 等启动期 raw SQL 查询 relation_types 表), 在 H2 启动会失败. 因此用最小 Spring 上下文
 * + 自定义 {@link FlowableTestConfig} 只装配 DataSource + Flowable engine, 不加载业务 bean.
 *
 * <p>跑业务全栈集成的工作交给 Phase 2 引入的真集成测试基础设施 (Testcontainers MySQL).
 */
@Tag("integration")
@SpringBootTest(
    classes = FlowableSmokeTest.FlowableTestConfig.class,
    properties = {
        // 不要 MODE=MYSQL —— Flowable H2 schema 用 native IDENTITY 关键字, MySQL 模式下不识别
        "spring.datasource.url=jdbc:h2:mem:flowable_smoke;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "flowable.database-schema-update=create-drop",
        "flowable.async-executor-activate=false",
        "flowable.history-level=audit",
        "flowable.process-definition-location-prefix=classpath*:/processes/",
        // 关键:覆盖 application-dev.yml 的 Flowable autoconfig exclude
        "spring.autoconfigure.exclude="
    }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlowableSmokeTest {

    @SpringBootConfiguration
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        ProcessEngineAutoConfiguration.class,
        ProcessEngineServicesAutoConfiguration.class
    })
    static class FlowableTestConfig {
    }

    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired TaskService taskService;

    /**
     * 显式部署 BPMN —— Flowable 7 在 sliced @SpringBootTest 上下文里 auto-deploy
     * 偶尔不触发 (依赖 ProcessEngineAutoDeploymentStrategy bean), 显式 deploy 更可靠.
     * 真正的应用启动 (StudentManagementApplication) 装配齐全 autoconfig, 会自动部署.
     */
    @BeforeAll
    void deployHelloWorld() {
        if (repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("hello_world").count() == 0) {
            repositoryService.createDeployment()
                .name("hello-world-smoke-test")
                .addClasspathResource("processes/hello-world.bpmn20.xml")
                .deploy();
        }
    }

    @Test
    void engineStarts_andHelloWorldIsDeployed() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        assertThat(deployments).isNotEmpty();
        long count = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("hello_world").count();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void canStartProcessAndCompleteTask() {
        // 1. 启动实例,assignee = "alice"
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
            "hello_world", Map.of("approver", "alice"));
        assertThat(pi).isNotNull();
        assertThat(pi.isEnded()).isFalse();

        // 2. 查询当前活跃任务,应该是 alice 的 approve_task
        List<Task> tasks = taskService.createTaskQuery()
            .processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("审批 hello world");
        assertThat(task.getAssignee()).isEqualTo("alice");

        // 3. 完成任务
        taskService.complete(task.getId());

        // 4. 流程应已结束
        long active = runtimeService.createProcessInstanceQuery()
            .processInstanceId(pi.getId()).count();
        assertThat(active).isZero();
    }
}
