package com.school.management.application.organization;

import java.util.Map;

/**
 * 组织影响分析贡献点 SPI — 让行业插件向 org-impact (删除/合并组织前的影响评估)
 * 贡献自己的受影响计数, 核心 {@code OrgUnitJdbcApplicationService} 不再硬编码行业概念。
 *
 * <p>核心负责通用项 (后代组织数 / 成员(学生·教师 feature) / 场所 / 关系数);
 * 行业项 (如教育的班级数 classCount) 由插件贡献, 合并进 impact map。
 * 插件禁用时其 contributor bean 不存在 → 对应项自动消失。
 *
 * <p>注: 班级本身是 org_unit, 已计入核心 descendantOrgCount; classCount 仅作展示项,
 * 不再重复计入 totalAffected/severity。
 */
public interface OrgImpactContributor {

    /** 返回要合并进 impact 的额外项 (如 {"classCount": N})。treePath 为目标子树 tree_path 前缀。 */
    Map<String, Object> contribute(String treePath);
}
