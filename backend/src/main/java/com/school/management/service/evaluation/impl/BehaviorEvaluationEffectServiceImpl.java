package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.BehaviorEvaluationEffect;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.BehaviorEvaluationEffectMapper;
import com.school.management.service.evaluation.BehaviorEvaluationEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 行为-综测映射服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorEvaluationEffectServiceImpl extends ServiceImpl<BehaviorEvaluationEffectMapper, BehaviorEvaluationEffect>
        implements BehaviorEvaluationEffectService {

    private final BehaviorEvaluationEffectMapper effectMapper;

    @Override
    public List<BehaviorEvaluationEffect> getByBehaviorTypeId(Long behaviorTypeId) {
        return effectMapper.selectByBehaviorTypeId(behaviorTypeId);
    }

    @Override
    public List<BehaviorEvaluationEffect> getByBehaviorTypeCode(String behaviorTypeCode) {
        return effectMapper.selectByBehaviorTypeCode(behaviorTypeCode);
    }

    @Override
    public List<Map<String, Object>> getEffectWithBehaviorType(Long behaviorTypeId) {
        return effectMapper.selectEffectWithBehaviorType(behaviorTypeId);
    }

    @Override
    public List<BehaviorEvaluationEffect> getByDimension(String dimension) {
        return effectMapper.selectByDimension(dimension);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEffect(BehaviorEvaluationEffect effect) {
        // 检查是否已存在相同行为类型和维度的映射
        LambdaQueryWrapper<BehaviorEvaluationEffect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BehaviorEvaluationEffect::getBehaviorTypeId, effect.getBehaviorTypeId())
                .eq(BehaviorEvaluationEffect::getEvaluationDimension, effect.getEvaluationDimension());
        if (count(wrapper) > 0) {
            throw new BusinessException("该行为类型在此维度已存在映射规则");
        }

        // 设置默认值
        if (effect.getStatus() == null) {
            effect.setStatus(1);
        }
        if (effect.getPriority() == null) {
            effect.setPriority(0);
        }
        if (effect.getEffectiveSemesters() == null) {
            effect.setEffectiveSemesters(1);
        }

        effectMapper.insert(effect);
        log.info("创建行为-综测映射: id={}, behaviorTypeId={}, dimension={}",
                effect.getId(), effect.getBehaviorTypeId(), effect.getEvaluationDimension());
        return effect.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEffect(BehaviorEvaluationEffect effect) {
        BehaviorEvaluationEffect existing = effectMapper.selectById(effect.getId());
        if (existing == null) {
            throw new BusinessException("映射规则不存在");
        }

        // 如果修改了维度，检查是否冲突
        if (effect.getEvaluationDimension() != null &&
                !effect.getEvaluationDimension().equals(existing.getEvaluationDimension())) {
            LambdaQueryWrapper<BehaviorEvaluationEffect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BehaviorEvaluationEffect::getBehaviorTypeId, existing.getBehaviorTypeId())
                    .eq(BehaviorEvaluationEffect::getEvaluationDimension, effect.getEvaluationDimension())
                    .ne(BehaviorEvaluationEffect::getId, effect.getId());
            if (count(wrapper) > 0) {
                throw new BusinessException("该行为类型在此维度已存在映射规则");
            }
        }

        effectMapper.updateById(effect);
        log.info("更新行为-综测映射: id={}", effect.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEffect(Long id) {
        BehaviorEvaluationEffect existing = effectMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("映射规则不存在");
        }

        effectMapper.deleteById(id);
        log.info("删除行为-综测映射: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEffects(Long behaviorTypeId, List<BehaviorEvaluationEffect> effects) {
        // 删除原有规则
        LambdaQueryWrapper<BehaviorEvaluationEffect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BehaviorEvaluationEffect::getBehaviorTypeId, behaviorTypeId);
        effectMapper.delete(wrapper);

        // 保存新规则
        if (effects != null && !effects.isEmpty()) {
            for (BehaviorEvaluationEffect effect : effects) {
                effect.setId(null);
                effect.setBehaviorTypeId(behaviorTypeId);
                if (effect.getStatus() == null) {
                    effect.setStatus(1);
                }
                if (effect.getPriority() == null) {
                    effect.setPriority(0);
                }
                if (effect.getEffectiveSemesters() == null) {
                    effect.setEffectiveSemesters(1);
                }
                effectMapper.insert(effect);
            }
        }

        log.info("批量保存行为-综测映射: behaviorTypeId={}, count={}", behaviorTypeId,
                effects != null ? effects.size() : 0);
    }
}
