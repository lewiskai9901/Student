package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ItemComplianceMappingMapper extends BaseMapper<ItemComplianceMappingPO> {

    @Select("SELECT * FROM insp_item_compliance_mappings WHERE template_item_id = #{itemId} AND deleted = 0")
    List<ItemComplianceMappingPO> findByItemId(@Param("itemId") Long itemId);

    @Select("SELECT * FROM insp_item_compliance_mappings WHERE clause_id = #{clauseId} AND deleted = 0")
    List<ItemComplianceMappingPO> findByClauseId(@Param("clauseId") Long clauseId);

    @Update("UPDATE insp_item_compliance_mappings SET deleted = 1 WHERE template_item_id = #{itemId} AND deleted = 0")
    void softDeleteByItemId(@Param("itemId") Long itemId);
}
