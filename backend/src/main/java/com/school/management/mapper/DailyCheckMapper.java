package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.DailyCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 日常检查Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DailyCheckMapper extends BaseMapper<DailyCheck> {

    /**
     * 统计年级在指定时间范围内的检查次数
     * 通过check_record_class_stats表关联年级
     *
     * @param gradeId 年级ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 检查次数
     */
    @Select("SELECT COUNT(DISTINCT dc.id) FROM daily_checks dc " +
            "INNER JOIN check_record_class_stats cs ON dc.id = cs.record_id " +
            "WHERE cs.grade_id = #{gradeId} " +
            "AND dc.check_date >= #{startDate} " +
            "AND dc.check_date <= #{endDate} " +
            "AND dc.deleted = 0")
    int countChecksByGradeAndDateRange(
            @Param("gradeId") Long gradeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
