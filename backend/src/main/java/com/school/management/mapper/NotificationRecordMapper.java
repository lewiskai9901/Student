package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.NotificationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通报记录Mapper
 *
 * @author system
 * @since 4.2.0
 */
@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {
}
