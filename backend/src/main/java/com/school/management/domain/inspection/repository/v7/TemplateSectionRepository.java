package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateSection;

import java.util.List;
import java.util.Optional;

public interface TemplateSectionRepository {

    TemplateSection save(TemplateSection section);

    Optional<TemplateSection> findById(Long id);

    List<TemplateSection> findByTemplateId(Long templateId);

    void deleteById(Long id);

    void deleteByTemplateId(Long templateId);

    List<TemplateSection> findByParentSectionId(Long parentSectionId);

    List<TemplateSection> findRootSections(Long templateId);

    /**
     * 查询所有根分区（templateId=null, parentSectionId=null）
     */
    List<TemplateSection> findAllRootSections();

    /**
     * 分页查询根分区
     */
    List<TemplateSection> findRootSectionsPaged(int offset, int size, String status, Long catalogId, String keyword);

    int countRootSections(String status, Long catalogId, String keyword);

    /**
     * 递归查询某个根分区下所有子孙分区
     */
    List<TemplateSection> findDescendants(Long rootSectionId);
}
