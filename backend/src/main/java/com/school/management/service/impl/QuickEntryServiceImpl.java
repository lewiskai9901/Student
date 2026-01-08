package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.quickentry.*;
import com.school.management.entity.*;
import com.school.management.entity.Class;
import com.school.management.enums.DailyCheckStatus;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.QuickEntryService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 快捷录入服务实现类
 *
 * @author system
 * @since 1.0.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuickEntryServiceImpl implements QuickEntryService {

    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckTargetMapper dailyCheckTargetMapper;
    private final DailyCheckCategoryMapper dailyCheckCategoryMapper;
    private final DailyCheckDetailMapper dailyCheckDetailMapper;
    private final DeductionItemMapper deductionItemMapper;
    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;
    private final UserMapper userMapper;

    @Override
    public List<QuickEntryDeductionItemDTO> getAvailableDeductionItems(Long checkId) {
        log.info("获取快捷录入可用扣分项, checkId: {}", checkId);

        // 验证检查是否存在
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查记录不存在");
        }

        // 查询检查关联的类别
        List<DailyCheckCategory> categories = dailyCheckCategoryMapper.selectList(
                new LambdaQueryWrapper<DailyCheckCategory>()
                        .eq(DailyCheckCategory::getCheckId, checkId)
                        .orderByAsc(DailyCheckCategory::getSortOrder)
        );

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有类别ID
        List<Long> categoryIds = categories.stream()
                .map(DailyCheckCategory::getCategoryId)
                .collect(Collectors.toList());

        // 创建类别ID到名称的映射
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(
                        DailyCheckCategory::getCategoryId,
                        DailyCheckCategory::getCategoryName,
                        (v1, v2) -> v1
                ));

        // 创建类别ID到参与轮次的映射
        Map<Long, String> categoryParticipatedRoundsMap = categories.stream()
                .collect(Collectors.toMap(
                        DailyCheckCategory::getCategoryId,
                        cat -> cat.getParticipatedRounds() != null ? cat.getParticipatedRounds() : "1",
                        (v1, v2) -> v1
                ));

        // 查询"按人次扣分"模式的扣分项 (deductMode = 2)
        List<DeductionItem> deductionItems = deductionItemMapper.selectList(
                new LambdaQueryWrapper<DeductionItem>()
                        .in(DeductionItem::getTypeId, categoryIds)
                        .eq(DeductionItem::getDeductMode, 2)  // 按人次扣分
                        .eq(DeductionItem::getDeleted, 0)
                        .eq(DeductionItem::getStatus, 1)
                        .orderByAsc(DeductionItem::getSortOrder)
        );

        // 转换为DTO
        return deductionItems.stream()
                .map(item -> {
                    QuickEntryDeductionItemDTO dto = new QuickEntryDeductionItemDTO();
                    dto.setId(item.getId());
                    dto.setItemName(item.getItemName());
                    dto.setCategoryId(item.getTypeId());
                    dto.setCategoryName(categoryNameMap.get(item.getTypeId()));
                    dto.setBaseScore(item.getBaseScore());
                    dto.setPerPersonScore(item.getPerPersonScore());
                    dto.setAllowPhoto(item.getAllowPhoto());
                    dto.setAllowRemark(item.getAllowRemark());
                    dto.setDescription(item.getDescription());
                    // 设置参与轮次信息
                    String participatedRounds = categoryParticipatedRoundsMap.get(item.getTypeId());
                    dto.setParticipatedRounds(participatedRounds);
                    // 解析为列表
                    if (participatedRounds != null && !participatedRounds.isEmpty()) {
                        List<Integer> roundsList = Arrays.stream(participatedRounds.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                        dto.setParticipatedRoundsList(roundsList);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<QuickEntryStudentDTO> searchStudents(Long checkId, String keyword, Integer limit) {
        log.info("快捷录入搜索学生, checkId: {}, keyword: {}", checkId, keyword);

        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        // 验证检查是否存在
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查记录不存在");
        }

        // 获取检查目标班级列表
        Set<Long> targetClassIds = getTargetClassIds(checkId);
        if (targetClassIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 搜索学生 (姓名或学号匹配)
        int searchLimit = limit != null && limit > 0 ? limit : 20;
        List<QuickEntryStudentDTO> results = studentMapper.searchStudentsForQuickEntry(
                keyword, new ArrayList<>(targetClassIds), searchLimit);

        return results;
    }

    @Override
    public QuickEntryCheckDuplicateResponse checkDuplicate(Long checkId, QuickEntryCheckDuplicateRequest request) {
        log.info("检查快捷录入重复, checkId: {}, deductionItemId: {}, studentId: {}, checkRound: {}",
                checkId, request.getDeductionItemId(), request.getStudentId(), request.getCheckRound());

        // 获取学生信息以确定班级
        Student student = studentMapper.selectById(request.getStudentId());
        if (student == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学生不存在");
        }

        // 查询是否存在相同的记录
        LambdaQueryWrapper<DailyCheckDetail> queryWrapper = new LambdaQueryWrapper<DailyCheckDetail>()
                .eq(DailyCheckDetail::getCheckId, checkId)
                .eq(DailyCheckDetail::getDeductionItemId, request.getDeductionItemId())
                .eq(DailyCheckDetail::getClassId, student.getClassId())
                .eq(DailyCheckDetail::getDeleted, 0)
                .like(DailyCheckDetail::getStudentIds, String.valueOf(request.getStudentId()));

        // 如果指定了轮次，则只检查该轮次
        if (request.getCheckRound() != null) {
            queryWrapper.eq(DailyCheckDetail::getCheckRound, request.getCheckRound());
        }

        List<DailyCheckDetail> existingDetails = dailyCheckDetailMapper.selectList(queryWrapper);

        // 检查是否真的包含该学生ID
        for (DailyCheckDetail detail : existingDetails) {
            if (detail.getStudentIds() != null) {
                String[] ids = detail.getStudentIds().split(",");
                for (String id : ids) {
                    if (id.trim().equals(String.valueOf(request.getStudentId()))) {
                        // 查询操作人
                        String operatorName = "未知";
                        if (detail.getCheckerId() != null) {
                            User operator = userMapper.selectById(detail.getCheckerId());
                            if (operator != null) {
                                operatorName = operator.getRealName();
                            }
                        }
                        return QuickEntryCheckDuplicateResponse.duplicate(
                                detail.getId(),
                                detail.getCreatedAt(),
                                operatorName
                        );
                    }
                }
            }
        }

        return QuickEntryCheckDuplicateResponse.noDuplicate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuickEntryRecordDTO submitEntry(Long checkId, QuickEntrySubmitRequest request, Long operatorId) {
        log.info("快捷录入提交, checkId: {}, deductionItemId: {}, studentId: {}",
                checkId, request.getDeductionItemId(), request.getStudentId());

        // 验证检查是否存在且状态正确
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查记录不存在");
        }
        if (DailyCheckStatus.PUBLISHED.getCode().equals(dailyCheck.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "检查已发布，无法录入");
        }

        // 获取扣分项信息
        DeductionItem deductionItem = deductionItemMapper.selectById(request.getDeductionItemId());
        if (deductionItem == null || deductionItem.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "扣分项不存在");
        }
        if (deductionItem.getDeductMode() != 2) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "该扣分项不支持快捷录入");
        }

        // 获取学生信息
        Student student = studentMapper.selectById(request.getStudentId());
        if (student == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学生不存在");
        }

        // 获取学生姓名
        User studentUser = userMapper.selectById(student.getUserId());
        String studentName = studentUser != null ? studentUser.getRealName() : "未知";

        // 验证学生班级是否在检查范围内
        Set<Long> targetClassIds = getTargetClassIds(checkId);
        if (!targetClassIds.contains(student.getClassId())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "该学生不在检查范围内");
        }

        // 获取班级名称
        Class studentClass = classMapper.selectById(student.getClassId());
        String className = studentClass != null ? studentClass.getClassName() : "未知班级";

        // 获取类别信息
        DailyCheckCategory category = dailyCheckCategoryMapper.selectOne(
                new LambdaQueryWrapper<DailyCheckCategory>()
                        .eq(DailyCheckCategory::getCheckId, checkId)
                        .eq(DailyCheckCategory::getCategoryId, deductionItem.getTypeId())
        );
        if (category == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "扣分项类别不在本次检查范围内");
        }

        // 计算扣分 (按人次: 基础分 + 每人分数 * 1)
        BigDecimal deductScore = deductionItem.getBaseScore() != null ? deductionItem.getBaseScore() : BigDecimal.ZERO;
        if (deductionItem.getPerPersonScore() != null) {
            deductScore = deductScore.add(deductionItem.getPerPersonScore());
        }

        // 创建扣分明细记录
        DailyCheckDetail detail = new DailyCheckDetail();
        detail.setCheckId(checkId);
        detail.setCheckRound(request.getCheckRound() != null ? request.getCheckRound() : 1); // 使用传入的轮次
        detail.setCategoryId(deductionItem.getTypeId());
        detail.setClassId(student.getClassId());
        detail.setDeductionItemId(request.getDeductionItemId());
        detail.setDeductionItemName(deductionItem.getItemName());
        detail.setDeductMode(2); // 按人次扣分
        detail.setLinkType(0); // 无关联
        detail.setDeductScore(deductScore);
        detail.setPersonCount(1);
        detail.setStudentIds(String.valueOf(request.getStudentId()));
        detail.setStudentNames(studentName);
        detail.setCheckerId(operatorId);
        detail.setCheckTime(LocalDateTime.now());

        // 设置备注
        if (StringUtils.hasText(request.getRemark())) {
            detail.setRemark(request.getRemark());
            detail.setDescription(request.getRemark());
        }

        // 设置图片
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            String photoUrlsStr = String.join(",", request.getPhotoUrls());
            detail.setImages(photoUrlsStr);
            detail.setPhotoUrls(photoUrlsStr);
        }

        dailyCheckDetailMapper.insert(detail);

        // 更新检查状态为进行中
        if (DailyCheckStatus.NOT_STARTED.getCode().equals(dailyCheck.getStatus())) {
            dailyCheck.setStatus(DailyCheckStatus.IN_PROGRESS.getCode());
            dailyCheckMapper.updateById(dailyCheck);
        }

        // 构建返回结果
        QuickEntryRecordDTO result = new QuickEntryRecordDTO();
        result.setId(detail.getId());
        result.setDeductionItemId(request.getDeductionItemId());
        result.setDeductionItemName(deductionItem.getItemName());
        result.setCategoryName(category.getCategoryName());
        result.setStudentId(request.getStudentId());
        result.setStudentName(studentName);
        result.setStudentNo(student.getStudentNo());
        result.setClassId(student.getClassId());
        result.setClassName(className);
        result.setDeductScore(deductScore);
        result.setRemark(request.getRemark());
        result.setPhotoUrls(request.getPhotoUrls());
        result.setCreatedAt(LocalDateTime.now());
        result.setCanRevoke(true);

        log.info("快捷录入成功, recordId: {}", detail.getId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeEntry(Long checkId, Long recordId, Long operatorId) {
        log.info("撤销快捷录入, checkId: {}, recordId: {}, operatorId: {}", checkId, recordId, operatorId);

        // 验证检查是否存在
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查记录不存在");
        }
        if (DailyCheckStatus.PUBLISHED.getCode().equals(dailyCheck.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "检查已发布，无法撤销");
        }

        // 获取明细记录
        DailyCheckDetail detail = dailyCheckDetailMapper.selectById(recordId);
        if (detail == null || detail.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "记录不存在");
        }

        // 验证是否属于该检查
        if (!detail.getCheckId().equals(checkId)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "记录不属于该检查");
        }

        // 验证是否是当天录入的
        if (detail.getCreatedAt() != null) {
            LocalDate recordDate = detail.getCreatedAt().toLocalDate();
            if (!recordDate.equals(LocalDate.now())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "仅支持撤销当天录入的记录");
            }
        }

        // 验证是否是本人录入的
        if (detail.getCheckerId() != null && !detail.getCheckerId().equals(operatorId)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "仅支持撤销本人录入的记录");
        }

        // 逻辑删除
        detail.setDeleted(1);
        dailyCheckDetailMapper.updateById(detail);

        log.info("撤销快捷录入成功, recordId: {}", recordId);
    }

    @Override
    public List<QuickEntryRecordDTO> getMyEntryRecords(Long checkId, Long operatorId) {
        log.info("获取快捷录入记录列表, checkId: {}, operatorId: {}", checkId, operatorId);

        // 查询明细记录 (按人次扣分模式)
        LambdaQueryWrapper<DailyCheckDetail> wrapper = new LambdaQueryWrapper<DailyCheckDetail>()
                .eq(DailyCheckDetail::getCheckId, checkId)
                .eq(DailyCheckDetail::getDeductMode, 2)  // 按人次扣分
                .eq(DailyCheckDetail::getDeleted, 0)
                .orderByDesc(DailyCheckDetail::getCreatedAt);

        if (operatorId != null) {
            wrapper.eq(DailyCheckDetail::getCheckerId, operatorId);
        }

        List<DailyCheckDetail> details = dailyCheckDetailMapper.selectList(wrapper);

        // 获取所有需要的类别ID
        Set<Long> categoryIds = details.stream()
                .map(DailyCheckDetail::getCategoryId)
                .collect(Collectors.toSet());

        // 查询类别名称映射
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<DailyCheckCategory> categories = dailyCheckCategoryMapper.selectList(
                    new LambdaQueryWrapper<DailyCheckCategory>()
                            .eq(DailyCheckCategory::getCheckId, checkId)
                            .in(DailyCheckCategory::getCategoryId, categoryIds)
            );
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(
                            DailyCheckCategory::getCategoryId,
                            DailyCheckCategory::getCategoryName,
                            (v1, v2) -> v1
                    ));
        }

        // 获取所有需要的班级ID
        Set<Long> classIds = details.stream()
                .map(DailyCheckDetail::getClassId)
                .collect(Collectors.toSet());

        // 查询班级名称映射
        Map<Long, String> classNameMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            for (Long classId : classIds) {
                Class cls = classMapper.selectById(classId);
                if (cls != null) {
                    classNameMap.put(classId, cls.getClassName());
                }
            }
        }

        // 获取所有学生ID，查询学号
        Map<Long, String> studentNoMap = new HashMap<>();
        for (DailyCheckDetail detail : details) {
            if (detail.getStudentIds() != null) {
                String[] ids = detail.getStudentIds().split(",");
                for (String id : ids) {
                    try {
                        Long studentId = Long.parseLong(id.trim());
                        if (!studentNoMap.containsKey(studentId)) {
                            Student student = studentMapper.selectById(studentId);
                            if (student != null) {
                                studentNoMap.put(studentId, student.getStudentNo());
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        LocalDate today = LocalDate.now();
        Map<Long, String> finalCategoryNameMap = categoryNameMap;

        // 转换为DTO
        return details.stream()
                .map(detail -> {
                    QuickEntryRecordDTO dto = new QuickEntryRecordDTO();
                    dto.setId(detail.getId());
                    dto.setDeductionItemId(detail.getDeductionItemId());
                    dto.setDeductionItemName(detail.getDeductionItemName());
                    dto.setCategoryName(finalCategoryNameMap.get(detail.getCategoryId()));
                    dto.setClassId(detail.getClassId());
                    dto.setClassName(classNameMap.get(detail.getClassId()));
                    dto.setDeductScore(detail.getDeductScore());
                    dto.setRemark(detail.getRemark());
                    dto.setCreatedAt(detail.getCreatedAt());

                    // 处理学生信息 (取第一个)
                    if (detail.getStudentIds() != null) {
                        String[] ids = detail.getStudentIds().split(",");
                        if (ids.length > 0) {
                            try {
                                Long studentId = Long.parseLong(ids[0].trim());
                                dto.setStudentId(studentId);
                                dto.setStudentNo(studentNoMap.get(studentId));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    if (detail.getStudentNames() != null) {
                        String[] names = detail.getStudentNames().split(",");
                        if (names.length > 0) {
                            dto.setStudentName(names[0].trim());
                        }
                    }

                    // 处理图片
                    if (StringUtils.hasText(detail.getImages())) {
                        dto.setPhotoUrls(Arrays.asList(detail.getImages().split(",")));
                    } else if (StringUtils.hasText(detail.getPhotoUrls())) {
                        dto.setPhotoUrls(Arrays.asList(detail.getPhotoUrls().split(",")));
                    }

                    // 判断是否可撤销 (当天录入的)
                    boolean canRevoke = detail.getCreatedAt() != null &&
                            detail.getCreatedAt().toLocalDate().equals(today);
                    dto.setCanRevoke(canRevoke);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取检查目标班级ID集合
     */
    private Set<Long> getTargetClassIds(Long checkId) {
        List<DailyCheckTarget> targets = dailyCheckTargetMapper.selectList(
                new LambdaQueryWrapper<DailyCheckTarget>()
                        .eq(DailyCheckTarget::getCheckId, checkId)
        );

        Set<Long> classIds = new LinkedHashSet<>();

        for (DailyCheckTarget target : targets) {
            if (target.getTargetType() == 1) {
                // 直接选择的班级
                classIds.add(target.getTargetId());
            } else if (target.getTargetType() == 2) {
                // 年级
                String gradeName = target.getTargetName();
                if (gradeName != null && gradeName.endsWith("级")) {
                    try {
                        Integer enrollmentYear = Integer.parseInt(gradeName.replace("级", ""));
                        List<com.school.management.dto.ClassResponse> gradeClasses = classMapper.selectByEnrollmentYear(enrollmentYear);
                        for (com.school.management.dto.ClassResponse cls : gradeClasses) {
                            classIds.add(cls.getId());
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (target.getTargetType() == 3) {
                // 院系
                List<com.school.management.dto.ClassResponse> deptClasses = classMapper.selectByDepartmentId(target.getTargetId());
                for (com.school.management.dto.ClassResponse cls : deptClasses) {
                    classIds.add(cls.getId());
                }
            }
        }

        return classIds;
    }
}
