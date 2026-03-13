package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.corrective.IssueCategory;
import com.school.management.domain.inspection.repository.v7.IssueCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IssueCategoryApplicationService {

    private final IssueCategoryRepository repository;

    public IssueCategoryApplicationService(IssueCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IssueCategory create(String categoryCode, String categoryName,
                                Long parentId, String description, String icon,
                                Integer sortOrder, Long createdBy) {
        IssueCategory category = IssueCategory.create(categoryCode, categoryName, createdBy);
        // 用 reconstruct 设置 parentId 等可选字段
        IssueCategory full = IssueCategory.reconstruct(IssueCategory.builder()
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .parentId(parentId)
                .description(description)
                .icon(icon)
                .sortOrder(sortOrder)
                .createdBy(createdBy));
        return repository.save(full);
    }

    @Transactional
    public IssueCategory update(Long id, String categoryName, String description,
                                String icon, Integer sortOrder, Boolean isEnabled,
                                Long updatedBy) {
        IssueCategory category = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("问题分类不存在: " + id));
        category.update(categoryName, description, icon, sortOrder, isEnabled, updatedBy);
        return repository.save(category);
    }

    public IssueCategory getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("问题分类不存在: " + id));
    }

    public List<IssueCategory> getAll() {
        return repository.findAll();
    }

    public List<IssueCategory> getRoots() {
        return repository.findRoots();
    }

    public List<IssueCategory> getChildren(Long parentId) {
        return repository.findByParentId(parentId);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
