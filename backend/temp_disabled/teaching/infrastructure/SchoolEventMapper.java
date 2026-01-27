package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校历事件Mapper
 */
@Mapper
public interface SchoolEventMapper extends BaseMapper<SchoolEventPO> {
}
