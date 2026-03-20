package com.school.management.infrastructure.casbin;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.adapter.JDBCAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Configuration
public class CasbinConfig {

    @Bean
    public Enforcer enforcer(DataSource dataSource) throws Exception {
        // Copy model.conf from classpath to temp file (jCasbin needs file path)
        Path tempModel = Files.createTempFile("casbin-model", ".conf");
        try (InputStream is = new ClassPathResource("casbin/model.conf").getInputStream()) {
            Files.copy(is, tempModel, StandardCopyOption.REPLACE_EXISTING);
        }

        // Create enforcer with model only (no adapter) to avoid NPE during construction
        Enforcer enforcer = new Enforcer(tempModel.toString());
        Files.deleteIfExists(tempModel);

        // Set adapter separately and load policy in try-catch
        try {
            JDBCAdapter adapter = new JDBCAdapter(dataSource);
            enforcer.setAdapter(adapter);
            enforcer.loadPolicy();
            log.info("Casbin enforcer initialized with JDBC adapter, {} policies loaded",
                    enforcer.getPolicy().size());
        } catch (Exception e) {
            log.warn("Casbin loadPolicy failed (table may be empty or have stale data): {}. " +
                    "Call CasbinPolicyService.syncFromDatabase() to rebuild.", e.getMessage());
            enforcer.clearPolicy();
        }

        return enforcer;
    }
}
