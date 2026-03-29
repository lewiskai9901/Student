package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.teaching.model.scheduling.ScheduleConflictRecord;
import com.school.management.domain.teaching.repository.ScheduleConflictRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ScheduleConflictRecordRepositoryImpl implements ScheduleConflictRecordRepository {
    private final ScheduleConflictRecordMapper mapper;

    @Override
    public ScheduleConflictRecord save(ScheduleConflictRecord record) {
        ScheduleConflictRecordPO po = toPO(record);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public List<ScheduleConflictRecord> findBySemesterId(Long semesterId) {
        return mapper.selectList(new LambdaQueryWrapper<ScheduleConflictRecordPO>()
                .eq(ScheduleConflictRecordPO::getSemesterId, semesterId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleConflictRecord> findByDetectionBatch(String batch) {
        return mapper.selectList(new LambdaQueryWrapper<ScheduleConflictRecordPO>()
                .eq(ScheduleConflictRecordPO::getDetectionBatch, batch))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleConflictRecord> findBySemesterIdAndStatus(Long semesterId, Integer status) {
        return mapper.selectList(new LambdaQueryWrapper<ScheduleConflictRecordPO>()
                .eq(ScheduleConflictRecordPO::getSemesterId, semesterId)
                .eq(ScheduleConflictRecordPO::getResolutionStatus, status))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByDetectionBatch(String batch) {
        mapper.delete(new LambdaQueryWrapper<ScheduleConflictRecordPO>()
                .eq(ScheduleConflictRecordPO::getDetectionBatch, batch));
    }

    private ScheduleConflictRecordPO toPO(ScheduleConflictRecord r) {
        ScheduleConflictRecordPO po = new ScheduleConflictRecordPO();
        po.setId(r.getId());
        po.setSemesterId(r.getSemesterId());
        po.setDetectionBatch(r.getDetectionBatch());
        po.setConflictCategory(r.getConflictCategory());
        po.setConflictType(r.getConflictType());
        po.setSeverity(r.getSeverity());
        po.setDescription(r.getDescription());
        po.setDetail(r.getDetail());
        po.setEntryId1(r.getEntryId1());
        po.setEntryId2(r.getEntryId2());
        po.setConstraintId(r.getConstraintId());
        po.setResolutionStatus(r.getResolutionStatus());
        po.setResolutionNote(r.getResolutionNote());
        po.setResolvedBy(r.getResolvedBy());
        return po;
    }

    private ScheduleConflictRecord toDomain(ScheduleConflictRecordPO po) {
        return ScheduleConflictRecord.reconstruct(
                po.getId(), po.getSemesterId(), po.getDetectionBatch(),
                po.getConflictCategory(), po.getConflictType(), po.getSeverity(),
                po.getDescription(), po.getDetail(),
                po.getEntryId1(), po.getEntryId2(), po.getConstraintId(),
                po.getResolutionStatus(), po.getResolutionNote(), po.getResolvedBy()
        );
    }
}
