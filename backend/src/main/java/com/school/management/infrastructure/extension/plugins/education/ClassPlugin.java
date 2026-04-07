package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.ExtensionContext;
import com.school.management.infrastructure.extension.FieldDefinition;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClassPlugin implements EntityTypePlugin {

    private final JdbcTemplate jdbc;

    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "CLASS"; }
    public String getTypeName() { return "班级"; }
    public String getCategory() { return "GROUP"; }
    public String getParentTypeCode() { return "GRADE"; }
    public List<String> getAllowedChildTypeCodes() { return List.of(); }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("enrollmentYear", "入学年份", "number", "基本信息", true,
                Map.of("min", 2020, "max", 2035)),
            FieldDefinition.of("majorId", "专业", "relation", "基本信息", false,
                Map.of("target", "majors", "labelField", "majorName")),
            FieldDefinition.of("headTeacher", "班主任", "user", "人员配置", false,
                Map.of("role", "TEACHER")),
            FieldDefinition.of("assistantTeacher", "副班主任", "user", "人员配置", false,
                Map.of("role", "TEACHER")),
            FieldDefinition.of("duration", "学制(年)", "number", "基本信息", false,
                Map.of("min", 1, "max", 6, "default", 3)),
            FieldDefinition.of("classType", "班级类型", "select", "基本信息", false,
                Map.of("options", List.of(
                    Map.of("value", 1, "label", "普通班"),
                    Map.of("value", 2, "label", "重点班"),
                    Map.of("value", 3, "label", "实验班"))))
        );
    }

    public Map<String, Boolean> getFeatures() {
        return Map.of("hasStudents", true, "hasTimetable", true, "hasAttendance", true);
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "users", "color", "#2563eb");
    }

    @Override
    public void beforeDelete(ExtensionContext ctx) {
        try {
            Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM students WHERE class_id = ? AND status = 1 AND deleted = 0",
                Long.class, ctx.getEntityId());
            if (count != null && count > 0) {
                throw new BusinessException("班级下有 " + count + " 名在读学生，无法删除");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
            // students 表可能不存在，忽略
        }
    }

    @Override
    public List<String> validate(ExtensionContext ctx) {
        List<String> errors = new ArrayList<>();
        if (ctx.get("enrollmentYear") == null) {
            errors.add("入学年份不能为空");
        }
        return errors;
    }
}
