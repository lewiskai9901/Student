package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.Examination;
import com.school.management.domain.teaching.model.entity.ExamArrangement;
import com.school.management.domain.teaching.model.entity.ExamRoom;
import com.school.management.domain.teaching.model.entity.ExamInvigilator;

import java.util.List;
import java.util.Optional;

/**
 * 考试仓储接口
 */
public interface ExaminationRepository {

    /**
     * 保存考试批次
     */
    Examination save(Examination examination);

    /**
     * 根据ID查询
     */
    Optional<Examination> findById(Long id);

    /**
     * 根据批次代码查询
     */
    Optional<Examination> findByBatchCode(String batchCode);

    /**
     * 根据ID查询（包含安排）
     */
    Optional<Examination> findByIdWithArrangements(Long id);

    /**
     * 查询学期的所有考试批次
     */
    List<Examination> findBySemesterId(Long semesterId);

    /**
     * 分页查询
     */
    List<Examination> findPage(int page, int size, Long semesterId, Integer examType, Integer status);

    /**
     * 统计总数
     */
    long count(Long semesterId, Integer examType, Integer status);

    /**
     * 删除考试批次
     */
    void deleteById(Long id);

    /**
     * 检查批次代码是否存在
     */
    boolean existsByBatchCode(String batchCode);

    // 考试安排相关

    /**
     * 保存考试安排
     */
    ExamArrangement saveArrangement(ExamArrangement arrangement);

    /**
     * 根据ID查询考试安排
     */
    Optional<ExamArrangement> findArrangementById(Long id);

    /**
     * 查询批次的所有安排
     */
    List<ExamArrangement> findArrangementsByBatchId(Long batchId);

    /**
     * 删除考试安排
     */
    void deleteArrangementById(Long id);

    // 考场相关

    /**
     * 保存考场
     */
    ExamRoom saveRoom(ExamRoom room);

    /**
     * 查询安排的考场
     */
    List<ExamRoom> findRoomsByArrangementId(Long arrangementId);

    /**
     * 删除考场
     */
    void deleteRoomById(Long id);

    // 监考相关

    /**
     * 保存监考安排
     */
    ExamInvigilator saveInvigilator(ExamInvigilator invigilator);

    /**
     * 查询考场的监考
     */
    List<ExamInvigilator> findInvigilatorsByRoomId(Long roomId);

    /**
     * 删除监考安排
     */
    void deleteInvigilatorById(Long id);
}
