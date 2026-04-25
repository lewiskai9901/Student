package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据资源 Registrar — 扫描所有 {@link DataResourceProvider}, UPDATE data_resources.allowed_scopes.
 *
 * 只写 allowed_scopes 一列, 不 INSERT 新行 — 行本身由早期 migration 落库.
 * 若 provider 声明的 resourceCode 在表里不存在, 记 WARN 并 SKIPPED, 不抛异常.
 *
 * @Order(700): 在 Permission(400) / DataScope(600) 之后, 不影响权限/维度注册.
 */
@Slf4j
@Component
@Order(700)
public class DataResourceRegistrar
        extends AbstractPluginRegistrar<DataResourceProvider, DataResourceProvider.DataResourceDef> {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<DataResourceProvider> providers;

    public DataResourceRegistrar(List<DataResourceProvider> providers,
                                  JdbcTemplate jdbc,
                                  PluginPackageRegistrar packageRegistrar) {
        super(jdbc, packageRegistrar);
        this.providers = providers;
    }

    @Override protected List<DataResourceProvider> getPluginList() { return providers; }

    @Override protected List<DataResourceProvider.DataResourceDef> extractDefs(DataResourceProvider p) {
        return p.getDataResources();
    }

    @Override protected String describeDef(DataResourceProvider.DataResourceDef def) {
        return def.resourceCode();
    }

    @Override
    protected UpsertResult upsertOne(DataResourceProvider provider,
                                      DataResourceProvider.DataResourceDef def,
                                      String industry, String pluginClass) {
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM data_resources WHERE resource_code=?",
            Long.class, def.resourceCode());
        if (exists == null || exists == 0) {
            log.warn("[DataResourceRegistrar] resource_code={} 不存在于 data_resources, 跳过 (插件 {})",
                    def.resourceCode(), pluginClass);
            return UpsertResult.SKIPPED;
        }
        String allowedScopesJson;
        try {
            allowedScopesJson = JSON.writeValueAsString(def.allowedScopes());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("allowed_scopes 序列化失败: " + def.resourceCode(), e);
        }
        jdbc.update(
            "UPDATE data_resources SET allowed_scopes=? WHERE resource_code=?",
            allowedScopesJson, def.resourceCode());
        return UpsertResult.UPDATED;
    }
}
