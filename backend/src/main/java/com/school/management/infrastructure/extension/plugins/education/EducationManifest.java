package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.PluginPackage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业插件包.
 *
 * 包含:
 *  - 用户类型: Student / Teacher / Parent / Counselor
 *  - 组织类型: School / Department / Grade / Class
 *  - 场所类型: Dormitory / Classroom
 *  - 关系: guardian_of / teaches / advisor_of / mentor_of (EducationRelationsPlugin)
 *  - 预置角色: CLASS_TEACHER / SUBJECT_TEACHER / GRADE_DIRECTOR 等
 *  - 业务消息: 入学 / 成绩 / 入住 / 考勤 (后续阶段加)
 *
 * Phase 2: 升级为 PluginPackage (继承自 PluginManifest, 默认 contribute()=空流).
 * 现有所有贡献仍通过旧 @Component SPI 路径扫描, 行为不变.
 */
@Component
public class EducationManifest implements PluginPackage {

    @Override
    public String getIndustryCode() { return "EDU"; }

    @Override
    public String getIndustryName() { return "教育行业"; }

    @Override
    public List<String> getDependsOn() { return List.of("CORE"); }

    /** EDU 要求 CORE >=1.0.0 <2.0.0 (兼容 1.x 核心, 2.0 需要 EDU 升级) */
    @Override
    public java.util.Map<String, String> getDependsOnWithVersion() {
        return java.util.Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    @Override
    public boolean owns(Class<?> pluginClass) {
        // 包路径约定: .plugins.education 或 .plugins.education.* → 属于 EDU
        String pkg = pluginClass.getPackageName();
        return pkg.contains(".plugins.education");
    }
}
