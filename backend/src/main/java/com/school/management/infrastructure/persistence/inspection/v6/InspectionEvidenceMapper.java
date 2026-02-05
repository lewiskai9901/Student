package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * V6检查证据Mapper
 */
@Mapper
public interface InspectionEvidenceMapper extends BaseMapper<InspectionEvidencePO> {

    @Select("SELECT * FROM inspection_evidences WHERE detail_id = #{detailId} ORDER BY id")
    List<InspectionEvidencePO> findByDetailId(@Param("detailId") Long detailId);

    @Select("SELECT * FROM inspection_evidences WHERE target_id = #{targetId} ORDER BY id")
    List<InspectionEvidencePO> findByTargetId(@Param("targetId") Long targetId);

    @Select("<script>" +
            "SELECT * FROM inspection_evidences WHERE detail_id IN " +
            "<foreach collection='detailIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY id" +
            "</script>")
    List<InspectionEvidencePO> findByDetailIds(@Param("detailIds") List<Long> detailIds);

    @Delete("DELETE FROM inspection_evidences WHERE detail_id = #{detailId}")
    void deleteByDetailId(@Param("detailId") Long detailId);

    @Delete("DELETE FROM inspection_evidences WHERE target_id = #{targetId}")
    void deleteByTargetId(@Param("targetId") Long targetId);

    @Select("SELECT COUNT(*) FROM inspection_evidences WHERE detail_id = #{detailId}")
    int countByDetailId(@Param("detailId") Long detailId);

    @Select("SELECT COUNT(*) FROM inspection_evidences WHERE target_id = #{targetId}")
    int countByTargetId(@Param("targetId") Long targetId);

    @Insert("<script>" +
            "INSERT INTO inspection_evidences (detail_id, target_id, file_name, file_path, file_url, " +
            "file_size, file_type, latitude, longitude, upload_by, upload_time, created_at) VALUES " +
            "<foreach collection='evidences' item='e' separator=','>" +
            "(#{e.detailId}, #{e.targetId}, #{e.fileName}, #{e.filePath}, #{e.fileUrl}, " +
            "#{e.fileSize}, #{e.fileType}, #{e.latitude}, #{e.longitude}, #{e.uploadBy}, " +
            "#{e.uploadTime}, NOW())" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("evidences") List<InspectionEvidencePO> evidences);
}
