package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.CheckTemplateResponse;
import com.school.management.dto.DailyCheckCreateRequest;
import com.school.management.dto.DailyCheckResponse;
import com.school.management.dto.DailyScoringInitResponse;
import com.school.management.dto.DailyScoringRequest;
import com.school.management.dto.ScoringDetailRequest;
import com.school.management.entity.*;
import com.school.management.enums.DailyCheckStatus;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.CheckPlanInspectorService;
import com.school.management.service.CheckTemplateService;
import com.school.management.service.DailyCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.service.record.CheckRecordService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日常检查服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCheckServiceImpl implements DailyCheckService {

    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckTargetMapper dailyCheckTargetMapper;
    private final DailyCheckCategoryMapper dailyCheckCategoryMapper;
    private final CheckTemplateService checkTemplateService;
    private final CheckCategoryMapper checkCategoryMapper;
    private final CheckTemplateMapper checkTemplateMapper;
    private final DeductionItemMapper deductionItemMapper;
    private final DailyCheckDetailMapper dailyCheckDetailMapper;
    private final ClassMapper classMapper;
    private final CheckRecordService checkRecordService;
    private final DormitoryMapper dormitoryMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassDormitoryBindingMapper classDormitoryBindingMapper;
    private final BuildingMapper buildingMapper;
    private final ClassWeightConfigMapper classWeightConfigMapper;
    private final ObjectMapper objectMapper;
    private final TemplateCategoryMapper templateCategoryMapper;
    private final DailyCheckWeightConfigMapper dailyCheckWeightConfigMapper;
    private final CheckPlanInspectorService checkPlanInspectorService;
    private final CheckPlanMapper checkPlanMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDailyCheck(DailyCheckCreateRequest request) {
        log.info("创建日常检查: {}, totalRounds: {}", request.getCheckName(), request.getTotalRounds());

        // 创建检查主记录
        DailyCheck dailyCheck = new DailyCheck();
        dailyCheck.setCheckDate(request.getCheckDate());
        dailyCheck.setCheckName(request.getCheckName());
        dailyCheck.setTemplateId(request.getTemplateId());
        dailyCheck.setCheckType(request.getCheckType() != null ? request.getCheckType() : 1);
        dailyCheck.setStatus(DailyCheckStatus.NOT_STARTED.getCode()); // 未开始
        dailyCheck.setDescription(request.getDescription());
        dailyCheck.setPlanId(request.getPlanId()); // 关联检查计划
        // 设置全局轮次信息
        dailyCheck.setTotalRounds(request.getTotalRounds() != null ? request.getTotalRounds() : 1);
        if (request.getRoundNames() != null && !request.getRoundNames().isEmpty()) {
            try {
                dailyCheck.setRoundNames(objectMapper.writeValueAsString(request.getRoundNames()));
            } catch (Exception e) {
                log.warn("序列化轮次名称失败", e);
            }
        }

        // 保存排除目标信息(JSON格式)
        if (request.getExcludedTargets() != null && !request.getExcludedTargets().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                dailyCheck.setExcludedTargets(mapper.writeValueAsString(request.getExcludedTargets()));
            } catch (Exception e) {
                log.warn("序列化排除目标失败", e);
            }
        }

        // 自动获取默认加权配置
        applyDefaultWeightConfig(dailyCheck);

        dailyCheckMapper.insert(dailyCheck);

        // 保存多加权配置（从模板的类别加权配置中读取）
        saveMultiWeightConfigs(dailyCheck);

        // 保存检查目标
        if (request.getTargets() != null && !request.getTargets().isEmpty()) {
            for (DailyCheckCreateRequest.CheckTargetItem targetItem : request.getTargets()) {
                DailyCheckTarget target = new DailyCheckTarget();
                target.setCheckId(dailyCheck.getId());
                target.setTargetType(targetItem.getTargetType());
                target.setTargetId(targetItem.getTargetId());
                target.setTargetName(targetItem.getTargetName());
                dailyCheckTargetMapper.insert(target);
            }
        }

        // 保存检查类别
        List<DailyCheckCreateRequest.CheckCategoryItem> categories = request.getCategories();

        // 如果选择了模板但没有提供类别,则从模板加载
        if ((categories == null || categories.isEmpty()) && request.getTemplateId() != null) {
            CheckTemplateResponse template = checkTemplateService.getTemplateById(request.getTemplateId());
            // 从模板继承全局轮次设置（如果请求中没有指定）
            if (request.getTotalRounds() == null && template.getTotalRounds() != null) {
                dailyCheck.setTotalRounds(template.getTotalRounds());
            }
            if (request.getRoundNames() == null && template.getRoundNames() != null) {
                try {
                    dailyCheck.setRoundNames(objectMapper.writeValueAsString(template.getRoundNames()));
                } catch (Exception e) {
                    log.warn("序列化模板轮次名称失败", e);
                }
            }
            // 更新日常检查主记录以保存轮次信息
            dailyCheckMapper.updateById(dailyCheck);

            if (template.getCategories() != null) {
                for (CheckTemplateResponse.TemplateCategoryResponse cat : template.getCategories()) {
                    DailyCheckCategory category = new DailyCheckCategory();
                    category.setCheckId(dailyCheck.getId());
                    category.setCategoryId(cat.getCategoryId());
                    category.setCategoryName(cat.getCategoryName());
                    category.setLinkType(cat.getLinkType());
                    category.setIsRequired(cat.getIsRequired());
                    category.setSortOrder(cat.getSortOrder());
                    // 优先使用模板中配置的轮次（已废弃字段，保持兼容）
                    if (cat.getCheckRounds() != null && cat.getCheckRounds() > 0) {
                        category.setCheckRounds(cat.getCheckRounds());
                    } else {
                        category.setCheckRounds(1);
                    }
                    // 设置参与的轮次
                    category.setParticipatedRounds(cat.getParticipatedRounds() != null ? cat.getParticipatedRounds() : "1");
                    dailyCheckCategoryMapper.insert(category);
                }
            }
        } else if (categories != null && !categories.isEmpty()) {
            // 使用提供的类别（来自前端，已包含用户调整后的轮次）
            for (DailyCheckCreateRequest.CheckCategoryItem catItem : categories) {
                DailyCheckCategory category = new DailyCheckCategory();
                category.setCheckId(dailyCheck.getId());
                category.setCategoryId(catItem.getCategoryId());
                category.setCategoryName(catItem.getCategoryName());
                category.setLinkType(catItem.getLinkType() != null ? catItem.getLinkType() : 0);
                category.setIsRequired(catItem.getIsRequired() != null ? catItem.getIsRequired() : 1);
                category.setSortOrder(catItem.getSortOrder() != null ? catItem.getSortOrder() : 0);
                // 设置检查轮次（已废弃字段，保持兼容），使用传入值，默认为1
                category.setCheckRounds(catItem.getCheckRounds() != null && catItem.getCheckRounds() > 0
                    ? catItem.getCheckRounds() : 1);
                // 设置参与的轮次
                category.setParticipatedRounds(catItem.getParticipatedRounds() != null ? catItem.getParticipatedRounds() : "1");
                dailyCheckCategoryMapper.insert(category);
            }
        }

        log.info("日常检查创建成功: {}", dailyCheck.getId());

        // 为打分人员分配检查任务
        if (request.getPlanId() != null) {
            try {
                // 收集目标班级ID
                List<Long> targetClassIds = new ArrayList<>();
                if (request.getTargets() != null) {
                    for (DailyCheckCreateRequest.CheckTargetItem target : request.getTargets()) {
                        if ("class".equals(target.getTargetType())) {
                            targetClassIds.add(target.getTargetId());
                        }
                    }
                }

                // 收集检查类别ID (需要转换Long为String)
                List<String> categoryIds = new ArrayList<>();
                if (categories != null) {
                    categoryIds = categories.stream()
                            .map(cat -> String.valueOf(cat.getCategoryId()))
                            .collect(Collectors.toList());
                }

                // 调用任务分配
                if (!targetClassIds.isEmpty() && !categoryIds.isEmpty()) {
                    checkPlanInspectorService.assignTasksForDailyCheck(
                            dailyCheck.getId(),
                            request.getPlanId(),
                            targetClassIds,
                            categoryIds
                    );
                }
            } catch (Exception e) {
                log.error("分配检查任务失败，但不影响检查创建", e);
            }
        }

        return dailyCheck.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDailyCheck(Long id, DailyCheckCreateRequest request) {
        log.info("更新日常检查: {}", id);

        DailyCheck dailyCheck = dailyCheckMapper.selectById(id);
        if (dailyCheck == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查不存在");
        }

        // 更新检查主记录
        dailyCheck.setCheckDate(request.getCheckDate());
        dailyCheck.setCheckName(request.getCheckName());
        dailyCheck.setTemplateId(request.getTemplateId());
        dailyCheck.setCheckType(request.getCheckType());
        dailyCheck.setDescription(request.getDescription());
        // 更新全局轮次信息
        dailyCheck.setTotalRounds(request.getTotalRounds() != null ? request.getTotalRounds() : 1);
        if (request.getRoundNames() != null && !request.getRoundNames().isEmpty()) {
            try {
                dailyCheck.setRoundNames(objectMapper.writeValueAsString(request.getRoundNames()));
            } catch (Exception e) {
                log.warn("序列化轮次名称失败", e);
            }
        } else {
            dailyCheck.setRoundNames(null);
        }

        dailyCheckMapper.updateById(dailyCheck);

        // 删除旧的目标
        LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(DailyCheckTarget::getCheckId, id);
        dailyCheckTargetMapper.delete(targetWrapper);

        // 保存新的目标
        if (request.getTargets() != null && !request.getTargets().isEmpty()) {
            for (DailyCheckCreateRequest.CheckTargetItem targetItem : request.getTargets()) {
                DailyCheckTarget target = new DailyCheckTarget();
                target.setCheckId(id);
                target.setTargetType(targetItem.getTargetType());
                target.setTargetId(targetItem.getTargetId());
                target.setTargetName(targetItem.getTargetName());
                dailyCheckTargetMapper.insert(target);
            }
        }

        // 删除旧的类别
        LambdaQueryWrapper<DailyCheckCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(DailyCheckCategory::getCheckId, id);
        dailyCheckCategoryMapper.delete(categoryWrapper);

        // 保存新的类别（来自前端，已包含用户调整后的轮次）
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (DailyCheckCreateRequest.CheckCategoryItem catItem : request.getCategories()) {
                DailyCheckCategory category = new DailyCheckCategory();
                category.setCheckId(id);
                category.setCategoryId(catItem.getCategoryId());
                category.setCategoryName(catItem.getCategoryName());
                category.setLinkType(catItem.getLinkType() != null ? catItem.getLinkType() : 0);
                category.setIsRequired(catItem.getIsRequired() != null ? catItem.getIsRequired() : 1);
                category.setSortOrder(catItem.getSortOrder() != null ? catItem.getSortOrder() : 0);
                // 设置检查轮次（已废弃字段，保持兼容），使用传入值，默认为1
                category.setCheckRounds(catItem.getCheckRounds() != null && catItem.getCheckRounds() > 0
                    ? catItem.getCheckRounds() : 1);
                // 设置参与的轮次
                category.setParticipatedRounds(catItem.getParticipatedRounds() != null ? catItem.getParticipatedRounds() : "1");
                dailyCheckCategoryMapper.insert(category);
            }
        }

        log.info("日常检查更新成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDailyCheck(Long id) {
        log.info("删除日常检查: {}", id);

        DailyCheck dailyCheck = dailyCheckMapper.selectById(id);
        if (dailyCheck == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查不存在");
        }

        // 检查是否可以删除(已发布的不能删除)
        if (DailyCheckStatus.PUBLISHED.getCode().equals(dailyCheck.getStatus())) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "已发布的检查不能删除");
        }

        // 删除检查(级联删除目标和类别)
        dailyCheckMapper.deleteById(id);

        log.info("日常检查删除成功: {}", id);
    }

    @Override
    public DailyCheckResponse getDailyCheckById(Long id) {
        DailyCheck dailyCheck = dailyCheckMapper.selectById(id);
        if (dailyCheck == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查不存在");
        }

        DailyCheckResponse response = convertToResponse(dailyCheck);

        // 查询检查目标
        LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(DailyCheckTarget::getCheckId, id);
        List<DailyCheckTarget> targets = dailyCheckTargetMapper.selectList(targetWrapper);

        List<DailyCheckResponse.CheckTargetResponse> targetResponses = targets.stream()
                .map(this::convertTargetToResponse)
                .collect(Collectors.toList());
        response.setTargets(targetResponses);

        // 查询检查类别
        LambdaQueryWrapper<DailyCheckCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(DailyCheckCategory::getCheckId, id)
                      .orderByAsc(DailyCheckCategory::getSortOrder);
        List<DailyCheckCategory> categories = dailyCheckCategoryMapper.selectList(categoryWrapper);

        List<DailyCheckResponse.CheckCategoryResponse> categoryResponses = categories.stream()
                .map(cat -> {
                    DailyCheckResponse.CheckCategoryResponse cr = new DailyCheckResponse.CheckCategoryResponse();
                    cr.setId(cat.getId());
                    cr.setCategoryId(cat.getCategoryId());
                    cr.setCategoryName(cat.getCategoryName());
                    cr.setLinkType(cat.getLinkType());
                    cr.setIsRequired(cat.getIsRequired());
                    cr.setSortOrder(cat.getSortOrder());
                    cr.setCheckRounds(cat.getCheckRounds() != null ? cat.getCheckRounds() : 1);
                    // 设置参与轮次信息
                    String participatedRounds = cat.getParticipatedRounds() != null ? cat.getParticipatedRounds() : "1";
                    cr.setParticipatedRounds(participatedRounds);
                    // 解析参与轮次列表
                    List<Integer> roundsList = Arrays.stream(participatedRounds.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    cr.setParticipatedRoundsList(roundsList);

                    // 查询类别编码
                    CheckCategory category = checkCategoryMapper.selectById(cat.getCategoryId());
                    if (category != null) {
                        cr.setCategoryCode(category.getCategoryCode());
                    }

                    return cr;
                })
                .collect(Collectors.toList());
        response.setCategories(categoryResponses);

        // 如果有模板ID,查询模板名称
        if (dailyCheck.getTemplateId() != null) {
            CheckTemplate template = checkTemplateMapper.selectById(dailyCheck.getTemplateId());
            if (template != null) {
                response.setTemplateName(template.getTemplateName());
            }
        }

        return response;
    }

    @Override
    public IPage<DailyCheckResponse> getDailyCheckPage(Integer pageNum, Integer pageSize,
                                                        LocalDate checkDate, Integer status, String checkName, Long planId) {
        Page<DailyCheck> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DailyCheck> wrapper = new LambdaQueryWrapper<>();

        if (checkDate != null) {
            wrapper.eq(DailyCheck::getCheckDate, checkDate);
        }
        if (status != null) {
            wrapper.eq(DailyCheck::getStatus, status);
        }
        if (StringUtils.hasText(checkName)) {
            wrapper.like(DailyCheck::getCheckName, checkName);
        }
        if (planId != null) {
            wrapper.eq(DailyCheck::getPlanId, planId);
        }

        wrapper.orderByDesc(DailyCheck::getCheckDate, DailyCheck::getCreatedAt);

        IPage<DailyCheck> checkPage = dailyCheckMapper.selectPage(page, wrapper);

        // 转换为响应DTO
        Page<DailyCheckResponse> responsePage = new Page<>(pageNum, pageSize, checkPage.getTotal());
        List<DailyCheckResponse> records = checkPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(records);

        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckStatus(Long id, Integer status) {
        log.info("更新检查状态: id={}, status={}", id, status);

        DailyCheck dailyCheck = dailyCheckMapper.selectById(id);
        if (dailyCheck == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查不存在");
        }

        Integer oldStatus = dailyCheck.getStatus();
        dailyCheck.setStatus(status);
        dailyCheckMapper.updateById(dailyCheck);

        // 如果状态从"进行中"变为"已完成"，自动生成检查记录
        // 注意：暂不删除日常检查，因为检查记录的权重配置等功能依赖日常检查数据
        if (oldStatus == 1 && status == 2) {
            log.info("检查结束，开始生成检查记录");
            try {
                generateCheckRecordOnFinish(dailyCheck);
                log.info("检查记录生成成功");
            } catch (Exception e) {
                log.error("生成检查记录失败", e);
                // 不抛出异常，避免影响状态更新
            }
        }

        log.info("检查状态更新成功");
    }

    /**
     * 结束检查时生成检查记录
     */
    private void generateCheckRecordOnFinish(DailyCheck dailyCheck) {
        log.info("开始生成检查记录, dailyCheckId={}", dailyCheck.getId());

        try {
            // 获取检查员信息，如果日常检查没有设置，则使用当前登录用户
            Long checkerId = dailyCheck.getCheckerId();
            String checkerName = dailyCheck.getCheckerName();

            if (checkerId == null) {
                checkerId = SecurityUtils.getCurrentUserId();
                checkerName = SecurityUtils.getCurrentUsername();
                log.info("日常检查未设置检查员，使用当前用户: id={}, name={}", checkerId, checkerName);
            }

            // 调用新版CheckRecordService生成检查记录
            var record = checkRecordService.generateFromDailyCheck(
                    dailyCheck.getId(),
                    checkerId,
                    checkerName
            );
            log.info("检查记录生成成功, recordId={}", record.getId());
        } catch (Exception e) {
            log.error("生成检查记录失败, dailyCheckId={}", dailyCheck.getId(), e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "生成检查记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除日常检查及其关联数据（用于生成检查记录后清理）
     */
    private void deleteDailyCheckAndDetails(Long checkId) {
        // 删除打分明细
        LambdaQueryWrapper<DailyCheckDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(DailyCheckDetail::getCheckId, checkId);
        dailyCheckDetailMapper.delete(detailWrapper);

        // 删除检查目标
        LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(DailyCheckTarget::getCheckId, checkId);
        dailyCheckTargetMapper.delete(targetWrapper);

        // 删除检查类别
        LambdaQueryWrapper<DailyCheckCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(DailyCheckCategory::getCheckId, checkId);
        dailyCheckCategoryMapper.delete(categoryWrapper);

        // 删除日常检查主记录
        dailyCheckMapper.deleteById(checkId);
    }

    /**
     * 转换为响应DTO
     */
    private DailyCheckResponse convertToResponse(DailyCheck dailyCheck) {
        DailyCheckResponse response = new DailyCheckResponse();
        response.setId(dailyCheck.getId());
        response.setCheckDate(dailyCheck.getCheckDate());
        response.setCheckName(dailyCheck.getCheckName());
        response.setTemplateId(dailyCheck.getTemplateId());
        response.setCheckType(dailyCheck.getCheckType());
        response.setStatus(dailyCheck.getStatus());
        response.setDescription(dailyCheck.getDescription());
        response.setCreatedBy(dailyCheck.getCreatedBy());
        response.setCreatedAt(dailyCheck.getCreatedAt());
        response.setUpdatedAt(dailyCheck.getUpdatedAt());
        // 设置全局轮次信息
        response.setTotalRounds(dailyCheck.getTotalRounds() != null ? dailyCheck.getTotalRounds() : 1);
        if (StringUtils.hasText(dailyCheck.getRoundNames())) {
            try {
                List<String> roundNames = objectMapper.readValue(dailyCheck.getRoundNames(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                response.setRoundNames(roundNames);
            } catch (Exception e) {
                log.warn("解析轮次名称失败", e);
                response.setRoundNames(Collections.emptyList());
            }
        } else {
            response.setRoundNames(Collections.emptyList());
        }
        return response;
    }

    /**
     * 转换目标为响应DTO
     */
    private DailyCheckResponse.CheckTargetResponse convertTargetToResponse(DailyCheckTarget target) {
        DailyCheckResponse.CheckTargetResponse response = new DailyCheckResponse.CheckTargetResponse();
        response.setId(target.getId());
        response.setTargetType(target.getTargetType());
        response.setTargetId(target.getTargetId());
        response.setTargetName(target.getTargetName());
        return response;
    }

    @Override
    public DailyScoringInitResponse getScoringInitData(Long checkId) {
        log.info("获取打分初始化数据, checkId: {}", checkId);

        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查记录不存在");
        }

        DailyScoringInitResponse response = new DailyScoringInitResponse();
        response.setCheckId(dailyCheck.getId());
        response.setCheckName(dailyCheck.getCheckName());
        response.setCheckDate(dailyCheck.getCheckDate());
        response.setCheckType(dailyCheck.getCheckType());
        response.setCheckerId(dailyCheck.getCheckerId());
        response.setCheckerName(dailyCheck.getCheckerName());
        // 设置全局轮次信息
        response.setTotalRounds(dailyCheck.getTotalRounds() != null ? dailyCheck.getTotalRounds() : 1);
        if (StringUtils.hasText(dailyCheck.getRoundNames())) {
            try {
                List<String> roundNames = objectMapper.readValue(dailyCheck.getRoundNames(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                response.setRoundNames(roundNames);
            } catch (Exception e) {
                log.warn("解析轮次名称失败", e);
                response.setRoundNames(Collections.emptyList());
            }
        } else {
            response.setRoundNames(Collections.emptyList());
        }

        // 查询所有检查目标（班级、年级、院系）
        List<DailyCheckTarget> allTargets = dailyCheckTargetMapper.selectList(
                new LambdaQueryWrapper<DailyCheckTarget>()
                        .eq(DailyCheckTarget::getCheckId, checkId)
        );

        // 展开所有目标类型为实际班级列表（去重）
        Set<Long> classIdSet = new LinkedHashSet<>();
        Map<Long, String> classNameMap = new HashMap<>();

        for (DailyCheckTarget target : allTargets) {
            if (target.getTargetType() == 1) {
                // targetType=1: 直接选择的班级
                classIdSet.add(target.getTargetId());
                classNameMap.put(target.getTargetId(), target.getTargetName());
            } else if (target.getTargetType() == 2) {
                // targetType=2: 选择的年级（targetName格式为"2024级"）
                String gradeName = target.getTargetName();
                if (gradeName != null && gradeName.endsWith("级")) {
                    try {
                        Integer enrollmentYear = Integer.parseInt(gradeName.replace("级", ""));
                        List<com.school.management.dto.ClassResponse> gradeClasses = classMapper.selectByEnrollmentYear(enrollmentYear);
                        for (com.school.management.dto.ClassResponse cls : gradeClasses) {
                            classIdSet.add(cls.getId());
                            classNameMap.put(cls.getId(), cls.getClassName());
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无法解析年级名称: {}", gradeName);
                    }
                }
            } else if (target.getTargetType() == 3) {
                // targetType=3: 选择的院系
                List<com.school.management.dto.ClassResponse> deptClasses = classMapper.selectByDepartmentId(target.getTargetId());
                for (com.school.management.dto.ClassResponse cls : deptClasses) {
                    classIdSet.add(cls.getId());
                    classNameMap.put(cls.getId(), cls.getClassName());
                }
            }
        }

        // 批量查询班级完整信息（包含年级、部门等）
        List<DailyScoringInitResponse.TargetClassInfo> targetClasses;
        if (!classIdSet.isEmpty()) {
            List<com.school.management.dto.ClassResponse> classResponseList = classMapper.selectClassResponseByIds(new ArrayList<>(classIdSet));
            targetClasses = classResponseList.stream()
                    .map(cls -> {
                        DailyScoringInitResponse.TargetClassInfo classInfo = new DailyScoringInitResponse.TargetClassInfo();
                        classInfo.setClassId(cls.getId());
                        classInfo.setClassName(cls.getClassName());
                        classInfo.setGradeId(cls.getGradeId());
                        classInfo.setGradeName(cls.getGrade());  // 如"2023级"
                        classInfo.setDepartmentId(cls.getDepartmentId());
                        classInfo.setDepartmentName(cls.getDepartmentName());
                        classInfo.setStudentCount(cls.getStudentCount());
                        return classInfo;
                    })
                    .collect(Collectors.toList());
        } else {
            targetClasses = new ArrayList<>();
        }
        response.setTargetClasses(targetClasses);
        log.info("展开检查目标，共{}个班级", targetClasses.size());

        // 查询检查类别及扣分项
        List<DailyCheckCategory> categories = dailyCheckCategoryMapper.selectList(
                new LambdaQueryWrapper<DailyCheckCategory>()
                        .eq(DailyCheckCategory::getCheckId, checkId)
                        .orderByAsc(DailyCheckCategory::getSortOrder)
        );

        List<DailyScoringInitResponse.CategoryInfo> categoryInfos = new ArrayList<>();
        Map<Long, DailyScoringInitResponse.LinkResourceInfo> linkResourcesMap = new HashMap<>();

        for (DailyCheckCategory category : categories) {
            DailyScoringInitResponse.CategoryInfo categoryInfo = new DailyScoringInitResponse.CategoryInfo();
            categoryInfo.setId(category.getId());
            categoryInfo.setCategoryId(category.getCategoryId());
            categoryInfo.setCategoryName(category.getCategoryName());
            categoryInfo.setLinkType(category.getLinkType());
            categoryInfo.setCheckRounds(category.getCheckRounds() != null ? category.getCheckRounds() : 1);
            categoryInfo.setSortOrder(category.getSortOrder());
            // 设置参与轮次信息
            String participatedRounds = category.getParticipatedRounds() != null ? category.getParticipatedRounds() : "1";
            categoryInfo.setParticipatedRounds(participatedRounds);
            // 解析参与轮次列表
            List<Integer> roundsList = Arrays.stream(participatedRounds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            categoryInfo.setParticipatedRoundsList(roundsList);

            // 查询扣分项
            List<DeductionItem> deductionItems = deductionItemMapper.selectList(
                    new LambdaQueryWrapper<DeductionItem>()
                            .eq(DeductionItem::getTypeId, category.getCategoryId())
                            .eq(DeductionItem::getDeleted, 0)
                            .eq(DeductionItem::getStatus, 1)
                            .orderByAsc(DeductionItem::getSortOrder)
            );

            List<DailyScoringInitResponse.DeductionItemInfo> itemInfos = deductionItems.stream()
                    .map(item -> {
                        DailyScoringInitResponse.DeductionItemInfo itemInfo = new DailyScoringInitResponse.DeductionItemInfo();
                        itemInfo.setId(item.getId());
                        itemInfo.setItemName(item.getItemName());
                        itemInfo.setDeductMode(item.getDeductMode());
                        itemInfo.setFixedScore(item.getFixedScore());
                        itemInfo.setBaseScore(item.getBaseScore());
                        itemInfo.setPerPersonScore(item.getPerPersonScore());
                        itemInfo.setRangeConfig(item.getRangeConfig());
                        itemInfo.setDescription(item.getDescription());
                        itemInfo.setAllowPhoto(item.getAllowPhoto());
                        itemInfo.setAllowRemark(item.getAllowRemark());
                        itemInfo.setAllowStudents(item.getAllowStudents());
                        itemInfo.setSortOrder(item.getSortOrder());
                        return itemInfo;
                    })
                    .collect(Collectors.toList());
            categoryInfo.setDeductionItems(itemInfos);
            categoryInfos.add(categoryInfo);

            // 如果关联了宿舍或教室，查询对应资源
            if (category.getLinkType() != null && category.getLinkType() > 0) {
                DailyScoringInitResponse.LinkResourceInfo linkResource = new DailyScoringInitResponse.LinkResourceInfo();
                linkResource.setLinkType(category.getLinkType());
                List<DailyScoringInitResponse.ClassLinkResource> classResources = new ArrayList<>();

                for (DailyScoringInitResponse.TargetClassInfo targetClass : targetClasses) {
                    DailyScoringInitResponse.ClassLinkResource classResource = new DailyScoringInitResponse.ClassLinkResource();
                    classResource.setClassId(targetClass.getClassId());
                    classResource.setClassName(targetClass.getClassName());

                    if (category.getLinkType() == 1) {
                        // 查询宿舍
                        List<ClassDormitoryBinding> bindings = classDormitoryBindingMapper.selectList(
                                new LambdaQueryWrapper<ClassDormitoryBinding>()
                                        .eq(ClassDormitoryBinding::getClassId, targetClass.getClassId())
                        );

                        List<DailyScoringInitResponse.DormitoryInfo> dormitories = new ArrayList<>();
                        for (ClassDormitoryBinding binding : bindings) {
                            Dormitory dormitory = dormitoryMapper.selectById(binding.getDormitoryId());
                            if (dormitory != null && dormitory.getDeleted() == 0) {
                                DailyScoringInitResponse.DormitoryInfo dormInfo = new DailyScoringInitResponse.DormitoryInfo();
                                dormInfo.setId(dormitory.getId());
                                dormInfo.setDormitoryNo(dormitory.getDormitoryNo());
                                dormInfo.setFloor(dormitory.getFloorNumber());
                                if (dormitory.getBuildingId() != null) {
                                    Building building = buildingMapper.selectById(dormitory.getBuildingId());
                                    if (building != null) {
                                        dormInfo.setBuildingName(building.getBuildingName());
                                    }
                                }
                                dormitories.add(dormInfo);
                            }
                        }
                        classResource.setDormitories(dormitories);
                    } else if (category.getLinkType() == 2) {
                        // 查询教室
                        List<Classroom> classrooms = classroomMapper.selectList(
                                new LambdaQueryWrapper<Classroom>()
                                        .eq(Classroom::getClassId, targetClass.getClassId())
                                        .eq(Classroom::getDeleted, 0)
                        );

                        List<DailyScoringInitResponse.ClassroomInfo> classroomInfos = classrooms.stream()
                                .map(classroom -> {
                                    DailyScoringInitResponse.ClassroomInfo classroomInfo = new DailyScoringInitResponse.ClassroomInfo();
                                    classroomInfo.setId(classroom.getId());
                                    classroomInfo.setClassroomNo(classroom.getClassroomCode());
                                    classroomInfo.setFloor(classroom.getFloor());
                                    if (classroom.getBuildingId() != null) {
                                        Building building = buildingMapper.selectById(classroom.getBuildingId());
                                        if (building != null) {
                                            classroomInfo.setBuildingName(building.getBuildingName());
                                        }
                                    }
                                    return classroomInfo;
                                })
                                .collect(Collectors.toList());
                        classResource.setClassrooms(classroomInfos);
                    }
                    classResources.add(classResource);
                }
                linkResource.setClassResources(classResources);
                linkResourcesMap.put(category.getCategoryId(), linkResource);
            }
        }

        response.setCategories(categoryInfos);
        response.setLinkResources(linkResourcesMap);

        // 查询已有打分明细
        List<DailyCheckDetail> existingDetails = dailyCheckDetailMapper.selectList(
                new LambdaQueryWrapper<DailyCheckDetail>()
                        .eq(DailyCheckDetail::getCheckId, checkId)
                        .eq(DailyCheckDetail::getDeleted, 0)
        );

        List<DailyScoringInitResponse.ScoringDetailInfo> detailInfos = existingDetails.stream()
                .map(detail -> {
                    DailyScoringInitResponse.ScoringDetailInfo detailInfo = new DailyScoringInitResponse.ScoringDetailInfo();
                    detailInfo.setId(detail.getId());
                    detailInfo.setCheckRound(detail.getCheckRound() != null ? detail.getCheckRound() : 1);
                    detailInfo.setCategoryId(detail.getCategoryId());
                    detailInfo.setClassId(detail.getClassId());
                    detailInfo.setDeductionItemId(detail.getDeductionItemId());
                    detailInfo.setDeductionItemName(detail.getDeductionItemName());
                    detailInfo.setDeductMode(detail.getDeductMode());
                    detailInfo.setLinkType(detail.getLinkType());
                    detailInfo.setLinkId(detail.getLinkId());
                    detailInfo.setLinkNo(detail.getLinkNo());
                    detailInfo.setDeductScore(detail.getDeductScore());
                    detailInfo.setPersonCount(detail.getPersonCount());
                    detailInfo.setStudentIds(detail.getStudentIds());
                    detailInfo.setStudentNames(detail.getStudentNames());
                    detailInfo.setDescription(detail.getDescription());
                    detailInfo.setRemark(detail.getRemark());
                    detailInfo.setPhotoUrls(detail.getPhotoUrls());
                    detailInfo.setAppealStatus(detail.getAppealStatus());
                    return detailInfo;
                })
                .collect(Collectors.toList());
        response.setExistingDetails(detailInfos);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScoring(Long checkId, DailyScoringRequest request) {
        log.info("保存打分数据, checkId: {}, version: {}", checkId, request.getVersion());

        // 查询当前记录
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查记录不存在");
        }

        // ⚠️ 乐观锁版本检查 - 防止并发覆盖
        // 前端必须传递获取数据时的version值，用于检测是否有其他用户修改
        if (request.getVersion() != null) {
            Integer dbVersion = dailyCheck.getVersion();
            if (dbVersion != null && !request.getVersion().equals(dbVersion)) {
                log.warn("【并发冲突】保存打分数据版本不匹配: checkId={}, requestVersion={}, dbVersion={}",
                        checkId, request.getVersion(), dbVersion);
                throw new BusinessException(ResultCode.CONFLICT,
                        "数据已被其他用户修改，请刷新页面后重试");
            }
        }

        // 更新检查人信息
        if (request.getCheckerId() != null) {
            dailyCheck.setCheckerId(request.getCheckerId());
            dailyCheck.setCheckerName(request.getCheckerName());
        }

        // 使用MyBatis Plus的@Version乐观锁更新
        // updateById会自动检查version并递增，如果version不匹配则更新0行
        int updateCount = dailyCheckMapper.updateById(dailyCheck);
        if (updateCount == 0) {
            log.warn("【并发冲突】乐观锁更新失败: checkId={}", checkId);
            throw new BusinessException(ResultCode.CONFLICT,
                    "数据已被其他用户修改，请刷新页面后重试");
        }

        // 使用批量操作替代逐条删除和插入，减少锁竞争
        // 先删除该检查记录的所有已有打分明细(覆盖式保存)
        LambdaQueryWrapper<DailyCheckDetail> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DailyCheckDetail::getCheckId, checkId);
        dailyCheckDetailMapper.delete(deleteWrapper);
        log.info("已删除检查记录 {} 的所有旧打分明细", checkId);

        // 保存新的打分明细
        for (ScoringDetailRequest detailRequest : request.getDetails()) {
            DailyCheckDetail detail = new DailyCheckDetail();
            detail.setCheckId(checkId);
            detail.setCheckRound(detailRequest.getCheckRound() != null ? detailRequest.getCheckRound() : 1);
            detail.setCategoryId(detailRequest.getCategoryId());
            detail.setClassId(detailRequest.getClassId());
            detail.setDeductionItemId(detailRequest.getDeductionItemId());
            detail.setLinkType(detailRequest.getLinkType());
            detail.setLinkId(detailRequest.getLinkId());
            detail.setDeductScore(detailRequest.getDeductScore());
            detail.setPersonCount(detailRequest.getPersonCount());
            detail.setStudentIds(detailRequest.getStudentIds());
            detail.setStudentNames(detailRequest.getStudentNames());
            detail.setDescription(detailRequest.getDescription());
            detail.setRemark(detailRequest.getRemark());
            detail.setPhotoUrls(detailRequest.getPhotoUrls());
            detail.setCheckerId(request.getCheckerId());
            detail.setAppealStatus(0);

            DeductionItem deductionItem = deductionItemMapper.selectById(detailRequest.getDeductionItemId());
            if (deductionItem != null) {
                detail.setDeductionItemName(deductionItem.getItemName());
                detail.setDeductMode(deductionItem.getDeductMode());

                // ⚠️ 后端验证：校验前端传入的扣分分数是否符合配置规则
                validateDeductScore(deductionItem, detailRequest.getDeductScore(), detailRequest.getPersonCount());
            }

            if (detailRequest.getLinkType() != null && detailRequest.getLinkId() != null) {
                if (detailRequest.getLinkType() == 1) {
                    Dormitory dormitory = dormitoryMapper.selectById(detailRequest.getLinkId());
                    if (dormitory != null) {
                        detail.setLinkNo(dormitory.getDormitoryNo());
                    }
                } else if (detailRequest.getLinkType() == 2) {
                    Classroom classroom = classroomMapper.selectById(detailRequest.getLinkId());
                    if (classroom != null) {
                        detail.setLinkNo(classroom.getClassroomCode());
                    }
                }
            }

            dailyCheckDetailMapper.insert(detail);
        }
    }

    /**
     * 验证扣分分数是否符合扣分项配置规则
     *
     * @param item 扣分项配置
     * @param deductScore 前端传入的扣分分数
     * @param personCount 涉及人数（模式2使用）
     */
    private void validateDeductScore(DeductionItem item, BigDecimal deductScore, Integer personCount) {
        if (deductScore == null || deductScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "扣分分数不能为空或负数");
        }

        Integer deductMode = item.getDeductMode();
        if (deductMode == null) {
            log.warn("扣分项未配置扣分模式: itemId={}", item.getId());
            return; // 兼容旧数据
        }

        switch (deductMode) {
            case 1: // 固定扣分
                validateFixedScore(item, deductScore);
                break;
            case 2: // 按人数扣分
                validatePerPersonScore(item, deductScore, personCount);
                break;
            case 3: // 区间扣分
                validateRangeScore(item, deductScore);
                break;
            default:
                log.warn("未知的扣分模式: {}", deductMode);
        }
    }

    /**
     * 验证固定扣分模式
     */
    private void validateFixedScore(DeductionItem item, BigDecimal deductScore) {
        BigDecimal fixedScore = item.getFixedScore();
        if (fixedScore == null) {
            log.warn("固定扣分模式未配置扣分值: itemId={}", item.getId());
            return;
        }

        // 允许1%的浮点误差
        BigDecimal tolerance = fixedScore.multiply(new BigDecimal("0.01"));
        if (deductScore.subtract(fixedScore).abs().compareTo(tolerance) > 0) {
            log.warn("【安全警告】固定扣分验证失败: itemId={}, itemName={}, 配置={}, 传入={}",
                    item.getId(), item.getItemName(), fixedScore, deductScore);
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    String.format("扣分项【%s】应扣分%.1f分，传入值不匹配", item.getItemName(), fixedScore));
        }
    }

    /**
     * 验证按人数扣分模式
     */
    private void validatePerPersonScore(DeductionItem item, BigDecimal deductScore, Integer personCount) {
        BigDecimal baseScore = item.getBaseScore() != null ? item.getBaseScore() : BigDecimal.ZERO;
        BigDecimal perPersonScore = item.getPerPersonScore() != null ? item.getPerPersonScore() : BigDecimal.ZERO;
        int count = personCount != null ? personCount : 1;

        // 计算预期扣分: baseScore + personCount * perPersonScore
        BigDecimal expectedScore = baseScore.add(perPersonScore.multiply(new BigDecimal(count)));

        // 允许1%的浮点误差
        BigDecimal tolerance = expectedScore.multiply(new BigDecimal("0.01")).max(new BigDecimal("0.1"));
        if (deductScore.subtract(expectedScore).abs().compareTo(tolerance) > 0) {
            log.warn("【安全警告】按人数扣分验证失败: itemId={}, itemName={}, 人数={}, 预期={}, 传入={}",
                    item.getId(), item.getItemName(), count, expectedScore, deductScore);
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    String.format("扣分项【%s】按%d人计算应扣分%.1f分，传入值不匹配",
                            item.getItemName(), count, expectedScore));
        }
    }

    /**
     * 验证区间扣分模式
     */
    private void validateRangeScore(DeductionItem item, BigDecimal deductScore) {
        String rangeConfig = item.getRangeConfig();
        if (rangeConfig == null || rangeConfig.isEmpty()) {
            log.warn("区间扣分模式未配置区间: itemId={}", item.getId());
            return;
        }

        try {
            // 解析区间配置 JSON: [{"min": 0, "max": 10}]
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> ranges = mapper.readValue(rangeConfig,
                    mapper.getTypeFactory().constructCollectionType(java.util.List.class, java.util.Map.class));

            boolean inRange = false;
            for (java.util.Map<String, Object> range : ranges) {
                BigDecimal min = new BigDecimal(range.get("min").toString());
                BigDecimal max = new BigDecimal(range.get("max").toString());
                if (deductScore.compareTo(min) >= 0 && deductScore.compareTo(max) <= 0) {
                    inRange = true;
                    break;
                }
            }

            if (!inRange) {
                log.warn("【安全警告】区间扣分验证失败: itemId={}, itemName={}, 配置={}, 传入={}",
                        item.getId(), item.getItemName(), rangeConfig, deductScore);
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        String.format("扣分项【%s】的扣分值不在允许范围内", item.getItemName()));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析区间配置失败: itemId={}, config={}", item.getId(), rangeConfig, e);
            // 配置错误时不阻止操作，但记录警告
        }
    }

    /**
     * 应用默认加权配置
     */
    private void applyDefaultWeightConfig(DailyCheck dailyCheck) {
        try {
            // 查找默认加权配置
            LambdaQueryWrapper<ClassWeightConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ClassWeightConfig::getIsDefault, 1)
                    .eq(ClassWeightConfig::getDeleted, 0);
            ClassWeightConfig defaultConfig = classWeightConfigMapper.selectOne(wrapper);

            if (defaultConfig != null && defaultConfig.getEnableWeight() != null && defaultConfig.getEnableWeight() == 1) {
                log.info("应用默认加权配置: {} (ID: {})", defaultConfig.getConfigName(), defaultConfig.getId());

                // 设置加权启用状态
                dailyCheck.setEnableWeight(1);
                dailyCheck.setWeightConfigId(defaultConfig.getId());

                // 保存配置快照（序列化为JSON）
                String configSnapshot = objectMapper.writeValueAsString(defaultConfig);
                dailyCheck.setWeightConfigSnapshot(configSnapshot);

                log.info("加权配置快照已保存");
            } else {
                // 没有默认配置或未启用加权
                dailyCheck.setEnableWeight(0);
                log.info("未找到默认加权配置或加权未启用");
            }
        } catch (Exception e) {
            log.warn("应用默认加权配置失败，将使用无加权模式: {}", e.getMessage());
            dailyCheck.setEnableWeight(0);
        }
    }


    /**
     * 从检查计划中读取加权配置并保存到daily_check_weight_configs表
     * 注意：新架构下加权配置从CheckPlan中获取，而不是从模板中获取
     * 支持多加权配置模式：不同的分类可以使用不同的加权方案
     */
    private void saveMultiWeightConfigs(DailyCheck dailyCheck) {
        if (dailyCheck.getPlanId() == null) {
            log.info("日常检查未关联检查计划，跳过加权配置保存");
            return;
        }

        try {
            CheckPlan plan = checkPlanMapper.selectById(dailyCheck.getPlanId());
            if (plan == null) {
                log.warn("检查计划不存在: {}", dailyCheck.getPlanId());
                return;
            }

            if (plan.getEnableWeight() == null || plan.getEnableWeight() != 1) {
                log.info("检查计划未启用加权，跳过加权配置保存");
                return;
            }

            Long defaultWeightConfigId = plan.getWeightConfigId();
            String itemWeightConfigsJson = plan.getItemWeightConfigs();
            List<ItemWeightConfigDTO> itemWeightConfigs = new ArrayList<>();

            if (StringUtils.hasText(itemWeightConfigsJson)) {
                try {
                    itemWeightConfigs = objectMapper.readValue(
                        itemWeightConfigsJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ItemWeightConfigDTO.class)
                    );
                    log.info("解析到 {} 个分类加权配置", itemWeightConfigs.size());
                } catch (Exception e) {
                    log.warn("解析分类加权配置JSON失败: {}", e.getMessage());
                }
            }

            Map<Long, Set<Long>> configToCategoryMap = new LinkedHashMap<>();

            for (ItemWeightConfigDTO itemConfig : itemWeightConfigs) {
                Long configId = itemConfig.getWeightConfigId();
                Long categoryId = itemConfig.getCategoryId();

                if (configId != null && categoryId != null) {
                    configToCategoryMap.computeIfAbsent(configId, k -> new LinkedHashSet<>()).add(categoryId);
                }
            }

            String[][] colorOptions = {
                {"#1890ff", "蓝色"},
                {"#52c41a", "绿色"},
                {"#faad14", "橙色"},
                {"#eb2f96", "粉色"},
                {"#722ed1", "紫色"},
                {"#13c2c2", "青色"}
            };

            int sortOrder = 0;
            int colorIndex = 0;

            if (!configToCategoryMap.isEmpty()) {
                for (Map.Entry<Long, Set<Long>> entry : configToCategoryMap.entrySet()) {
                    Long weightConfigId = entry.getKey();
                    Set<Long> categoryIds = entry.getValue();

                    ClassWeightConfig weightConfig = classWeightConfigMapper.selectById(weightConfigId);
                    if (weightConfig == null || weightConfig.getDeleted() == 1) {
                        log.warn("加权配置不存在或已删除: {}", weightConfigId);
                        continue;
                    }

                    DailyCheckWeightConfig config = new DailyCheckWeightConfig();
                    config.setDailyCheckId(dailyCheck.getId());
                    config.setWeightConfigId(weightConfigId);

                    String[] color = colorOptions[colorIndex % colorOptions.length];
                    config.setColorCode(color[0]);
                    config.setColorName(color[1]);
                    colorIndex++;

                    config.setApplyScope("CATEGORY");
                    String categoryIdsJson = objectMapper.writeValueAsString(categoryIds);
                    config.setApplyCategoryIds(categoryIdsJson);

                    config.setIsDefault(sortOrder == 0 ? 1 : 0);
                    config.setPriority(sortOrder + 1);
                    config.setSortOrder(sortOrder + 1);
                    sortOrder++;

                    String configSnapshot = objectMapper.writeValueAsString(weightConfig);
                    config.setWeightConfigSnapshot(configSnapshot);
                    config.setCalculatedStandardSize(plan.getCustomStandardSize());

                    dailyCheckWeightConfigMapper.insert(config);
                    log.info("保存分类加权配置: configName={}, categoryIds={}",
                            weightConfig.getConfigName(), categoryIds);
                }

                log.info("多加权配置保存完成，共 {} 个配置", configToCategoryMap.size());
            } else if (defaultWeightConfigId != null) {
                ClassWeightConfig weightConfig = classWeightConfigMapper.selectById(defaultWeightConfigId);
                if (weightConfig == null || weightConfig.getDeleted() == 1) {
                    log.warn("默认加权配置不存在或已删除: {}", defaultWeightConfigId);
                    return;
                }

                DailyCheckWeightConfig config = new DailyCheckWeightConfig();
                config.setDailyCheckId(dailyCheck.getId());
                config.setWeightConfigId(defaultWeightConfigId);
                config.setColorCode("#1890ff");
                config.setColorName("蓝色");
                config.setApplyScope("ALL");
                config.setApplyCategoryIds("[]");
                config.setIsDefault(1);
                config.setPriority(1);
                config.setSortOrder(1);

                String configSnapshot = objectMapper.writeValueAsString(weightConfig);
                config.setWeightConfigSnapshot(configSnapshot);
                config.setCalculatedStandardSize(plan.getCustomStandardSize());

                dailyCheckWeightConfigMapper.insert(config);
                log.info("保存默认加权配置: {}", weightConfig.getConfigName());
            } else {
                log.info("检查计划未配置任何加权方案，跳过加权配置保存");
            }

        } catch (Exception e) {
            log.error("保存多加权配置失败", e);
        }
    }

    /**
     * 分类加权配置DTO（用于解析JSON）
     */
    @lombok.Data
    private static class ItemWeightConfigDTO {
        private Long itemId;
        private Long categoryId;
        private Long weightConfigId;
    }
}
