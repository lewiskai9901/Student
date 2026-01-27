package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.entity.ScheduleAdjustment;
import com.school.management.domain.teaching.repository.ScheduleAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 调课记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ScheduleAdjustmentRepositoryImpl implements ScheduleAdjustmentRepository {

    private final ScheduleAdjustmentMapper adjustmentMapper;

    @Override
    public ScheduleAdjustment save(ScheduleAdjustment adjustment) {
        ScheduleAdjustmentPO po = toPO(adjustment);
        if (po.getId() == null) {
            adjustmentMapper.insert(po);
        } else {
            adjustmentMapper.updateById(po);
        }
        adjustment.setId(po.getId());
        return adjustment;
    }

    @Override
    public Optional<ScheduleAdjustment> findById(Long id) {
        ScheduleAdjustmentPO po = adjustmentMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ScheduleAdjustment> findByEntryId(Long entryId) {
        LambdaQueryWrapper<ScheduleAdjustmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAdjustmentPO::getEntryId, entryId)
                .orderByDesc(ScheduleAdjustmentPO::getCreatedAt);
        return adjustmentMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleAdjustment> findByApplicantId(Long applicantId) {
        LambdaQueryWrapper<ScheduleAdjustmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAdjustmentPO::getApplicantId, applicantId)
                .orderByDesc(ScheduleAdjustmentPO::getCreatedAt);
        return adjustmentMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleAdjustment> findPendingBySemesterId(Long semesterId) {
        return adjustmentMapper.findPendingBySemesterId(semesterId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsPendingByEntryIdAndDate(Long entryId, LocalDate date) {
        LambdaQueryWrapper<ScheduleAdjustmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAdjustmentPO::getEntryId, entryId)
                .eq(ScheduleAdjustmentPO::getOriginalDate, date)
                .eq(ScheduleAdjustmentPO::getStatus, 0);
        return adjustmentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<ScheduleAdjustment> findApprovedByDateRange(Long entryId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ScheduleAdjustmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAdjustmentPO::getEntryId, entryId)
                .eq(ScheduleAdjustmentPO::getStatus, 1)
                .ge(ScheduleAdjustmentPO::getOriginalDate, startDate)
                .le(ScheduleAdjustmentPO::getOriginalDate, endDate);
        return adjustmentMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleAdjustment> findPage(int page, int size, Long semesterId,
                                              Integer adjustType, Integer status) {
        LambdaQueryWrapper<ScheduleAdjustmentPO> wrapper = new LambdaQueryWrapper<>();
        if (adjustType != null) {
            wrapper.eq(ScheduleAdjustmentPO::getAdjustType, adjustType);
        }
        if (status != null) {
            wrapper.eq(ScheduleAdjustmentPO::getStatus, status);
        }
        wrapper.orderByDesc(ScheduleAdjustmentPO::getCreatedAt);

        Page<ScheduleAdjustmentPO> pageResult = adjustmentMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long semesterId, Integer adjustType, Integer status) {
        LambdaQueryWrapper<ScheduleAdjustmentPO> wrapper = new LambdaQueryWrapper<>();
        if (adjustType != null) {
            wrapper.eq(ScheduleAdjustmentPO::getAdjustType, adjustType);
        }
        if (status != null) {
            wrapper.eq(ScheduleAdjustmentPO::getStatus, status);
        }
        return adjustmentMapper.selectCount(wrapper);
    }

    private ScheduleAdjustmentPO toPO(ScheduleAdjustment domain) {
        ScheduleAdjustmentPO po = new ScheduleAdjustmentPO();
        po.setId(domain.getId());
        po.setEntryId(domain.getEntryId());
        po.setAdjustType(domain.getAdjustType());
        po.setOriginalDate(domain.getOriginalDate());
        po.setOriginalSlot(domain.getOriginalSlot());
        po.setNewDate(domain.getNewDate());
        po.setNewSlot(domain.getNewSlot());
        po.setNewClassroomId(domain.getNewClassroomId());
        po.setSubstituteTeacherId(domain.getSubstituteTeacherId());
        po.setReason(domain.getReason());
        po.setStatus(domain.getStatus());
        po.setApplicantId(domain.getApplicantId());
        po.setApproverId(domain.getApproverId());
        po.setApprovedAt(domain.getApprovedAt());
        po.setApprovalRemark(domain.getApprovalRemark());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private ScheduleAdjustment toDomain(ScheduleAdjustmentPO po) {
        return ScheduleAdjustment.builder()
                .id(po.getId())
                .entryId(po.getEntryId())
                .adjustType(po.getAdjustType())
                .originalDate(po.getOriginalDate())
                .originalSlot(po.getOriginalSlot())
                .newDate(po.getNewDate())
                .newSlot(po.getNewSlot())
                .newClassroomId(po.getNewClassroomId())
                .substituteTeacherId(po.getSubstituteTeacherId())
                .reason(po.getReason())
                .status(po.getStatus())
                .applicantId(po.getApplicantId())
                .approverId(po.getApproverId())
                .approvedAt(po.getApprovedAt())
                .approvalRemark(po.getApprovalRemark())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
