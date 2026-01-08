package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.Course;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.CourseMapper;
import com.school.management.service.evaluation.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 课程服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course>
        implements CourseService {

    private final CourseMapper courseMapper;

    @Override
    public Page<Map<String, Object>> pageCourses(Page<?> page, Map<String, Object> query) {
        return courseMapper.selectCoursePage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(Course course) {
        // 检查课程编码是否存在
        if (existsByCode(course.getCourseCode(), null)) {
            throw new BusinessException("课程编码已存在");
        }

        // 设置默认值
        if (course.getStatus() == null) {
            course.setStatus(1);
        }

        courseMapper.insert(course);
        log.info("创建课程: id={}, code={}, name={}", course.getId(), course.getCourseCode(), course.getCourseName());
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(Course course) {
        Course existing = courseMapper.selectById(course.getId());
        if (existing == null) {
            throw new BusinessException("课程不存在");
        }

        // 检查课程编码是否存在（排除自己）
        if (course.getCourseCode() != null && existsByCode(course.getCourseCode(), course.getId())) {
            throw new BusinessException("课程编码已存在");
        }

        courseMapper.updateById(course);
        log.info("更新课程: id={}", course.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        Course existing = courseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("课程不存在");
        }

        // 检查是否有成绩数据
        int scoreCount = courseMapper.countScoresByCourseId(id);
        if (scoreCount > 0) {
            throw new BusinessException("该课程已有成绩数据，无法删除");
        }

        courseMapper.deleteById(id);
        log.info("删除课程: id={}", id);
    }

    @Override
    public Map<String, Object> getCourseDetail(Long id) {
        Map<String, Object> detail = courseMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("课程不存在");
        }
        return detail;
    }

    @Override
    public List<Course> getBySemesterId(Long semesterId) {
        return courseMapper.selectBySemesterId(semesterId);
    }

    @Override
    public List<Map<String, Object>> getByClassId(Long classId, Long semesterId) {
        return courseMapper.selectByClassId(classId, semesterId);
    }

    @Override
    public boolean existsByCode(String courseCode, Long excludeId) {
        Course existing = courseMapper.selectByCode(courseCode);
        if (existing == null) {
            return false;
        }
        return excludeId == null || !existing.getId().equals(excludeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importCourses(List<Course> courses, Long semesterId) {
        int count = 0;
        for (Course course : courses) {
            try {
                // Course实体不包含semesterId字段，课程与学期的关系通过课程安排表管理
                if (!existsByCode(course.getCourseCode(), null)) {
                    courseMapper.insert(course);
                    count++;
                } else {
                    // 更新已存在的课程
                    Course existing = courseMapper.selectByCode(course.getCourseCode());
                    course.setId(existing.getId());
                    courseMapper.updateById(course);
                }
            } catch (Exception e) {
                log.warn("导入课程失败: code={}, error={}", course.getCourseCode(), e.getMessage());
            }
        }
        log.info("导入课程完成: total={}, imported={}", courses.size(), count);
        return count;
    }

    @Override
    public List<Map<String, Object>> getCourseTypes() {
        List<Map<String, Object>> types = new ArrayList<>();

        Map<String, Object> required = new HashMap<>();
        required.put("value", "required");
        required.put("label", "必修课");
        types.add(required);

        Map<String, Object> elective = new HashMap<>();
        elective.put("value", "elective");
        elective.put("label", "选修课");
        types.add(elective);

        Map<String, Object> practice = new HashMap<>();
        practice.put("value", "practice");
        practice.put("label", "实践课");
        types.add(practice);

        Map<String, Object> general = new HashMap<>();
        general.put("value", "general");
        general.put("label", "通识课");
        types.add(general);

        return types;
    }
}
