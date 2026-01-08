package com.school.management.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.task.WorkflowTemplateDTO;
import com.school.management.dto.task.WorkflowTemplateRequest;
import com.school.management.entity.task.WorkflowTemplate;

import java.util.List;

/**
 * 流程模板服务接口
 */
public interface WorkflowTemplateService extends IService<WorkflowTemplate> {

    /**
     * 分页查询流程模板
     */
    IPage<WorkflowTemplateDTO> pageQuery(Integer pageNum, Integer pageSize, String keyword, String templateType, Integer status);

    /**
     * 获取所有启用的流程模板
     */
    List<WorkflowTemplateDTO> listEnabled(String templateType);

    /**
     * 获取模板详情
     */
    WorkflowTemplateDTO getDetail(Long id);

    /**
     * 创建流程模板
     */
    WorkflowTemplateDTO create(WorkflowTemplateRequest request, Long userId, String userName);

    /**
     * 更新流程模板
     */
    WorkflowTemplateDTO update(WorkflowTemplateRequest request, Long userId);

    /**
     * 删除流程模板
     */
    boolean delete(Long id);

    /**
     * 部署流程定义
     */
    WorkflowTemplateDTO deployProcess(Long id);

    /**
     * 设为默认模板
     */
    boolean setDefault(Long id, String templateType);

    /**
     * 获取默认模板
     */
    WorkflowTemplate getDefaultTemplate(String templateType);

    /**
     * DTO转换
     */
    WorkflowTemplateDTO convertToDTO(WorkflowTemplate template);
}
