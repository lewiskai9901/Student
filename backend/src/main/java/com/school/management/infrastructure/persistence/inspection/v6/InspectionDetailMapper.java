package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * V6检查明细Mapper
 */
@Mapper
public interface InspectionDetailMapper extends BaseMapper<InspectionDetailPO> {

    @Select("SELECT * FROM inspection_details WHERE target_id = #{targetId} ORDER BY id")
    List<InspectionDetailPO> findByTargetId(@Param("targetId") Long targetId);

    @Select("SELECT * FROM inspection_details WHERE target_id = #{targetId} AND scoring_mode = #{scoringMode}")
    List<InspectionDetailPO> findByTargetIdAndScoringMode(@Param("targetId") Long targetId,
                                                           @Param("scoringMode") String scoringMode);

    @Select("SELECT * FROM inspection_details WHERE target_id = #{targetId} AND category_id = #{categoryId}")
    List<InspectionDetailPO> findByTargetIdAndCategoryId(@Param("targetId") Long targetId,
                                                          @Param("categoryId") Long categoryId);

    @Select("SELECT * FROM inspection_details WHERE individual_type = #{individualType} AND individual_id = #{individualId}")
    List<InspectionDetailPO> findByIndividual(@Param("individualType") String individualType,
                                               @Param("individualId") Long individualId);

    @Delete("DELETE FROM inspection_details WHERE target_id = #{targetId}")
    void deleteByTargetId(@Param("targetId") Long targetId);

    @Select("SELECT COUNT(*) FROM inspection_details WHERE target_id = #{targetId}")
    int countByTargetId(@Param("targetId") Long targetId);

    @Insert("<script>" +
            "INSERT INTO inspection_details (target_id, category_id, category_code, category_name, " +
            "item_id, item_code, item_name, scope, individual_type, individual_id, individual_name, " +
            "scoring_mode, score, quantity, total_score, grade_code, grade_name, checklist_checked, " +
            "remark, evidence_ids, created_by, created_at) VALUES " +
            "<foreach collection='details' item='d' separator=','>" +
            "(#{d.targetId}, #{d.categoryId}, #{d.categoryCode}, #{d.categoryName}, " +
            "#{d.itemId}, #{d.itemCode}, #{d.itemName}, #{d.scope}, #{d.individualType}, " +
            "#{d.individualId}, #{d.individualName}, #{d.scoringMode}, #{d.score}, #{d.quantity}, " +
            "#{d.totalScore}, #{d.gradeCode}, #{d.gradeName}, #{d.checklistChecked}, " +
            "#{d.remark}, #{d.evidenceIds}, #{d.createdBy}, NOW())" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("details") List<InspectionDetailPO> details);
}
