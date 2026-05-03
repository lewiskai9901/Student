package com.school.management.infrastructure.persistence.inspection.execution;

import lombok.Data;

/**
 * 模板使用计数 (按 rootSectionId 聚合).
 * P1-160: 模板列表显示"被多少在用项目引用".
 */
@Data
public class RootSectionUsageRow {
    private Long rootSectionId;
    private Integer cnt;
}
