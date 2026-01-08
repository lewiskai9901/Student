package com.school.management.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.task.WorkflowTemplateDTO;
import com.school.management.dto.task.WorkflowTemplateRequest;
import com.school.management.entity.task.WorkflowTemplate;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.task.WorkflowTemplateMapper;
import com.school.management.service.task.FlowableProcessService;
import com.school.management.service.task.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTemplateServiceImpl extends ServiceImpl<WorkflowTemplateMapper, WorkflowTemplate>
        implements WorkflowTemplateService {

    private final FlowableProcessService flowableProcessService;

    @Override
    public IPage<WorkflowTemplateDTO> pageQuery(Integer pageNum, Integer pageSize, String keyword, String templateType, Integer status) {
        Page<WorkflowTemplate> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<WorkflowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), WorkflowTemplate::getTemplateName, keyword)
                .eq(StringUtils.hasText(templateType), WorkflowTemplate::getTemplateType, templateType)
                .eq(status != null, WorkflowTemplate::getStatus, status)
                .orderByAsc(WorkflowTemplate::getSortOrder)
                .orderByDesc(WorkflowTemplate::getCreatedAt);

        IPage<WorkflowTemplate> result = page(page, wrapper);

        return result.convert(this::convertToDTO);
    }

    @Override
    public List<WorkflowTemplateDTO> listEnabled(String templateType) {
        LambdaQueryWrapper<WorkflowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTemplate::getStatus, 1)
                .eq(StringUtils.hasText(templateType), WorkflowTemplate::getTemplateType, templateType)
                .orderByAsc(WorkflowTemplate::getSortOrder);

        List<WorkflowTemplate> list = list(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public WorkflowTemplateDTO getDetail(Long id) {
        WorkflowTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }
        return convertToDTO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTemplateDTO create(WorkflowTemplateRequest request, Long userId, String userName) {
        // 检查模板编码是否重复
        LambdaQueryWrapper<WorkflowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTemplate::getTemplateCode, request.getTemplateCode());
        if (count(wrapper) > 0) {
            throw new BusinessException("模板编码已存在");
        }

        WorkflowTemplate template = new WorkflowTemplate();
        BeanUtils.copyProperties(request, template);
        template.setCreatedBy(userId);
        template.setCreatedByName(userName);
        template.setVersion(1);

        save(template);

        log.info("创建流程模板成功: id={}, name={}", template.getId(), template.getTemplateName());
        return convertToDTO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTemplateDTO update(WorkflowTemplateRequest request, Long userId) {
        WorkflowTemplate template = getById(request.getId());
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        // 检查模板编码是否重复(排除自己)
        LambdaQueryWrapper<WorkflowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTemplate::getTemplateCode, request.getTemplateCode())
                .ne(WorkflowTemplate::getId, request.getId());
        if (count(wrapper) > 0) {
            throw new BusinessException("模板编码已存在");
        }

        BeanUtils.copyProperties(request, template, "id", "createdBy", "createdByName", "version", "processDefinitionId", "processDefinitionKey");
        template.setUpdatedBy(userId);

        // 如果BPMN XML有变化，需要重新部署
        if (StringUtils.hasText(request.getBpmnXml()) && !request.getBpmnXml().equals(template.getBpmnXml())) {
            template.setBpmnXml(request.getBpmnXml());
            template.setVersion(template.getVersion() + 1);
            template.setProcessDefinitionId(null); // 清空流程定义ID，需要重新部署
        }

        updateById(template);

        log.info("更新流程模板成功: id={}, name={}", template.getId(), template.getTemplateName());
        return convertToDTO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        WorkflowTemplate template = getById(id);
        if (template == null) {
            return false;
        }

        // 如果是默认模板，不允许删除
        if (template.getIsDefault() == 1) {
            throw new BusinessException("默认模板不允许删除");
        }

        removeById(id);
        log.info("删除流程模板成功: id={}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTemplateDTO deployProcess(Long id) {
        WorkflowTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        if (!StringUtils.hasText(template.getBpmnXml())) {
            throw new BusinessException("请先设计流程图");
        }

        // 部署流程
        String processDefinitionId = flowableProcessService.deployProcess(
                template.getTemplateName() + "_v" + template.getVersion(),
                template.getBpmnXml()
        );

        // 更新模板
        template.setProcessDefinitionId(processDefinitionId);
        // 从processDefinitionId中提取Key
        String[] parts = processDefinitionId.split(":");
        if (parts.length > 0) {
            template.setProcessDefinitionKey(parts[0]);
        }
        updateById(template);

        log.info("部署流程成功: templateId={}, processDefinitionId={}", id, processDefinitionId);
        return convertToDTO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(Long id, String templateType) {
        // 先将同类型的所有模板设为非默认
        LambdaUpdateWrapper<WorkflowTemplate> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WorkflowTemplate::getTemplateType, templateType)
                .set(WorkflowTemplate::getIsDefault, 0);
        update(updateWrapper);

        // 将指定模板设为默认
        LambdaUpdateWrapper<WorkflowTemplate> setDefaultWrapper = new LambdaUpdateWrapper<>();
        setDefaultWrapper.eq(WorkflowTemplate::getId, id)
                .set(WorkflowTemplate::getIsDefault, 1);
        update(setDefaultWrapper);

        log.info("设置默认模板成功: id={}", id);
        return true;
    }

    @Override
    public WorkflowTemplate getDefaultTemplate(String templateType) {
        LambdaQueryWrapper<WorkflowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowTemplate::getTemplateType, templateType)
                .eq(WorkflowTemplate::getIsDefault, 1)
                .eq(WorkflowTemplate::getStatus, 1);
        return getOne(wrapper);
    }

    @Override
    public WorkflowTemplateDTO convertToDTO(WorkflowTemplate template) {
        if (template == null) {
            return null;
        }
        WorkflowTemplateDTO dto = new WorkflowTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }
}
