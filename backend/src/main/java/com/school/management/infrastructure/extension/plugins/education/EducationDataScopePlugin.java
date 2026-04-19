package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.DataScopePlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业数据维度插件.
 *
 * 贡献教育特有的权限维度(由 admin 在角色 → 数据权限配置中选用):
 *  - BY_MAJOR: 按专业隔离
 *  - BY_GRADE: 按年级隔离
 *  - BY_CLASS: 按班级隔离(当前 ORG 维度已能做,保留为语义清晰化)
 *
 * Resolver 类(后续实现):
 *  - MajorDataScopeResolver
 *  - GradeDataScopeResolver
 *  - ClassDataScopeResolver
 *
 * 本插件声明后,admin 在 UI 里可选"按专业" 作为角色数据范围.
 */
@Component
public class EducationDataScopePlugin implements DataScopePlugin {

    @Override
    public String getDomainCode() { return "education"; }

    @Override
    public List<DimensionDef> getDimensions() {
        return List.of(
            new DimensionDef("BY_MAJOR", "按专业",
                "按照用户所属专业的所有学生/课程数据",
                "com.school.management.infrastructure.extension.plugins.education.scope.MajorDataScopeResolver"),

            new DimensionDef("BY_GRADE", "按年级",
                "按照用户所属年级的所有学生数据",
                "com.school.management.infrastructure.extension.plugins.education.scope.GradeDataScopeResolver"),

            new DimensionDef("BY_CLASS", "按班级",
                "仅访问用户所在班级的学生/成绩数据",
                "com.school.management.infrastructure.extension.plugins.education.scope.ClassDataScopeResolver")
        );
    }
}
