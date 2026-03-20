package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DataModuleMapper extends BaseMapper<DataModulePO> {

    @Select("SELECT item_type_code FROM module_scope_item_types " +
            "WHERE tenant_id = #{tenantId} AND module_code = #{moduleCode}")
    List<String> findScopeItemTypesByModule(@Param("tenantId") Long tenantId,
                                            @Param("moduleCode") String moduleCode);
}
