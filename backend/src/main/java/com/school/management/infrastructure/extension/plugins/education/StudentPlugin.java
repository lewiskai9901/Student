package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentPlugin implements EntityTypePlugin {

    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "STUDENT"; }
    public String getTypeName() { return "学生"; }
    public String getCategory() { return "LEARNER"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("studentNo", "学号", "text", "基本信息", true, Map.of()),
            FieldDefinition.of("classId", "班级", "relation", "学籍信息", true,
                Map.of("target", "org_units", "targetTypeCode", "CLASS", "labelField", "unitName")),
            FieldDefinition.of("enrollmentDate", "入学日期", "date", "学籍信息", false, Map.of()),
            FieldDefinition.of("guardianName", "监护人姓名", "text", "家庭信息", false, Map.of()),
            FieldDefinition.of("guardianPhone", "监护人电话", "text", "家庭信息", false, Map.of())
        );
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "graduation-cap", "color", "#2563eb");
    }
}
