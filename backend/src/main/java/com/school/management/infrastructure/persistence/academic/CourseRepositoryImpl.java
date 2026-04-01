package com.school.management.infrastructure.persistence.academic;

import com.school.management.domain.academic.model.Course;
import com.school.management.domain.academic.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 课程仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CoursePersistenceMapper courseMapper;

    @Override
    public Optional<Course> findById(Long id) {
        CoursePO po = courseMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Course save(Course course) {
        CoursePO po = toPO(course);
        if (course.getId() == null) {
            courseMapper.insert(po);
            course.setId(po.getId());
        } else {
            courseMapper.updateById(po);
        }
        return course;
    }

    @Override
    public void delete(Course aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            courseMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        courseMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return courseMapper.selectById(id) != null;
    }

    @Override
    public Optional<Course> findByCourseCode(String courseCode) {
        CoursePO po = courseMapper.findByCourseCode(courseCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean existsByCourseCode(String courseCode) {
        return courseMapper.countByCourseCode(courseCode) > 0;
    }

    @Override
    public List<Course> findAllEnabled() {
        return courseMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoursePO>()
                .eq(CoursePO::getStatus, 1)
                .orderByAsc(CoursePO::getCourseCode)
        ).stream().map(this::toDomain).collect(Collectors.toList());
    }

    // ======================== Mapping ========================

    private CoursePO toPO(Course domain) {
        CoursePO po = new CoursePO();
        po.setId(domain.getId());
        po.setCourseCode(domain.getCourseCode());
        po.setCourseName(domain.getCourseName());
        po.setCourseNameEn(domain.getCourseNameEn());
        po.setCourseCategory(domain.getCourseCategory());
        po.setCourseType(domain.getCourseType());
        po.setCourseNature(domain.getCourseNature());
        po.setCredits(domain.getCredits());
        po.setTotalHours(domain.getTotalHours());
        po.setTheoryHours(domain.getTheoryHours());
        po.setPracticeHours(domain.getPracticeHours());
        po.setWeeklyHours(domain.getWeeklyHours());
        po.setExamType(domain.getExamType());
        po.setOrgUnitId(domain.getOrgUnitId());
        po.setDescription(domain.getDescription());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    Course toDomain(CoursePO po) {
        return Course.builder()
            .id(po.getId())
            .courseCode(po.getCourseCode())
            .courseName(po.getCourseName())
            .courseNameEn(po.getCourseNameEn())
            .courseCategory(po.getCourseCategory())
            .courseType(po.getCourseType())
            .courseNature(po.getCourseNature())
            .credits(po.getCredits())
            .totalHours(po.getTotalHours())
            .theoryHours(po.getTheoryHours())
            .practiceHours(po.getPracticeHours())
            .weeklyHours(po.getWeeklyHours())
            .examType(po.getExamType())
            .orgUnitId(po.getOrgUnitId())
            .description(po.getDescription())
            .status(po.getStatus())
            .createdBy(po.getCreatedBy())
            .build();
    }
}
