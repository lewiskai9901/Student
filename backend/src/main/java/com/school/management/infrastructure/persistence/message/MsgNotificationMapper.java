package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 站内消息 MyBatis Mapper
 */
@Mapper
public interface MsgNotificationMapper extends BaseMapper<MsgNotificationPO> {

    @Select("<script>" +
            "SELECT * FROM msg_notifications WHERE user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{offset}, #{size}" +
            "</script>")
    List<MsgNotificationPO> findByUserId(@Param("userId") Long userId,
                                          @Param("isRead") Integer isRead,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM msg_notifications WHERE user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "</script>")
    long countByUserId(@Param("userId") Long userId, @Param("isRead") Integer isRead);

    @Select("SELECT COUNT(*) FROM msg_notifications WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE msg_notifications SET is_read = 1, read_at = NOW() WHERE id = #{id} AND deleted = 0")
    void markRead(@Param("id") Long id);

    @Update("UPDATE msg_notifications SET is_read = 1, read_at = NOW() WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    void markAllRead(@Param("userId") Long userId);

    @Update("UPDATE msg_notifications SET deleted = 1 WHERE id = #{id} AND user_id = #{userId}")
    void softDelete(@Param("id") Long id, @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM msg_notifications WHERE user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "<if test='msgType != null'> AND msg_type = #{msgType}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{offset}, #{size}" +
            "</script>")
    List<MsgNotificationPO> findByUserIdWithType(@Param("userId") Long userId,
                                                  @Param("isRead") Integer isRead,
                                                  @Param("msgType") String msgType,
                                                  @Param("offset") int offset,
                                                  @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM msg_notifications WHERE user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "<if test='msgType != null'> AND msg_type = #{msgType}</if>" +
            "</script>")
    long countByUserIdWithType(@Param("userId") Long userId,
                                @Param("isRead") Integer isRead,
                                @Param("msgType") String msgType);
}
