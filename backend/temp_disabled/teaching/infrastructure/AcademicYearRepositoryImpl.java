package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.aggregate.AcademicYear;
import com.school.management.domain.teaching.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 学年仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AcademicYearRepositoryImpl implements AcademicYearRepository {

    private final AcademicYearMapper academicYearMapper;

    @Override
    public AcademicYear save(AcademicYear academicYear) {
        AcademicYearPO po = toPO(academicYear);
        if (po.getId() == null) {
            academicYearMapper.insert(po);
        } else {
            academicYearMapper.updateById(po);
        }
        academicYear.setId(po.getId());
        return academicYear;
    }

    @Override
    public Optional<AcademicYear> findById(Long id) {
        AcademicYearPO po = academicYearMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AcademicYear> findByYearCode(String yearCode) {
        LambdaQueryWrapper<AcademicYearPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AcademicYearPO::getYearCode, yearCode);
        AcademicYearPO po = academicYearMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AcademicYear> findAll() {
        LambdaQueryWrapper<AcademicYearPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AcademicYearPO::getYearCode);
        return academicYearMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AcademicYear> findCurrent() {
        LambdaQueryWrapper<AcademicYearPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AcademicYearPO::getIsCurrent, true);
        AcademicYearPO po = academicYearMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        academicYearMapper.deleteById(id);
    }

    @Override
    public void clearAllCurrent() {
        academicYearMapper.clearAllCurrent();
    }

    @Override
    public boolean existsByYearCode(String yearCode) {
        LambdaQueryWrapper<AcademicYearPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AcademicYearPO::getYearCode, yearCode);
        return academicYearMapper.selectCount(wrapper) > 0;
    }

    private AcademicYearPO toPO(AcademicYear domain) {
        AcademicYearPO po = new AcademicYearPO();
        po.setId(domain.getId());
        po.setYearCode(domain.getYearCode());
        po.setYearName(domain.getYearName());
        po.setStartDate(domain.getStartDate());
        po.setEndDate(domain.getEndDate());
        po.setIsCurrent(domain.getIsCurrent());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private AcademicYear toDomain(AcademicYearPO po) {
        return AcademicYear.builder()
                .id(po.getId())
                .yearCode(po.getYearCode())
                .yearName(po.getYearName())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .isCurrent(po.getIsCurrent())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
