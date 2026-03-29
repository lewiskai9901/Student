package com.school.management.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.offering.ClassCourseAssignment;
import com.school.management.domain.teaching.repository.ClassCourseAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ClassCourseAssignmentRepositoryImpl implements ClassCourseAssignmentRepository {
    private final ClassCourseAssignmentMapper mapper;

    @Override
    public ClassCourseAssignment save(ClassCourseAssignment assignment) {
        ClassCourseAssignmentPO po = toPO(assignment);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<ClassCourseAssignment> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ClassCourseAssignment> findBySemesterIdAndClassId(Long semesterId, Long classId) {
        return mapper.selectList(new LambdaQueryWrapper<ClassCourseAssignmentPO>()
                .eq(ClassCourseAssignmentPO::getSemesterId, semesterId)
                .eq(ClassCourseAssignmentPO::getClassId, classId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ClassCourseAssignment> findBySemesterId(Long semesterId) {
        return mapper.selectList(new LambdaQueryWrapper<ClassCourseAssignmentPO>()
                .eq(ClassCourseAssignmentPO::getSemesterId, semesterId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ClassCourseAssignment> findByOfferingId(Long offeringId) {
        return mapper.selectList(new LambdaQueryWrapper<ClassCourseAssignmentPO>()
                .eq(ClassCourseAssignmentPO::getOfferingId, offeringId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ClassCourseAssignmentPO toPO(ClassCourseAssignment a) {
        ClassCourseAssignmentPO po = new ClassCourseAssignmentPO();
        po.setId(a.getId());
        po.setSemesterId(a.getSemesterId());
        po.setClassId(a.getClassId());
        po.setOfferingId(a.getOfferingId());
        po.setCourseId(a.getCourseId());
        po.setWeeklyHours(a.getWeeklyHours());
        po.setStudentCount(a.getStudentCount());
        po.setStatus(a.getStatus());
        return po;
    }

    private ClassCourseAssignment toDomain(ClassCourseAssignmentPO po) {
        return ClassCourseAssignment.reconstruct(
                po.getId(), po.getSemesterId(), po.getClassId(),
                po.getOfferingId(), po.getCourseId(), po.getWeeklyHours(),
                po.getStudentCount(), po.getStatus()
        );
    }
}
