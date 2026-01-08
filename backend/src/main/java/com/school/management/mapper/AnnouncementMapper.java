package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告Mapper接口
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
