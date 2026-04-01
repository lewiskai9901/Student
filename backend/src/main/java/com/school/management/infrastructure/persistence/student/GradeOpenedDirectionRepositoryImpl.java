package com.school.management.infrastructure.persistence.student;

import com.school.management.domain.student.model.GradeOpenedDirection;
import com.school.management.domain.student.repository.GradeOpenedDirectionRepository;
import com.school.management.infrastructure.persistence.academic.GradeMajorDirectionMapper;
import com.school.management.infrastructure.persistence.academic.GradeMajorDirectionPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 年级开设专业方向 仓储实现
 */
@Repository
public class GradeOpenedDirectionRepositoryImpl implements GradeOpenedDirectionRepository {

    private final GradeMajorDirectionMapper mapper;

    public GradeOpenedDirectionRepositoryImpl(GradeMajorDirectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<GradeOpenedDirection> findById(Long id) {
        GradeMajorDirectionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public GradeOpenedDirection save(GradeOpenedDirection entity) {
        GradeMajorDirectionPO po = toPO(entity);
        if (entity.getId() == null) {
            mapper.insert(po);
            entity.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return entity;
    }

    @Override
    public void delete(GradeOpenedDirection entity) {
        if (entity != null && entity.getId() != null) {
            mapper.deleteById(entity.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public List<GradeOpenedDirection> findByGradeId(Long gradeId) {
        return mapper.findByGradeId(gradeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GradeOpenedDirection> findByMajorDirectionId(Long majorDirectionId) {
        return mapper.findByMajorDirectionId(majorDirectionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GradeOpenedDirection> findByGradeIdAndMajorDirectionId(Long gradeId, Long majorDirectionId) {
        GradeMajorDirectionPO po = mapper.findByGradeIdAndMajorDirectionId(gradeId, majorDirectionId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean existsByGradeIdAndMajorDirectionId(Long gradeId, Long majorDirectionId) {
        return mapper.countByGradeIdAndMajorDirectionId(gradeId, majorDirectionId) > 0;
    }

    @Override
    public int countByGradeId(Long gradeId) {
        return mapper.countByGradeId(gradeId);
    }

    @Override
    public int countByMajorDirectionId(Long majorDirectionId) {
        return mapper.countByMajorDirectionId(majorDirectionId);
    }

    @Override
    public void deleteByGradeId(Long gradeId) {
        mapper.softDeleteByGradeId(gradeId);
    }

    @Override
    public void deleteByMajorDirectionId(Long majorDirectionId) {
        mapper.softDeleteByMajorDirectionId(majorDirectionId);
    }

    private GradeMajorDirectionPO toPO(GradeOpenedDirection domain) {
        GradeMajorDirectionPO po = new GradeMajorDirectionPO();
        po.setId(domain.getId());
        po.setGradeId(domain.getGradeId());
        po.setMajorDirectionId(domain.getMajorDirectionId());
        po.setPlannedClassCount(domain.getPlannedClasses());
        po.setPlannedStudentCount(domain.getPlannedStudents());
        po.setRemarks(domain.getRemarks());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }

    private GradeOpenedDirection toDomain(GradeMajorDirectionPO po) {
        return GradeOpenedDirection.reconstitute(
                po.getId(),
                po.getGradeId(),
                po.getMajorDirectionId(),
                po.getPlannedClassCount(),
                po.getPlannedStudentCount(),
                po.getRemarks(),
                true, // enabled
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getCreatedBy(),
                po.getUpdatedBy()
        );
    }
}
