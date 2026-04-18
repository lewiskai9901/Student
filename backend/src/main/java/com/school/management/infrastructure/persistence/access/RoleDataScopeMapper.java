package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * role_data_scopes 表 Mapper (v3 替代 role_data_permissions_v5 直接 JdbcTemplate)
 */
@Mapper
public interface RoleDataScopeMapper extends BaseMapper<RoleDataScopePO> {
}
