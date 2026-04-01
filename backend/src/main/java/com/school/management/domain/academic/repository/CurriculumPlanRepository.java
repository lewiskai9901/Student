package com.school.management.domain.academic.repository;

import com.school.management.domain.academic.model.CurriculumPlan;
import com.school.management.domain.shared.Repository;

/**
 * 培养方案仓储接口
 */
public interface CurriculumPlanRepository extends Repository<CurriculumPlan, Long> {

    /**
     * 查找同编码下最大版本号
     */
    Integer findMaxVersionByPlanCode(String planCode);
}
