package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.EvaluationPeriod;
import com.school.management.entity.evaluation.EvaluationSemester;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.EvaluationPeriodMapper;
import com.school.management.mapper.evaluation.SemesterMapper;
import com.school.management.service.evaluation.EvaluationPeriodService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 综测评定周期服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationPeriodServiceImpl extends ServiceImpl<EvaluationPeriodMapper, EvaluationPeriod>
        implements EvaluationPeriodService {

    private final EvaluationPeriodMapper periodMapper;
    private final SemesterMapper semesterMapper;

    @Override
    public Page<Map<String, Object>> pagePeriods(Page<?> page, Map<String, Object> query) {
        return periodMapper.selectPeriodPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPeriod(EvaluationPeriod period) {
        // 验证学期
        EvaluationSemester semester = semesterMapper.selectById(period.getSemesterId());
        if (semester == null) {
            throw new BusinessException("学期不存在");
        }

        // 检查该学期是否已有周期
        EvaluationPeriod existing = periodMapper.selectBySemesterId(period.getSemesterId());
        if (existing != null) {
            throw new BusinessException("该学期已存在综测周期");
        }

        // 检查编码是否存在
        if (periodMapper.selectByCode(period.getPeriodCode()) != null) {
            throw new BusinessException("周期编码已存在");
        }

        // 设置学期信息
        period.setAcademicYear(semester.getAcademicYear());
        period.setSemesterType(semester.getSemesterType());

        // 设置默认值
        if (period.getStatus() == null) {
            period.setStatus(EvaluationPeriod.STATUS_NOT_STARTED);
        }
        if (period.getIsLocked() == null) {
            period.setIsLocked(0);
        }

        periodMapper.insert(period);
        log.info("创建综测周期: id={}, code={}", period.getId(), period.getPeriodCode());
        return period.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePeriod(EvaluationPeriod period) {
        EvaluationPeriod existing = periodMapper.selectById(period.getId());
        if (existing == null) {
            throw new BusinessException("综测周期不存在");
        }

        // 已锁定的周期不能修改
        if (existing.getIsLocked() == 1) {
            throw new BusinessException("周期已锁定，无法修改");
        }

        periodMapper.updateById(period);
        log.info("更新综测周期: id={}", period.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePeriod(Long id) {
        EvaluationPeriod existing = periodMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("综测周期不存在");
        }

        // 已开始的周期不能删除
        if (existing.getStatus() > EvaluationPeriod.STATUS_NOT_STARTED) {
            throw new BusinessException("周期已开始，无法删除");
        }

        periodMapper.deleteById(id);
        log.info("删除综测周期: id={}", id);
    }

    @Override
    public Map<String, Object> getPeriodDetail(Long id) {
        Map<String, Object> detail = periodMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("综测周期不存在");
        }
        return detail;
    }

    @Override
    public EvaluationPeriod getCurrentPeriod() {
        return periodMapper.selectCurrentPeriod();
    }

    @Override
    public EvaluationPeriod getBySemesterId(Long semesterId) {
        return periodMapper.selectBySemesterId(semesterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startDataCollection(Long id) {
        updateStatus(id, EvaluationPeriod.STATUS_NOT_STARTED, EvaluationPeriod.STATUS_DATA_COLLECTION, "开始数据采集");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startApplication(Long id) {
        updateStatus(id, EvaluationPeriod.STATUS_DATA_COLLECTION, EvaluationPeriod.STATUS_APPLICATION, "开始荣誉申报");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long id) {
        updateStatus(id, EvaluationPeriod.STATUS_APPLICATION, EvaluationPeriod.STATUS_REVIEW, "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPublicity(Long id) {
        updateStatus(id, EvaluationPeriod.STATUS_REVIEW, EvaluationPeriod.STATUS_PUBLICITY, "开始公示");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startAppeal(Long id) {
        updateStatus(id, EvaluationPeriod.STATUS_PUBLICITY, EvaluationPeriod.STATUS_APPEAL, "开始申诉处理");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishPeriod(Long id) {
        EvaluationPeriod existing = periodMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("综测周期不存在");
        }

        // 结束周期
        EvaluationPeriod update = new EvaluationPeriod();
        update.setId(id);
        update.setStatus(EvaluationPeriod.STATUS_FINISHED);
        periodMapper.updateById(update);

        log.info("综测周期已结束: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockPeriod(Long id) {
        EvaluationPeriod existing = periodMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("综测周期不存在");
        }

        if (existing.getIsLocked() == 1) {
            throw new BusinessException("周期已锁定");
        }

        EvaluationPeriod update = new EvaluationPeriod();
        update.setId(id);
        update.setIsLocked(1);
        update.setLockedAt(LocalDateTime.now());
        update.setLockedBy(SecurityUtils.getCurrentUserId());
        periodMapper.updateById(update);

        log.info("综测周期已锁定: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockPeriod(Long id) {
        EvaluationPeriod existing = periodMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("综测周期不存在");
        }

        if (existing.getIsLocked() == 0) {
            throw new BusinessException("周期未锁定");
        }

        EvaluationPeriod update = new EvaluationPeriod();
        update.setId(id);
        update.setIsLocked(0);
        update.setLockedAt(null);
        update.setLockedBy(null);
        periodMapper.updateById(update);

        log.info("综测周期已解锁: id={}", id);
    }

    @Override
    public List<EvaluationPeriod> getByAcademicYear(String academicYear) {
        return periodMapper.selectByAcademicYear(academicYear);
    }

    /**
     * 更新状态
     */
    private void updateStatus(Long id, int expectedStatus, int newStatus, String operation) {
        EvaluationPeriod existing = periodMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("综测周期不存在");
        }

        if (existing.getStatus() != expectedStatus) {
            throw new BusinessException("当前状态不允许" + operation);
        }

        if (existing.getIsLocked() == 1) {
            throw new BusinessException("周期已锁定，无法操作");
        }

        EvaluationPeriod update = new EvaluationPeriod();
        update.setId(id);
        update.setStatus(newStatus);
        periodMapper.updateById(update);

        log.info("{}成功: id={}, 新状态={}", operation, id, newStatus);
    }
}
