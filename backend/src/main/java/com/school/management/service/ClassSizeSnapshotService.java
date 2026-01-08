package com.school.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.ClassSizeSnapshot;

import java.time.LocalDate;
import java.util.List;

/**
 * 班级人数快照服务接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
public interface ClassSizeSnapshotService extends IService<ClassSizeSnapshot> {

    /**
     * 为检查记录创建人数快照
     *
     * @param recordId 检查记录ID
     * @param classIds 班级ID列表
     * @return 创建的快照列表
     */
    List<ClassSizeSnapshot> createSnapshotsForCheck(Long recordId, List<Long> classIds);

    /**
     * 创建单个班级的快照
     *
     * @param classId 班级ID
     * @param snapshotDate 快照日期
     * @param recordId 检查记录ID(可选)
     * @param source 快照来源
     * @return 快照
     */
    ClassSizeSnapshot createSnapshot(Long classId, LocalDate snapshotDate,
                                      Long recordId, String source);

    /**
     * 确保快照存在
     * 如果不存在则创建,存在则返回
     *
     * @param classId 班级ID
     * @param snapshotDate 快照日期
     * @return 快照
     */
    ClassSizeSnapshot ensureSnapshot(Long classId, LocalDate snapshotDate);

    /**
     * 获取班级在指定日期的快照
     *
     * @param classId 班级ID
     * @param snapshotDate 快照日期
     * @return 快照
     */
    ClassSizeSnapshot getSnapshotByDate(Long classId, LocalDate snapshotDate);

    /**
     * 获取班级的最新快照
     *
     * @param classId 班级ID
     * @return 快照
     */
    ClassSizeSnapshot getLatestSnapshot(Long classId);

    /**
     * 创建每日快照(定时任务调用)
     *
     * @return 创建的快照数量
     */
    int createDailySnapshot();

    /**
     * 清理过期快照
     *
     * @param daysToKeep 保留天数
     * @return 清理数量
     */
    int cleanExpiredSnapshots(int daysToKeep);
}
