package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.ClassQueryRequest;
import com.school.management.dto.ClassResponse;
import com.school.management.entity.Class;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班级Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface ClassMapper extends BaseMapper<Class> {

    /**
     * 分页查询班级
     *
     * @param page 分页参数
     * @param request 查询条件
     * @return 班级列表
     */
    IPage<ClassResponse> selectClassPage(Page<ClassResponse> page,
                                        @Param("req") ClassQueryRequest request);

    /**
     * 根据班级编码获取班级
     *
     * @param classCode 班级编码
     * @return 班级
     */
    Class selectByClassCode(@Param("classCode") String classCode);

    /**
     * 检查班级编码是否存在
     *
     * @param classCode 班级编码
     * @param excludeId 排除的ID
     * @return 数量
     */
    Integer countByClassCode(@Param("classCode") String classCode, @Param("excludeId") Long excludeId);

    /**
     * 根据组织单元ID查询班级列表
     *
     * @param orgUnitId 组织单元ID
     * @return 班级列表
     */
    List<ClassResponse> selectByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据班主任ID查询班级列表
     *
     * @param teacherId 班主任ID
     * @return 班级列表
     */
    List<ClassResponse> selectByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 根据年级查询班级列表
     *
     * @param gradeLevel 年级
     * @return 班级列表
     */
    List<ClassResponse> selectByGradeLevel(@Param("gradeLevel") Integer gradeLevel);

    /**
     * 根据入学年份查询班级列表
     *
     * @param enrollmentYear 入学年份
     * @return 班级列表
     */
    List<ClassResponse> selectByEnrollmentYear(@Param("enrollmentYear") Integer enrollmentYear);

    /**
     * 获取所有启用的班级
     *
     * @return 班级列表
     */
    List<ClassResponse> selectAllEnabled();

    /**
     * 根据ID查询班级详情（包含关联数据和实时学生数量）
     *
     * @param id 班级ID
     * @return 班级详情
     */
    ClassResponse selectClassResponseById(@Param("id") Long id);

    /**
     * 更新班级学生数量
     *
     * @param classId 班级ID
     * @param studentCount 学生数量
     * @return 更新数量
     */
    Integer updateStudentCount(@Param("classId") Long classId, @Param("studentCount") Integer studentCount);

    /**
     * 根据年级ID和专业ID统计班级数量
     *
     * @param gradeId 年级ID
     * @param majorId 专业ID
     * @return 班级数量
     */
    Integer countByGradeIdAndMajorId(@Param("gradeId") Long gradeId, @Param("majorId") Long majorId);

    /**
     * 根据专业ID统计班级数量
     *
     * @param majorId 专业ID
     * @return 班级数量
     */
    Integer countByMajorId(@Param("majorId") Long majorId);

    /**
     * 统计年级专业方向关联的实际班级数和学生数
     *
     * @param gradeId 年级ID
     * @param majorId 专业ID
     * @return 包含classCount和studentCount的Map
     */
    java.util.Map<String, Object> countActualStatsByGradeAndMajor(@Param("gradeId") Long gradeId, @Param("majorId") Long majorId);

    /**
     * 根据ID列表批量查询班级详情
     *
     * @param ids 班级ID列表
     * @return 班级详情列表
     */
    List<ClassResponse> selectClassResponseByIds(@Param("ids") List<Long> ids);

    // ==================== 数据权限相关查询 ====================

    /**
     * 根据组织单元ID列表查询班级ID列表
     *
     * @param orgUnitIds 组织单元ID列表
     * @return 班级ID列表
     */
    List<Long> selectIdsByOrgUnitIds(@Param("orgUnitIds") List<Long> orgUnitIds);

    /**
     * 根据年级ID列表查询班级ID列表
     *
     * @param gradeIds 年级ID列表
     * @return 班级ID列表
     */
    List<Long> selectIdsByGradeIds(@Param("gradeIds") List<Long> gradeIds);

    /**
     * 根据教师ID查询其管理的班级ID列表（班主任或副班主任）
     *
     * @param userId 教师用户ID
     * @return 班级ID列表
     */
    List<Long> selectManagedClassIds(@Param("userId") Long userId);

    /**
     * 根据教师ID查询其管理的班级所在年级ID列表
     *
     * @param userId 教师用户ID
     * @return 年级ID列表
     */
    List<Long> selectManagedGradeIds(@Param("userId") Long userId);
}