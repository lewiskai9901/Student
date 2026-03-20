package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.ViolationRecord;
import com.school.management.domain.inspection.repository.v7.ViolationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 违纪记录应用服务
 * 管理检查过程中产生的违纪记录（VIOLATION_RECORD 字段类型的数据存储）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ViolationRecordApplicationService {

    private final ViolationRecordRepository violationRecordRepository;

    // ========== CRUD ==========

    /**
     * 创建违纪记录
     *
     * @param submissionId       关联提交ID
     * @param submissionDetailId 关联提交明细ID
     * @param sectionId          所属分区ID
     * @param itemId             检查项ID
     * @param userId             违纪人员ID
     * @param userName           违纪人员姓名
     * @param classInfo          班级信息
     * @param occurredAt         发生时间
     * @param severity           严重程度: MINOR / MODERATE / SEVERE
     * @param description        描述
     * @param evidenceUrls       证据URL（JSON）
     * @param score              扣分值
     * @param createdBy          创建人ID
     * @return 创建的违纪记录
     */
    @Transactional
    public ViolationRecord createViolationRecord(Long submissionId, Long submissionDetailId,
                                                  Long sectionId, Long itemId,
                                                  Long userId, String userName, String classInfo,
                                                  LocalDateTime occurredAt, String severity,
                                                  String description, String evidenceUrls,
                                                  BigDecimal score, Long createdBy) {
        ViolationRecord record = ViolationRecord.builder()
                .submissionId(submissionId)
                .submissionDetailId(submissionDetailId)
                .sectionId(sectionId)
                .itemId(itemId)
                .userId(userId)
                .userName(userName)
                .classInfo(classInfo)
                .occurredAt(occurredAt != null ? occurredAt : LocalDateTime.now())
                .severity(severity)
                .description(description)
                .evidenceUrls(evidenceUrls)
                .score(score)
                .createdBy(createdBy)
                .build();

        ViolationRecord saved = violationRecordRepository.save(record);
        log.info("创建违纪记录: submissionId={}, userId={}, userName={}, severity={}",
                submissionId, userId, userName, severity);
        return saved;
    }

    /**
     * 更新违纪记录
     *
     * @param recordId     记录ID
     * @param description  描述
     * @param severity     严重程度
     * @param score        扣分值
     * @param evidenceUrls 证据URL（JSON）
     * @param classInfo    班级信息
     * @return 更新后的违纪记录
     */
    @Transactional
    public ViolationRecord updateViolationRecord(Long recordId, String description,
                                                  String severity, BigDecimal score,
                                                  String evidenceUrls, String classInfo) {
        ViolationRecord record = violationRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("违纪记录不存在: " + recordId));

        record.update(description, severity, score, evidenceUrls, classInfo);

        ViolationRecord saved = violationRecordRepository.save(record);
        log.info("更新违纪记录: recordId={}, severity={}, score={}", recordId, severity, score);
        return saved;
    }

    /**
     * 删除违纪记录
     */
    @Transactional
    public void deleteViolationRecord(Long recordId) {
        violationRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("违纪记录不存在: " + recordId));
        violationRecordRepository.deleteById(recordId);
        log.info("删除违纪记录: recordId={}", recordId);
    }

    /**
     * 获取单个违纪记录
     */
    @Transactional(readOnly = true)
    public Optional<ViolationRecord> getViolationRecord(Long recordId) {
        return violationRecordRepository.findById(recordId);
    }

    /**
     * 查询某提交下的所有违纪记录
     */
    @Transactional(readOnly = true)
    public List<ViolationRecord> listBySubmission(Long submissionId) {
        return violationRecordRepository.findBySubmissionId(submissionId);
    }

    /**
     * 查询某用户的所有违纪记录
     */
    @Transactional(readOnly = true)
    public List<ViolationRecord> listByUser(Long userId) {
        return violationRecordRepository.findByUserId(userId);
    }
}
