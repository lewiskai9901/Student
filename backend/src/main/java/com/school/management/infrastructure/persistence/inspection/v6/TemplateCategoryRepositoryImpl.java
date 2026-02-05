package com.school.management.infrastructure.persistence.inspection.v6;

import com.school.management.domain.inspection.model.v6.TemplateCategory;
import com.school.management.domain.inspection.model.v6.TemplateScoreItem;
import com.school.management.domain.inspection.repository.v6.TemplateCategoryRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模板类别仓储实现
 */
@Repository
public class TemplateCategoryRepositoryImpl implements TemplateCategoryRepository {

    private final TemplateCategoryMapper categoryMapper;
    private final TemplateScoreItemMapper itemMapper;

    public TemplateCategoryRepositoryImpl(TemplateCategoryMapper categoryMapper,
                                          TemplateScoreItemMapper itemMapper) {
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public List<TemplateCategory> findByTemplateIdWithItems(Long templateId) {
        List<TemplateCategoryPO> categoryPOs = categoryMapper.selectByTemplateId(templateId);
        return categoryPOs.stream().map(po -> {
            TemplateCategory category = toDomain(po);
            List<TemplateScoreItemPO> itemPOs = itemMapper.selectByCategoryId(po.getId());
            category.setItems(itemPOs.stream().map(this::toItemDomain).collect(Collectors.toList()));
            return category;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TemplateCategory> findByTemplateId(Long templateId) {
        List<TemplateCategoryPO> pos = categoryMapper.selectByTemplateId(templateId);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<TemplateCategory> findById(Long id) {
        TemplateCategoryPO po = categoryMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public TemplateCategory save(TemplateCategory category) {
        TemplateCategoryPO po = toPO(category);
        categoryMapper.insert(po);
        category.setId(po.getId());
        return category;
    }

    @Override
    public void update(TemplateCategory category) {
        TemplateCategoryPO po = toPO(category);
        categoryMapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        categoryMapper.deleteById(id);
    }

    @Override
    public boolean existsByTemplateIdAndCode(Long templateId, String categoryCode) {
        return categoryMapper.countByTemplateIdAndCode(templateId, categoryCode) > 0;
    }

    private TemplateCategory toDomain(TemplateCategoryPO po) {
        TemplateCategory category = new TemplateCategory();
        BeanUtils.copyProperties(po, category);
        return category;
    }

    private TemplateCategoryPO toPO(TemplateCategory category) {
        TemplateCategoryPO po = new TemplateCategoryPO();
        BeanUtils.copyProperties(category, po);
        return po;
    }

    private TemplateScoreItem toItemDomain(TemplateScoreItemPO po) {
        TemplateScoreItem item = new TemplateScoreItem();
        BeanUtils.copyProperties(po, item);
        if (po.getScoringMode() != null) {
            item.setScoringMode(TemplateScoreItem.ScoringMode.valueOf(po.getScoringMode()));
        }
        return item;
    }
}
