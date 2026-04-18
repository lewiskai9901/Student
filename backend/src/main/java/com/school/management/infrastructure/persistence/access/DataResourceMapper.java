package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * data_resources 表 Mapper (v3 替代 DataModuleMapper)
 */
@Mapper
public interface DataResourceMapper extends BaseMapper<DataResourcePO> {
}
