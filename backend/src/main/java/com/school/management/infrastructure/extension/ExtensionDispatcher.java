package com.school.management.infrastructure.extension;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 插件调度器 — 自动发现所有 EntityTypePlugin 实现，按 entityType+typeCode 分组调度。
 */
@Slf4j
@Service
public class ExtensionDispatcher {

    private final Map<String, List<EntityTypePlugin>> registry = new HashMap<>();
    private final List<EntityTypePlugin> allPlugins;

    @Autowired
    public ExtensionDispatcher(@Autowired(required = false) List<EntityTypePlugin> plugins) {
        this.allPlugins = plugins != null ? plugins : List.of();
        for (EntityTypePlugin plugin : this.allPlugins) {
            String key = plugin.getEntityType() + "_" + plugin.getTypeCode();
            registry.computeIfAbsent(key, k -> new ArrayList<>()).add(plugin);
        }
        registry.values().forEach(list ->
            list.sort(Comparator.comparingInt(EntityTypePlugin::getOrder)));
        log.info("ExtensionDispatcher: 注册 {} 个插件, {} 个类型",
            this.allPlugins.size(), registry.size());
    }

    public List<EntityTypePlugin> getAllPlugins() {
        return Collections.unmodifiableList(allPlugins);
    }

    public void fireBeforeCreate(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.beforeCreate(ctx));
    }

    public void fireAfterCreate(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.afterCreate(ctx));
    }

    public void fireBeforeUpdate(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.beforeUpdate(ctx));
    }

    public void fireAfterUpdate(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.afterUpdate(ctx));
    }

    public void fireBeforeDelete(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.beforeDelete(ctx));
    }

    public void fireAfterDelete(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.afterDelete(ctx));
    }

    public List<String> fireValidate(String entityType, String typeCode, ExtensionContext ctx) {
        return getPlugins(entityType, typeCode).stream()
            .flatMap(p -> p.validate(ctx).stream())
            .collect(Collectors.toList());
    }

    /**
     * 获取某个 entityType+typeCode 的所有插件（精确匹配 + 通配符匹配）
     */
    private List<EntityTypePlugin> getPlugins(String entityType, String typeCode) {
        List<EntityTypePlugin> exact = registry.getOrDefault(entityType + "_" + typeCode, List.of());
        List<EntityTypePlugin> wildcard = registry.getOrDefault(entityType + "_*", List.of());
        if (wildcard.isEmpty()) return exact;
        List<EntityTypePlugin> merged = new ArrayList<>(exact);
        merged.addAll(wildcard);
        merged.sort(Comparator.comparingInt(EntityTypePlugin::getOrder));
        return merged;
    }
}
