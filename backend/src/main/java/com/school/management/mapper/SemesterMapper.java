package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.Semester;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学期Mapper接口
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
public interface SemesterMapper extends BaseMapper<Semester> {

    /**
     * 根据学期编码查询学期
     *
     * @param semesterCode 学期编码
     * @return 学期
     */
    Semester selectBySemesterCode(@Param("semesterCode") String semesterCode);

    /**
     * 获取当前学期
     *
     * @return 当前学期
     */
    Semester selectCurrentSemester();

    /**
     * 取消所有学期的当前状态
     *
     * @return 更新数量
     */
    Integer cancelAllCurrent();
}
