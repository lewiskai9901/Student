package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.StudentQueryRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.dto.quickentry.QuickEntryStudentDTO;
import com.school.management.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询学生信息
     *
     * @param page 分页参数
     * @param request 查询条件
     * @return 学生列表
     */
    IPage<StudentResponse> selectStudentPage(Page<StudentResponse> page, @Param("req") StudentQueryRequest request);

    /**
     * 根据ID获取学生详细信息
     *
     * @param id 学生ID
     * @return 学生详细信息
     */
    StudentResponse selectStudentById(@Param("id") Long id);

    /**
     * 根据学号获取学生信息
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    StudentResponse selectStudentByNo(@Param("studentNo") String studentNo);

    /**
     * 根据用户ID获取学生信息
     *
     * @param userId 用户ID
     * @return 学生信息
     */
    StudentResponse selectStudentByUserId(@Param("userId") Long userId);

    /**
     * 统计班级学生数量
     *
     * @param classId 班级ID
     * @return 学生数量
     */
    Integer countStudentsByClassId(@Param("classId") Long classId);

    /**
     * 统计宿舍学生数量
     *
     * @param dormitoryId 宿舍ID
     * @return 学生数量
     */
    Integer countStudentsByDormitoryId(@Param("dormitoryId") Long dormitoryId);

    /**
     * 清空指定宿舍内所有学生的宿舍信息
     *
     * @param dormitoryId 宿舍ID
     */
    void clearDormitoryByDormitoryId(@Param("dormitoryId") Long dormitoryId);

    /**
     * 清空指定宿舍楼内所有学生的宿舍信息
     *
     * @param buildingId 宿舍楼ID
     */
    void clearDormitoryByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据班级ID查询学生列表
     *
     * @param classId 班级ID
     * @return 学生列表
     */
    List<StudentResponse> selectByClassId(@Param("classId") Long classId);

    /**
     * 快捷录入 - 搜索学生
     *
     * @param keyword  搜索关键词 (姓名或学号)
     * @param classIds 目标班级ID列表
     * @param limit    返回数量限制
     * @return 学生列表
     */
    List<QuickEntryStudentDTO> searchStudentsForQuickEntry(
            @Param("keyword") String keyword,
            @Param("classIds") List<Long> classIds,
            @Param("limit") Integer limit
    );

    /**
     * 统计班级学生数量（按性别）
     *
     * @param classId 班级ID
     * @param gender 性别（男/女）
     * @return 学生数量
     */
    Long countByClassIdAndGender(@Param("classId") Long classId, @Param("gender") String gender);

    /**
     * 简单搜索学生（用于选择器，不使用分页）
     *
     * @param keyword 搜索关键词（姓名或学号）
     * @param classId 班级ID（可选）
     * @param limit   返回数量限制
     * @return 学生列表
     */
    List<StudentResponse> searchStudentsSimple(
            @Param("keyword") String keyword,
            @Param("classId") Long classId,
            @Param("limit") Integer limit
    );
}