package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.ExtensionContext;
import com.school.management.infrastructure.extension.FieldDefinition;
import com.school.management.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SuperAdminPlugin implements EntityTypePlugin {

    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "SUPER_ADMIN"; }
    public String getTypeName() { return "超级管理员"; }
    public String getCategory() { return "ADMIN"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(); // 超管不需要额外字段，但需要生命周期保护
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "shield", "color", "#dc2626");
    }

    @Override
    public void beforeDelete(ExtensionContext ctx) {
        throw new BusinessException("超级管理员账号不允许删除");
    }
}
