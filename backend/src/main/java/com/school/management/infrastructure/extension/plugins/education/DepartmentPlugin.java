package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DepartmentPlugin implements EntityTypePlugin {
    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "DEPARTMENT"; }
    public String getTypeName() { return "系部"; }
    public String getCategory() { return "BRANCH"; }
    public String getParentTypeCode() { return "SCHOOL"; }
    public List<String> getAllowedChildTypeCodes() { return List.of("GRADE", "TEACHING_GROUP"); }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("deanName", "系主任", "user", "人员配置", false, Map.of()),
            FieldDefinition.of("deptPhone", "办公电话", "text", "联系方式", false, Map.of())
        );
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "building", "color", "#7c3aed");
    }
}
