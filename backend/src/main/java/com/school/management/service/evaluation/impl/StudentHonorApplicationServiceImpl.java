package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.*;
import com.school.management.service.evaluation.StudentHonorApplicationService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 学生荣誉申报服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentHonorApplicationServiceImpl extends ServiceImpl<StudentHonorApplicationMapper, StudentHonorApplication>
        implements StudentHonorApplicationService {

    private final StudentHonorApplicationMapper applicationMapper;
    private final HonorTypeMapper honorTypeMapper;
    private final EvaluationPeriodMapper periodMapper;

    @Override
    public Page<Map<String, Object>> pageApplications(Page<?> page, Map<String, Object> query) {
        return applicationMapper.selectApplicationPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitApplication(StudentHonorApplication application) {
        // 验证周期
        EvaluationPeriod period = periodMapper.selectById(application.getEvaluationPeriodId());
        if (period == null) {
            throw new BusinessException("综测周期不存在");
        }

        // 检查周期状态是否允许申报
        if (period.getStatus() != EvaluationPeriod.STATUS_APPLICATION) {
            throw new BusinessException("当前周期不在申报阶段");
        }

        // 检查是否在申报时间范围内
        LocalDateTime now = LocalDateTime.now();
        if (period.getApplicationStartDate() != null && now.toLocalDate().isBefore(period.getApplicationStartDate())) {
            throw new BusinessException("申报尚未开始");
        }
        if (period.getApplicationEndDate() != null && now.toLocalDate().isAfter(period.getApplicationEndDate())) {
            throw new BusinessException("申报已截止");
        }

        // 验证荣誉类型
        HonorType honorType = honorTypeMapper.selectById(application.getHonorTypeId());
        if (honorType == null || honorType.getStatus() != 1) {
            throw new BusinessException("荣誉类型不存在或已禁用");
        }

        // 检查是否可申报
        Map<String, Object> checkResult = checkCanApply(
                application.getStudentId(),
                application.getHonorTypeId(),
                application.getEvaluationPeriodId()
        );
        if (!(Boolean) checkResult.get("canApply")) {
            throw new BusinessException((String) checkResult.get("reason"));
        }

        // 设置默认值
        application.setStatus(StudentHonorApplication.STATUS_CLASS_REVIEW);
        application.setSubmittedAt(LocalDateTime.now());
        application.setCurrentStep(1);

        // 根据荣誉类型设置影响维度
        application.setEvaluationDimension(honorType.getEvaluationDimension());

        applicationMapper.insert(application);
        log.info("学生提交荣誉申报: studentId={}, honorTypeId={}, applicationId={}",
                application.getStudentId(), application.getHonorTypeId(), application.getId());
        return application.getId();
    }

    @Override
    public Map<String, Object> getApplicationDetail(Long id) {
        Map<String, Object> detail = applicationMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("申报记录不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApplication(StudentHonorApplication application) {
        StudentHonorApplication existing = applicationMapper.selectById(application.getId());
        if (existing == null) {
            throw new BusinessException("申报记录不存在");
        }

        // 只有待审核状态才能修改
        if (existing.getStatus() != StudentHonorApplication.STATUS_DRAFT &&
                existing.getStatus() != StudentHonorApplication.STATUS_CLASS_REVIEW) {
            throw new BusinessException("当前状态不允许修改");
        }

        applicationMapper.updateById(application);
        log.info("更新荣誉申报: id={}", application.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApplication(Long id) {
        StudentHonorApplication existing = applicationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("申报记录不存在");
        }

        // 只有待审核状态才能撤回
        if (existing.getStatus() != StudentHonorApplication.STATUS_CLASS_REVIEW &&
                existing.getStatus() != StudentHonorApplication.STATUS_DEPT_REVIEW) {
            throw new BusinessException("当前状态不允许撤回");
        }

        StudentHonorApplication update = new StudentHonorApplication();
        update.setId(id);
        update.setStatus(StudentHonorApplication.STATUS_WITHDRAWN);
        applicationMapper.updateById(update);

        log.info("撤回荣誉申报: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void classReview(Long id, boolean approved, String comment) {
        doReview(id, "class", approved, comment,
                StudentHonorApplication.STATUS_CLASS_REVIEW,
                approved ? StudentHonorApplication.STATUS_DEPT_REVIEW : StudentHonorApplication.STATUS_REJECTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void departmentReview(Long id, boolean approved, String comment) {
        doReview(id, "department", approved, comment,
                StudentHonorApplication.STATUS_DEPT_REVIEW,
                approved ? StudentHonorApplication.STATUS_APPROVED : StudentHonorApplication.STATUS_REJECTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schoolReview(Long id, boolean approved, String comment) {
        // 学校级审核暂不实现
        throw new BusinessException("暂不支持学校级审核");
    }

    /**
     * 执行审核
     */
    private void doReview(Long id, String level, boolean approved, String comment,
                          int expectedStatus, int newStatus) {
        StudentHonorApplication existing = applicationMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("申报记录不存在");
        }

        if (existing.getStatus() != expectedStatus) {
            throw new BusinessException("当前状态不允许审核");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        StudentHonorApplication update = new StudentHonorApplication();
        update.setId(id);
        update.setStatus(newStatus);

        switch (level) {
            case "class":
                update.setClassReviewerId(currentUserId);
                update.setClassReviewTime(now);
                update.setClassReviewOpinion(comment);
                update.setClassReviewStatus(approved ? StudentHonorApplication.REVIEW_APPROVED : StudentHonorApplication.REVIEW_REJECTED);
                update.setCurrentStep(approved ? 2 : 1);
                break;
            case "department":
                update.setDeptReviewerId(currentUserId);
                update.setDeptReviewTime(now);
                update.setDeptReviewOpinion(comment);
                update.setDeptReviewStatus(approved ? StudentHonorApplication.REVIEW_APPROVED : StudentHonorApplication.REVIEW_REJECTED);
                update.setCurrentStep(approved ? 3 : 2);
                break;
            default:
                throw new BusinessException("无效的审核级别");
        }

        applicationMapper.updateById(update);
        log.info("{}审核荣誉申报: id={}, approved={}", level, id, approved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchReview(List<Long> ids, String level, boolean approved, String comment) {
        int count = 0;
        for (Long id : ids) {
            try {
                switch (level) {
                    case "class":
                        classReview(id, approved, comment);
                        break;
                    case "department":
                        departmentReview(id, approved, comment);
                        break;
                    default:
                        continue;
                }
                count++;
            } catch (BusinessException e) {
                log.warn("批量审核跳过: id={}, reason={}", id, e.getMessage());
            }
        }
        return count;
    }

    @Override
    public List<Map<String, Object>> getStudentApplications(Long studentId, Long periodId) {
        return applicationMapper.selectByStudentId(studentId, periodId);
    }

    @Override
    public List<Map<String, Object>> getPendingReviewList(String reviewLevel, Long reviewerId) {
        // 根据审核级别确定需要查询的状态
        int targetStatus;
        switch (reviewLevel) {
            case "class":
                targetStatus = StudentHonorApplication.STATUS_CLASS_REVIEW;
                break;
            case "department":
                targetStatus = StudentHonorApplication.STATUS_DEPT_REVIEW;
                break;
            default:
                return Collections.emptyList();
        }

        return applicationMapper.selectPendingReview(targetStatus, reviewerId, reviewLevel);
    }

    @Override
    public Map<String, Object> checkCanApply(Long studentId, Long honorTypeId, Long periodId) {
        Map<String, Object> result = new HashMap<>();

        // 检查是否已申报过同类型荣誉
        StudentHonorApplication existing = applicationMapper.selectByStudentAndHonorType(
                studentId, honorTypeId, periodId);
        if (existing != null && existing.getStatus() != StudentHonorApplication.STATUS_WITHDRAWN
                && existing.getStatus() != StudentHonorApplication.STATUS_REJECTED) {
            result.put("canApply", false);
            result.put("reason", "您已申报过该荣誉类型");
            return result;
        }

        // 获取荣誉类型信息
        HonorType honorType = honorTypeMapper.selectById(honorTypeId);
        if (honorType == null || honorType.getStatus() != 1) {
            result.put("canApply", false);
            result.put("reason", "荣誉类型不存在或已禁用");
            return result;
        }

        // 注：材料在申请时提交，此处仅做资格预检
        // 如需实现"必须先上传材料才能申请"的逻辑，需扩展学生档案附件模块

        result.put("canApply", true);
        result.put("reason", "");
        return result;
    }

    @Override
    public List<Map<String, Object>> getAvailableHonorTypes(Long studentId, Long periodId) {
        // 获取所有启用的荣誉类型
        List<Map<String, Object>> honorTypes = honorTypeMapper.selectAvailableTypes();

        // 过滤已申报的类型
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> honorType : honorTypes) {
            Long honorTypeId = ((Number) honorType.get("id")).longValue();
            Map<String, Object> checkResult = checkCanApply(studentId, honorTypeId, periodId);
            if ((Boolean) checkResult.get("canApply")) {
                result.add(honorType);
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getReviewStatistics(Long periodId) {
        return applicationMapper.selectReviewStatistics(periodId);
    }

    @Override
    public List<Map<String, Object>> exportApplications(Map<String, Object> query) {
        return applicationMapper.selectForExport(query);
    }
}
