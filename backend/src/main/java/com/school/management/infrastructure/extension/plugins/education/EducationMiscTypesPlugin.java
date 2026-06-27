package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 教育行业其他类型 — 家长、行政部门、教研组、教研室.
 */
public class EducationMiscTypesPlugin {
    // marker
}

@Component
class ParentPlugin implements EntityTypePlugin {
    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "PARENT"; }
    public String getTypeName() { return "家长"; }
    public String getCategory() { return "EXTERNAL"; }   // 家长=外部人员 (与其 isExternal 特性一致)
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Boolean> getFeatures() {
        return Map.of("canLogin", true, "isExternal", true, "hasGuardian", false);
    }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "users", "color", "#ea580c"); }
}

@Component
class AdminOfficePlugin implements EntityTypePlugin {
    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "ADMIN_OFFICE"; }
    public String getTypeName() { return "行政部门"; }
    public String getCategory() { return "FUNCTIONAL"; }   // 职能部门
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "briefcase", "color", "#475569"); }
}

@Component
class TeachingGroupPlugin implements EntityTypePlugin {
    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "TEACHING_GROUP"; }
    public String getTypeName() { return "教研组"; }
    public String getCategory() { return "FUNCTIONAL"; }   // 教研组=职能性单位
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "users-round", "color", "#16a34a"); }
}

@Component
class SectionPlugin implements EntityTypePlugin {
    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "SECTION"; }
    public String getTypeName() { return "教研室"; }
    public String getCategory() { return "FUNCTIONAL"; }   // 教研室=职能性单位
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "library", "color", "#16a34a"); }
}
