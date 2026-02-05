package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.TemplateCategory;

import java.util.List;
import java.util.Optional;

/**
 * 模板类别仓储接口
 */
public interface TemplateCategoryRepository {

    /**
     * 根据模板ID获取所有类别（包含扣分项）
     */
    List<TemplateCategory> findByTemplateIdWithItems(Long templateId);

    /**
     * 根据模板ID获取所有类别
     */
    List<TemplateCategory> findByTemplateId(Long templateId);

    /**
     * 根据ID获取类别
     */
    Optional<TemplateCategory> findById(Long id);

    /**
     * 保存类别
     */
    TemplateCategory save(TemplateCategory category);

    /**
     * 更新类别
     */
    void update(TemplateCategory category);

    /**
     * 删除类别
     */
    void deleteById(Long id);

    /**
     * 检查编码是否存在
     */
    boolean existsByTemplateIdAndCode(Long templateId, String categoryCode);
}
