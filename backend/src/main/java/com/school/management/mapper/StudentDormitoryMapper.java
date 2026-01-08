package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.StudentDormitoryQueryRequest;
import com.school.management.entity.StudentDormitory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生住宿记录Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface StudentDormitoryMapper extends BaseMapper<StudentDormitory> {

    /**
     * 分页查询住宿记录(含关联信息)
     */
    Page<StudentDormitory> selectPageWithDetails(Page<StudentDormitory> page, @Param("req") StudentDormitoryQueryRequest request);

    /**
     * 根据学生ID查询当前住宿信息
     */
    StudentDormitory selectCurrentByStudentId(@Param("studentId") Long studentId);

    /**
     * 根据宿舍ID查询当前入住学生列表
     */
    List<StudentDormitory> selectCurrentByDormitoryId(@Param("dormitoryId") Long dormitoryId);

    /**
     * 查询学生住宿历史记录
     */
    List<StudentDormitory> selectHistoryByStudentId(@Param("studentId") Long studentId);

    /**
     * 统计宿舍当前入住人数
     */
    Integer countCurrentOccupancy(@Param("dormitoryId") Long dormitoryId);

    /**
     * 检查床位是否已被占用
     */
    Integer checkBedOccupied(@Param("dormitoryId") Long dormitoryId, @Param("bedNumber") String bedNumber, @Param("excludeId") Long excludeId);
}
