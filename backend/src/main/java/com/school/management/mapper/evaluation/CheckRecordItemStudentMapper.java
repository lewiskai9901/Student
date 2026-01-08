package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.CheckRecordItemStudent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 扣分项-学生关联Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface CheckRecordItemStudentMapper extends BaseMapper<CheckRecordItemStudent> {

    /**
     * 分页查询待确认的数据
     */
    Page<Map<String, Object>> selectPendingConfirm(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 查询学生在日期范围内的扣分数据
     */
    List<CheckRecordItemStudent> selectByStudentAndDateRange(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询未同步到综测的数据
     */
    List<CheckRecordItemStudent> selectNotSynced(@Param("periodId") Long periodId);

    /**
     * 批量插入
     */
    void batchInsert(@Param("items") List<CheckRecordItemStudent> items);

    /**
     * 批量更新确认状态
     */
    void batchUpdateConfirmed(@Param("ids") List<Long> ids, @Param("confirmed") Integer confirmed, @Param("confirmedBy") Long confirmedBy);

    /**
     * 批量更新同步状态
     */
    void batchUpdateSynced(@Param("ids") List<Long> ids, @Param("periodId") Long periodId);

    /**
     * 统计学生各维度扣分
     */
    Map<String, Object> selectStudentDeductSummary(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    /**
     * 根据检查记录ID查询
     */
    List<CheckRecordItemStudent> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 查询日期范围内的所有扣分数据
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM check_record_item_students WHERE check_date >= #{startDate} AND check_date <= #{endDate}")
    List<CheckRecordItemStudent> selectByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
