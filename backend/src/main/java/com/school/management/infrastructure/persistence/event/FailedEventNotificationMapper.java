package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 事件通知死信队列 Mapper (区块3-B)。 */
@Mapper
public interface FailedEventNotificationMapper extends BaseMapper<FailedEventNotificationPO> {
}
