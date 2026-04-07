package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TeacherPlugin implements EntityTypePlugin {

    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "TEACHER"; }
    public String getTypeName() { return "教师"; }
    public String getCategory() { return "STAFF"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("subject", "学科", "select", "教学信息", false,
                Map.of("options", List.of(
                    Map.of("value", "MATH", "label", "数学"),
                    Map.of("value", "CHINESE", "label", "语文"),
                    Map.of("value", "ENGLISH", "label", "英语"),
                    Map.of("value", "PHYSICS", "label", "物理"),
                    Map.of("value", "CHEMISTRY", "label", "化学"),
                    Map.of("value", "BIOLOGY", "label", "生物"),
                    Map.of("value", "HISTORY", "label", "历史"),
                    Map.of("value", "GEOGRAPHY", "label", "地理"),
                    Map.of("value", "PE", "label", "体育"),
                    Map.of("value", "CS", "label", "计算机"),
                    Map.of("value", "OTHER", "label", "其他")))),
            FieldDefinition.of("title", "职称", "select", "教学信息", false,
                Map.of("options", List.of(
                    Map.of("value", "PRIMARY", "label", "初级"),
                    Map.of("value", "INTERMEDIATE", "label", "中级"),
                    Map.of("value", "SENIOR", "label", "高级"),
                    Map.of("value", "PROFESSOR", "label", "正高级")))),
            FieldDefinition.of("maxWeeklyHours", "周最大课时", "number", "排课配置", false,
                Map.of("min", 0, "max", 30, "default", 16))
        );
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "user-check", "color", "#16a34a");
    }
}
