package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.school.management.domain.inspection.model.v7.corrective.IssueCategory;
import com.school.management.domain.inspection.repository.v7.IssueCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class IssueCategoryRepositoryImpl implements IssueCategoryRepository {

    private final IssueCategoryMapper mapper;

    public IssueCategoryRepositoryImpl(IssueCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public IssueCategory save(IssueCategory category) {
        IssueCategoryPO po = toPO(category);
        if (category.getId() == null) {
            mapper.insert(po);
            category.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return category;
    }

    @Override
    public Optional<IssueCategory> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<IssueCategory> findAll() {
        return mapper.findAllActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<IssueCategory> findByParentId(Long parentId) {
        return mapper.findByParentId(parentId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<IssueCategory> findRoots() {
        return mapper.findRoots().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private IssueCategoryPO toPO(IssueCategory d) {
        IssueCategoryPO po = new IssueCategoryPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setParentId(d.getParentId());
        po.setCategoryCode(d.getCategoryCode());
        po.setCategoryName(d.getCategoryName());
        po.setDescription(d.getDescription());
        po.setIcon(d.getIcon());
        po.setSortOrder(d.getSortOrder());
        po.setIsEnabled(d.getIsEnabled());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private IssueCategory toDomain(IssueCategoryPO po) {
        return IssueCategory.reconstruct(IssueCategory.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .parentId(po.getParentId())
                .categoryCode(po.getCategoryCode())
                .categoryName(po.getCategoryName())
                .description(po.getDescription())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder())
                .isEnabled(po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
