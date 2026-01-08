package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.UserDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户部门关联Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface UserDepartmentMapper extends BaseMapper<UserDepartment> {

    /**
     * 根据用户ID查询部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    @Select("SELECT department_id FROM user_departments WHERE user_id = #{userId}")
    List<Long> selectDepartmentIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户部门关联信息
     *
     * @param userId 用户ID
     * @return 用户部门关联列表
     */
    @Select("SELECT ud.*, d.dept_name as departmentName " +
            "FROM user_departments ud " +
            "LEFT JOIN departments d ON ud.department_id = d.id " +
            "WHERE ud.user_id = #{userId}")
    List<UserDepartment> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据部门ID查询用户ID列表
     *
     * @param departmentId 部门ID
     * @return 用户ID列表
     */
    @Select("SELECT user_id FROM user_departments WHERE department_id = #{departmentId}")
    List<Long> selectUserIdsByDepartmentId(@Param("departmentId") Long departmentId);
}
