package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 批量任务项明细Mapper
 */
@Mapper
public interface PlaceBatchJobItemMapper extends BaseMapper<PlaceBatchJobItemPO> {

    /**
     * 查询指定Job的所有明细项
     *
     * @param jobId 任务ID
     * @return 明细项列表
     */
    @Select("SELECT * FROM place_batch_job_items " +
            "WHERE job_id = #{jobId} " +
            "ORDER BY item_index ASC")
    List<PlaceBatchJobItemPO> findByJobId(String jobId);

    /**
     * 查询指定Job的失败明细项
     *
     * @param jobId 任务ID
     * @return 失败明细项列表
     */
    @Select("SELECT * FROM place_batch_job_items " +
            "WHERE job_id = #{jobId} " +
            "  AND item_status = 'FAILED' " +
            "ORDER BY item_index ASC")
    List<PlaceBatchJobItemPO> findFailedItems(String jobId);

    /**
     * 查询指定Job的待处理明细项
     *
     * @param jobId 任务ID
     * @return 待处理明细项列表
     */
    @Select("SELECT * FROM place_batch_job_items " +
            "WHERE job_id = #{jobId} " +
            "  AND item_status = 'PENDING' " +
            "ORDER BY item_index ASC")
    List<PlaceBatchJobItemPO> findPendingItems(String jobId);
}
