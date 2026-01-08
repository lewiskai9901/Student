package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.BehaviorType;

import java.util.List;
import java.util.Map;

/**
 * 行为类型服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface BehaviorTypeService extends IService<BehaviorType> {

    /**
     * 分页查询行为类型
     */
    Page<Map<String, Object>> pageBehaviorTypes(Page<?> page, Map<String, Object> query);

    /**
     * 创建行为类型
     */
    Long createBehaviorType(BehaviorType behaviorType);

    /**
     * 更新行为类型
     */
    void updateBehaviorType(BehaviorType behaviorType);

    /**
     * 删除行为类型
     */
    void deleteBehaviorType(Long id);

    /**
     * 获取行为类型详情(含映射规则)
     */
    Map<String, Object> getBehaviorTypeDetail(Long id);

    /**
     * 根据类别获取行为类型列表
     */
    List<BehaviorType> getByCategory(String category);

    /**
     * 根据行为性质获取行为类型列表
     */
    List<BehaviorType> getByNature(Integer nature);

    /**
     * 根据编码获取行为类型
     */
    BehaviorType getByCode(String code);

    /**
     * 获取所有行为类别
     */
    List<Map<String, Object>> getAllCategories();

    /**
     * 检查编码是否存在
     */
    boolean isCodeExists(String code, Long excludeId);
}
