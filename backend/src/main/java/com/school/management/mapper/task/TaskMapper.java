package com.school.management.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.task.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 任务Mapper
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 查询用户待审批的任务数量
     */
    @Select("SELECT COUNT(1) FROM tasks t " +
            "WHERE t.deleted = 0 " +
            "AND t.status IN (2, 6) " +
            "AND JSON_CONTAINS(t.current_approvers, CAST(#{userId} AS JSON), '$')")
    Long countPendingApproval(@Param("userId") Long userId);

    /**
     * 获取任务编号(按日期生成)
     */
    @Select("SELECT CONCAT('TASK-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', " +
            "LPAD(IFNULL(MAX(CAST(SUBSTRING(task_code, -4) AS UNSIGNED)), 0) + 1, 4, '0')) " +
            "FROM tasks WHERE task_code LIKE CONCAT('TASK-', DATE_FORMAT(NOW(), '%Y%m%d'), '-%')")
    String generateTaskCode();

    /**
     * 统计各状态任务数量
     */
    @Select("<script>" +
            "SELECT status, COUNT(1) as count FROM tasks " +
            "WHERE deleted = 0 " +
            "<if test='orgUnitId != null'>AND org_unit_id = #{orgUnitId}</if> " +
            "GROUP BY status" +
            "</script>")
    List<Map<String, Object>> countByStatus(@Param("orgUnitId") Long orgUnitId);

    /**
     * 统计超期任务数量
     */
    @Select("<script>" +
            "SELECT COUNT(1) FROM tasks " +
            "WHERE deleted = 0 " +
            "AND status NOT IN (3, 5) " +
            "AND due_date IS NOT NULL " +
            "AND due_date &lt; NOW() " +
            "<if test='orgUnitId != null'>AND org_unit_id = #{orgUnitId}</if>" +
            "</script>")
    Long countOverdue(@Param("orgUnitId") Long orgUnitId);
}
