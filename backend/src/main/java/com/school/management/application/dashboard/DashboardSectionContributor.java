package com.school.management.application.dashboard;

import java.util.Map;

/**
 * 看板分区贡献点 SPI — 让行业插件向核心总览看板贡献自己的统计分区,
 * 核心 {@code DashboardOverviewQueryService} 不再硬编码行业统计 (如教务/班级)。
 *
 * <p>核心负责通用分区 (organization/inspection/system) 并解析数据范围 {@link DashboardScope};
 * 每个实现返回一个分区的统计 Map, 以 {@link #sectionKey()} 为键合并进总览结果。
 * 插件禁用时其 contributor bean 不存在 → 对应分区自动消失。
 */
public interface DashboardSectionContributor {

    /** 分区键, 如 "teaching"。合并进总览 result 的该键下。 */
    String sectionKey();

    /** 在给定数据范围内产出本分区统计。 */
    Map<String, Object> contribute(DashboardScope scope);
}
