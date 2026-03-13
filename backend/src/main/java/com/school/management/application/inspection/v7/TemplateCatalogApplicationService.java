package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateCatalog;
import com.school.management.domain.inspection.repository.v7.TemplateCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class TemplateCatalogApplicationService {

    private final TemplateCatalogRepository catalogRepository;

    @Transactional
    public TemplateCatalog createCatalog(String catalogCode, String catalogName,
                                          Long parentId, String description, String icon,
                                          Integer sortOrder, Long createdBy) {
        TemplateCatalog catalog = TemplateCatalog.create(catalogCode, catalogName, createdBy);
        catalog.update(catalogName, description, parentId, icon, sortOrder, true, createdBy);
        return catalogRepository.save(catalog);
    }

    @Transactional(readOnly = true)
    public List<TemplateCatalog> listAllCatalogs() {
        return catalogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCatalogTree() {
        List<TemplateCatalog> all = catalogRepository.findAll();
        return buildTree(all, null);
    }

    @Transactional
    public TemplateCatalog updateCatalog(Long id, String catalogName, String description,
                                          Long parentId, String icon, Integer sortOrder,
                                          Boolean isEnabled, Long updatedBy) {
        TemplateCatalog catalog = catalogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + id));
        catalog.update(catalogName, description, parentId, icon, sortOrder, isEnabled, updatedBy);
        return catalogRepository.save(catalog);
    }

    @Transactional
    public void deleteCatalog(Long id) {
        // Check for children
        List<TemplateCatalog> children = catalogRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("请先删除子分类");
        }
        catalogRepository.deleteById(id);
    }

    private List<Map<String, Object>> buildTree(List<TemplateCatalog> all, Long parentId) {
        return all.stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                .map(c -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", c.getId());
                    node.put("catalogCode", c.getCatalogCode());
                    node.put("catalogName", c.getCatalogName());
                    node.put("description", c.getDescription());
                    node.put("icon", c.getIcon());
                    node.put("sortOrder", c.getSortOrder());
                    node.put("isEnabled", c.getIsEnabled());
                    node.put("children", buildTree(all, c.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
