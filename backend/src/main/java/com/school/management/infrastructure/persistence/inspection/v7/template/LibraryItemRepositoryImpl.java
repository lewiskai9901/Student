package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.template.ItemType;
import com.school.management.domain.inspection.model.v7.template.LibraryItem;
import com.school.management.domain.inspection.repository.v7.LibraryItemRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class LibraryItemRepositoryImpl implements LibraryItemRepository {

    private final LibraryItemMapper mapper;

    public LibraryItemRepositoryImpl(LibraryItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LibraryItem save(LibraryItem item) {
        LibraryItemPO po = toPO(item);
        if (item.getId() == null) {
            mapper.insert(po);
            item.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return item;
    }

    @Override
    public Optional<LibraryItem> findById(Long id) {
        LibraryItemPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<LibraryItem> findByItemCode(String itemCode) {
        LibraryItemPO po = mapper.findByItemCode(itemCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<LibraryItem> findAll() {
        LambdaQueryWrapper<LibraryItemPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(LibraryItemPO::getItemCode);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LibraryItem> search(String keyword, String category) {
        LambdaQueryWrapper<LibraryItemPO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(LibraryItemPO::getItemCode, keyword)
                    .or().like(LibraryItemPO::getItemName, keyword)
                    .or().like(LibraryItemPO::getTags, keyword)
                    .or().like(LibraryItemPO::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(LibraryItemPO::getCategory, category);
        }
        wrapper.orderByAsc(LibraryItemPO::getCategory, LibraryItemPO::getItemCode);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDistinctCategories() {
        return mapper.findDistinctCategories();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private LibraryItemPO toPO(LibraryItem domain) {
        LibraryItemPO po = new LibraryItemPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setItemCode(domain.getItemCode());
        po.setItemName(domain.getItemName());
        po.setDescription(domain.getDescription());
        po.setItemType(domain.getItemType() != null ? domain.getItemType().name() : null);
        po.setCategory(domain.getCategory());
        po.setTags(domain.getTags());
        po.setDefaultConfig(domain.getDefaultConfig());
        po.setDefaultValidationRules(domain.getDefaultValidationRules());
        po.setDefaultScoringConfig(domain.getDefaultScoringConfig());
        po.setDefaultHelpContent(domain.getDefaultHelpContent());
        po.setUsageCount(domain.getUsageCount());
        po.setIsStandard(domain.getIsStandard());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private LibraryItem toDomain(LibraryItemPO po) {
        return LibraryItem.reconstruct(LibraryItem.builder()
                .id(po.getId())
                .itemCode(po.getItemCode())
                .itemName(po.getItemName())
                .description(po.getDescription())
                .itemType(po.getItemType() != null ? ItemType.valueOf(po.getItemType()) : null)
                .category(po.getCategory())
                .tags(po.getTags())
                .defaultConfig(po.getDefaultConfig())
                .defaultValidationRules(po.getDefaultValidationRules())
                .defaultScoringConfig(po.getDefaultScoringConfig())
                .defaultHelpContent(po.getDefaultHelpContent())
                .usageCount(po.getUsageCount())
                .isStandard(po.getIsStandard())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
