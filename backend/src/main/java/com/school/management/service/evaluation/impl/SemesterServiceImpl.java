package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.EvaluationSemester;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.SemesterMapper;
import com.school.management.service.evaluation.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学期服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service("evaluationSemesterServiceImpl")
@RequiredArgsConstructor
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, EvaluationSemester>
        implements SemesterService {

    private final SemesterMapper semesterMapper;

    @Override
    public Page<Map<String, Object>> pageSemesters(Page<?> page, Map<String, Object> query) {
        return semesterMapper.selectSemesterPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSemester(EvaluationSemester semester) {
        // 检查编码是否存在
        if (existsByCode(semester.getSemesterCode(), null)) {
            throw new BusinessException("学期编码已存在");
        }

        // 设置默认值
        if (semester.getStatus() == null) {
            semester.setStatus(1);
        }
        if (semester.getIsCurrent() == null) {
            semester.setIsCurrent(0);
        }

        semesterMapper.insert(semester);
        log.info("创建学期: id={}, code={}", semester.getId(), semester.getSemesterCode());
        return semester.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSemester(EvaluationSemester semester) {
        EvaluationSemester existing = semesterMapper.selectById(semester.getId());
        if (existing == null) {
            throw new BusinessException("学期不存在");
        }

        // 检查编码是否存在（排除自己）
        if (semester.getSemesterCode() != null && existsByCode(semester.getSemesterCode(), semester.getId())) {
            throw new BusinessException("学期编码已存在");
        }

        semesterMapper.updateById(semester);
        log.info("更新学期: id={}", semester.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSemester(Long id) {
        EvaluationSemester existing = semesterMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学期不存在");
        }

        // 检查是否为当前学期
        if (existing.getIsCurrent() == 1) {
            throw new BusinessException("当前学期无法删除");
        }

        // 检查是否有关联数据
        int relatedCount = semesterMapper.countRelatedData(id);
        if (relatedCount > 0) {
            throw new BusinessException("该学期已有关联数据，无法删除");
        }

        semesterMapper.deleteById(id);
        log.info("删除学期: id={}", id);
    }

    @Override
    public Map<String, Object> getSemesterDetail(Long id) {
        Map<String, Object> detail = semesterMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("学期不存在");
        }
        return detail;
    }

    @Override
    public EvaluationSemester getCurrentSemester() {
        return semesterMapper.selectCurrent();
    }

    @Override
    public List<EvaluationSemester> getByAcademicYear(String academicYear) {
        return semesterMapper.selectByAcademicYear(academicYear);
    }

    @Override
    public List<String> getAllAcademicYears() {
        return semesterMapper.selectAllAcademicYears();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAsCurrent(Long id) {
        EvaluationSemester existing = semesterMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学期不存在");
        }

        // 将其他学期设为非当前
        semesterMapper.clearCurrentFlag();

        // 设置当前学期
        EvaluationSemester update = new EvaluationSemester();
        update.setId(id);
        update.setIsCurrent(1);
        semesterMapper.updateById(update);

        log.info("设置当前学期: id={}", id);
    }

    @Override
    public boolean existsByCode(String semesterCode, Long excludeId) {
        EvaluationSemester existing = semesterMapper.selectByCode(semesterCode);
        if (existing == null) {
            return false;
        }
        return excludeId == null || !existing.getId().equals(excludeId);
    }
}
