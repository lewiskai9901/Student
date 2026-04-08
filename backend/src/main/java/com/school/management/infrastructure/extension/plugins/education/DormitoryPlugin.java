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
public class DormitoryPlugin implements EntityTypePlugin {

    private final JdbcTemplate jdbc;

    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "DORM_ROOM"; }
    public String getTypeName() { return "宿舍"; }
    public String getCategory() { return "ROOM"; }
    public String getParentTypeCode() { return "DORM_FLOOR"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("bedCount", "床位数", "number", "基本信息", true,
                Map.of("min", 1, "max", 12, "default", 6)),
            FieldDefinition.of("genderLimit", "性别限制", "select", "基本信息", false,
                Map.of("options", List.of(
                    Map.of("value", "MALE", "label", "男生"),
                    Map.of("value", "FEMALE", "label", "女生"),
                    Map.of("value", "NONE", "label", "不限")))),
            FieldDefinition.of("floorNumber", "楼层", "number", "位置信息", false, Map.of()),
            FieldDefinition.of("roomManager", "舍长", "user", "人员配置", false,
                Map.of("role", "STUDENT"))
        );
    }

    public Map<String, Boolean> getFeatures() {
        return Map.of("hasCapacity", true, "hasOccupancy", true, "hasGender", true);
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "bed-double", "color", "#0d9488");
    }

    @Override
    public void beforeDelete(ExtensionContext ctx) {
        try {
            Long occupants = jdbc.queryForObject(
                "SELECT COUNT(*) FROM place_occupant_records WHERE place_id = ? AND status = 'CHECKED_IN' AND deleted = 0",
                Long.class, ctx.getEntityId());
            if (occupants != null && occupants > 0) {
                throw new BusinessException("宿舍内有 " + occupants + " 名在住人员，无法删除");
            }
        } catch (BusinessException e) { throw e; }
        catch (Exception ignored) {}
    }

    @Override
    public List<String> validate(ExtensionContext ctx) {
        List<String> errors = new ArrayList<>();
        Object bedCount = ctx.get("bedCount");
        if (bedCount == null) errors.add("床位数不能为空");
        return errors;
    }
}
