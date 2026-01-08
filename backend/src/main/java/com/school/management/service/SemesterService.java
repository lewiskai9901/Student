package com.school.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.Semester;

/**
 * 学期服务接口
 *
 * @author system
 * @since 2.0.0
 */
public interface SemesterService extends IService<Semester> {

    /**
     * 根据学期编码查询学期
     *
     * @param semesterCode 学期编码
     * @return 学期
     */
    Semester getBySemesterCode(String semesterCode);

    /**
     * 获取当前学期
     *
     * @return 当前学期
     */
    Semester getCurrentSemester();

    /**
     * 设置当前学期
     *
     * @param id 学期ID
     * @return 是否成功
     */
    boolean setCurrentSemester(Long id);

    /**
     * 生成学期编码
     *
     * @param startYear 开始年份
     * @param semesterType 学期类型
     * @return 学期编码
     */
    String generateSemesterCode(Integer startYear, Integer semesterType);
}
