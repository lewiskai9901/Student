package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 通用用户类型 — 任何行业部署都需要的基础用户身份.
 */
public class CoreUserTypesPlugin {
    // marker
}

@Component
class CoreAdminPlugin implements EntityTypePlugin {
    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "ADMIN"; }
    public String getTypeName() { return "管理员"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Boolean> getFeatures() {
        return Map.of("canLogin", true, "isStaff", true);
    }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "shield-check", "color", "#4338ca"); }
}

@Component
class CoreStaffPlugin implements EntityTypePlugin {
    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "STAFF"; }
    public String getTypeName() { return "职工"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Boolean> getFeatures() {
        return Map.of("canLogin", true, "isStaff", true);
    }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "user", "color", "#475569"); }
}

@Component
class CoreGuestPlugin implements EntityTypePlugin {
    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "GUEST"; }
    public String getTypeName() { return "访客"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Boolean> getFeatures() {
        return Map.of("canLogin", false, "isExternal", true);
    }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "user-circle", "color", "#94a3b8"); }
}
