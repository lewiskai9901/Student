package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GradePlugin implements EntityTypePlugin {

    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "GRADE"; }
    public String getTypeName() { return "年级"; }
    public String getCategory() { return "ACADEMIC"; }
    public String getParentTypeCode() { return "DEPARTMENT"; }
    public List<String> getAllowedChildTypeCodes() { return List.of("CLASS"); }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("enrollmentYear", "入学年份", "number", "基本信息", true,
                Map.of("min", 2020, "max", 2035)),
            FieldDefinition.of("schoolingYears", "学制(年)", "number", "基本信息", false,
                Map.of("min", 1, "max", 6, "default", 3)),
            FieldDefinition.of("gradeDirector", "年级主任", "user", "人员配置", false,
                Map.of("role", "TEACHER"))
        );
    }

    public Map<String, Boolean> getFeatures() {
        return Map.of("hasClasses", true, "hasExams", true);
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "layers", "color", "#7c3aed");
    }
}
