package com.school.management.application.semester;

import com.school.management.application.semester.command.CreateSemesterCommand;
import com.school.management.application.semester.command.UpdateSemesterCommand;
import com.school.management.domain.semester.model.aggregate.Semester;
import com.school.management.domain.semester.model.valueobject.SemesterType;
import com.school.management.domain.semester.repository.SemesterRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 学期应用服务
 *
 * 职责：
 * 1. 协调领域对象完成用例
 * 2. 事务边界管理
 * 3. 领域事件发布
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterApplicationService {

    private final SemesterRepository semesterRepository;
    private final DomainEventPublisher eventPublisher;

    // ==================== 学期创建 ====================

    /**
     * 创建学期
     */
    @Transactional
    public Semester createSemester(CreateSemesterCommand command) {
        log.info("创建学期: {}", command.getSemesterName());

        // 生成学期编码
        SemesterType semesterType = command.getSemesterType() != null
                ? SemesterType.fromCode(command.getSemesterType())
                : SemesterType.FIRST;

        String semesterCode = Semester.generateCode(command.getStartYear(), semesterType);

        // 检查学期编码是否已存在
        if (semesterRepository.existsBySemesterCode(semesterCode)) {
            throw new BusinessException("学期编码已存在: " + semesterCode);
        }

        // 创建学期聚合
        Semester semester = Semester.create(
                command.getSemesterName(),
                semesterCode,
                command.getStartDate(),
                command.getEndDate(),
                command.getStartYear(),
                semesterType
        );

        // 保存
        semester = semesterRepository.save(semester);

        // 发布事件
        publishEvents(semester);

        log.info("学期创建成功: id={}, code={}", semester.getId(), semesterCode);
        return semester;
    }

    // ==================== 学期更新 ====================

    /**
     * 更新学期
     */
    @Transactional
    public Semester updateSemester(Long semesterId, UpdateSemesterCommand command) {
        log.info("更新学期: {}", semesterId);

        Semester semester = getSemesterOrThrow(semesterId);

        semester.updateBasicInfo(
                command.getSemesterName(),
                command.getStartDate(),
                command.getEndDate()
        );

        semester = semesterRepository.save(semester);
        publishEvents(semester);

        log.info("学期更新成功: {}", semesterId);
        return semester;
    }

    // ==================== 当前学期管理 ====================

    /**
     * 设置当前学期
     */
    @Transactional
    public Semester setCurrentSemester(Long semesterId) {
        log.info("设置当前学期: {}", semesterId);

        Semester semester = getSemesterOrThrow(semesterId);

        // 先清除所有当前学期标识
        semesterRepository.clearAllCurrentFlags();

        // 设置新的当前学期
        semester.setAsCurrent();
        semester = semesterRepository.save(semester);

        publishEvents(semester);

        log.info("当前学期设置成功: {}", semesterId);
        return semester;
    }

    /**
     * 获取当前学期
     */
    @Transactional(readOnly = true)
    public Optional<Semester> getCurrentSemester() {
        return semesterRepository.findCurrentSemester();
    }

    // ==================== 学期状态管理 ====================

    /**
     * 结束学期
     */
    @Transactional
    public Semester endSemester(Long semesterId) {
        log.info("结束学期: {}", semesterId);

        Semester semester = getSemesterOrThrow(semesterId);
        semester.end();
        semester = semesterRepository.save(semester);

        publishEvents(semester);
        return semester;
    }

    /**
     * 重新激活学期
     */
    @Transactional
    public Semester reactivateSemester(Long semesterId) {
        log.info("重新激活学期: {}", semesterId);

        Semester semester = getSemesterOrThrow(semesterId);
        semester.reactivate();
        semester = semesterRepository.save(semester);

        return semester;
    }

    // ==================== 删除操作 ====================

    /**
     * 删除学期
     */
    @Transactional
    public void deleteSemester(Long semesterId) {
        log.info("删除学期: {}", semesterId);

        Semester semester = getSemesterOrThrow(semesterId);

        if (semester.getIsCurrent() != null && semester.getIsCurrent()) {
            throw new BusinessException("不能删除当前学期");
        }

        semesterRepository.deleteById(semesterId);
    }

    // ==================== 查询操作 ====================

    /**
     * 根据ID获取学期
     */
    @Transactional(readOnly = true)
    public Optional<Semester> getSemester(Long semesterId) {
        return semesterRepository.findById(semesterId);
    }

    /**
     * 根据学期编码获取学期
     */
    @Transactional(readOnly = true)
    public Optional<Semester> getSemesterByCode(String semesterCode) {
        return semesterRepository.findBySemesterCode(semesterCode);
    }

    /**
     * 获取所有正常状态的学期
     */
    @Transactional(readOnly = true)
    public List<Semester> getActiveSemesters() {
        return semesterRepository.findAllActive();
    }

    /**
     * 根据年份获取学期列表
     */
    @Transactional(readOnly = true)
    public List<Semester> getSemestersByYear(Integer year) {
        return semesterRepository.findByStartYear(year);
    }

    /**
     * 获取所有学期（按开始日期降序）
     */
    @Transactional(readOnly = true)
    public List<Semester> getAllSemesters() {
        return semesterRepository.findAllOrderByStartDateDesc();
    }

    /**
     * 检查学期编码是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsSemesterCode(String semesterCode, Long excludeId) {
        if (excludeId != null) {
            return semesterRepository.existsBySemesterCodeAndIdNot(semesterCode, excludeId);
        }
        return semesterRepository.existsBySemesterCode(semesterCode);
    }

    /**
     * 生成学期编码
     */
    public String generateSemesterCode(Integer startYear, Integer semesterType) {
        SemesterType type = SemesterType.fromCode(semesterType);
        return Semester.generateCode(startYear, type);
    }

    // ==================== Helper Methods ====================

    private Semester getSemesterOrThrow(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException("学期不存在: " + semesterId));
    }

    private void publishEvents(Semester semester) {
        semester.getDomainEvents().forEach(eventPublisher::publish);
        semester.clearDomainEvents();
    }
}
