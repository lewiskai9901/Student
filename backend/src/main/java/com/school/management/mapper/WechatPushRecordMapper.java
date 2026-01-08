package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.WechatPushRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 微信推送记录 Mapper
 *
 * @author system
 * @since 4.5.0
 */
@Mapper
public interface WechatPushRecordMapper extends BaseMapper<WechatPushRecord> {

    /**
     * 根据业务查询推送记录
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 推送记录列表
     */
    @Select("SELECT * FROM wechat_push_record WHERE business_type = #{businessType} AND business_id = #{businessId} AND deleted = 0 ORDER BY created_at DESC")
    List<WechatPushRecord> selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 统计业务推送成功数
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 成功数量
     */
    @Select("SELECT COUNT(*) FROM wechat_push_record WHERE business_type = #{businessType} AND business_id = #{businessId} AND status = 'SUCCESS' AND deleted = 0")
    int countSuccessByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 统计业务推送失败数
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 失败数量
     */
    @Select("SELECT COUNT(*) FROM wechat_push_record WHERE business_type = #{businessType} AND business_id = #{businessId} AND status = 'FAILED' AND deleted = 0")
    int countFailedByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 获取待发送的记录
     *
     * @param limit 数量限制
     * @return 待发送记录列表
     */
    @Select("SELECT * FROM wechat_push_record WHERE status = 'PENDING' AND deleted = 0 ORDER BY created_at ASC LIMIT #{limit}")
    List<WechatPushRecord> selectPendingRecords(@Param("limit") int limit);
}
