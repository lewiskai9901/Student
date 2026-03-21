package com.school.management.application.evaluation;

import com.school.management.domain.event.model.EntityEventType;
import com.school.management.domain.event.repository.EntityEventTypeRepository;
import com.school.management.infrastructure.persistence.inspection.v7.execution.InspProjectMapper;
import com.school.management.infrastructure.persistence.inspection.v7.execution.InspProjectPO;
import com.school.management.infrastructure.persistence.inspection.v7.template.TemplateSectionMapper;
import com.school.management.infrastructure.persistence.inspection.v7.template.TemplateSectionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 条件编辑器所需的选项数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalConditionOptionsService {

    private final InspProjectMapper inspProjectMapper;
    private final TemplateSectionMapper templateSectionMapper;
    private final EntityEventTypeRepository entityEventTypeRepository;

    /**
     * 获取可选的检查项目列表
     */
    public List<Map<String, Object>> listProjects() {
        List<InspProjectPO> projects = inspProjectMapper.findAll();
        return projects.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("projectCode", p.getProjectCode());
            m.put("projectName", p.getProjectName());
            m.put("status", p.getStatus());
            return m;
        }).toList();
    }

    /**
     * 获取项目下的分区列表（通过模板）
     * projectId 实际对应 insp_projects.template_id（rootSectionId）
     * 这里查 insp_template_sections WHERE template_id = project.templateId
     */
    public List<Map<String, Object>> listSections(Long projectId) {
        InspProjectPO project = inspProjectMapper.selectById(projectId);
        if (project == null) {
            return List.of();
        }
        Long templateId = project.getTemplateId();
        if (templateId == null) {
            return List.of();
        }
        List<TemplateSectionPO> sections = templateSectionMapper.findByTemplateId(templateId);
        return sections.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("sectionCode", s.getSectionCode());
            m.put("sectionName", s.getSectionName());
            m.put("targetType", s.getTargetType());
            m.put("parentSectionId", s.getParentSectionId());
            return m;
        }).toList();
    }

    /**
     * 获取事件类型列表（条件编辑器用）
     */
    public List<Map<String, Object>> listEventTypes() {
        List<EntityEventType> types = entityEventTypeRepository.findEnabled();
        return types.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("typeCode", t.getTypeCode());
            m.put("typeName", t.getTypeName());
            m.put("categoryCode", t.getCategoryCode());
            m.put("categoryName", t.getCategoryName());
            m.put("hasScore", t.getHasScore());
            return m;
        }).toList();
    }
}
