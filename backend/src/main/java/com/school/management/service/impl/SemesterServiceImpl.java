package com.school.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.Semester;
import com.school.management.mapper.AcademicWeekMapper;
import com.school.management.mapper.SemesterMapper;
import com.school.management.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学期服务实现
 *
 * @author system
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements SemesterService {

    private final AcademicWeekMapper academicWeekMapper;

    @Override
    public Semester getBySemesterCode(String semesterCode) {
        return baseMapper.selectBySemesterCode(semesterCode);
    }

    @Override
    public Semester getCurrentSemester() {
        return baseMapper.selectCurrentSemester();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setCurrentSemester(Long id) {
        // 取消所有学期的当前状态
        baseMapper.cancelAllCurrent();
        academicWeekMapper.cancelAllCurrent();

        // 设置新的当前学期
        Semester semester = getById(id);
        if (semester == null) {
            return false;
        }

        semester.setIsCurrent(1);
        return updateById(semester);
    }

    @Override
    public String generateSemesterCode(Integer startYear, Integer semesterType) {
        return startYear + "-" + (startYear + 1) + "-" + semesterType;
    }
}
