package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.entity.*;
import com.school.management.enums.DailyCheckStatus;
import com.school.management.enums.InspectionSessionStatus;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.DailyCheckConversionService;
import com.school.management.service.InspectionSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日常检查转换服务实现
 *
 * @author system
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCheckConversionServiceImpl implements DailyCheckConversionService {

    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckTargetMapper dailyCheckTargetMapper;
    private final DailyCheckCategoryMapper dailyCheckCategoryMapper;
    private final DailyCheckDetailMapper dailyCheckDetailMapper;

    private final InspectionSessionMapper inspectionSessionMapper;
    private final InspectionTargetMapper inspectionTargetMapper;
    private final InspectionCategoryMapper inspectionCategoryMapper;
    private final InspectionDeductionItemMapper inspectionDeductionItemMapper;

    private final InspectionSessionService inspectionSessionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertDailyCheckToInspectionSession(Long dailyCheckId) {
        // 1. 检查是否已经转换
        if (isAlreadyConverted(dailyCheckId)) {
            log.warn("日常检查[{}]已经转换过,跳过", dailyCheckId);
            return getConvertedSessionId(dailyCheckId);
        }

        // 2. 查询daily_check基本信息
        DailyCheck dailyCheck = dailyCheckMapper.selectById(dailyCheckId);
        if (dailyCheck == null) {
            throw new BusinessException("日常检查不存在");
        }

        if (!DailyCheckStatus.SUBMITTED.getCode().equals(dailyCheck.getStatus())) {
            throw new BusinessException("只能转换已结束的日常检查");
        }

        log.info("开始转换日常检查[{}]到检查记录", dailyCheckId);

        // 3. 创建inspection_session
        InspectionSession session = new InspectionSession();
        session.setDailyCheckId(dailyCheckId);
        session.setSessionCode(inspectionSessionService.generateSessionCode());
        session.setInspectionDate(dailyCheck.getCheckDate());
        session.setInspectionTime(LocalTime.now()); // 使用当前时间
        session.setInspectorId(dailyCheck.getCheckerId() != null ? dailyCheck.getCheckerId() : 1L);
        session.setInspectorName(dailyCheck.getCheckerName() != null ? dailyCheck.getCheckerName() : "System");
        session.setRemarks(dailyCheck.getDescription());
        session.setStatus(InspectionSessionStatus.PUBLISHED.getCode()); // 直接设置为已发布
        session.setPublishedAt(dailyCheck.getUpdatedAt());
        session.setTotalTargets(0);
        session.setTotalDeductions(BigDecimal.ZERO);

        inspectionSessionMapper.insert(session);
        Long sessionId = session.getId();

        // 4. 查询并转换检查目标
        List<DailyCheckTarget> dailyTargets = dailyCheckTargetMapper.selectList(
            new LambdaQueryWrapper<DailyCheckTarget>()
                .eq(DailyCheckTarget::getCheckId, dailyCheckId)
        );

        if (dailyTargets == null || dailyTargets.isEmpty()) {
            log.warn("日常检查[{}]没有检查目标", dailyCheckId);
            return sessionId;
        }

        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (DailyCheckTarget dailyTarget : dailyTargets) {
            // 创建inspection_target
            InspectionTarget target = new InspectionTarget();
            target.setSessionId(sessionId);
            target.setTargetType(getTargetTypeString(dailyTarget.getTargetType()));
            target.setTargetId(dailyTarget.getTargetId());
            target.setTargetName(dailyTarget.getTargetName());
            target.setTotalDeductions(BigDecimal.ZERO);
            target.setCategoryCount(0);
            target.setItemCount(0);

            inspectionTargetMapper.insert(target);
            Long targetId = target.getId();

            // 5. 查询该目标的检查类别(通过details表反推)
            List<DailyCheckDetail> details = dailyCheckDetailMapper.selectList(
                new LambdaQueryWrapper<DailyCheckDetail>()
                    .eq(DailyCheckDetail::getCheckId, dailyCheckId)
                    .eq(DailyCheckDetail::getClassId, dailyTarget.getTargetId())
            );

            if (details == null || details.isEmpty()) {
                continue;
            }

            // 按categoryId分组
            Map<Long, List<DailyCheckDetail>> categoryMap = new HashMap<>();
            for (DailyCheckDetail detail : details) {
                categoryMap.computeIfAbsent(detail.getCategoryId(), k -> new java.util.ArrayList<>()).add(detail);
            }

            BigDecimal targetDeductions = BigDecimal.ZERO;
            int targetItemCount = 0;

            // 6. 转换每个类别和扣分项
            for (Map.Entry<Long, List<DailyCheckDetail>> entry : categoryMap.entrySet()) {
                Long categoryId = entry.getKey();
                List<DailyCheckDetail> categoryDetails = entry.getValue();

                // 获取类别信息
                DailyCheckCategory dailyCategory = dailyCheckCategoryMapper.selectOne(
                    new LambdaQueryWrapper<DailyCheckCategory>()
                        .eq(DailyCheckCategory::getCheckId, dailyCheckId)
                        .eq(DailyCheckCategory::getCategoryId, categoryId)
                        .last("LIMIT 1")
                );

                if (dailyCategory == null) {
                    log.warn("找不到类别[{}]信息", categoryId);
                    continue;
                }

                // 创建inspection_category
                InspectionCategory inspectionCategory = new InspectionCategory();
                inspectionCategory.setSessionId(sessionId);
                inspectionCategory.setTargetId(targetId);
                inspectionCategory.setTypeId(categoryId);
                inspectionCategory.setTypeName(dailyCategory.getCategoryName());
                inspectionCategory.setTypeCode(String.valueOf(categoryId)); // 使用ID作为code
                inspectionCategory.setCategoryDeductions(BigDecimal.ZERO);
                inspectionCategory.setItemCount(categoryDetails.size());

                inspectionCategoryMapper.insert(inspectionCategory);
                Long inspectionCategoryId = inspectionCategory.getId();

                BigDecimal categoryDeductions = BigDecimal.ZERO;

                // 转换扣分项
                for (DailyCheckDetail detail : categoryDetails) {
                    InspectionDeductionItem item = new InspectionDeductionItem();
                    item.setSessionId(sessionId);
                    item.setTargetId(targetId);
                    item.setCategoryId(inspectionCategoryId);
                    item.setItemName(detail.getDeductionItemName());
                    item.setDeductScore(detail.getDeductScore());
                    item.setPersonCount(detail.getPersonCount() != null ? detail.getPersonCount() : 0);
                    item.setDeductReason(detail.getDescription());
                    item.setEvidenceImages(detail.getImages());

                    inspectionDeductionItemMapper.insert(item);

                    categoryDeductions = categoryDeductions.add(detail.getDeductScore());
                    targetItemCount++;
                }

                // 更新类别总扣分
                inspectionCategory.setCategoryDeductions(categoryDeductions);
                inspectionCategoryMapper.updateById(inspectionCategory);

                targetDeductions = targetDeductions.add(categoryDeductions);
            }

            // 更新目标统计信息
            target.setTotalDeductions(targetDeductions);
            target.setCategoryCount(categoryMap.size());
            target.setItemCount(targetItemCount);
            inspectionTargetMapper.updateById(target);

            totalDeductions = totalDeductions.add(targetDeductions);
        }

        // 7. 更新session统计信息
        session.setTotalTargets(dailyTargets.size());
        session.setTotalDeductions(totalDeductions);
        inspectionSessionMapper.updateById(session);

        log.info("转换完成: 日常检查[{}] -> 检查批次[{}], 总扣分: {}",
            dailyCheckId, session.getSessionCode(), totalDeductions);

        return sessionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer convertAllCompletedDailyChecks() {
        // 查询所有已结束但未转换的日常检查
        List<DailyCheck> completedChecks = dailyCheckMapper.selectList(
            new LambdaQueryWrapper<DailyCheck>()
                .eq(DailyCheck::getStatus, 2)
        );

        if (completedChecks == null || completedChecks.isEmpty()) {
            log.info("没有需要转换的日常检查");
            return 0;
        }

        int count = 0;
        for (DailyCheck dailyCheck : completedChecks) {
            try {
                if (!isAlreadyConverted(dailyCheck.getId())) {
                    convertDailyCheckToInspectionSession(dailyCheck.getId());
                    count++;
                }
            } catch (Exception e) {
                log.error("转换日常检查[{}]失败: {}", dailyCheck.getId(), e.getMessage(), e);
            }
        }

        log.info("批量转换完成,成功转换 {} 条记录", count);
        return count;
    }

    @Override
    public boolean isAlreadyConverted(Long dailyCheckId) {
        Long count = inspectionSessionMapper.selectCount(
            new LambdaQueryWrapper<InspectionSession>()
                .eq(InspectionSession::getDailyCheckId, dailyCheckId)
        );
        return count != null && count > 0;
    }

    /**
     * 获取已转换的sessionId
     */
    private Long getConvertedSessionId(Long dailyCheckId) {
        InspectionSession session = inspectionSessionMapper.selectOne(
            new LambdaQueryWrapper<InspectionSession>()
                .eq(InspectionSession::getDailyCheckId, dailyCheckId)
                .last("LIMIT 1")
        );
        return session != null ? session.getId() : null;
    }

    /**
     * 转换目标类型
     */
    private String getTargetTypeString(Integer targetType) {
        if (targetType == null) {
            return "class";
        }
        switch (targetType) {
            case 1: return "class";   // 班级
            case 2: return "grade";   // 年级
            case 3: return "dept";    // 院系
            default: return "class";
        }
    }
}
