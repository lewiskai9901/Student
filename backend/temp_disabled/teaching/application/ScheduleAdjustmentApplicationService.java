package com.school.management.application.teaching;

import com.school.management.application.teaching.command.CreateAdjustmentCommand;
import com.school.management.application.teaching.query.ScheduleAdjustmentDTO;
import com.school.management.domain.teaching.model.entity.ScheduleAdjustment;
import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.repository.CourseScheduleRepository;
import com.school.management.domain.teaching.repository.ScheduleAdjustmentRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调课管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleAdjustmentApplicationService {

    private final ScheduleAdjustmentRepository adjustmentRepository;
    private final CourseScheduleRepository scheduleRepository;

    /**
     * 创建调课申请
     */
    @Transactional
    public Long createAdjustment(CreateAdjustmentCommand command) {
        // 验证课表条目存在
        ScheduleEntry entry = scheduleRepository.findEntryById(command.getEntryId())
                .orElseThrow(() -> new BusinessException("课表条目不存在"));

        // 验证调整类型参数
        if (command.getAdjustType() == 1 || command.getAdjustType() == 3) {
            // 调课或补课必须有新时间
            if (command.getNewDate() == null || command.getNewSlot() == null) {
                throw new BusinessException("调课或补课必须指定新的日期和节次");
            }
        }
        if (command.getAdjustType() == 4) {
            // 代课必须有代课教师
            if (command.getSubstituteTeacherId() == null) {
                throw new BusinessException("代课必须指定代课教师");
            }
        }

        // 检查是否已有待审批的调课申请
        if (adjustmentRepository.existsPendingByEntryIdAndDate(
                command.getEntryId(), command.getOriginalDate())) {
            throw new BusinessException("该课程该日期已有待审批的调课申请");
        }

        ScheduleAdjustment adjustment = ScheduleAdjustment.builder()
                .entryId(command.getEntryId())
                .adjustType(command.getAdjustType())
                .originalDate(command.getOriginalDate())
                .originalSlot(command.getOriginalSlot())
                .newDate(command.getNewDate())
                .newSlot(command.getNewSlot())
                .newClassroomId(command.getNewClassroomId())
                .substituteTeacherId(command.getSubstituteTeacherId())
                .reason(command.getReason())
                .status(0) // 待审批
                .applicantId(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        adjustment = adjustmentRepository.save(adjustment);
        log.info("创建调课申请成功: id={}, type={}", adjustment.getId(), command.getAdjustType());
        return adjustment.getId();
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(Long id, Long approverId, String remark) {
        ScheduleAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("调课申请不存在"));

        if (adjustment.getStatus() != 0) {
            throw new BusinessException("只能审批待审批的申请");
        }

        adjustment.setStatus(1);
        adjustment.setApproverId(approverId);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setApprovalRemark(remark);

        adjustmentRepository.save(adjustment);

        // 发布领域事件
        // eventPublisher.publish(new ScheduleAdjustedEvent(adjustment));

        log.info("审批通过调课申请: id={}", id);
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(Long id, Long approverId, String remark) {
        ScheduleAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("调课申请不存在"));

        if (adjustment.getStatus() != 0) {
            throw new BusinessException("只能审批待审批的申请");
        }

        adjustment.setStatus(2);
        adjustment.setApproverId(approverId);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setApprovalRemark(remark);

        adjustmentRepository.save(adjustment);
        log.info("审批拒绝调课申请: id={}", id);
    }

    /**
     * 取消申请
     */
    @Transactional
    public void cancel(Long id, Long operatorId) {
        ScheduleAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("调课申请不存在"));

        if (adjustment.getStatus() != 0) {
            throw new BusinessException("只能取消待审批的申请");
        }

        if (!adjustment.getApplicantId().equals(operatorId)) {
            throw new BusinessException("只能取消自己的申请");
        }

        adjustment.setStatus(3);
        adjustmentRepository.save(adjustment);
        log.info("取消调课申请: id={}", id);
    }

    /**
     * 获取调课申请详情
     */
    public ScheduleAdjustmentDTO getAdjustment(Long id) {
        return adjustmentRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("调课申请不存在"));
    }

    /**
     * 获取待审批列表
     */
    public List<ScheduleAdjustmentDTO> getPendingList(Long semesterId) {
        return adjustmentRepository.findPendingBySemesterId(semesterId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据课表条目获取调课记录
     */
    public List<ScheduleAdjustmentDTO> getByEntryId(Long entryId) {
        return adjustmentRepository.findByEntryId(entryId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据申请人获取调课记录
     */
    public List<ScheduleAdjustmentDTO> getByApplicant(Long applicantId) {
        return adjustmentRepository.findByApplicantId(applicantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public List<ScheduleAdjustmentDTO> getPage(int page, int size, Long semesterId,
                                                Integer adjustType, Integer status) {
        return adjustmentRepository.findPage(page, size, semesterId, adjustType, status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计数量
     */
    public long count(Long semesterId, Integer adjustType, Integer status) {
        return adjustmentRepository.count(semesterId, adjustType, status);
    }

    private ScheduleAdjustmentDTO toDTO(ScheduleAdjustment adjustment) {
        ScheduleAdjustmentDTO dto = ScheduleAdjustmentDTO.builder()
                .id(adjustment.getId())
                .entryId(adjustment.getEntryId())
                .adjustType(adjustment.getAdjustType())
                .adjustTypeName(ScheduleAdjustmentDTO.getAdjustTypeName(adjustment.getAdjustType()))
                .originalDate(adjustment.getOriginalDate())
                .originalSlot(adjustment.getOriginalSlot())
                .newDate(adjustment.getNewDate())
                .newSlot(adjustment.getNewSlot())
                .newClassroomId(adjustment.getNewClassroomId())
                .substituteTeacherId(adjustment.getSubstituteTeacherId())
                .reason(adjustment.getReason())
                .status(adjustment.getStatus())
                .statusName(ScheduleAdjustmentDTO.getStatusName(adjustment.getStatus()))
                .applicantId(adjustment.getApplicantId())
                .approverId(adjustment.getApproverId())
                .approvedAt(adjustment.getApprovedAt())
                .approvalRemark(adjustment.getApprovalRemark())
                .createdAt(adjustment.getCreatedAt())
                .build();

        // 设置星期信息
        if (adjustment.getOriginalDate() != null) {
            dto.setOriginalWeekday(adjustment.getOriginalDate().getDayOfWeek().getValue());
            dto.setOriginalWeekdayName(ScheduleEntryDTO.getWeekdayName(dto.getOriginalWeekday()));
        }
        if (adjustment.getNewDate() != null) {
            dto.setNewWeekday(adjustment.getNewDate().getDayOfWeek().getValue());
            dto.setNewWeekdayName(ScheduleEntryDTO.getWeekdayName(dto.getNewWeekday()));
        }

        return dto;
    }
}
