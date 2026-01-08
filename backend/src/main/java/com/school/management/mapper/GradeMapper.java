package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Grade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 年级Mapper接口
 * 年级为全校共享资源，不再绑定特定院系
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-29
 */
@Mapper
public interface GradeMapper extends BaseMapper<Grade> {

    /**
     * 分页查询年级列表(带关联信息)
     *
     * @param page 分页对象
     * @param enrollmentYear 入学年份
     * @param status 状态
     * @param keyword 关键词(年级名称或编码)
     * @return 年级列表
     */
    IPage<Grade> selectGradePageWithDetails(
            Page<Grade> page,
            @Param("enrollmentYear") Integer enrollmentYear,
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    /**
     * 根据ID查询年级详情(带关联信息)
     *
     * @param id 年级ID
     * @return 年级详情
     */
    Grade selectGradeWithDetails(@Param("id") Long id);

    /**
     * 根据年级编码查询年级
     *
     * @param gradeCode 年级编码
     * @return 年级
     */
    Grade selectByGradeCode(@Param("gradeCode") String gradeCode);

    /**
     * 查询指定年级主任管理的年级列表
     *
     * @param directorId 年级主任ID
     * @return 年级列表
     */
    List<Grade> selectByDirectorId(@Param("directorId") Long directorId);

    /**
     * 同步年级统计信息(班级数、学生数、平均班级人数)
     *
     * @param gradeId 年级ID
     * @return 更新行数
     */
    int syncGradeStatistics(@Param("gradeId") Long gradeId);

    /**
     * 批量同步所有年级统计信息
     *
     * @return 更新行数
     */
    int batchSyncGradeStatistics();

    /**
     * 检查年级编码是否存在
     *
     * @param gradeCode 年级编码
     * @param excludeId 排除的ID(用于更新时检查)
     * @return 存在数量
     */
    int checkGradeCodeExists(
            @Param("gradeCode") String gradeCode,
            @Param("excludeId") Long excludeId
    );

    /**
     * 检查年级是否有关联的班级
     *
     * @param gradeId 年级ID
     * @return 班级数量
     */
    int countClassesByGradeId(@Param("gradeId") Long gradeId);

    /**
     * 检查年级是否有关联的学生
     *
     * @param gradeId 年级ID
     * @return 学生数量
     */
    int countStudentsByGradeId(@Param("gradeId") Long gradeId);

    /**
     * 更新年级主任
     *
     * @param gradeId 年级ID
     * @param directorId 年级主任ID
     * @return 更新行数
     */
    int updateGradeDirector(
            @Param("gradeId") Long gradeId,
            @Param("directorId") Long directorId
    );

    /**
     * 批量更新年级状态
     *
     * @param gradeIds 年级ID列表
     * @param status 状态
     * @return 更新行数
     */
    int batchUpdateStatus(
            @Param("gradeIds") List<Long> gradeIds,
            @Param("status") Integer status
    );

    /**
     * 查询年级下所有班级的详细信息(用于统计)
     *
     * @param gradeId 年级ID
     * @return 班级列表，包含班级名称和学生数
     */
    List<Map<String, Object>> selectClassDetailsForStatistics(@Param("gradeId") Long gradeId);

    // ==================== 数据权限相关查询 ====================

    /**
     * 查询指定年级主任管理的年级ID列表
     *
     * @param directorId 年级主任ID
     * @return 年级ID列表
     */
    List<Long> selectIdsByDirectorId(@Param("directorId") Long directorId);
}
