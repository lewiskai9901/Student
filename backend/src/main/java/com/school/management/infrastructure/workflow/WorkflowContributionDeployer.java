package com.school.management.infrastructure.workflow;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.PluginPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 插件 BPMN 自动部署器 (Phase 5).
 *
 * <p>启动期扫所有 PluginPackage.contribute() 流, 找出 WorkflowContribution,
 * 从 classpath 加载对应 .bpmn20.xml 文件, 部署到 Flowable RepositoryService.
 *
 * <p>幂等: Flowable RepositoryService.deploy() 在 enableDuplicateFiltering()
 * 模式下按 deployment name + resource hash 去重 — 同 name + 同 BPMN content
 * 不会重复 deploy. 文件 hash 变了会创新 version.
 *
 * <p>仅当 Flowable RepositoryService bean 存在时启用 (dev profile exclude
 * Flowable autoconfig 时本 deployer 不工作, 不抛 NPE).
 */
@Slf4j
@Component
@ConditionalOnBean(RepositoryService.class)
@RequiredArgsConstructor
public class WorkflowContributionDeployer {

    private final RepositoryService repositoryService;
    private final ResourceLoader resourceLoader;
    @Autowired(required = false) private List<PluginPackage> plugins = List.of();

    @EventListener(ApplicationReadyEvent.class)
    public void deployAll() {
        int total = 0, deployed = 0, skipped = 0;

        for (PluginPackage plugin : plugins) {
            List<Contribution.WorkflowContribution> wfs = plugin.contribute()
                .filter(c -> c instanceof Contribution.WorkflowContribution)
                .map(c -> (Contribution.WorkflowContribution) c)
                .toList();
            for (Contribution.WorkflowContribution wf : wfs) {
                total++;
                try {
                    Resource resource = resourceLoader.getResource("classpath:" + wf.resourcePath());
                    if (!resource.exists()) {
                        log.warn("[WF Deployer] 跳过 {} — classpath 资源不存在", wf.resourcePath());
                        skipped++;
                        continue;
                    }
                    String name = "[" + wf.industryCode() + "] " + wf.description();
                    try (InputStream in = resource.getInputStream()) {
                        Deployment d = repositoryService.createDeployment()
                            .name(name)
                            .key(wf.industryCode() + ":" + wf.resourcePath())
                            .addInputStream(extractFileName(wf.resourcePath()), in)
                            .enableDuplicateFiltering()
                            .deploy();
                        log.info("[WF Deployer] deployed {} (id={}, name={})",
                            wf.resourcePath(), d.getId(), name);
                        deployed++;
                    }
                } catch (Exception e) {
                    log.error("[WF Deployer] 部署失败 {} ({}): {}",
                        wf.resourcePath(), wf.industryCode(), e.getMessage(), e);
                }
            }
        }

        log.info("[WF Deployer] 完成 — 总 {} 个 WorkflowContribution, 部署 {}, 跳过 {}",
            total, deployed, skipped);
    }

    private String extractFileName(String classpathPath) {
        int slash = classpathPath.lastIndexOf('/');
        return slash >= 0 ? classpathPath.substring(slash + 1) : classpathPath;
    }
}
