package com.school.management.service;

import com.school.management.dto.export.*;

import java.util.List;

/**
 * 检查导出模板服务接口
 */
public interface CheckExportTemplateService {

    /**
     * 创建导出模板
     */
    ExportTemplateDTO createTemplate(Long planId, ExportTemplateRequest request, Long operatorId);

    /**
     * 更新导出模板
     */
    ExportTemplateDTO updateTemplate(Long templateId, ExportTemplateRequest request);

    /**
     * 删除导出模板
     */
    void deleteTemplate(Long templateId);

    /**
     * 获取模板详情
     */
    ExportTemplateDTO getTemplate(Long templateId);

    /**
     * 获取检查计划的所有导出模板
     */
    List<ExportTemplateDTO> getTemplatesByPlanId(Long planId);

    /**
     * 获取日常检查可用的导出模板列表
     * @param checkId 日常检查ID
     */
    List<ExportTemplateDTO> getTemplatesForCheck(Long checkId);

    /**
     * 获取导出预览数据
     * @param checkId 日常检查ID
     * @param request 导出请求
     */
    ExportPreviewDTO getExportPreview(Long checkId, ExportRequest request);

    /**
     * 导出文件
     * @param checkId 日常检查ID
     * @param request 导出请求
     * @return 生成的文件字节数组
     */
    byte[] exportFile(Long checkId, ExportRequest request);

    /**
     * 获取导出文件名
     */
    String getExportFileName(Long checkId, ExportRequest request);
}
