package com.school.management.domain.inspection.model.scoring;

/**
 * Indicator 排名方向. NULL=不排名(值映射等级).
 */
public enum RankDirection {
    /** 升序: 越小越好 (扣分制) */
    ASC,
    /** 降序: 越大越好 (得分制) */
    DESC
}
