package com.school.management.infrastructure.external;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 站内消息 Mapper（DDD基础设施层）
 * 替代V1的 mapper.task.SystemMessageMapper
 */
@Mapper
public interface SystemMessageDomainMapper extends BaseMapper<SystemMessagePO> {

    /**
     * 统计未读消息数量
     */
    @Select("SELECT COUNT(1) FROM system_messages " +
            "WHERE receiver_id = #{receiverId} " +
            "AND is_read = 0 " +
            "AND deleted = 0")
    Long countUnread(@Param("receiverId") Long receiverId);

    /**
     * 标记所有消息为已读
     */
    @Update("UPDATE system_messages SET is_read = 1, read_time = NOW() " +
            "WHERE receiver_id = #{receiverId} " +
            "AND is_read = 0 " +
            "AND deleted = 0")
    int markAllAsRead(@Param("receiverId") Long receiverId);
}
