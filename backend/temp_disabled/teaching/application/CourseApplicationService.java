package com.school.management.application.teaching;

import com.school.management.application.teaching.command.CreateCourseCommand;
import com.school.management.application.teaching.command.UpdateCourseCommand;
import com.school.management.application.teaching.query.CourseDTO;
import com.school.management.domain.teaching.model.aggregate.Course;
import com.school.management.domain.teaching.repository.CourseRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseApplicationService {

    private final CourseRepository courseRepository;

    /**
     * 创建课程
     */
    @Transactional
    public Long createCourse(CreateCourseCommand command) {
        // 检查课程代码是否已存在
        if (courseRepository.existsByCourseCode(command.getCourseCode())) {
            throw new BusinessException("课程代码已存在: " + command.getCourseCode());
        }

        Course course = Course.builder()
                .courseCode(command.getCourseCode())
                .courseName(command.getCourseName())
                .englishName(command.getEnglishName())
                .courseType(command.getCourseType())
                .courseNature(command.getCourseNature())
                .credits(command.getCredits())
                .totalHours(command.getTotalHours())
                .theoryHours(command.getTheoryHours())
                .labHours(command.getLabHours())
                .practiceHours(command.getPracticeHours())
                .weeklyHours(command.getWeeklyHours())
                .departmentId(command.getDepartmentId())
                .examType(command.getExamType())
                .gradeType(command.getGradeType())
                .prerequisites(command.getPrerequisites())
                .description(command.getDescription())
                .syllabus(command.getSyllabus())
                .status(1) // 默认启用
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        course = courseRepository.save(course);
        log.info("创建课程成功: id={}, code={}", course.getId(), course.getCourseCode());
        return course.getId();
    }

    /**
     * 更新课程
     */
    @Transactional
    public void updateCourse(UpdateCourseCommand command) {
        Course course = courseRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("课程不存在: " + command.getId()));

        if (command.getCourseName() != null) {
            course.setCourseName(command.getCourseName());
        }
        if (command.getEnglishName() != null) {
            course.setEnglishName(command.getEnglishName());
        }
        if (command.getCourseType() != null) {
            course.setCourseType(command.getCourseType());
        }
        if (command.getCourseNature() != null) {
            course.setCourseNature(command.getCourseNature());
        }
        if (command.getCredits() != null) {
            course.setCredits(command.getCredits());
        }
        if (command.getTotalHours() != null) {
            course.setTotalHours(command.getTotalHours());
        }
        if (command.getTheoryHours() != null) {
            course.setTheoryHours(command.getTheoryHours());
        }
        if (command.getLabHours() != null) {
            course.setLabHours(command.getLabHours());
        }
        if (command.getPracticeHours() != null) {
            course.setPracticeHours(command.getPracticeHours());
        }
        if (command.getWeeklyHours() != null) {
            course.setWeeklyHours(command.getWeeklyHours());
        }
        if (command.getDepartmentId() != null) {
            course.setDepartmentId(command.getDepartmentId());
        }
        if (command.getExamType() != null) {
            course.setExamType(command.getExamType());
        }
        if (command.getGradeType() != null) {
            course.setGradeType(command.getGradeType());
        }
        if (command.getPrerequisites() != null) {
            course.setPrerequisites(command.getPrerequisites());
        }
        if (command.getDescription() != null) {
            course.setDescription(command.getDescription());
        }
        if (command.getSyllabus() != null) {
            course.setSyllabus(command.getSyllabus());
        }
        if (command.getStatus() != null) {
            course.setStatus(command.getStatus());
        }

        course.setUpdatedBy(command.getOperatorId());
        course.setUpdatedAt(LocalDateTime.now());

        courseRepository.save(course);
        log.info("更新课程成功: id={}", course.getId());
    }

    /**
     * 删除课程
     */
    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.findById(id).isPresent()) {
            throw new BusinessException("课程不存在: " + id);
        }
        // TODO: 检查课程是否被引用(培养方案、教学任务等)
        courseRepository.deleteById(id);
        log.info("删除课程成功: id={}", id);
    }

    /**
     * 获取课程详情
     */
    public CourseDTO getCourse(Long id) {
        return courseRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("课程不存在: " + id));
    }

    /**
     * 根据课程代码获取课程
     */
    public CourseDTO getCourseByCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("课程不存在: " + courseCode));
    }

    /**
     * 获取所有课程
     */
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据部门获取课程
     */
    public List<CourseDTO> getCoursesByDepartment(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据课程类型获取课程
     */
    public List<CourseDTO> getCoursesByType(Integer courseType) {
        return courseRepository.findByCourseType(courseType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索课程
     */
    public List<CourseDTO> searchCourses(String keyword) {
        return courseRepository.searchByKeyword(keyword).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询课程
     */
    public List<CourseDTO> getCoursesPage(int page, int size, Long departmentId, Integer courseType, Integer status) {
        return courseRepository.findPage(page, size, departmentId, courseType, status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计课程数量
     */
    public long countCourses(Long departmentId, Integer courseType, Integer status) {
        return courseRepository.count(departmentId, courseType, status);
    }

    private CourseDTO toDTO(Course course) {
        return CourseDTO.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .englishName(course.getEnglishName())
                .courseType(course.getCourseType())
                .courseTypeName(CourseDTO.getCourseTypeName(course.getCourseType()))
                .courseNature(course.getCourseNature())
                .courseNatureName(CourseDTO.getCourseNatureName(course.getCourseNature()))
                .credits(course.getCredits())
                .totalHours(course.getTotalHours())
                .theoryHours(course.getTheoryHours())
                .labHours(course.getLabHours())
                .practiceHours(course.getPracticeHours())
                .weeklyHours(course.getWeeklyHours())
                .departmentId(course.getDepartmentId())
                .examType(course.getExamType())
                .examTypeName(CourseDTO.getExamTypeName(course.getExamType()))
                .gradeType(course.getGradeType())
                .gradeTypeName(CourseDTO.getGradeTypeName(course.getGradeType()))
                .prerequisites(course.getPrerequisites())
                .description(course.getDescription())
                .syllabus(course.getSyllabus())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
