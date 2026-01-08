package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.dto.DepartmentResponse;
import com.school.management.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 查询所有部门（用于构建树形结构）
     *
     * @return 部门列表
     */
    List<DepartmentResponse> selectAllDepartments();

    /**
     * 根据部门编码获取部门
     *
     * @param deptCode 部门编码
     * @return 部门
     */
    Department selectByDeptCode(@Param("deptCode") String deptCode);

    /**
     * 检查部门编码是否存在
     *
     * @param deptCode 部门编码
     * @param excludeId 排除的ID
     * @return 数量
     */
    Integer countByDeptCode(@Param("deptCode") String deptCode, @Param("excludeId") Long excludeId);

    /**
     * 根据父部门ID查询子部门
     *
     * @param parentId 父部门ID
     * @return 部门列表
     */
    List<DepartmentResponse> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 获取所有启用的部门
     *
     * @return 部门列表
     */
    List<DepartmentResponse> selectAllEnabled();

    /**
     * 根据负责人ID查询部门
     *
     * @param leaderId 负责人ID
     * @return 部门列表
     */
    List<DepartmentResponse> selectByLeaderId(@Param("leaderId") Long leaderId);

    /**
     * 根据部门路径查询所有子部门ID（包含自身）
     *
     * @param deptPath 部门路径
     * @return 部门ID列表
     */
    List<Long> selectChildDepartmentIds(@Param("deptPath") String deptPath);

    /**
     * 根据负责人ID查询其管理的所有部门ID（包含子部门）
     * 用于数据权限过滤：部门负责人可访问其管理部门及所有子部门的数据
     *
     * @param leaderId 负责人ID
     * @return 部门ID列表（包含所有子部门）
     */
    List<Long> selectManagedDepartmentIdsWithChildren(@Param("leaderId") Long leaderId);
}