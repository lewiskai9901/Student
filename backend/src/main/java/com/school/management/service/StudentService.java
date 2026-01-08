package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.dto.StudentCreateRequest;
import com.school.management.dto.StudentQueryRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.dto.StudentUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 学生服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface StudentService {

    /**
     * 创建学生
     *
     * @param request 创建请求
     * @return 学生ID
     */
    Long createStudent(StudentCreateRequest request);

    /**
     * 更新学生信息
     *
     * @param request 更新请求
     */
    void updateStudent(StudentUpdateRequest request);

    /**
     * 删除学生
     *
     * @param id 学生ID
     */
    void deleteStudent(Long id);

    /**
     * 批量删除学生
     *
     * @param ids 学生ID列表
     */
    void deleteStudents(List<Long> ids);

    /**
     * 根据ID获取学生信息
     *
     * @param id 学生ID
     * @return 学生信息
     */
    StudentResponse getStudentById(Long id);

    /**
     * 根据学号获取学生信息
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    StudentResponse getStudentByNo(String studentNo);

    /**
     * 根据用户ID获取学生信息
     *
     * @param userId 用户ID
     * @return 学生信息
     */
    StudentResponse getStudentByUserId(Long userId);

    /**
     * 分页查询学生
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<StudentResponse> getStudentPage(StudentQueryRequest request);

    /**
     * 快速搜索学生(用于选择器)
     *
     * @param keyword 搜索关键字(姓名或学号)
     * @param classId 班级ID(可选)
     * @param limit 返回数量限制
     * @return 学生列表
     */
    List<StudentResponse> searchStudents(String keyword, Long classId, Integer limit);

    /**
     * 检查学号是否存在
     *
     * @param studentNo 学号
     * @param excludeId 排除的学生ID
     * @return 是否存在
     */
    boolean existsStudentNo(String studentNo, Long excludeId);

    /**
     * 更新学生状态
     *
     * @param id 学生ID
     * @param status 状态
     */
    void updateStudentStatus(Long id, Integer status);

    /**
     * 分配宿舍
     *
     * @param studentId 学生ID
     * @param dormitoryId 宿舍ID
     * @param bedNumber 床位号
     */
    void assignDormitory(Long studentId, Long dormitoryId, String bedNumber);

    /**
     * 从宿舍移除学生
     *
     * @param studentId 学生ID
     */
    void removeDormitory(Long studentId);

    /**
     * 转班
     *
     * @param studentId 学生ID
     * @param newClassId 新班级ID
     */
    void transferClass(Long studentId, Long newClassId);

    /**
     * 重置密码
     *
     * @param studentId 学生ID
     * @param newPassword 新密码
     */
    void resetPassword(Long studentId, String newPassword);

    /**
     * 统计班级学生数量
     *
     * @param classId 班级ID
     * @return 学生数量
     */
    Integer countStudentsByClassId(Long classId);

    /**
     * 统计宿舍学生数量
     *
     * @param dormitoryId 宿舍ID
     * @return 学生数量
     */
    Integer countStudentsByDormitoryId(Long dormitoryId);

    /**
     * 导出学生数据到Excel
     *
     * @param request 查询条件
     * @return Excel文件字节数组
     */
    byte[] exportStudents(StudentQueryRequest request) throws IOException;

    /**
     * 从Excel导入学生数据
     *
     * @param file Excel文件
     * @return 导入结果信息
     */
    String importStudents(MultipartFile file) throws IOException;

    /**
     * 获取导入模板
     *
     * @return Excel模板字节数组
     */
    byte[] getImportTemplate() throws IOException;

    /**
     * 根据班级ID获取学生列表
     *
     * @param classId 班级ID
     * @return 学生列表
     */
    List<StudentResponse> getStudentsByClassId(Long classId);

    /**
     * 预览导入数据
     *
     * @param file Excel文件
     * @return 预览数据
     */
    Object previewImportData(MultipartFile file) throws IOException;

    /**
     * 确认导入数据
     *
     * @param data 待导入数据
     * @return 导入结果
     */
    Object confirmImport(java.util.List<java.util.Map<String, Object>> data) throws IOException;

    /**
     * 导出导入失败的数据
     *
     * @param data 失败数据列表
     * @return Excel字节数组
     */
    byte[] exportFailedData(java.util.List<java.util.Map<String, Object>> data) throws IOException;
}