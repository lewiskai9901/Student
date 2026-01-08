package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 检查覆盖率VO
 * 用于展示检查计划的班级覆盖情况
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckCoverageVO {

    /**
     * 计划目标班级数（根据targetScopeConfig计算）
     */
    private Integer planTargetClasses;

    /**
     * 实际检查到的班级数
     */
    private Integer actualCheckedClasses;

    /**
     * 覆盖率 = actualCheckedClasses / planTargetClasses
     */
    private BigDecimal coverageRate;

    /**
     * 全覆盖的检查记录数（一次检查覆盖所有目标班级）
     */
    private Integer fullCoverageRecords;

    /**
     * 部分覆盖的检查记录数
     */
    private Integer partialCoverageRecords;

    /**
     * 缺检班级数（计划目标但从未被检查）
     */
    private Integer missedClasses;

    /**
     * 缺检班级列表（班级ID -> 班级名称）
     */
    private List<MissedClassVO> missedClassList;

    /**
     * 各班级被检查次数分布
     */
    private List<ClassCheckCountVO> classCheckCounts;

    /**
     * 缺检班级信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissedClassVO {
        private Long classId;
        private String className;
        private String gradeName;
        private String reason; // 缺检原因说明
    }

    /**
     * 班级检查次数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassCheckCountVO {
        private Long classId;
        private String className;
        private Integer checkCount;
        private Integer totalRounds; // 总检查轮次
    }
}
