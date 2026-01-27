package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.aggregate.Semester;
import com.school.management.domain.teaching.model.entity.TeachingWeek;
import com.school.management.domain.teaching.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 学期仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SemesterRepositoryImpl implements SemesterRepository {

    private final SemesterMapper semesterMapper;
    private final TeachingWeekMapper teachingWeekMapper;

    @Override
    public Semester save(Semester semester) {
        SemesterPO po = toPO(semester);
        if (po.getId() == null) {
            semesterMapper.insert(po);
        } else {
            semesterMapper.updateById(po);
        }
        semester.setId(po.getId());
        return semester;
    }

    @Override
    public Optional<Semester> findById(Long id) {
        SemesterPO po = semesterMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Semester> findBySemesterCode(String semesterCode) {
        LambdaQueryWrapper<SemesterPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SemesterPO::getSemesterCode, semesterCode);
        SemesterPO po = semesterMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Semester> findByAcademicYearId(Long academicYearId) {
        LambdaQueryWrapper<SemesterPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SemesterPO::getAcademicYearId, academicYearId)
                .orderByAsc(SemesterPO::getSemesterType);
        return semesterMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Semester> findAll() {
        LambdaQueryWrapper<SemesterPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SemesterPO::getSemesterCode);
        return semesterMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Semester> findCurrent() {
        LambdaQueryWrapper<SemesterPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SemesterPO::getIsCurrent, true);
        SemesterPO po = semesterMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        semesterMapper.deleteById(id);
        teachingWeekMapper.deleteBySemesterId(id);
    }

    @Override
    public void clearAllCurrent() {
        semesterMapper.clearAllCurrent();
    }

    @Override
    public boolean existsBySemesterCode(String semesterCode) {
        LambdaQueryWrapper<SemesterPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SemesterPO::getSemesterCode, semesterCode);
        return semesterMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Optional<Semester> findByIdWithWeeks(Long id) {
        return findById(id).map(semester -> {
            LambdaQueryWrapper<TeachingWeekPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TeachingWeekPO::getSemesterId, id)
                    .orderByAsc(TeachingWeekPO::getWeekNumber);
            List<TeachingWeek> weeks = teachingWeekMapper.selectList(wrapper).stream()
                    .map(this::toTeachingWeekDomain)
                    .collect(Collectors.toList());
            semester.setTeachingWeeks(weeks);
            return semester;
        });
    }

    @Override
    public void saveTeachingWeeks(Long semesterId, List<TeachingWeek> weeks) {
        // 先删除原有教学周
        teachingWeekMapper.deleteBySemesterId(semesterId);

        // 保存新的教学周
        for (TeachingWeek week : weeks) {
            TeachingWeekPO po = toTeachingWeekPO(week);
            po.setSemesterId(semesterId);
            teachingWeekMapper.insert(po);
            week.setId(po.getId());
        }
    }

    private SemesterPO toPO(Semester domain) {
        SemesterPO po = new SemesterPO();
        po.setId(domain.getId());
        po.setAcademicYearId(domain.getAcademicYearId());
        po.setSemesterCode(domain.getSemesterCode());
        po.setSemesterName(domain.getSemesterName());
        po.setSemesterType(domain.getSemesterType());
        po.setStartDate(domain.getStartDate());
        po.setEndDate(domain.getEndDate());
        po.setTeachingStartDate(domain.getTeachingStartDate());
        po.setTeachingEndDate(domain.getTeachingEndDate());
        po.setExamStartDate(domain.getExamStartDate());
        po.setExamEndDate(domain.getExamEndDate());
        po.setTotalTeachingWeeks(domain.getTotalTeachingWeeks());
        po.setIsCurrent(domain.getIsCurrent());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private Semester toDomain(SemesterPO po) {
        return Semester.builder()
                .id(po.getId())
                .academicYearId(po.getAcademicYearId())
                .semesterCode(po.getSemesterCode())
                .semesterName(po.getSemesterName())
                .semesterType(po.getSemesterType())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .teachingStartDate(po.getTeachingStartDate())
                .teachingEndDate(po.getTeachingEndDate())
                .examStartDate(po.getExamStartDate())
                .examEndDate(po.getExamEndDate())
                .totalTeachingWeeks(po.getTotalTeachingWeeks())
                .isCurrent(po.getIsCurrent())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private TeachingWeekPO toTeachingWeekPO(TeachingWeek domain) {
        TeachingWeekPO po = new TeachingWeekPO();
        po.setId(domain.getId());
        po.setSemesterId(domain.getSemesterId());
        po.setWeekNumber(domain.getWeekNumber());
        po.setStartDate(domain.getStartDate());
        po.setEndDate(domain.getEndDate());
        po.setWeekType(domain.getWeekType());
        po.setWeekLabel(domain.getWeekLabel());
        po.setIsActive(domain.getIsActive());
        po.setRemark(domain.getRemark());
        return po;
    }

    private TeachingWeek toTeachingWeekDomain(TeachingWeekPO po) {
        return TeachingWeek.builder()
                .id(po.getId())
                .semesterId(po.getSemesterId())
                .weekNumber(po.getWeekNumber())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .weekType(po.getWeekType())
                .weekLabel(po.getWeekLabel())
                .isActive(po.getIsActive())
                .remark(po.getRemark())
                .build();
    }
}
