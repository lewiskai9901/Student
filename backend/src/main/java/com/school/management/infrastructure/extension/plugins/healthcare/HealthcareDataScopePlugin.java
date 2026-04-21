package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.DataScopePlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 医疗行业数据维度插件.
 *
 * 贡献医疗特有的数据权限维度 (admin 在角色 → 数据权限配置中选用):
 *  - BY_WARD: 按病区隔离 (护士/医师仅看本病区病人)
 *  - BY_ATTENDING_DOCTOR: 按主治医师隔离 (医师仅看自己主治的病人)
 *
 * Resolver 实现类为占位路径 (Phase 2 MVP 只登记维度到 data_scope_dims 表,
 * 具体 resolve 逻辑留后续扩展).
 */
@Component
@SuppressWarnings("deprecation")
public class HealthcareDataScopePlugin implements DataScopePlugin {

    @Override
    public String getDomainCode() { return "healthcare"; }

    @Override
    public List<DimensionDef> getDimensions() {
        return List.of(
            new DimensionDef("BY_WARD", "按病区",
                "仅访问用户所在病区的所有病人/医护数据",
                "com.school.management.infrastructure.extension.plugins.healthcare.scope.WardDataScopeResolver"),

            new DimensionDef("BY_ATTENDING_DOCTOR", "按主治医师",
                "医师仅访问自己主治 (attending_of) 的病人数据",
                "com.school.management.infrastructure.extension.plugins.healthcare.scope.AttendingDoctorDataScopeResolver")
        );
    }
}
