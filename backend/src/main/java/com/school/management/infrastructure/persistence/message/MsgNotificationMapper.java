package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 站内消息 MyBatis Mapper
 *
 * 多租户安全: 所有查询/更新/删除均带 tenant_id 过滤, 由调用方(Repository)
 * 从 TenantContextHolder 取当前租户注入, 杜绝跨租户 user_id 撞号时的消息泄漏。
 */
@Mapper
public interface MsgNotificationMapper extends BaseMapper<MsgNotificationPO> {

    @Select("<script>" +
            "SELECT * FROM msg_notifications WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{offset}, #{size}" +
            "</script>")
    List<MsgNotificationPO> findByUserId(@Param("tenantId") Long tenantId,
                                          @Param("userId") Long userId,
                                          @Param("isRead") Integer isRead,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM msg_notifications WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "</script>")
    long countByUserId(@Param("tenantId") Long tenantId,
                        @Param("userId") Long userId,
                        @Param("isRead") Integer isRead);

    @Select("SELECT COUNT(*) FROM msg_notifications WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Update("UPDATE msg_notifications SET is_read = 1, read_at = NOW() WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0")
    void markRead(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Update("UPDATE msg_notifications SET is_read = 1, read_at = NOW() WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND is_read = 0 AND deleted = 0")
    void markAllRead(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Update("UPDATE msg_notifications SET deleted = 1 WHERE tenant_id = #{tenantId} AND id = #{id} AND user_id = #{userId}")
    void softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("userId") Long userId);

    /**
     * 按 id 查询 (带 tenant 过滤), 替代 selectById 避免跨租户读取。
     */
    @Select("SELECT * FROM msg_notifications WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0")
    MsgNotificationPO findByIdScoped(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 批量插入站内消息（单条 INSERT 多 VALUES），用于订阅规则命中多用户时减少 round-trip。
     * 一次插入 N 条仅 1 次 DB round-trip，典型 1000 用户场景从 ~1000ms 降到 ~30ms。
     */
    @Insert("<script>" +
            "INSERT INTO msg_notifications(" +
            "  tenant_id, user_id, title, content, msg_type, source_event_type, " +
            "  source_ref_type, source_ref_id, subject_type, subject_id, subject_name, " +
            "  event_category, source_module, event_id, is_read, created_at, " +
            "  send_status, retry_count, last_error, sent_at" +
            ") VALUES " +
            "<foreach collection='list' item='n' separator=','>" +
            "(" +
            "  #{n.tenantId}, #{n.userId}, #{n.title}, #{n.content}, #{n.msgType}, #{n.sourceEventType}," +
            "  #{n.sourceRefType}, #{n.sourceRefId}, #{n.subjectType}, #{n.subjectId}, #{n.subjectName}," +
            "  #{n.eventCategory}, #{n.sourceModule}, #{n.eventId}, #{n.isRead}, #{n.createdAt}," +
            "  #{n.sendStatus}, #{n.retryCount}, #{n.lastError}, #{n.sentAt}" +
            ")" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<MsgNotificationPO> notifications);

    @Select("<script>" +
            "SELECT * FROM msg_notifications WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "<if test='msgType != null'> AND msg_type = #{msgType}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{offset}, #{size}" +
            "</script>")
    List<MsgNotificationPO> findByUserIdWithType(@Param("tenantId") Long tenantId,
                                                  @Param("userId") Long userId,
                                                  @Param("isRead") Integer isRead,
                                                  @Param("msgType") String msgType,
                                                  @Param("offset") int offset,
                                                  @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM msg_notifications WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND deleted = 0" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "<if test='msgType != null'> AND msg_type = #{msgType}</if>" +
            "</script>")
    long countByUserIdWithType(@Param("tenantId") Long tenantId,
                                @Param("userId") Long userId,
                                @Param("isRead") Integer isRead,
                                @Param("msgType") String msgType);
}
