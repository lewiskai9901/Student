package com.school.management.application.inspection.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 组织树得分视图 — roll-up 结果对前端的投影 (Phase 3.5a).
 *
 * <p>对应 {@code org_unit_scores} 表一行, 额外补上 {@code orgUnitName} 供排名直接展示组织名.
 * orgUnitId 用 Long, 全局 Jackson 配置序列化为 string (防 JS 53-bit 精度丢失).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgScoreView {

    private Long orgUnitId;
    private String orgUnitName;
    private BigDecimal score;
    private String grade;
    private Integer childCount;
    private Integer sourceCount;
}
