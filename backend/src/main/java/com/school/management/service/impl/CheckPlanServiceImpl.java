package com.school.management.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.CheckTemplateResponse;
import com.school.management.dto.request.CheckPlanCreateRequest;
import com.school.management.dto.request.CheckPlanQueryRequest;
import com.school.management.dto.request.CheckPlanUpdateRequest;
import com.school.management.dto.response.CheckPlanListVO;
import com.school.management.dto.response.CheckPlanStatisticsVO;
import com.school.management.dto.response.CheckPlanVO;
import com.school.management.entity.CheckPlan;
import com.school.management.enums.CheckPlanStatus;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckPlanMapper;
import com.school.management.service.CheckPlanService;
import com.school.management.service.CheckTemplateService;
import com.school.management.entity.DeductionItem;
import com.school.management.mapper.DeductionItemMapper;
import com.school.management.dto.ClassResponse;
import com.school.management.dto.DepartmentResponse;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查计划Service实现
 *
 * @author system
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckPlanServiceImpl extends ServiceImpl<CheckPlanMapper, CheckPlan> implements CheckPlanService {

    private final CheckTemplateService checkTemplateService;
    private final DeductionItemMapper deductionItemMapper;
    private final ClassMapper classMapper;
    private final DepartmentMapper departmentMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckPlan createPlan(CheckPlanCreateRequest request) {
        // 验证日期范围
        request.validateDateRange();

        // 验证模板存在
        CheckTemplateResponse template = checkTemplateService.getTemplateById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("模板未启用");
        }

        // 生成模板快照
        String templateSnapshot = buildTemplateSnapshot(request.getTemplateId());

        // 生成计划编号
        String planCode = generatePlanCode();

        // 创建计划实体
        CheckPlan plan = new CheckPlan();
        plan.setPlanCode(planCode);
        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setTemplateId(request.getTemplateId());
        plan.setTemplateName(template.getTemplateName());
        plan.setTemplateSnapshot(templateSnapshot);
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setWeightConfigId(request.getWeightConfigId());
        plan.setEnableWeight(request.getEnableWeight() != null ? request.getEnableWeight() : 0);
        plan.setCustomStandardSize(request.getCustomStandardSize());

        // 序列化并保存分类级别的加权配置
        if (request.getItemWeightConfigs() != null && !request.getItemWeightConfigs().isEmpty()) {
            try {
                String itemWeightConfigsJson = objectMapper.writeValueAsString(request.getItemWeightConfigs());
                plan.setItemWeightConfigs(itemWeightConfigsJson);
                log.info("保存分类加权配置: count={}", request.getItemWeightConfigs().size());
            } catch (JsonProcessingException e) {
                log.warn("序列化分类加权配置失败", e);
            }
        }

        // 设置目标范围配置
        plan.setTargetScopeType(request.getTargetScopeType() != null ? request.getTargetScopeType() : "all");
        if (request.getTargetScopeConfig() != null) {
            try {
                String targetScopeConfigJson = objectMapper.writeValueAsString(request.getTargetScopeConfig());
                plan.setTargetScopeConfig(targetScopeConfigJson);
                log.info("保存目标范围配置: type={}", request.getTargetScopeType());
            } catch (JsonProcessingException e) {
                log.warn("序列化目标范围配置失败", e);
            }
        }

        plan.setStatus(CheckPlanStatus.DRAFT.getCode()); // 草稿状态
        plan.setTotalChecks(0);
        plan.setTotalRecords(0);

        // 设置创建人信息
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserName = SecurityUtils.getCurrentUsername();
        plan.setCreatedBy(currentUserId);
        plan.setCreatorName(currentUserName);

        // 保存计划
        save(plan);

        log.info("创建检查计划成功: planCode={}, planName={}, templateId={}",
                planCode, request.getPlanName(), request.getTemplateId());

        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckPlan updatePlan(CheckPlanUpdateRequest request) {
        // 验证日期范围
        request.validateDateRange();

        // 获取现有计划
        CheckPlan plan = getById(request.getId());
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        // 已归档的计划不能修改
        if (CheckPlanStatus.ARCHIVED.getCode().equals(plan.getStatus())) {
            throw new BusinessException("已归档的计划不能修改");
        }

        // 更新计划信息(不更新模板快照)
        if (request.getPlanName() != null) {
            plan.setPlanName(request.getPlanName());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            plan.setEndDate(request.getEndDate());
        }
        if (request.getWeightConfigId() != null) {
            plan.setWeightConfigId(request.getWeightConfigId());
        }
        if (request.getEnableWeight() != null) {
            plan.setEnableWeight(request.getEnableWeight());
        }
        if (request.getCustomStandardSize() != null) {
            plan.setCustomStandardSize(request.getCustomStandardSize());
        }

        // 序列化并保存分类级别的加权配置
        if (request.getItemWeightConfigs() != null && !request.getItemWeightConfigs().isEmpty()) {
            try {
                String itemWeightConfigsJson = objectMapper.writeValueAsString(request.getItemWeightConfigs());
                plan.setItemWeightConfigs(itemWeightConfigsJson);
                log.info("保存分类加权配置: count={}", request.getItemWeightConfigs().size());
            } catch (JsonProcessingException e) {
                log.warn("序列化分类加权配置失败", e);
            }
        }

        // 更新目标范围配置
        if (request.getTargetScopeType() != null) {
            plan.setTargetScopeType(request.getTargetScopeType());
        }
        if (request.getTargetScopeConfig() != null) {
            try {
                String targetScopeConfigJson = objectMapper.writeValueAsString(request.getTargetScopeConfig());
                plan.setTargetScopeConfig(targetScopeConfigJson);
                log.info("更新目标范围配置: type={}", request.getTargetScopeType());
            } catch (JsonProcessingException e) {
                log.warn("序列化目标范围配置失败", e);
            }
        }

        updateById(plan);

        log.info("更新检查计划成功: planId={}", request.getId());

        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        CheckPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        // 只有草稿状态才能删除
        if (!CheckPlanStatus.DRAFT.getCode().equals(plan.getStatus())) {
            throw new BusinessException("只有草稿状态的计划才能删除");
        }

        removeById(id);
        log.info("删除检查计划成功: planId={}", id);
    }

    @Override
    public CheckPlanVO getPlanDetail(Long id) {
        CheckPlan plan = baseMapper.selectPlanWithDetails(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        CheckPlanVO vo = new CheckPlanVO();
        BeanUtils.copyProperties(plan, vo);
        return vo;
    }

    @Override
    public Page<CheckPlanListVO> getPlanPage(CheckPlanQueryRequest request) {
        Page<CheckPlanListVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        return baseMapper.selectPlanPage(page, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPlan(Long id) {
        CheckPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        if (!CheckPlanStatus.DRAFT.getCode().equals(plan.getStatus())) {
            throw new BusinessException("只有草稿状态的计划才能开始");
        }

        plan.setStatus(CheckPlanStatus.IN_PROGRESS.getCode());
        updateById(plan);
        log.info("开始检查计划: planId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishPlan(Long id) {
        CheckPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        if (!CheckPlanStatus.IN_PROGRESS.getCode().equals(plan.getStatus())) {
            throw new BusinessException("只有进行中的计划才能结束");
        }

        // 更新统计数据
        updatePlanStatistics(id);

        plan.setStatus(CheckPlanStatus.COMPLETED.getCode());
        updateById(plan);
        log.info("结束检查计划: planId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archivePlan(Long id) {
        CheckPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        if (!CheckPlanStatus.COMPLETED.getCode().equals(plan.getStatus())) {
            throw new BusinessException("只有已结束的计划才能归档");
        }

        plan.setStatus(CheckPlanStatus.ARCHIVED.getCode());
        updateById(plan);
        log.info("归档检查计划: planId={}", id);
    }

    @Override
    public CheckPlanStatisticsVO getPlanStatistics() {
        return baseMapper.selectPlanStatistics();
    }

    @Override
    public void updatePlanStatistics(Long planId) {
        baseMapper.updatePlanStatistics(planId);
    }

    @Override
    public String buildTemplateSnapshot(Long templateId) {
        CheckTemplateResponse template = checkTemplateService.getTemplateById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("templateId", template.getId());
        snapshot.put("templateName", template.getTemplateName());
        snapshot.put("templateCode", template.getTemplateCode());
        snapshot.put("description", template.getDescription());
        snapshot.put("totalRounds", template.getTotalRounds());
        snapshot.put("roundNames", template.getRoundNames());
        snapshot.put("snapshotTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 添加完整的类别数据（包含扣分项）
        if (template.getCategories() != null && !template.getCategories().isEmpty()) {
            List<Map<String, Object>> categoriesSnapshot = new ArrayList<>();
            for (CheckTemplateResponse.TemplateCategoryResponse category : template.getCategories()) {
                Map<String, Object> categoryMap = new LinkedHashMap<>();
                categoryMap.put("id", category.getId());
                categoryMap.put("categoryId", category.getCategoryId());
                categoryMap.put("categoryName", category.getCategoryName());
                categoryMap.put("categoryCode", category.getCategoryCode());
                categoryMap.put("linkType", category.getLinkType());
                categoryMap.put("sortOrder", category.getSortOrder());
                categoryMap.put("isRequired", category.getIsRequired());
                categoryMap.put("checkRounds", category.getCheckRounds());
                categoryMap.put("participatedRounds", category.getParticipatedRounds());

                // 查询该类别下的扣分项
                List<DeductionItem> deductionItems = deductionItemMapper.selectEnabledByTypeId(category.getCategoryId());
                if (deductionItems != null && !deductionItems.isEmpty()) {
                    List<Map<String, Object>> itemsSnapshot = new ArrayList<>();
                    for (DeductionItem item : deductionItems) {
                        Map<String, Object> itemMap = new LinkedHashMap<>();
                        itemMap.put("itemId", item.getId());
                        itemMap.put("itemName", item.getItemName());
                        itemMap.put("deductMode", item.getDeductMode());
                        itemMap.put("fixedScore", item.getFixedScore());
                        itemMap.put("baseScore", item.getBaseScore());
                        itemMap.put("perPersonScore", item.getPerPersonScore());
                        itemMap.put("rangeConfig", item.getRangeConfig());
                        itemMap.put("description", item.getDescription());
                        itemMap.put("sortOrder", item.getSortOrder());
                        itemMap.put("allowPhoto", item.getAllowPhoto());
                        itemMap.put("allowRemark", item.getAllowRemark());
                        itemMap.put("allowStudents", item.getAllowStudents());
                        itemsSnapshot.add(itemMap);
                    }
                    categoryMap.put("deductionItems", itemsSnapshot);
                } else {
                    categoryMap.put("deductionItems", new ArrayList<>());
                }

                categoriesSnapshot.add(categoryMap);
            }
            snapshot.put("categories", categoriesSnapshot);
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("生成模板快照失败: " + e.getMessage());
        }
    }

    /**
     * 生成计划编号: PLAN-yyyyMMdd-xxxx
     */
    private String generatePlanCode() {
        String datePrefix = "PLAN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Integer count = baseMapper.countTodayPlans(datePrefix);
        int seq = (count == null ? 0 : count) + 1;
        return datePrefix + String.format("%04d", seq);
    }

    @Override
    public List<?> getTargetClasses(Long planId) {
        CheckPlan plan = getById(planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        String scopeType = plan.getTargetScopeType();
        String scopeConfigJson = plan.getTargetScopeConfig();

        // 如果没有设置目标范围或者是全部，返回所有启用的班级
        if (scopeType == null || "all".equals(scopeType) || scopeConfigJson == null) {
            return classMapper.selectAllEnabled();
        }

        try {
            // 解析目标范围配置
            Map<String, Object> scopeConfig = objectMapper.readValue(scopeConfigJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            List<ClassResponse> allClasses = classMapper.selectAllEnabled();
            List<ClassResponse> filteredClasses = new ArrayList<>();

            switch (scopeType) {
                case "department":
                    // 按院系筛选（包含子部门，支持字符串或数字类型的ID）
                    @SuppressWarnings("unchecked")
                    List<?> deptIds = (List<?>) scopeConfig.get("departmentIds");
                    if (deptIds != null && !deptIds.isEmpty()) {
                        // 获取所有选中部门及其所有子部门ID
                        Set<Long> allDeptIds = new HashSet<>();
                        for (Object deptId : deptIds) {
                            Long id = deptId instanceof Number
                                    ? ((Number) deptId).longValue()
                                    : Long.parseLong(String.valueOf(deptId));
                            allDeptIds.add(id);
                            // 递归获取所有子部门ID
                            collectDescendantDeptIds(id, allDeptIds);
                        }
                        log.info("部门筛选: 原始部门数={}, 包含子部门后总数={}", deptIds.size(), allDeptIds.size());
                        filteredClasses = allClasses.stream()
                                .filter(c -> c.getDepartmentId() != null && allDeptIds.contains(c.getDepartmentId()))
                                .collect(Collectors.toList());
                    }
                    break;

                case "grade":
                    // 按年级筛选（支持字符串或数字类型的ID）
                    @SuppressWarnings("unchecked")
                    List<?> gradeIds = (List<?>) scopeConfig.get("gradeIds");
                    if (gradeIds != null && !gradeIds.isEmpty()) {
                        Set<Long> gradeIdSet = gradeIds.stream()
                                .map(id -> id instanceof Number
                                        ? ((Number) id).longValue()
                                        : Long.parseLong(String.valueOf(id)))
                                .collect(Collectors.toSet());
                        filteredClasses = allClasses.stream()
                                .filter(c -> c.getGradeId() != null && gradeIdSet.contains(c.getGradeId()))
                                .collect(Collectors.toList());
                    }
                    break;

                case "custom":
                    // 自定义班级列表（支持字符串或数字类型的ID）
                    @SuppressWarnings("unchecked")
                    List<?> classIds = (List<?>) scopeConfig.get("classIds");
                    if (classIds != null && !classIds.isEmpty()) {
                        Set<Long> classIdSet = classIds.stream()
                                .map(id -> {
                                    if (id instanceof Number) {
                                        return ((Number) id).longValue();
                                    }
                                    return Long.parseLong(String.valueOf(id));
                                })
                                .collect(Collectors.toSet());
                        filteredClasses = allClasses.stream()
                                .filter(c -> c.getId() != null && classIdSet.contains(c.getId()))
                                .collect(Collectors.toList());
                    }
                    break;

                default:
                    filteredClasses = allClasses;
            }

            // 处理排除班级（支持字符串或数字类型的ID）
            @SuppressWarnings("unchecked")
            List<?> excludeIds = (List<?>) scopeConfig.get("excludeClassIds");
            if (excludeIds != null && !excludeIds.isEmpty()) {
                Set<Long> excludeIdSet = excludeIds.stream()
                        .map(id -> id instanceof Number
                                ? ((Number) id).longValue()
                                : Long.parseLong(String.valueOf(id)))
                        .collect(Collectors.toSet());
                filteredClasses = filteredClasses.stream()
                        .filter(c -> c.getId() == null || !excludeIdSet.contains(c.getId()))
                        .collect(Collectors.toList());
            }

            return filteredClasses;

        } catch (Exception e) {
            log.error("解析目标范围配置失败: {}", e.getMessage(), e);
            return classMapper.selectAllEnabled();
        }
    }

    /**
     * 递归收集所有子部门ID
     *
     * @param parentId 父部门ID
     * @param resultSet 结果集合
     */
    private void collectDescendantDeptIds(Long parentId, Set<Long> resultSet) {
        List<DepartmentResponse> children = departmentMapper.selectByParentId(parentId);
        if (children != null && !children.isEmpty()) {
            for (DepartmentResponse child : children) {
                if (child.getId() != null) {
                    resultSet.add(child.getId());
                    // 递归查询子部门
                    collectDescendantDeptIds(child.getId(), resultSet);
                }
            }
        }
    }
}
