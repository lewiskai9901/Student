package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.CheckTemplateCreateRequest;
import com.school.management.dto.CheckTemplateResponse;
import com.school.management.dto.CheckTemplateUpdateRequest;
import com.school.management.entity.CheckCategory;
import com.school.management.entity.CheckTemplate;
import com.school.management.entity.TemplateCategory;
import com.school.management.exception.BusinessException;
import com.school.management.entity.DeductionItem;
import com.school.management.mapper.CheckCategoryMapper;
import com.school.management.mapper.CheckTemplateMapper;
import com.school.management.mapper.DeductionItemMapper;
import com.school.management.mapper.TemplateCategoryMapper;
import com.school.management.service.CheckTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查模板服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckTemplateServiceImpl implements CheckTemplateService {

    private final CheckTemplateMapper checkTemplateMapper;
    private final TemplateCategoryMapper templateCategoryMapper;
    private final CheckCategoryMapper checkCategoryMapper;
    private final DeductionItemMapper deductionItemMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(CheckTemplateCreateRequest request) {
        log.info("创建检查模板: {}", request.getTemplateName());

        // 自动生成模板编码（如果未提供）
        String templateCode = request.getTemplateCode();
        if (!StringUtils.hasText(templateCode)) {
            templateCode = "TPL_" + System.currentTimeMillis();
        } else {
            // 检查模板编码是否重复（仅当手动提供编码时）
            LambdaQueryWrapper<CheckTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CheckTemplate::getTemplateCode, templateCode);
            if (checkTemplateMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ResultCode.DATA_EXISTS, "模板编码已存在");
            }
        }

        // 创建模板
        CheckTemplate template = new CheckTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateCode(templateCode);
        template.setDescription(request.getDescription());
        template.setTotalRounds(request.getTotalRounds() != null ? request.getTotalRounds() : 1);
        // 将轮次名称列表转为JSON字符串
        if (request.getRoundNames() != null && !request.getRoundNames().isEmpty()) {
            try {
                template.setRoundNames(objectMapper.writeValueAsString(request.getRoundNames()));
            } catch (JsonProcessingException e) {
                log.warn("序列化轮次名称失败", e);
            }
        }
        template.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        template.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        checkTemplateMapper.insert(template);

        // 保存检查类别关联
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (CheckTemplateCreateRequest.TemplateCategoryItem item : request.getCategories()) {
                TemplateCategory tc = new TemplateCategory();
                tc.setTemplateId(template.getId());
                tc.setCategoryId(item.getCategoryId());
                tc.setLinkType(item.getLinkType() != null ? item.getLinkType() : 0);
                tc.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
                tc.setIsRequired(item.getIsRequired() != null ? item.getIsRequired() : 1);
                tc.setCheckRounds(item.getCheckRounds() != null ? item.getCheckRounds() : 1);
                // 设置参与的轮次
                if (StringUtils.hasText(item.getParticipatedRounds())) {
                    tc.setParticipatedRounds(item.getParticipatedRounds());
                } else {
                    // 默认参与所有轮次
                    int totalRounds = request.getTotalRounds() != null ? request.getTotalRounds() : 1;
                    tc.setParticipatedRounds(generateAllRounds(totalRounds));
                }
                templateCategoryMapper.insert(tc);
            }
        }

        log.info("检查模板创建成功: {}", template.getId());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(CheckTemplateUpdateRequest request) {
        log.info("更新检查模板: {}", request.getId());

        CheckTemplate template = checkTemplateMapper.selectById(request.getId());
        if (template == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        // 更新模板（不更新编码，保持原值）
        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setIsDefault(request.getIsDefault());
        template.setStatus(request.getStatus());
        // 更新轮次相关字段
        template.setTotalRounds(request.getTotalRounds() != null ? request.getTotalRounds() : 1);
        if (request.getRoundNames() != null && !request.getRoundNames().isEmpty()) {
            try {
                template.setRoundNames(objectMapper.writeValueAsString(request.getRoundNames()));
            } catch (JsonProcessingException e) {
                log.warn("序列化轮次名称失败", e);
            }
        } else {
            template.setRoundNames(null);
        }

        checkTemplateMapper.updateById(template);

        // 物理删除旧的类别关联（避免软删除导致的唯一约束冲突）
        templateCategoryMapper.physicalDeleteByTemplateId(template.getId());

        // 保存新的类别关联
        int totalRounds = request.getTotalRounds() != null ? request.getTotalRounds() : 1;
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (CheckTemplateCreateRequest.TemplateCategoryItem item : request.getCategories()) {
                TemplateCategory tc = new TemplateCategory();
                tc.setTemplateId(template.getId());
                tc.setCategoryId(item.getCategoryId());
                tc.setLinkType(item.getLinkType() != null ? item.getLinkType() : 0);
                tc.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
                tc.setIsRequired(item.getIsRequired() != null ? item.getIsRequired() : 1);
                tc.setCheckRounds(item.getCheckRounds() != null ? item.getCheckRounds() : 1);
                // 设置参与的轮次
                if (StringUtils.hasText(item.getParticipatedRounds())) {
                    tc.setParticipatedRounds(item.getParticipatedRounds());
                } else {
                    tc.setParticipatedRounds(generateAllRounds(totalRounds));
                }
                templateCategoryMapper.insert(tc);
            }
        }

        log.info("检查模板更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        log.info("删除检查模板: {}", id);

        CheckTemplate template = checkTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        // 删除模板
        checkTemplateMapper.deleteById(id);

        // 物理删除类别关联
        templateCategoryMapper.physicalDeleteByTemplateId(id);

        log.info("检查模板删除成功: {}", id);
    }

    @Override
    public CheckTemplateResponse getTemplateById(Long id) {
        CheckTemplate template = checkTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        CheckTemplateResponse response = new CheckTemplateResponse();
        response.setId(template.getId());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateCode(template.getTemplateCode());
        response.setDescription(template.getDescription());
        response.setTotalRounds(template.getTotalRounds() != null ? template.getTotalRounds() : 1);
        response.setRoundNames(parseRoundNames(template.getRoundNames()));
        response.setIsDefault(template.getIsDefault());
        response.setStatus(template.getStatus());
        response.setCreatedBy(template.getCreatedBy());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());

        // 查询关联的类别
        LambdaQueryWrapper<TemplateCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateCategory::getTemplateId, id)
               .orderByAsc(TemplateCategory::getSortOrder);
        List<TemplateCategory> categories = templateCategoryMapper.selectList(wrapper);

        List<CheckTemplateResponse.TemplateCategoryResponse> categoryResponses = categories.stream()
                .map(tc -> {
                    CheckTemplateResponse.TemplateCategoryResponse cr = new CheckTemplateResponse.TemplateCategoryResponse();
                    cr.setId(tc.getId());
                    cr.setCategoryId(tc.getCategoryId());
                    cr.setLinkType(tc.getLinkType());
                    cr.setSortOrder(tc.getSortOrder());
                    cr.setIsRequired(tc.getIsRequired());
                    cr.setCheckRounds(tc.getCheckRounds() != null ? tc.getCheckRounds() : 1);
                    // 设置参与轮次
                    cr.setParticipatedRounds(tc.getParticipatedRounds());
                    cr.setParticipatedRoundsList(parseParticipatedRounds(tc.getParticipatedRounds()));

                    // 查询类别名称
                    CheckCategory category = checkCategoryMapper.selectById(tc.getCategoryId());
                    if (category != null) {
                        cr.setCategoryName(category.getCategoryName());
                        cr.setCategoryCode(category.getCategoryCode());
                    }

                    return cr;
                })
                .collect(Collectors.toList());

        response.setCategories(categoryResponses);

        return response;
    }

    @Override
    public List<CheckTemplate> getAllTemplates() {
        LambdaQueryWrapper<CheckTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckTemplate::getStatus, 1)
               .orderByDesc(CheckTemplate::getCreatedAt);
        return checkTemplateMapper.selectList(wrapper);
    }

    @Override
    public IPage<CheckTemplate> getTemplatePage(Integer pageNum, Integer pageSize, String templateName, Integer status) {
        Page<CheckTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CheckTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(templateName)) {
            wrapper.like(CheckTemplate::getTemplateName, templateName);
        }
        if (status != null) {
            wrapper.eq(CheckTemplate::getStatus, status);
        }

        wrapper.orderByDesc(CheckTemplate::getCreatedAt);

        return checkTemplateMapper.selectPage(page, wrapper);
    }

    @Override
    public List<CheckTemplateResponse> getAllTemplatesWithCategories() {
        // 获取所有启用的模板
        LambdaQueryWrapper<CheckTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(CheckTemplate::getStatus, 1)
                       .orderByDesc(CheckTemplate::getCreatedAt);
        List<CheckTemplate> templates = checkTemplateMapper.selectList(templateWrapper);

        return templates.stream().map(template -> {
            CheckTemplateResponse response = new CheckTemplateResponse();
            response.setId(template.getId());
            response.setTemplateName(template.getTemplateName());
            response.setTemplateCode(template.getTemplateCode());
            response.setDescription(template.getDescription());
            response.setTotalRounds(template.getTotalRounds() != null ? template.getTotalRounds() : 1);
            response.setRoundNames(parseRoundNames(template.getRoundNames()));
            response.setIsDefault(template.getIsDefault());
            response.setStatus(template.getStatus());
            response.setCreatedBy(template.getCreatedBy());
            response.setCreatedAt(template.getCreatedAt());
            response.setUpdatedAt(template.getUpdatedAt());

            // 查询关联的类别
            LambdaQueryWrapper<TemplateCategory> catWrapper = new LambdaQueryWrapper<>();
            catWrapper.eq(TemplateCategory::getTemplateId, template.getId())
                      .orderByAsc(TemplateCategory::getSortOrder);
            List<TemplateCategory> categories = templateCategoryMapper.selectList(catWrapper);

            List<CheckTemplateResponse.TemplateCategoryResponse> categoryResponses = categories.stream()
                    .map(tc -> {
                        CheckTemplateResponse.TemplateCategoryResponse cr = new CheckTemplateResponse.TemplateCategoryResponse();
                        cr.setId(tc.getId());
                        cr.setCategoryId(tc.getCategoryId());
                        cr.setLinkType(tc.getLinkType());
                        cr.setSortOrder(tc.getSortOrder());
                        cr.setIsRequired(tc.getIsRequired());
                        cr.setCheckRounds(tc.getCheckRounds() != null ? tc.getCheckRounds() : 1);
                        // 设置参与轮次
                        cr.setParticipatedRounds(tc.getParticipatedRounds());
                        cr.setParticipatedRoundsList(parseParticipatedRounds(tc.getParticipatedRounds()));

                        // 查询类别名称
                        CheckCategory category = checkCategoryMapper.selectById(tc.getCategoryId());
                        if (category != null) {
                            cr.setCategoryName(category.getCategoryName());
                            cr.setCategoryCode(category.getCategoryCode());
                        }

                        // 查询该类别下的启用扣分项
                        List<DeductionItem> deductionItems = deductionItemMapper.selectEnabledByTypeId(tc.getCategoryId());
                        if (deductionItems != null && !deductionItems.isEmpty()) {
                            List<CheckTemplateResponse.DeductionItemResponse> itemResponses = deductionItems.stream()
                                    .map(item -> {
                                        CheckTemplateResponse.DeductionItemResponse ir = new CheckTemplateResponse.DeductionItemResponse();
                                        ir.setId(item.getId());
                                        ir.setItemName(item.getItemName());
                                        ir.setDeductMode(item.getDeductMode());
                                        ir.setFixedScore(item.getFixedScore());
                                        ir.setBaseScore(item.getBaseScore());
                                        ir.setPerPersonScore(item.getPerPersonScore());
                                        ir.setRangeConfig(item.getRangeConfig());
                                        ir.setDescription(item.getDescription());
                                        ir.setSortOrder(item.getSortOrder());
                                        ir.setAllowPhoto(item.getAllowPhoto());
                                        ir.setAllowRemark(item.getAllowRemark());
                                        ir.setAllowStudents(item.getAllowStudents());
                                        return ir;
                                    })
                                    .collect(Collectors.toList());
                            cr.setDeductionItems(itemResponses);
                        }

                        return cr;
                    })
                    .collect(Collectors.toList());

            response.setCategories(categoryResponses);
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 生成所有轮次字符串，如 totalRounds=3 返回 "1,2,3"
     */
    private String generateAllRounds(int totalRounds) {
        if (totalRounds <= 1) return "1";
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= totalRounds; i++) {
            if (i > 1) sb.append(",");
            sb.append(i);
        }
        return sb.toString();
    }

    /**
     * 解析参与轮次字符串为列表
     */
    private List<Integer> parseParticipatedRounds(String participatedRounds) {
        if (!StringUtils.hasText(participatedRounds)) {
            return Collections.singletonList(1);
        }
        return Arrays.stream(participatedRounds.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /**
     * 解析轮次名称JSON字符串为列表
     */
    private List<String> parseRoundNames(String roundNamesJson) {
        if (!StringUtils.hasText(roundNamesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(roundNamesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析轮次名称失败: {}", roundNamesJson, e);
            return Collections.emptyList();
        }
    }
}
