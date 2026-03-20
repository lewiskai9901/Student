package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 场所批量Job Mapper
 */
@Mapper
public interface PlaceBatchJobMapper extends BaseMapper<PlaceBatchJobPO> {

    /**
     * 查询指定用户创建的最近Job
     *
     * @param createdBy 创建用户ID
     * @param limit 限制数量
     * @return Job列表
     */
    @Select("SELECT * FROM place_batch_jobs " +
            "WHERE created_by = #{createdBy} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<PlaceBatchJobPO> findRecentByCreatedBy(Long createdBy, int limit);

    /**
     * 查询指定状态的Job
     *
     * @param status Job状态
     * @param limit 限制数量
     * @return Job列表
     */
    @Select("SELECT * FROM place_batch_jobs " +
            "WHERE job_status = #{status} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<PlaceBatchJobPO> findByStatus(String status, int limit);

    /**
     * 查询待执行的Job（PENDING状态）
     *
     * @param limit 限制数量
     * @return Job列表
     */
    @Select("SELECT * FROM place_batch_jobs " +
            "WHERE job_status = 'PENDING' " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<PlaceBatchJobPO> findPendingJobs(int limit);

    /**
     * 原子更新Job进度
     *
     * @param jobId 任务ID
     * @param processedIncrement 已处理增量
     * @param successIncrement 成功增量
     * @param failureIncrement 失败增量
     * @return 影响行数
     */
    @Update("UPDATE place_batch_jobs " +
            "SET processed_items = processed_items + #{processedIncrement}, " +
            "    success_count = success_count + #{successIncrement}, " +
            "    failure_count = failure_count + #{failureIncrement}, " +
            "    started_at = COALESCE(started_at, NOW()), " +
            "    job_status = CASE " +
            "        WHEN job_status = 'PENDING' THEN 'RUNNING' " +
            "        ELSE job_status " +
            "    END " +
            "WHERE job_id = #{jobId}")
    int updateProgress(String jobId, int processedIncrement, int successIncrement, int failureIncrement);

    /**
     * 标记Job为完成状态
     *
     * @param jobId 任务ID
     * @return 影响行数
     */
    @Update("UPDATE place_batch_jobs " +
            "SET job_status = CASE " +
            "        WHEN failure_count = 0 THEN 'COMPLETED' " +
            "        WHEN failure_count < total_items THEN 'PARTIALLY_COMPLETED' " +
            "        ELSE 'FAILED' " +
            "    END, " +
            "    completed_at = NOW() " +
            "WHERE job_id = #{jobId} " +
            "  AND processed_items >= total_items " +
            "  AND job_status = 'RUNNING'")
    int markAsCompleted(String jobId);
}
