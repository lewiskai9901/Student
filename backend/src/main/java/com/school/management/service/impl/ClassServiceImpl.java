package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.ClassCreateRequest;
import com.school.management.dto.ClassQueryRequest;
import com.school.management.dto.ClassResponse;
import com.school.management.dto.ClassUpdateRequest;
import com.school.management.entity.Class;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.ClassMapper;
import com.school.management.service.ClassService;
import com.school.management.util.AssertUtil;
import com.school.management.annotation.DataPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * 班级服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {

    private final ClassMapper classMapper;
    private final com.school.management.mapper.UserMapper userMapper;
    private final com.school.management.mapper.ClassroomMapper classroomMapper;
    private final com.school.management.mapper.DormitoryMapper dormitoryMapper;
    private final com.school.management.mapper.ClassDormitoryMapper classDormitoryMapper;
    private final com.school.management.mapper.BuildingMapper buildingMapper;
    private final com.school.management.mapper.StudentMapper studentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createClass(ClassCreateRequest request) {
        log.info("创建班级: {}", request.getClassCode());

        // 检查班级编码是否存在
        if (existsClassCode(request.getClassCode(), null)) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "班级编码已存在");
        }

        // 创建班级
        Class clazz = new Class();
        clazz.setClassName(request.getClassName());
        clazz.setClassCode(request.getClassCode());
        clazz.setGradeLevel(request.getGradeLevel());
        clazz.setDepartmentId(request.getDepartmentId());
        clazz.setMajorId(request.getMajorId());
        clazz.setTeacherId(request.getTeacherId());
        clazz.setAssistantTeacherId(request.getAssistantTeacherId());
        clazz.setStudentCount(0); // 初始学生数量为0
        clazz.setClassroomLocation(request.getClassroomLocation());
        clazz.setEnrollmentYear(request.getEnrollmentYear());
        clazz.setGraduationYear(request.getGraduationYear());
        clazz.setClassType(request.getClassType());
        clazz.setStatus(request.getStatus());
        clazz.setGradeId(request.getGradeId());
        clazz.setMajorDirectionId(request.getMajorDirectionId());

        classMapper.insert(clazz);

        log.info("班级创建成功: {} - {}", clazz.getId(), request.getClassCode());
        return clazz.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClass(ClassUpdateRequest request) {
        log.info("更新班级: {}", request.getId());

        Class clazz = classMapper.selectById(request.getId());
        AssertUtil.entityExists(clazz, "班级", request.getId());

        // 检查班级编码是否重复
        AssertUtil.isFalse(
            !clazz.getClassCode().equals(request.getClassCode())
                && existsClassCode(request.getClassCode(), request.getId()),
            "班级编码已存在"
        );

        // 更新班级信息
        clazz.setClassName(request.getClassName());
        clazz.setClassCode(request.getClassCode());
        clazz.setGradeLevel(request.getGradeLevel());
        clazz.setDepartmentId(request.getDepartmentId());
        clazz.setMajorId(request.getMajorId());
        clazz.setTeacherId(request.getTeacherId());
        clazz.setAssistantTeacherId(request.getAssistantTeacherId());
        clazz.setClassroomLocation(request.getClassroomLocation());
        clazz.setEnrollmentYear(request.getEnrollmentYear());
        clazz.setGraduationYear(request.getGraduationYear());
        clazz.setClassType(request.getClassType());
        clazz.setStatus(request.getStatus());
        clazz.setGradeId(request.getGradeId());
        clazz.setMajorDirectionId(request.getMajorDirectionId());
        clazz.setUpdatedAt(LocalDateTime.now());

        classMapper.updateById(clazz);

        log.info("班级更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long id) {
        log.info("删除班级: {}", id);

        Class clazz = classMapper.selectById(id);
        AssertUtil.entityExists(clazz, "班级", id);

        // 实时查询班级下未删除的学生数量
        Integer studentCount = studentMapper.countStudentsByClassId(id);
        AssertUtil.isTrue(
            studentCount == null || studentCount == 0,
            ResultCode.OPERATION_NOT_ALLOWED,
            "班级下还有学生，无法删除"
        );

        // 删除前修改班级编码，加上时间戳后缀，避免唯一约束冲突
        // 这样被删除的班级编码可以被重新使用
        String timestamp = String.valueOf(System.currentTimeMillis());
        clazz.setClassCode(clazz.getClassCode() + "_DEL_" + timestamp);
        classMapper.updateById(clazz);

        // 使用MyBatis-Plus的removeById方法进行逻辑删除
        // 该方法会自动处理@TableLogic注解标注的deleted字段
        classMapper.deleteById(id);

        log.info("班级删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClasses(List<Long> ids) {
        log.info("批量删除班级: {}", ids);

        for (Long id : ids) {
            deleteClass(id);
        }
    }

    @Override
    public ClassResponse getClassById(Long id) {
        // 先查询基本信息
        Class clazz = classMapper.selectById(id);
        if (clazz == null) {
            return null;
        }

        // 通过分页查询获取完整信息（包括关联数据和实时学生数量）
        ClassQueryRequest request = new ClassQueryRequest();
        request.setPageNum(1);
        request.setPageSize(1);

        Page<ClassResponse> page = new Page<>(1, 1);
        // 使用WHERE条件过滤指定ID - 需要在mapper中添加ID查询条件
        IPage<ClassResponse> result = classMapper.selectClassPage(page, request);

        // 从结果中找到匹配的班级
        for (ClassResponse response : result.getRecords()) {
            if (response.getId().equals(id)) {
                return response;
            }
        }

        // 如果没找到，返回基本信息转换的响应
        ClassResponse response = new ClassResponse();
        response.setId(clazz.getId());
        response.setClassName(clazz.getClassName());
        response.setClassCode(clazz.getClassCode());
        response.setGradeLevel(clazz.getGradeLevel());
        response.setGradeId(clazz.getGradeId());
        response.setDepartmentId(clazz.getDepartmentId());
        response.setMajorId(clazz.getMajorId());
        response.setMajorDirectionId(clazz.getMajorDirectionId());
        response.setTeacherId(clazz.getTeacherId());
        response.setAssistantTeacherId(clazz.getAssistantTeacherId());
        response.setClassroomLocation(clazz.getClassroomLocation());
        response.setEnrollmentYear(clazz.getEnrollmentYear() != null ? Year.of(clazz.getEnrollmentYear()) : null);
        response.setGraduationYear(clazz.getGraduationYear() != null ? Year.of(clazz.getGraduationYear()) : null);
        response.setClassType(clazz.getClassType());
        response.setStatus(clazz.getStatus());
        response.setCreatedAt(clazz.getCreatedAt());
        response.setUpdatedAt(clazz.getUpdatedAt());

        return response;
    }

    @Override
    public Class getClassByCode(String classCode) {
        return classMapper.selectByClassCode(classCode);
    }

    @Override
    @DataPermission(module = "class", classField = "classIds", deptField = "departmentIds")
    public IPage<ClassResponse> getClassPage(ClassQueryRequest request) {
        // 数据权限过滤由AOP自动处理
        Page<ClassResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        return classMapper.selectClassPage(page, request);
    }

    @Override
    public List<ClassResponse> getClassesByDepartmentId(Long departmentId) {
        return classMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<ClassResponse> getClassesByTeacherId(Long teacherId) {
        return classMapper.selectByTeacherId(teacherId);
    }

    @Override
    public List<ClassResponse> getClassesByGradeLevel(Integer gradeLevel) {
        return classMapper.selectByGradeLevel(gradeLevel);
    }

    @Override
    public List<ClassResponse> getAllEnabledClasses() {
        return classMapper.selectAllEnabled();
    }

    @Override
    public boolean existsClassCode(String classCode, Long excludeId) {
        return classMapper.countByClassCode(classCode, excludeId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudentCount(Long classId, Integer studentCount) {
        log.info("更新班级学生数量: {} -> {}", classId, studentCount);

        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班级不存在");
        }

        classMapper.updateStudentCount(classId, studentCount);

        log.info("班级学生数量更新成功: {}", classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("更新班级状态: {} -> {}", id, status);

        Class clazz = classMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班级不存在");
        }

        clazz.setStatus(status);
        clazz.setUpdatedAt(LocalDateTime.now());
        classMapper.updateById(clazz);

        log.info("班级状态更新成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTeacher(Long classId, Long teacherId) {
        log.info("设置班主任: 班级ID={}, 教师ID={}", classId, teacherId);

        // 检查班级是否存在
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班级不存在");
        }

        // 如果teacherId不为空,检查教师是否存在
        if (teacherId != null) {
            com.school.management.entity.User teacher = userMapper.selectById(teacherId);
            if (teacher == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "教师不存在");
            }
        }

        // 更新班主任
        clazz.setTeacherId(teacherId);
        clazz.setUpdatedAt(LocalDateTime.now());
        classMapper.updateById(clazz);

        log.info("班主任设置成功: 班级ID={}, 教师ID={}", classId, teacherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignClassroom(Long classId, Long classroomId) {
        log.info("为班级分配教室: 班级ID={}, 教室ID={}", classId, classroomId);

        // 检查班级是否存在
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班级不存在");
        }

        // 检查教室是否存在
        com.school.management.entity.Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "教室不存在");
        }

        // 检查教室是否已被其他班级占用
        if (classroom.getClassId() != null && !classroom.getClassId().equals(classId)) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "该教室已被其他班级占用");
        }

        // 更新教室的班级关联
        classroom.setClassId(classId);
        classroom.setClassName(clazz.getClassName());
        classroom.setHeadTeacherId(clazz.getTeacherId());
        classroom.setStudentCount(clazz.getStudentCount());
        classroomMapper.updateById(classroom);

        log.info("班级教室分配成功: 班级ID={}, 教室ID={}", classId, classroomId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeClassroom(Long classId) {
        log.info("取消班级教室分配: {}", classId);

        // 查找该班级的教室
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.school.management.entity.Classroom> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("class_id", classId);

        List<com.school.management.entity.Classroom> classrooms = classroomMapper.selectList(wrapper);
        for (com.school.management.entity.Classroom classroom : classrooms) {
            classroom.setClassId(null);
            classroom.setClassName(null);
            classroom.setHeadTeacherId(null);
            classroom.setStudentCount(0);
            classroomMapper.updateById(classroom);
        }

        log.info("班级教室分配已取消: {}", classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDormitory(Long classId, Long dormitoryId, Integer allocatedBeds) {
        log.info("为班级添加宿舍: 班级ID={}, 宿舍ID={}, 床位数={}", classId, dormitoryId, allocatedBeds);

        // 检查班级是否存在
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班级不存在");
        }

        // 检查宿舍是否存在
        com.school.management.entity.Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
        if (dormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "宿舍不存在");
        }

        // 检查床位数是否合理
        Integer bedCount = dormitory.getBedCount();
        if (bedCount == null || bedCount <= 0) {
            throw new BusinessException(ResultCode.DATA_INVALID, "宿舍床位数配置不正确");
        }
        if (allocatedBeds > bedCount) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "分配床位数不能超过宿舍容量");
        }

        // 检查剩余床位是否足够
        Integer occupiedBeds = dormitory.getOccupiedBeds() != null ? dormitory.getOccupiedBeds() : 0;
        if (occupiedBeds + allocatedBeds > bedCount) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED,
                String.format("宿舍剩余床位不足,总床位:%d,已占用:%d,剩余:%d", bedCount, occupiedBeds, bedCount - occupiedBeds));
        }

        // 检查是否已存在该关联 (注意: class_dormitory_bindings表没有deleted字段)
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.school.management.entity.ClassDormitory> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("class_id", classId).eq("dormitory_id", dormitoryId);

        if (classDormitoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "该宿舍已分配给此班级");
        }

        // 创建班级宿舍关联
        com.school.management.entity.ClassDormitory classDormitory = new com.school.management.entity.ClassDormitory();
        classDormitory.setClassId(classId);
        classDormitory.setDormitoryId(dormitoryId);
        classDormitory.setStudentCount(allocatedBeds); // 使用studentCount字段，表示分配的床位数
        classDormitoryMapper.insert(classDormitory);

        // 注意: 不更新occupiedBeds，因为occupiedBeds表示实际入住人数
        // 只有当学生真正入住宿舍时才会更新occupiedBeds
        // allocatedBeds(分配床位数)和occupiedBeds(实际入住数)是两个不同的概念

        log.info("班级宿舍添加成功: 班级ID={}, 宿舍ID={}, 分配床位数={}", classId, dormitoryId, allocatedBeds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDormitory(Long classId, Long dormitoryId) {
        log.info("移除班级宿舍: 班级ID={}, 宿舍ID={}", classId, dormitoryId);

        // 查找关联记录 (注意: class_dormitory_bindings表没有deleted字段)
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.school.management.entity.ClassDormitory> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("class_id", classId).eq("dormitory_id", dormitoryId);

        com.school.management.entity.ClassDormitory classDormitory = classDormitoryMapper.selectOne(wrapper);
        if (classDormitory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班级宿舍关联不存在");
        }

        // 物理删除关联记录 (表中没有deleted字段)
        classDormitoryMapper.deleteById(classDormitory.getId());

        // 注意: 不更新occupiedBeds，因为occupiedBeds表示实际入住人数
        // 移除班级宿舍分配不影响实际入住人数
        // 只有学生退宿时才需要更新occupiedBeds

        log.info("班级宿舍移除成功: 班级ID={}, 宿舍ID={}, 释放分配床位数={}", classId, dormitoryId, classDormitory.getStudentCount());
    }

    @Override
    public List getClassDormitories(Long classId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.school.management.entity.ClassDormitory> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("class_id", classId);

        List<com.school.management.entity.ClassDormitory> classDormitories = classDormitoryMapper.selectList(wrapper);

        // 获取宿舍详情
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.school.management.entity.ClassDormitory cd : classDormitories) {
            com.school.management.entity.Dormitory dormitory = dormitoryMapper.selectById(cd.getDormitoryId());
            if (dormitory != null) {
                // 查询关联的建筑信息
                com.school.management.entity.Building building = null;
                if (dormitory.getBuildingId() != null) {
                    building = buildingMapper.selectById(dormitory.getBuildingId());
                }

                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", dormitory.getId());
                map.put("dormitoryId", dormitory.getId()); // 添加dormitoryId字段，与前端保持一致
                map.put("dormitoryNo", dormitory.getDormitoryNo());
                map.put("buildingNo", building != null ? building.getBuildingNo() : null); // 楼号
                map.put("buildingName", building != null ? building.getBuildingName() : null); // 楼宇名称
                map.put("floorNumber", dormitory.getFloorNumber());
                // 使用新的标准字段
                map.put("bedCapacity", dormitory.getBedCapacity()); // 床位容量规格
                map.put("bedCount", dormitory.getBedCount()); // 实际床位数
                map.put("roomUsageType", dormitory.getRoomUsageType()); // 房间用途类型
                map.put("occupiedBeds", dormitory.getOccupiedBeds());
                map.put("allocatedBeds", cd.getStudentCount()); // 使用studentCount字段
                map.put("genderType", dormitory.getGenderType());
                map.put("status", dormitory.getStatus());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public Object getClassClassroom(Long classId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.school.management.entity.Classroom> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("class_id", classId);

        com.school.management.entity.Classroom classroom = classroomMapper.selectOne(wrapper);
        if (classroom == null) {
            return null;
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", classroom.getId());
        result.put("classroomName", classroom.getClassroomNo()); // 使用classroomNo字段
        result.put("classroomCode", classroom.getClassroomNo()); // 使用classroomNo字段
        result.put("buildingId", classroom.getBuildingId());
        result.put("floor", classroom.getFloor());
        result.put("roomNumber", classroom.getClassroomNo()); // 使用classroomNo作为房间号
        result.put("capacity", classroom.getCapacity());
        result.put("classroomType", classroom.getClassroomType());
        result.put("facilities", ""); // 数据库中没有此字段，返回空字符串
        result.put("status", classroom.getStatus());

        return result;
    }
}