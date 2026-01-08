package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.ClassCreateRequest;
import com.school.management.dto.ClassQueryRequest;
import com.school.management.dto.ClassResponse;
import com.school.management.dto.ClassUpdateRequest;
import com.school.management.entity.Class;

import java.util.List;

/**
 * 班级服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface ClassService {

    /**
     * 创建班级
     *
     * @param request 创建请求
     * @return 班级ID
     */
    Long createClass(ClassCreateRequest request);

    /**
     * 更新班级
     *
     * @param request 更新请求
     */
    void updateClass(ClassUpdateRequest request);

    /**
     * 删除班级
     *
     * @param id 班级ID
     */
    void deleteClass(Long id);

    /**
     * 批量删除班级
     *
     * @param ids 班级ID列表
     */
    void deleteClasses(List<Long> ids);

    /**
     * 根据ID获取班级
     *
     * @param id 班级ID
     * @return 班级信息
     */
    ClassResponse getClassById(Long id);

    /**
     * 根据班级编码获取班级
     *
     * @param classCode 班级编码
     * @return 班级
     */
    Class getClassByCode(String classCode);

    /**
     * 分页查询班级
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<ClassResponse> getClassPage(ClassQueryRequest request);

    /**
     * 根据部门ID查询班级列表
     *
     * @param departmentId 部门ID
     * @return 班级列表
     */
    List<ClassResponse> getClassesByDepartmentId(Long departmentId);

    /**
     * 根据班主任ID查询班级列表
     *
     * @param teacherId 班主任ID
     * @return 班级列表
     */
    List<ClassResponse> getClassesByTeacherId(Long teacherId);

    /**
     * 根据年级查询班级列表
     *
     * @param gradeLevel 年级
     * @return 班级列表
     */
    List<ClassResponse> getClassesByGradeLevel(Integer gradeLevel);

    /**
     * 获取所有启用的班级
     *
     * @return 班级列表
     */
    List<ClassResponse> getAllEnabledClasses();

    /**
     * 检查班级编码是否存在
     *
     * @param classCode 班级编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsClassCode(String classCode, Long excludeId);

    /**
     * 更新班级学生数量
     *
     * @param classId 班级ID
     * @param studentCount 学生数量
     */
    void updateStudentCount(Long classId, Integer studentCount);

    /**
     * 更新班级状态
     *
     * @param id 班级ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 设置班主任
     *
     * @param classId 班级ID
     * @param teacherId 教师ID
     */
    void assignTeacher(Long classId, Long teacherId);

    /**
     * 为班级分配教室
     *
     * @param classId 班级ID
     * @param classroomId 教室ID
     */
    void assignClassroom(Long classId, Long classroomId);

    /**
     * 取消班级教室分配
     *
     * @param classId 班级ID
     */
    void removeClassroom(Long classId);

    /**
     * 为班级添加宿舍
     *
     * @param classId 班级ID
     * @param dormitoryId 宿舍ID
     * @param allocatedBeds 分配床位数
     */
    void addDormitory(Long classId, Long dormitoryId, Integer allocatedBeds);

    /**
     * 移除班级宿舍
     *
     * @param classId 班级ID
     * @param dormitoryId 宿舍ID
     */
    void removeDormitory(Long classId, Long dormitoryId);

    /**
     * 获取班级的宿舍列表
     *
     * @param classId 班级ID
     * @return 宿舍列表
     */
    List getClassDormitories(Long classId);

    /**
     * 获取班级的教室信息
     *
     * @param classId 班级ID
     * @return 教室信息
     */
    Object getClassClassroom(Long classId);
}