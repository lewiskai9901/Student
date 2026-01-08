package com.school.management.infrastructure.persistence.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 评价结果 Mapper
 */
@Mapper
public interface RatingResultMapper extends BaseMapper<RatingResultPO> {

    /**
     * 根据评价配置ID查找结果
     */
    @Select("SELECT * FROM rating_results WHERE rating_config_id = #{ratingConfigId} AND deleted = 0")
    List<RatingResultPO> findByRatingConfigId(@Param("ratingConfigId") Long ratingConfigId);

    /**
     * 根据班级ID查找结果
     */
    @Select("SELECT * FROM rating_results WHERE class_id = #{classId} AND deleted = 0")
    List<RatingResultPO> findByClassId(@Param("classId") Long classId);

    /**
     * 根据状态查找结果
     */
    @Select("SELECT * FROM rating_results WHERE status = #{status} AND deleted = 0")
    List<RatingResultPO> findByStatus(@Param("status") String status);

    /**
     * 根据配置ID和周期查找结果
     */
    @Select("SELECT * FROM rating_results WHERE rating_config_id = #{ratingConfigId} " +
            "AND period_start = #{periodStart} AND period_end = #{periodEnd} AND deleted = 0")
    List<RatingResultPO> findByConfigAndPeriod(
            @Param("ratingConfigId") Long ratingConfigId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * 查找待审批的结果
     */
    @Select("SELECT * FROM rating_results WHERE status = 'SUBMITTED' AND deleted = 0")
    List<RatingResultPO> findPendingApproval();

    /**
     * 根据状态统计数量
     */
    @Select("SELECT COUNT(1) FROM rating_results WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);

    /**
     * 检查ID是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM rating_results WHERE id = #{id} AND deleted = 0")
    boolean existsById(@Param("id") Long id);
}
