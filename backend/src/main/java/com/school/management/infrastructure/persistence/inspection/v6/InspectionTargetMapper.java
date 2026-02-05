package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * V6检查目标Mapper
 */
@Mapper
public interface InspectionTargetMapper extends BaseMapper<InspectionTargetPO> {

    @Select("SELECT * FROM inspection_targets WHERE task_id = #{taskId} ORDER BY id")
    List<InspectionTargetPO> findByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM inspection_targets WHERE task_id = #{taskId} AND status = #{status}")
    List<InspectionTargetPO> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") String status);

    @Select("SELECT * FROM inspection_targets WHERE task_id = #{taskId} AND target_type = #{targetType} AND target_id = #{targetId}")
    InspectionTargetPO findByTaskAndTarget(@Param("taskId") Long taskId,
                                            @Param("targetType") String targetType,
                                            @Param("targetId") Long targetId);

    @Select("SELECT * FROM inspection_targets WHERE org_unit_id = #{orgUnitId}")
    List<InspectionTargetPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    @Select("SELECT * FROM inspection_targets WHERE class_id = #{classId}")
    List<InspectionTargetPO> findByClassId(@Param("classId") Long classId);

    @Update("UPDATE inspection_targets SET status = 'LOCKED', locked_by = #{lockedBy}, locked_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int lockTarget(@Param("id") Long id, @Param("lockedBy") Long lockedBy);

    @Update("UPDATE inspection_targets SET status = 'PENDING', locked_by = NULL, locked_at = NULL, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'LOCKED'")
    int unlockTarget(@Param("id") Long id);

    @Update("UPDATE inspection_targets SET status = 'COMPLETED', completed_at = NOW(), " +
            "final_score = base_score - deduction_total + bonus_total, updated_at = NOW() " +
            "WHERE id = #{id}")
    void completeTarget(@Param("id") Long id);

    @Update("UPDATE inspection_targets SET status = 'SKIPPED', skip_reason = #{skipReason}, " +
            "completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    void skipTarget(@Param("id") Long id, @Param("skipReason") String skipReason);

    @Update("UPDATE inspection_targets SET deduction_total = deduction_total + #{deduction}, updated_at = NOW() WHERE id = #{id}")
    void addDeduction(@Param("id") Long id, @Param("deduction") java.math.BigDecimal deduction);

    @Update("UPDATE inspection_targets SET bonus_total = bonus_total + #{bonus}, updated_at = NOW() WHERE id = #{id}")
    void addBonus(@Param("id") Long id, @Param("bonus") java.math.BigDecimal bonus);

    @Select("SELECT COUNT(*) FROM inspection_targets WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT COUNT(*) FROM inspection_targets WHERE task_id = #{taskId} AND status = 'COMPLETED'")
    int countCompletedByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT COUNT(*) FROM inspection_targets WHERE task_id = #{taskId} AND status = 'SKIPPED'")
    int countSkippedByTaskId(@Param("taskId") Long taskId);

    @Insert("<script>" +
            "INSERT INTO inspection_targets (task_id, target_type, target_id, target_name, target_code, " +
            "org_unit_id, org_unit_name, class_id, class_name, weight_ratio, status, base_score, " +
            "deduction_total, bonus_total, created_at) VALUES " +
            "<foreach collection='targets' item='t' separator=','>" +
            "(#{t.taskId}, #{t.targetType}, #{t.targetId}, #{t.targetName}, #{t.targetCode}, " +
            "#{t.orgUnitId}, #{t.orgUnitName}, #{t.classId}, #{t.className}, #{t.weightRatio}, " +
            "#{t.status}, #{t.baseScore}, #{t.deductionTotal}, #{t.bonusTotal}, NOW())" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("targets") List<InspectionTargetPO> targets);
}
