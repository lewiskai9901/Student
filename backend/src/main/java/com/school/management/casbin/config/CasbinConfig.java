package com.school.management.casbin.config;

import lombok.extern.slf4j.Slf4j;
import org.casbin.adapter.JDBCAdapter;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Casbin 配置类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class CasbinConfig {

    @Value("${casbin.model-path:casbin/model.conf}")
    private String modelPath;

    @Value("${casbin.auto-save:true}")
    private boolean autoSave;

    /**
     * 配置 JDBC Adapter
     */
    @Bean
    public Adapter casbinAdapter(DataSource dataSource) throws Exception {
        log.info("初始化 Casbin JDBC Adapter...");
        JDBCAdapter adapter = new JDBCAdapter(dataSource);
        log.info("Casbin JDBC Adapter 初始化成功");
        return adapter;
    }

    /**
     * 配置 Enforcer
     */
    @Bean
    @DependsOn("casbinAdapter")
    public Enforcer enforcer(Adapter adapter) throws IOException {
        log.info("初始化 Casbin Enforcer...");

        // 从 classpath 加载模型配置
        ClassPathResource modelResource = new ClassPathResource(modelPath);

        // 读取模型内容
        String modelText;
        try (InputStream inputStream = modelResource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            modelText = reader.lines().collect(Collectors.joining("\n"));
        }

        // 从文本创建模型
        Model model = new Model();
        model.loadModelFromText(modelText);

        // 创建 Enforcer
        Enforcer enforcer = new Enforcer(model, adapter);

        // 配置自动保存
        enforcer.enableAutoSave(autoSave);
        log.info("Casbin Enforcer 自动保存: {}", autoSave);

        // 加载策略
        enforcer.loadPolicy();
        log.info("Casbin Enforcer 初始化成功，已加载 {} 条策略", enforcer.getPolicy().size());

        return enforcer;
    }
}
