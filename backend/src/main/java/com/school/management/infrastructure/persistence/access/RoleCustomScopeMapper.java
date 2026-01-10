package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色自定义数据范围Mapper
 */
@Mapper
public interface RoleCustomScopeMapper extends BaseMapper<RoleCustomScopePO> {
}
