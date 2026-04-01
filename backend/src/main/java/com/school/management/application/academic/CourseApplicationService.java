package com.school.management.application.academic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.academic.command.CreateCourseCommand;
import com.school.management.application.academic.command.UpdateCourseCommand;
import com.school.management.application.academic.query.CourseDTO;
import com.school.management.domain.academic.model.Course;
import com.school.management.domain.academic.repository.CourseRepository;
import com.school.management.infrastructure.persistence.academic.CoursePO;
import com.school.management.infrastructure.persistence.academic.CoursePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程管理应用服务
 */
@RequiredArgsConstructor
@Service
public class CourseApplicationService {

    private final CourseRepository courseRepository;
    private final CoursePersistenceMapper courseMapper;

    // ======================== 查询 ========================

    @Transactional(readOnly = true)
    public Page<CourseDTO> getCourseList(String keyword, Integer courseCategory,
                                          Integer courseType, Integer status,
                                          int pageNum, int pageSize) {
        Page<CoursePO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CoursePO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(CoursePO::getCourseCode, keyword)
                .or()
                .like(CoursePO::getCourseName, keyword)
            );
        }
        if (courseCategory != null) {
            wrapper.eq(CoursePO::getCourseCategory, courseCategory);
        }
        if (courseType != null) {
            wrapper.eq(CoursePO::getCourseType, courseType);
        }
        if (status != null) {
            wrapper.eq(CoursePO::getStatus, status);
        }
        wrapper.orderByDesc(CoursePO::getCreatedAt);

        Page<CoursePO> result = courseMapper.selectPage(page, wrapper);

        Page<CourseDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::poToDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        List<CoursePO> courses = courseMapper.selectList(
            new LambdaQueryWrapper<CoursePO>().orderByAsc(CoursePO::getCourseCode)
        );
        return courses.stream().map(this::poToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseDTO getCourse(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + id));
        return toDTO(course);
    }

    @Transactional(readOnly = true)
    public CourseDTO getCourseByCode(String code) {
        Course course = courseRepository.findByCourseCode(code)
            .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + code));
        return toDTO(course);
    }

    // ======================== 命令 ========================

    @Transactional
    public CourseDTO createCourse(CreateCourseCommand command) {
        if (courseRepository.existsByCourseCode(command.getCourseCode())) {
            throw new IllegalArgumentException("课程代码已存在: " + command.getCourseCode());
        }

        Course course = Course.builder()
            .courseCode(command.getCourseCode())
            .courseName(command.getCourseName())
            .courseNameEn(command.getCourseNameEn())
            .courseCategory(command.getCourseCategory())
            .courseType(command.getCourseType())
            .courseNature(command.getCourseNature())
            .credits(command.getCredits())
            .totalHours(command.getTotalHours())
            .theoryHours(command.getTheoryHours())
            .practiceHours(command.getPracticeHours())
            .weeklyHours(command.getWeeklyHours())
            .examType(command.getExamType())
            .orgUnitId(command.getOrgUnitId())
            .description(command.getDescription())
            .status(command.getStatus())
            .createdBy(command.getCreatedBy())
            .build();

        course = courseRepository.save(course);
        return toDTO(course);
    }

    @Transactional
    public CourseDTO updateCourse(Long id, UpdateCourseCommand command) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + id));

        course.update(
            command.getCourseName(), command.getCourseNameEn(),
            command.getCourseCategory(), command.getCourseType(), command.getCourseNature(),
            command.getCredits(), command.getTotalHours(), command.getTheoryHours(),
            command.getPracticeHours(), command.getWeeklyHours(), command.getExamType(),
            command.getOrgUnitId(), command.getDescription(), command.getUpdatedBy()
        );

        if (command.getStatus() != null) {
            course.updateStatus(command.getStatus());
        }

        course = courseRepository.save(course);
        return toDTO(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @Transactional
    public void updateCourseStatus(Long id, Integer status) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + id));
        course.updateStatus(status);
        courseRepository.save(course);
    }

    // ======================== Mapping ========================

    private CourseDTO toDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCourseCode(course.getCourseCode());
        dto.setCourseName(course.getCourseName());
        dto.setCourseNameEn(course.getCourseNameEn());
        dto.setCourseCategory(course.getCourseCategory());
        dto.setCourseType(course.getCourseType());
        dto.setCourseNature(course.getCourseNature());
        dto.setCredits(course.getCredits());
        dto.setTotalHours(course.getTotalHours());
        dto.setTheoryHours(course.getTheoryHours());
        dto.setPracticeHours(course.getPracticeHours());
        dto.setWeeklyHours(course.getWeeklyHours());
        dto.setExamType(course.getExamType());
        dto.setOrgUnitId(course.getOrgUnitId());
        dto.setDescription(course.getDescription());
        dto.setStatus(course.getStatus());
        dto.setCreatedBy(course.getCreatedBy());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setUpdatedAt(course.getUpdatedAt());
        return dto;
    }

    private CourseDTO poToDTO(CoursePO po) {
        CourseDTO dto = new CourseDTO();
        dto.setId(po.getId());
        dto.setCourseCode(po.getCourseCode());
        dto.setCourseName(po.getCourseName());
        dto.setCourseNameEn(po.getCourseNameEn());
        dto.setCourseCategory(po.getCourseCategory());
        dto.setCourseType(po.getCourseType());
        dto.setCourseNature(po.getCourseNature());
        dto.setCredits(po.getCredits());
        dto.setTotalHours(po.getTotalHours());
        dto.setTheoryHours(po.getTheoryHours());
        dto.setPracticeHours(po.getPracticeHours());
        dto.setWeeklyHours(po.getWeeklyHours());
        dto.setExamType(po.getExamType());
        dto.setOrgUnitId(po.getOrgUnitId());
        dto.setDescription(po.getDescription());
        dto.setStatus(po.getStatus());
        dto.setCreatedBy(po.getCreatedBy());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }
}
