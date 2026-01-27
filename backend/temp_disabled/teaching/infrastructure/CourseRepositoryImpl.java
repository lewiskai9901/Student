package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.aggregate.Course;
import com.school.management.domain.teaching.repository.CourseRepository;
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

    private final CourseMapper courseMapper;

    @Override
    public Course save(Course course) {
        CoursePO po = toPO(course);
        if (po.getId() == null) {
            courseMapper.insert(po);
        } else {
            courseMapper.updateById(po);
        }
        course.setId(po.getId());
        return course;
    }

    @Override
    public Optional<Course> findById(Long id) {
        CoursePO po = courseMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Course> findByCourseCode(String courseCode) {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoursePO::getCourseCode, courseCode);
        CoursePO po = courseMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Course> findAll() {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CoursePO::getCourseCode);
        return courseMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Course> findByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoursePO::getDepartmentId, departmentId)
                .orderByAsc(CoursePO::getCourseCode);
        return courseMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Course> findByCourseType(Integer courseType) {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoursePO::getCourseType, courseType)
                .orderByAsc(CoursePO::getCourseCode);
        return courseMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Course> searchByKeyword(String keyword) {
        return courseMapper.searchByKeyword(keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        courseMapper.deleteById(id);
    }

    @Override
    public boolean existsByCourseCode(String courseCode) {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoursePO::getCourseCode, courseCode);
        return courseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Course> findPage(int page, int size, Long departmentId, Integer courseType, Integer status) {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) {
            wrapper.eq(CoursePO::getDepartmentId, departmentId);
        }
        if (courseType != null) {
            wrapper.eq(CoursePO::getCourseType, courseType);
        }
        if (status != null) {
            wrapper.eq(CoursePO::getStatus, status);
        }
        wrapper.orderByAsc(CoursePO::getCourseCode);

        Page<CoursePO> pageResult = courseMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long departmentId, Integer courseType, Integer status) {
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) {
            wrapper.eq(CoursePO::getDepartmentId, departmentId);
        }
        if (courseType != null) {
            wrapper.eq(CoursePO::getCourseType, courseType);
        }
        if (status != null) {
            wrapper.eq(CoursePO::getStatus, status);
        }
        return courseMapper.selectCount(wrapper);
    }

    private CoursePO toPO(Course domain) {
        CoursePO po = new CoursePO();
        po.setId(domain.getId());
        po.setCourseCode(domain.getCourseCode());
        po.setCourseName(domain.getCourseName());
        po.setEnglishName(domain.getEnglishName());
        po.setCourseType(domain.getCourseType());
        po.setCourseNature(domain.getCourseNature());
        po.setCredits(domain.getCredits());
        po.setTotalHours(domain.getTotalHours());
        po.setTheoryHours(domain.getTheoryHours());
        po.setLabHours(domain.getLabHours());
        po.setPracticeHours(domain.getPracticeHours());
        po.setWeeklyHours(domain.getWeeklyHours());
        po.setDepartmentId(domain.getDepartmentId());
        po.setExamType(domain.getExamType());
        po.setGradeType(domain.getGradeType());
        po.setPrerequisites(domain.getPrerequisites());
        po.setDescription(domain.getDescription());
        po.setSyllabus(domain.getSyllabus());
        po.setStatus(domain.getStatus());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private Course toDomain(CoursePO po) {
        return Course.builder()
                .id(po.getId())
                .courseCode(po.getCourseCode())
                .courseName(po.getCourseName())
                .englishName(po.getEnglishName())
                .courseType(po.getCourseType())
                .courseNature(po.getCourseNature())
                .credits(po.getCredits())
                .totalHours(po.getTotalHours())
                .theoryHours(po.getTheoryHours())
                .labHours(po.getLabHours())
                .practiceHours(po.getPracticeHours())
                .weeklyHours(po.getWeeklyHours())
                .departmentId(po.getDepartmentId())
                .examType(po.getExamType())
                .gradeType(po.getGradeType())
                .prerequisites(po.getPrerequisites())
                .description(po.getDescription())
                .syllabus(po.getSyllabus())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
