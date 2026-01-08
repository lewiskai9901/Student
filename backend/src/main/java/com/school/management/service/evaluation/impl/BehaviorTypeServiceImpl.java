package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.BehaviorEvaluationEffect;
import com.school.management.entity.evaluation.BehaviorType;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.BehaviorEvaluationEffectMapper;
import com.school.management.mapper.evaluation.BehaviorTypeMapper;
import com.school.management.service.evaluation.BehaviorTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 行为类型服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorTypeServiceImpl extends ServiceImpl<BehaviorTypeMapper, BehaviorType> implements BehaviorTypeService {

    private final BehaviorTypeMapper behaviorTypeMapper;
    private final BehaviorEvaluationEffectMapper behaviorEvaluationEffectMapper;

    @Override
    public Page<Map<String, Object>> pageBehaviorTypes(Page<?> page, Map<String, Object> query) {
        return behaviorTypeMapper.selectBehaviorTypePage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBehaviorType(BehaviorType behaviorType) {
        // 检查编码是否存在
        if (isCodeExists(behaviorType.getBehaviorCode(), null)) {
            throw new BusinessException("行为编码已存在");
        }

        // 设置默认值
        if (behaviorType.getSortOrder() == null) {
            behaviorType.setSortOrder(0);
        }
        if (behaviorType.getStatus() == null) {
            behaviorType.setStatus(1);
        }
        if (behaviorType.getBehaviorNature() == null) {
            behaviorType.setBehaviorNature(BehaviorType.NATURE_NEGATIVE);
        }
        if (behaviorType.getDefaultAffectScope() == null) {
            behaviorType.setDefaultAffectScope(BehaviorType.SCOPE_INDIVIDUAL);
        }

        behaviorTypeMapper.insert(behaviorType);
        log.info("创建行为类型: id={}, code={}", behaviorType.getId(), behaviorType.getBehaviorCode());
        return behaviorType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBehaviorType(BehaviorType behaviorType) {
        BehaviorType existing = behaviorTypeMapper.selectById(behaviorType.getId());
        if (existing == null) {
            throw new BusinessException("行为类型不存在");
        }

        // 检查编码是否存在(排除自己)
        if (behaviorType.getBehaviorCode() != null &&
            isCodeExists(behaviorType.getBehaviorCode(), behaviorType.getId())) {
            throw new BusinessException("行为编码已存在");
        }

        behaviorTypeMapper.updateById(behaviorType);
        log.info("更新行为类型: id={}", behaviorType.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBehaviorType(Long id) {
        BehaviorType existing = behaviorTypeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("行为类型不存在");
        }

        // 检查是否有关联的映射规则
        LambdaQueryWrapper<BehaviorEvaluationEffect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BehaviorEvaluationEffect::getBehaviorTypeId, id);
        Long effectCount = behaviorEvaluationEffectMapper.selectCount(wrapper);
        if (effectCount > 0) {
            throw new BusinessException("该行为类型已配置综测映射规则，无法删除");
        }

        behaviorTypeMapper.deleteById(id);
        log.info("删除行为类型: id={}", id);
    }

    @Override
    public Map<String, Object> getBehaviorTypeDetail(Long id) {
        Map<String, Object> detail = behaviorTypeMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("行为类型不存在");
        }

        // 获取映射规则
        List<BehaviorEvaluationEffect> effects = behaviorEvaluationEffectMapper.selectByBehaviorTypeId(id);
        detail.put("effects", effects);

        return detail;
    }

    @Override
    public List<BehaviorType> getByCategory(String category) {
        return behaviorTypeMapper.selectByCategory(category);
    }

    @Override
    public List<BehaviorType> getByNature(Integer nature) {
        return behaviorTypeMapper.selectByNature(nature);
    }

    @Override
    public BehaviorType getByCode(String code) {
        return behaviorTypeMapper.selectByCode(code);
    }

    @Override
    public List<Map<String, Object>> getAllCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();

        categories.add(createCategoryMap(BehaviorType.CATEGORY_ATTENDANCE, "考勤", "考勤相关行为"));
        categories.add(createCategoryMap(BehaviorType.CATEGORY_DISCIPLINE, "纪律", "纪律相关行为"));
        categories.add(createCategoryMap(BehaviorType.CATEGORY_HYGIENE, "卫生", "卫生相关行为"));
        categories.add(createCategoryMap(BehaviorType.CATEGORY_STUDY, "学习", "学习相关行为"));
        categories.add(createCategoryMap(BehaviorType.CATEGORY_ACTIVITY, "活动", "活动相关行为"));
        categories.add(createCategoryMap(BehaviorType.CATEGORY_HONOR, "荣誉", "荣誉相关行为"));

        return categories;
    }

    @Override
    public boolean isCodeExists(String code, Long excludeId) {
        LambdaQueryWrapper<BehaviorType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BehaviorType::getBehaviorCode, code);
        if (excludeId != null) {
            wrapper.ne(BehaviorType::getId, excludeId);
        }
        return behaviorTypeMapper.selectCount(wrapper) > 0;
    }

    private Map<String, Object> createCategoryMap(String code, String name, String description) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", name);
        map.put("description", description);
        return map;
    }
}
