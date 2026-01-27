package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产预警Mapper
 */
@Mapper
public interface AssetAlertMapper extends BaseMapper<AssetAlertPO> {
}
