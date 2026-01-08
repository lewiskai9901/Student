package com.school.management.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.record.CheckRecordCategoryStatsNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 类别检查统计Mapper（重构版）
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
@Repository("newCheckRecordCategoryStatsMapper")
public interface CheckRecordCategoryStatsMapper extends BaseMapper<CheckRecordCategoryStatsNew> {

    /**
     * 根据检查记录ID查询所有类别统计
     *
     * @param recordId 检查记录ID
     * @return 类别统计列表
     */
    List<CheckRecordCategoryStatsNew> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据班级统计ID查询类别统计
     *
     * @param classStatId 班级统计ID
     * @return 类别统计列表
     */
    List<CheckRecordCategoryStatsNew> selectByClassStatId(@Param("classStatId") Long classStatId);

    /**
     * 根据班级ID和检查记录ID查询类别统计
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 类别统计列表
     */
    List<CheckRecordCategoryStatsNew> selectByRecordAndClass(
            @Param("recordId") Long recordId,
            @Param("classId") Long classId
    );

    /**
     * 根据类别类型查询统计
     *
     * @param recordId 检查记录ID
     * @param categoryType 类别类型
     * @return 类别统计列表
     */
    List<CheckRecordCategoryStatsNew> selectByCategoryType(
            @Param("recordId") Long recordId,
            @Param("categoryType") String categoryType
    );

    /**
     * 根据检查轮次查询类别统计
     *
     * @param recordId 检查记录ID
     * @param checkRound 检查轮次
     * @return 类别统计列表
     */
    List<CheckRecordCategoryStatsNew> selectByCheckRound(
            @Param("recordId") Long recordId,
            @Param("checkRound") Integer checkRound
    );

    /**
     * 批量插入类别统计
     *
     * @param list 类别统计列表
     * @return 插入行数
     */
    int batchInsert(@Param("list") List<CheckRecordCategoryStatsNew> list);

    /**
     * 重新计算类别统计分数
     *
     * @param categoryStatId 类别统计ID
     * @return 更新行数
     */
    int recalculateScore(@Param("categoryStatId") Long categoryStatId);

    /**
     * 删除指定检查记录的所有类别统计
     *
     * @param recordId 检查记录ID
     * @return 删除行数
     */
    int deleteByRecordId(@Param("recordId") Long recordId);
}
