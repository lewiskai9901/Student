package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.CheckTemplateCreateRequest;
import com.school.management.dto.CheckTemplateResponse;
import com.school.management.dto.CheckTemplateUpdateRequest;
import com.school.management.entity.CheckTemplate;

import java.util.List;

/**
 * 检查模板服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface CheckTemplateService {

    /**
     * 创建检查模板
     *
     * @param request 创建请求
     * @return 模板ID
     */
    Long createTemplate(CheckTemplateCreateRequest request);

    /**
     * 更新检查模板
     *
     * @param request 更新请求
     */
    void updateTemplate(CheckTemplateUpdateRequest request);

    /**
     * 删除检查模板
     *
     * @param id 模板ID
     */
    void deleteTemplate(Long id);

    /**
     * 获取模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    CheckTemplateResponse getTemplateById(Long id);

    /**
     * 获取所有模板列表
     *
     * @return 模板列表
     */
    List<CheckTemplate> getAllTemplates();

    /**
     * 获取所有模板列表(包含类别和扣分项信息)
     *
     * @return 完整模板列表
     */
    List<CheckTemplateResponse> getAllTemplatesWithCategories();

    /**
     * 分页查询模板
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param templateName 模板名称
     * @param status 状态
     * @return 分页结果
     */
    IPage<CheckTemplate> getTemplatePage(Integer pageNum, Integer pageSize, String templateName, Integer status);
}
