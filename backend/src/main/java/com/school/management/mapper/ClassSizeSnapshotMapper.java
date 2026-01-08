package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.ClassSizeSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 班级人数快照Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface ClassSizeSnapshotMapper extends BaseMapper<ClassSizeSnapshot> {

    /**
     * 查询检查记录的快照
     *
     * @param recordId 检查记录ID
     * @return 快照列表
     */
    List<ClassSizeSnapshot> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 查询班级在指定日期的快照
     *
     * @param classId 班级ID
     * @param snapshotDate 快照日期
     * @return 快照
     */
    ClassSizeSnapshot selectByClassAndDate(
            @Param("classId") Long classId,
            @Param("snapshotDate") LocalDate snapshotDate
    );

    /**
     * 查询班级的最新快照
     *
     * @param classId 班级ID
     * @return 快照
     */
    ClassSizeSnapshot selectLatestByClass(@Param("classId") Long classId);

    /**
     * 批量插入快照
     *
     * @param snapshots 快照列表
     * @return 插入行数
     */
    int batchInsert(@Param("snapshots") List<ClassSizeSnapshot> snapshots);

    /**
     * 增加快照使用次数
     *
     * @param id 快照ID
     * @return 更新行数
     */
    int increaseUsageCount(@Param("id") Long id);
}
