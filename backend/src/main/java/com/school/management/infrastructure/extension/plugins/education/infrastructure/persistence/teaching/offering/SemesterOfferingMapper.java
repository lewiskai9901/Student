package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

// R2.2 前置①: 表 semester_offerings 不存在(半成品功能)→ 死 mapper, 移除空转的 @DataPermission。
// 若将来补建该表/查询, 再按 data_resources 行 + @DataPermission 激活数据权限。
@Mapper
public interface SemesterOfferingMapper extends BaseMapper<SemesterOfferingPO> {
}
