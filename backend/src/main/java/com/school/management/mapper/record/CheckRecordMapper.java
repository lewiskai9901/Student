package com.school.management.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.record.CheckRecordNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 检查记录主表Mapper（重构版）
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
@Repository("newCheckRecordMapper")
public interface CheckRecordMapper extends BaseMapper<CheckRecordNew> {

    /**
     * 分页查询检查记录
     *
     * @param page 分页参数
     * @param checkName 检查名称（模糊匹配）
     * @param checkDateStart 检查日期开始
     * @param checkDateEnd 检查日期结束
     * @param status 状态
     * @param checkType 检查类型
     * @param planId 检查计划ID
     * @return 分页结果
     */
    IPage<CheckRecordNew> selectRecordPage(
            Page<CheckRecordNew> page,
            @Param("checkName") String checkName,
            @Param("checkDateStart") LocalDate checkDateStart,
            @Param("checkDateEnd") LocalDate checkDateEnd,
            @Param("status") Integer status,
            @Param("checkType") Integer checkType,
            @Param("planId") Long planId,
            @Param("classIds") List<Long> classIds
    );

    /**
     * 根据日常检查ID查询记录
     *
     * @param dailyCheckId 日常检查ID
     * @return 检查记录
     */
    CheckRecordNew selectByDailyCheckId(@Param("dailyCheckId") Long dailyCheckId);

    /**
     * 获取指定日期范围内的记录列表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 记录列表
     */
    List<CheckRecordNew> selectByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 更新申诉统计
     *
     * @param recordId 记录ID
     * @return 更新行数
     */
    int updateAppealStats(@Param("recordId") Long recordId);

    /**
     * 重新计算总分
     *
     * @param recordId 记录ID
     * @return 更新行数
     */
    int recalculateTotalScore(@Param("recordId") Long recordId);

    /**
     * 生成记录编号
     *
     * @param checkDate 检查日期
     * @return 新编号
     */
    String generateRecordCode(@Param("checkDate") LocalDate checkDate);
}
