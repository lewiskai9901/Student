package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.ClassSizeStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班级标准人数Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface ClassSizeStandardMapper extends BaseMapper<ClassSizeStandard> {

    /**
     * 查询标准人数(按优先级: 学期+部门+年级 > 学期+部门 > 学期 > 系统默认)
     *
     * @param semesterId 学期ID
     * @param departmentId 部门ID
     * @param gradeLevel 年级级别
     * @return 标准人数
     */
    ClassSizeStandard selectStandardSize(
            @Param("semesterId") Long semesterId,
            @Param("departmentId") Long departmentId,
            @Param("gradeLevel") Integer gradeLevel
    );

    /**
     * 查询学期的所有标准人数配置
     *
     * @param semesterId 学期ID
     * @return 标准人数列表
     */
    List<ClassSizeStandard> selectBySemesterId(@Param("semesterId") Long semesterId);

    /**
     * 锁定标准人数
     *
     * @param id 标准人数ID
     * @param lockedBy 锁定人ID
     * @return 更新行数
     */
    int lockStandard(
            @Param("id") Long id,
            @Param("lockedBy") Long lockedBy
    );

    /**
     * 解锁标准人数
     *
     * @param id 标准人数ID
     * @return 更新行数
     */
    int unlockStandard(@Param("id") Long id);
}
