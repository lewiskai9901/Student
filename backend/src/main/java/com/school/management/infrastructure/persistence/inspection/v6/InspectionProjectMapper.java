package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * V6检查项目Mapper
 */
@Mapper
public interface InspectionProjectMapper extends BaseMapper<InspectionProjectPO> {

    @Select("SELECT * FROM inspection_projects WHERE project_code = #{projectCode} AND deleted = 0")
    InspectionProjectPO findByProjectCode(@Param("projectCode") String projectCode);

    @Select("SELECT * FROM inspection_projects WHERE template_id = #{templateId} AND deleted = 0")
    List<InspectionProjectPO> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM inspection_projects WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionProjectPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM inspection_projects WHERE created_by = #{createdBy} AND deleted = 0 ORDER BY created_at DESC")
    List<InspectionProjectPO> findByCreatedBy(@Param("createdBy") Long createdBy);

    @Select("<script>" +
            "SELECT * FROM inspection_projects WHERE deleted = 0 " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (project_name LIKE CONCAT('%', #{keyword}, '%') OR project_code LIKE CONCAT('%', #{keyword}, '%'))</if> " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<InspectionProjectPO> findPagedWithConditions(@Param("offset") int offset,
                                                       @Param("limit") int limit,
                                                       @Param("status") String status,
                                                       @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COUNT(*) FROM inspection_projects WHERE deleted = 0 " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (project_name LIKE CONCAT('%', #{keyword}, '%') OR project_code LIKE CONCAT('%', #{keyword}, '%'))</if> " +
            "</script>")
    long countWithConditions(@Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT * FROM inspection_projects WHERE status = 'ACTIVE' AND start_date <= #{date} AND (end_date IS NULL OR end_date >= #{date}) AND deleted = 0")
    List<InspectionProjectPO> findActiveProjectsForDate(@Param("date") LocalDate date);

    @Update("UPDATE inspection_projects SET total_tasks = #{totalTasks}, updated_at = NOW() WHERE id = #{id}")
    void updateTotalTasks(@Param("id") Long id, @Param("totalTasks") Integer totalTasks);

    @Update("UPDATE inspection_projects SET completed_tasks = completed_tasks + 1, updated_at = NOW() WHERE id = #{id}")
    void incrementCompletedTasks(@Param("id") Long id);
}
