package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * access_relations Mapper
 */
@Mapper
public interface AccessRelationMapper extends BaseMapper<AccessRelationPO> {

    /**
     * 查询某 subject 能访问的资源ID列表（直接关系）
     */
    @Select("SELECT DISTINCT resource_id FROM access_relations " +
            "WHERE resource_type = #{resourceType} " +
            "AND subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND deleted = 0")
    List<Long> findAccessibleResourceIds(
            @Param("resourceType") String resourceType,
            @Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId);

    /**
     * 查询某组织（含 include_children）能访问的资源ID列表
     */
    @Select("<script>" +
            "SELECT DISTINCT resource_id FROM access_relations " +
            "WHERE resource_type = #{resourceType} " +
            "AND subject_type = 'org_unit' " +
            "AND subject_id IN " +
            "<foreach collection='orgUnitIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    List<Long> findAccessibleResourceIdsByOrgUnits(
            @Param("resourceType") String resourceType,
            @Param("orgUnitIds") List<Long> orgUnitIds);

    /**
     * 检查关系是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM access_relations " +
            "WHERE resource_type = #{resourceType} AND resource_id = #{resourceId} " +
            "AND relation = #{relation} " +
            "AND subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND deleted = 0")
    boolean existsRelation(
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("relation") String relation,
            @Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId);
}
