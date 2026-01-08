package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.BehaviorEvaluationEffect;

import java.util.List;
import java.util.Map;

/**
 * 行为-综测映射服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface BehaviorEvaluationEffectService extends IService<BehaviorEvaluationEffect> {

    /**
     * 根据行为类型ID查询映射规则
     */
    List<BehaviorEvaluationEffect> getByBehaviorTypeId(Long behaviorTypeId);

    /**
     * 根据行为类型编码查询映射规则
     */
    List<BehaviorEvaluationEffect> getByBehaviorTypeCode(String behaviorTypeCode);

    /**
     * 查询映射规则详情(含行为类型信息)
     */
    List<Map<String, Object>> getEffectWithBehaviorType(Long behaviorTypeId);

    /**
     * 根据维度查询所有映射规则
     */
    List<BehaviorEvaluationEffect> getByDimension(String dimension);

    /**
     * 创建映射规则
     */
    Long createEffect(BehaviorEvaluationEffect effect);

    /**
     * 更新映射规则
     */
    void updateEffect(BehaviorEvaluationEffect effect);

    /**
     * 删除映射规则
     */
    void deleteEffect(Long id);

    /**
     * 批量保存行为类型的映射规则
     */
    void saveEffects(Long behaviorTypeId, List<BehaviorEvaluationEffect> effects);
}
