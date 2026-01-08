package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckRatingResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 检查评级结果Mapper
 *
 * @author system
 * @since 3.1.0
 */
@Mapper
public interface CheckRatingResultMapper extends BaseMapper<CheckRatingResult> {

    /**
     * 根据记录ID查询评级结果,并连接等级表获取颜色和积分
     */
    @Select("SELECT r.*, l.level_color, l.reward_points " +
            "FROM check_rating_results r " +
            "LEFT JOIN check_rating_levels l ON r.level_id = l.id " +
            "WHERE r.record_id = #{recordId} " +
            "ORDER BY r.ranking")
    List<CheckRatingResult> selectByRecordIdWithLevel(@Param("recordId") Long recordId);

    /**
     * 调用存储过程计算评级
     */
    @Select("CALL calculate_ratings(#{recordId}, #{configId})")
    void calculateRatings(@Param("recordId") Long recordId, @Param("configId") Long configId);

    /**
     * 根据记录ID和班级ID查询评级结果
     */
    @Select("SELECT r.*, l.level_color, l.reward_points " +
            "FROM check_rating_results r " +
            "LEFT JOIN check_rating_levels l ON r.level_id = l.id " +
            "WHERE r.record_id = #{recordId} AND r.class_id = #{classId}")
    CheckRatingResult selectByRecordIdAndClassId(@Param("recordId") Long recordId, @Param("classId") Long classId);

    /**
     * 统计各等级班级数量
     */
    @Select("SELECT level_name, COUNT(*) as count FROM check_rating_results " +
            "WHERE record_id = #{recordId} GROUP BY level_name ORDER BY level_name")
    List<java.util.Map<String, Object>> countByLevelName(@Param("recordId") Long recordId);
}
