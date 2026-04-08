package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CounselorPlugin implements EntityTypePlugin {

    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "COUNSELOR"; }
    public String getTypeName() { return "辅导员"; }
    public String getCategory() { return "STAFF"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("managedGrades", "管理年级", "tags", "职责范围", false,
                Map.of("options", List.of(
                    Map.of("value", "2023", "label", "2023级"),
                    Map.of("value", "2024", "label", "2024级"),
                    Map.of("value", "2025", "label", "2025级")))),
            FieldDefinition.of("officeLocation", "办公室", "text", "联系方式", false, Map.of()),
            FieldDefinition.of("officePhone", "办公电话", "text", "联系方式", false, Map.of()),
            FieldDefinition.of("maxStudents", "管理学生上限", "number", "职责范围", false,
                Map.of("min", 0, "max", 500, "default", 200))
        );
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "user-check", "color", "#7c3aed");
    }
}
