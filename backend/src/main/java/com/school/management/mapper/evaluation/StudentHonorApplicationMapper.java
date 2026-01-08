package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.StudentHonorApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 学生荣誉申报Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface StudentHonorApplicationMapper extends BaseMapper<StudentHonorApplication> {

    /**
     * 分页查询荣誉申报
     */
    Page<Map<String, Object>> selectApplicationPage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 查询学生的申报列表
     */
    List<Map<String, Object>> selectByStudentId(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    /**
     * 查询申报详情
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 根据学生和荣誉类型查询
     */
    StudentHonorApplication selectByStudentAndHonorType(@Param("studentId") Long studentId,
                                                        @Param("honorTypeId") Long honorTypeId,
                                                        @Param("periodId") Long periodId);

    /**
     * 查询待审核列表
     */
    List<Map<String, Object>> selectPendingReview(@Param("targetStatus") int targetStatus,
                                                   @Param("reviewerId") Long reviewerId,
                                                   @Param("reviewLevel") String reviewLevel);

    /**
     * 获取审核统计
     */
    Map<String, Object> selectReviewStatistics(@Param("periodId") Long periodId);

    /**
     * 导出申报数据
     */
    List<Map<String, Object>> selectForExport(@Param("query") Map<String, Object> query);

    /**
     * 按周期和学生查询已通过的荣誉申报
     */
    List<Map<String, Object>> selectApprovedByStudentAndPeriod(@Param("studentId") Long studentId,
                                                                @Param("periodId") Long periodId);

    /**
     * 按维度查询学生已通过的荣誉
     */
    List<Map<String, Object>> selectApprovedByDimension(@Param("studentId") Long studentId,
                                                         @Param("periodId") Long periodId,
                                                         @Param("dimension") String dimension);

    /**
     * 根据申报编号查询
     */
    StudentHonorApplication selectByCode(@Param("code") String code);

    /**
     * 查询同一事迹的申报
     */
    List<StudentHonorApplication> selectBySameEventGroup(@Param("sameEventGroup") String sameEventGroup);

    /**
     * 查询已通过待同步的申报
     */
    List<StudentHonorApplication> selectApprovedNotSynced(@Param("periodId") Long periodId);
}
