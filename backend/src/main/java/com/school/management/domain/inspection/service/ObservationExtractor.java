package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.v7.execution.ScoringObservation;
import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;

import java.util.List;

/**
 * 评分观察提取器 — Strategy 接口
 *
 * 从 SubmissionDetail 提取 0~N 个归一化的 ScoringObservation。
 * 每种需要特殊处理的检查项类型实现一个，其余走 DefaultObservationExtractor。
 *
 * 实现类通过 @Component + @Order 注册，优先级高的先匹配。
 */
public interface ObservationExtractor {

    /**
     * 是否支持该检查项类型
     * @return true 表示使用此 extractor（优先于 default）
     */
    boolean supports(String itemType);

    /**
     * 从一个 SubmissionDetail 提取评分观察列表
     * @param detail  检查项明细（含评分结果）
     * @param ctx     提取上下文（submission/project/target 信息）
     * @return 0~N 个观察，空列表表示该 detail 不产生观察
     */
    List<ScoringObservation> extract(SubmissionDetail detail, ObservationContext ctx);
}
