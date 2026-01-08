package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.AcademicWeek;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 教学周Mapper接口
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
public interface AcademicWeekMapper extends BaseMapper<AcademicWeek> {

    /**
     * 根据学期ID查询教学周列表
     *
     * @param semesterId 学期ID
     * @return 教学周列表
     */
    List<AcademicWeek> selectBySemesterId(@Param("semesterId") Long semesterId);

    /**
     * 根据日期查询教学周
     *
     * @param date 日期
     * @return 教学周
     */
    AcademicWeek selectByDate(@Param("date") LocalDate date);

    /**
     * 获取当前教学周
     *
     * @return 当前教学周
     */
    AcademicWeek selectCurrentWeek();

    /**
     * 取消所有教学周的当前状态
     *
     * @return 更新数量
     */
    Integer cancelAllCurrent();
}
