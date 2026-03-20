package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InspSubmissionMapper extends BaseMapper<InspSubmissionPO> {

    /**
     * 按任务查询提交记录 — 应用数据权限过滤（org_unit_id）
     * 检查记录的数据权限：根据提交所属组织单元过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_submissions WHERE task_id = #{taskId} AND deleted = 0 ORDER BY id")
    List<InspSubmissionPO> findByTaskId(@Param("taskId") Long taskId);

    /**
     * 按被检查目标查询提交记录 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_submissions WHERE target_id = #{targetId} AND deleted = 0 ORDER BY created_at DESC")
    List<InspSubmissionPO> findByTargetId(@Param("targetId") Long targetId);

    /**
     * 查询增量变更记录 — 应用数据权限过滤（用于同步场景）
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_submissions WHERE task_id = #{taskId} AND updated_at > #{since} AND deleted = 0 ORDER BY id")
    List<InspSubmissionPO> findModifiedAfter(@Param("taskId") Long taskId, @Param("since") LocalDateTime since);
}
