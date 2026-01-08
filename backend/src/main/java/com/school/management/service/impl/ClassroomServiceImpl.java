package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Class;
import com.school.management.entity.Classroom;
import com.school.management.entity.User;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.BuildingMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.ClassroomMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教室服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomMapper classroomMapper;
    private final BuildingMapper buildingMapper;
    private final ClassMapper classMapper;
    private final UserMapper userMapper;

    @Override
    public IPage<Classroom> page(Page<Classroom> page, Long buildingId, Integer floor,
                                String classroomType, Integer status) {
        return classroomMapper.selectClassroomPage(page, buildingId, floor, classroomType, status);
    }

    @Override
    public Classroom getById(Long id) {
        Classroom classroom = classroomMapper.selectById(id);
        if (classroom == null) {
            throw new BusinessException("教室不存在");
        }
        return classroom;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Classroom create(Classroom classroom) {
        // 检查教学楼是否存在
        if (buildingMapper.selectById(classroom.getBuildingId()) == null) {
            throw new BusinessException("教学楼不存在");
        }

        // 检查编号是否重复
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Classroom::getClassroomCode, classroom.getClassroomCode());
        if (classroomMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("教室编号已存在");
        }

        classroomMapper.insert(classroom);
        return classroom;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Classroom update(Long id, Classroom classroom) {
        Classroom existing = getById(id);

        // 检查教学楼是否存在
        if (classroom.getBuildingId() != null &&
            !classroom.getBuildingId().equals(existing.getBuildingId())) {
            if (buildingMapper.selectById(classroom.getBuildingId()) == null) {
                throw new BusinessException("教学楼不存在");
            }
        }

        // 如果编号有变化,检查新编号是否重复
        if (classroom.getClassroomCode() != null &&
            !existing.getClassroomCode().equals(classroom.getClassroomCode())) {
            LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Classroom::getClassroomCode, classroom.getClassroomCode())
                   .ne(Classroom::getId, id);
            if (classroomMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("教室编号已存在");
            }
        }

        classroom.setId(id);
        classroomMapper.updateById(classroom);
        return classroom;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        classroomMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Classroom assignClass(Long id, Long classId) {
        Classroom classroom = getById(id);

        // 如果是解除关联
        if (classId == null) {
            classroom.setClassId(null);
            classroom.setClassName(null);
            classroom.setHeadTeacherId(null);
            classroom.setHeadTeacherName(null);
            classroom.setStudentCount(0);
            classroomMapper.updateById(classroom);
            return classroom;
        }

        // 检查班级是否存在
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }

        // 设置班级信息
        classroom.setClassId(classId);
        classroom.setClassName(clazz.getClassName());

        // 设置班主任信息
        if (clazz.getTeacherId() != null) {
            User teacher = userMapper.selectById(clazz.getTeacherId());
            if (teacher != null) {
                classroom.setHeadTeacherId(teacher.getId());
                classroom.setHeadTeacherName(teacher.getRealName());
            }
        }

        // 设置学生人数
        classroom.setStudentCount(clazz.getStudentCount() != null ? clazz.getStudentCount() : 0);

        classroomMapper.updateById(classroom);
        return classroom;
    }
}
