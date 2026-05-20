package com.school.management.interfaces.rest.extension;

import com.school.management.application.extension.EntityTypeConfigApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.PluginEnabledGuard;
import com.school.management.domain.organization.model.entity.OrgCategory;
import com.school.management.domain.place.model.valueobject.BaseCategory;
import com.school.management.domain.user.model.entity.UserCategory;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * 统一实体类型配置 API
 */
@Slf4j
@RestController
@RequestMapping("/entity-type-configs")
@RequiredArgsConstructor
public class EntityTypeConfigController {

    private final EntityTypeConfigApplicationService service;
    private final PluginEnabledGuard pluginEnabledGuard;

    @GetMapping
    @CasbinAccess(resource = "entity-type-config", action = "view")
    public Result<List<Map<String, Object>>> list(
            @RequestParam String entityType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        // includeDisabled=true: 管理员视角, 返回所属插件被禁的类型 (灰显)
        boolean admin = Boolean.TRUE.equals(includeDisabled);
        return Result.success(service.list(entityType, category, admin));
    }

    @GetMapping("/detail")
    @CasbinAccess(resource = "entity-type-config", action = "view")
    public Result<Map<String, Object>> detail(
            @RequestParam String entityType,
            @RequestParam String typeCode) {
        return Result.success(service.detail(entityType, typeCode));
    }

    @GetMapping("/categories")
    @CasbinAccess(resource = "entity-type-config", action = "view")
    public Result<List<Map<String, Object>>> categories(@RequestParam String entityType) {
        List<Map<String, Object>> list;
        switch (entityType) {
            case "ORG_UNIT":
                list = Stream.of(OrgCategory.values())
                        .map(c -> Map.<String, Object>of(
                                "code", c.name(),
                                "label", c.getLabel(),
                                "defaultFeatures", c.getDefaultFeatures()))
                        .toList();
                break;
            case "USER":
                list = Stream.of(UserCategory.values())
                        .map(c -> Map.<String, Object>of(
                                "code", c.name(),
                                "label", c.getLabel(),
                                "defaultFeatures", c.getDefaultFeatures()))
                        .toList();
                break;
            case "PLACE":
                list = Stream.of(BaseCategory.values())
                        .map(c -> Map.<String, Object>of(
                                "code", c.name(),
                                "label", c.getLabel(),
                                "allowedChildCodes", c.getAllowedChildCategories(),
                                "defaultFeatures", c.getDefaultFeatures()))
                        .toList();
                break;
            default:
                return Result.error("未知的 entityType: " + entityType);
        }
        return Result.success(list);
    }

    @GetMapping("/allowed-children")
    @CasbinAccess(resource = "entity-type-config", action = "view")
    public Result<List<Map<String, Object>>> allowedChildren(
            @RequestParam String entityType,
            @RequestParam String parentTypeCode) {
        return Result.success(service.allowedChildren(entityType, parentTypeCode));
    }

    @PostMapping
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        try {
            service.create(data);
            return Result.success(Map.of("message", "created"));
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        pluginEnabledGuard.check("entity_type_configs", id);
        try {
            service.update(id, data);
            return Result.success();
        } catch (IllegalArgumentException iae) {
            return Result.error(iae.getMessage());
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 恢复某个字段为插件默认值 — 从 overridden_fields 移除该字段并回填插件声明值.
     * PluginRegistrar 下次启动会重新按插件声明写, 这里立即生效而不用等重启.
     */
    @PostMapping("/{id}/reset-field")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> resetField(@PathVariable Long id, @RequestParam String field) {
        pluginEnabledGuard.check("entity_type_configs", id);
        if (!EntityTypeConfigApplicationService.OVERRIDABLE_FIELDS.contains(field)) {
            return Result.error("字段 [" + field + "] 不在可恢复白名单内: "
                    + EntityTypeConfigApplicationService.OVERRIDABLE_FIELDS);
        }
        try {
            String error = service.resetField(id, field);
            if (error != null) {
                return Result.error(error);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        pluginEnabledGuard.check("entity_type_configs", id);
        // 不允许删除插件注册的类型
        try {
            String error = service.delete(id);
            if (error != null) {
                return Result.error(error);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    private static final Set<String> ALLOWED_CUSTOM_FIELD_TYPES = Set.of(
        "text", "number", "date", "datetime", "boolean", "select", "multiselect", "textarea", "radio");

    @PostMapping("/{id}/custom-fields")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> addCustomField(@PathVariable Long id, @RequestBody Map<String, Object> field) {
        pluginEnabledGuard.check("entity_type_configs", id);
        // 1. 验证必填字段
        Object keyObj = field.get("key");
        Object labelObj = field.get("label");
        Object typeObj = field.get("type");

        String key = keyObj instanceof String ? (String) keyObj : null;
        String label = labelObj instanceof String ? (String) labelObj : null;
        String type = typeObj instanceof String ? (String) typeObj : null;

        if (key == null || !key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return Result.error("字段 key 只能包含字母、数字、下划线，且不能以数字开头");
        }
        if (label == null || label.isEmpty() || label.length() > 100) {
            return Result.error("字段名称长度须在 1-100 之间");
        }
        if (type == null || !ALLOWED_CUSTOM_FIELD_TYPES.contains(type)) {
            return Result.error("不支持的字段类型: " + type);
        }

        try {
            String error = service.addCustomField(id, field, key);
            if (error != null) {
                return Result.error(error);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("添加自定义字段失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/custom-fields/{fieldKey}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> removeCustomField(@PathVariable Long id, @PathVariable String fieldKey) {
        pluginEnabledGuard.check("entity_type_configs", id);
        try {
            service.removeCustomField(id, fieldKey);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除自定义字段失败: " + e.getMessage());
        }
    }
}
