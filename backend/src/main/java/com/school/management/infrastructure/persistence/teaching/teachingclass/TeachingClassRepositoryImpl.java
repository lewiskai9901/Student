package com.school.management.infrastructure.persistence.teaching.teachingclass;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.teachingclass.TeachingClass;
import com.school.management.domain.teaching.model.teachingclass.TeachingClassType;
import com.school.management.domain.teaching.repository.TeachingClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TeachingClassRepositoryImpl implements TeachingClassRepository {
    private final TeachingClassMapper mapper;

    @Override
    public TeachingClass save(TeachingClass teachingClass) {
        TeachingClassPO po = toPO(teachingClass);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<TeachingClass> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<TeachingClass> findBySemesterId(Long semesterId) {
        return mapper.selectList(new LambdaQueryWrapper<TeachingClassPO>()
                .eq(TeachingClassPO::getSemesterId, semesterId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TeachingClass> findBySemesterIdAndCourseId(Long semesterId, Long courseId) {
        return mapper.selectList(new LambdaQueryWrapper<TeachingClassPO>()
                .eq(TeachingClassPO::getSemesterId, semesterId)
                .eq(TeachingClassPO::getCourseId, courseId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TeachingClassPO toPO(TeachingClass tc) {
        TeachingClassPO po = new TeachingClassPO();
        po.setId(tc.getId());
        po.setSemesterId(tc.getSemesterId());
        po.setClassName(tc.getClassName());
        po.setClassCode(tc.getClassCode());
        po.setCourseId(tc.getCourseId());
        po.setClassType(tc.getClassType() != null ? tc.getClassType().getCode() : null);
        po.setWeeklyHours(tc.getWeeklyHours());
        po.setStudentCount(tc.getStudentCount());
        po.setRequiredRoomType(tc.getRequiredRoomType());
        po.setRequiredCapacity(tc.getRequiredCapacity());
        po.setStartWeek(tc.getStartWeek());
        po.setEndWeek(tc.getEndWeek());
        po.setStatus(tc.getStatus());
        po.setRemark(tc.getRemark());
        po.setCreatedBy(tc.getCreatedBy());
        return po;
    }

    private TeachingClass toDomain(TeachingClassPO po) {
        return TeachingClass.reconstruct(
                po.getId(), po.getSemesterId(), po.getClassName(),
                po.getClassCode(), po.getCourseId(),
                po.getClassType() != null ? TeachingClassType.fromCode(po.getClassType()) : null,
                po.getWeeklyHours(), po.getStudentCount(),
                po.getRequiredRoomType(), po.getRequiredCapacity(),
                po.getStartWeek(), po.getEndWeek(),
                po.getStatus(), po.getRemark(), po.getCreatedBy()
        );
    }
}
