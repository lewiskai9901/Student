package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.teaching.model.aggregate.Examination;
import com.school.management.domain.teaching.model.entity.ExamArrangement;
import com.school.management.domain.teaching.model.entity.ExamInvigilator;
import com.school.management.domain.teaching.model.entity.ExamRoom;
import com.school.management.domain.teaching.repository.ExaminationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExaminationRepositoryImpl implements ExaminationRepository {

    private final ExamBatchMapper batchMapper;
    private final ExamArrangementMapper arrangementMapper;
    private final ExamRoomMapper roomMapper;
    private final ExamInvigilatorMapper invigilatorMapper;

    @Override
    public Examination save(Examination exam) {
        ExamBatchPO po = toBatchPO(exam);
        if (po.getId() == null) {
            batchMapper.insert(po);
        } else {
            batchMapper.updateById(po);
        }
        exam.setId(po.getId());
        return exam;
    }

    @Override
    public Optional<Examination> findById(Long id) {
        ExamBatchPO po = batchMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toBatchDomain);
    }

    @Override
    public Optional<Examination> findByIdWithArrangements(Long id) {
        return findById(id).map(exam -> {
            List<ExamArrangement> arrangements = findArrangementsByBatchId(id);
            exam.setArrangements(arrangements);
            return exam;
        });
    }

    @Override
    public List<Examination> findBySemesterId(Long semesterId) {
        LambdaQueryWrapper<ExamBatchPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamBatchPO::getSemesterId, semesterId)
                .orderByDesc(ExamBatchPO::getStartDate);
        return batchMapper.selectList(wrapper).stream()
                .map(this::toBatchDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        List<ExamArrangement> arrangements = findArrangementsByBatchId(id);
        for (ExamArrangement arr : arrangements) {
            deleteArrangement(arr.getId());
        }
        batchMapper.deleteById(id);
    }

    @Override
    public List<Examination> findBatchesPage(int page, int size, Long semesterId, Integer examType, Integer status) {
        LambdaQueryWrapper<ExamBatchPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) wrapper.eq(ExamBatchPO::getSemesterId, semesterId);
        if (examType != null) wrapper.eq(ExamBatchPO::getExamType, examType);
        if (status != null) wrapper.eq(ExamBatchPO::getStatus, status);
        wrapper.orderByDesc(ExamBatchPO::getStartDate);
        Page<ExamBatchPO> pageResult = batchMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream().map(this::toBatchDomain).collect(Collectors.toList());
    }

    @Override
    public long countBatches(Long semesterId, Integer examType, Integer status) {
        LambdaQueryWrapper<ExamBatchPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) wrapper.eq(ExamBatchPO::getSemesterId, semesterId);
        if (examType != null) wrapper.eq(ExamBatchPO::getExamType, examType);
        if (status != null) wrapper.eq(ExamBatchPO::getStatus, status);
        return batchMapper.selectCount(wrapper);
    }

    @Override
    public ExamArrangement saveArrangement(ExamArrangement arrangement) {
        ExamArrangementPO po = toArrangementPO(arrangement);
        if (po.getId() == null) {
            arrangementMapper.insert(po);
        } else {
            arrangementMapper.updateById(po);
        }
        arrangement.setId(po.getId());
        return arrangement;
    }

    @Override
    public Optional<ExamArrangement> findArrangementById(Long id) {
        ExamArrangementPO po = arrangementMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toArrangementDomain);
    }

    @Override
    public Optional<ExamArrangement> findArrangementByIdWithRooms(Long id) {
        return findArrangementById(id).map(arr -> {
            List<ExamRoom> rooms = findRoomsByArrangementId(id);
            arr.setExamRooms(rooms);
            return arr;
        });
    }

    @Override
    public List<ExamArrangement> findArrangementsByBatchId(Long batchId) {
        LambdaQueryWrapper<ExamArrangementPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamArrangementPO::getBatchId, batchId).orderByAsc(ExamArrangementPO::getExamDate);
        return arrangementMapper.selectList(wrapper).stream()
                .map(this::toArrangementDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamArrangement> findArrangementsByCourseId(Long semesterId, Long courseId) {
        return arrangementMapper.findByCourseId(semesterId, courseId).stream()
                .map(this::toArrangementDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamArrangement> findArrangementsByClassroomId(Long semesterId, Long classroomId) {
        return arrangementMapper.findByClassroomId(semesterId, classroomId).stream()
                .map(this::toArrangementDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteArrangement(Long id) {
        List<ExamRoom> rooms = findRoomsByArrangementId(id);
        for (ExamRoom room : rooms) {
            invigilatorMapper.deleteByRoomId(room.getId());
        }
        roomMapper.deleteByArrangementId(id);
        arrangementMapper.deleteById(id);
    }

    @Override
    public void saveExamRooms(Long arrangementId, List<ExamRoom> rooms) {
        roomMapper.deleteByArrangementId(arrangementId);
        for (ExamRoom room : rooms) {
            ExamRoomPO po = toRoomPO(room);
            po.setArrangementId(arrangementId);
            po.setCreatedAt(LocalDateTime.now());
            roomMapper.insert(po);
            room.setId(po.getId());
        }
    }

    @Override
    public List<ExamRoom> findRoomsByArrangementId(Long arrangementId) {
        LambdaQueryWrapper<ExamRoomPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRoomPO::getArrangementId, arrangementId);
        return roomMapper.selectList(wrapper).stream()
                .map(po -> {
                    ExamRoom room = toRoomDomain(po);
                    room.setInvigilators(findInvigilatorsByRoomId(room.getId()));
                    return room;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void saveInvigilators(Long roomId, List<ExamInvigilator> invigilators) {
        invigilatorMapper.deleteByRoomId(roomId);
        for (ExamInvigilator inv : invigilators) {
            ExamInvigilatorPO po = toInvigilatorPO(inv);
            po.setRoomId(roomId);
            po.setCreatedAt(LocalDateTime.now());
            invigilatorMapper.insert(po);
            inv.setId(po.getId());
        }
    }

    @Override
    public List<ExamInvigilator> findInvigilatorsByRoomId(Long roomId) {
        LambdaQueryWrapper<ExamInvigilatorPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamInvigilatorPO::getRoomId, roomId);
        return invigilatorMapper.selectList(wrapper).stream()
                .map(this::toInvigilatorDomain)
                .collect(Collectors.toList());
    }

    private ExamBatchPO toBatchPO(Examination d) {
        ExamBatchPO po = new ExamBatchPO();
        po.setId(d.getId()); po.setSemesterId(d.getSemesterId()); po.setBatchName(d.getBatchName());
        po.setExamType(d.getExamType()); po.setStartDate(d.getStartDate()); po.setEndDate(d.getEndDate());
        po.setStatus(d.getStatus()); po.setRemark(d.getRemark()); po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt()); po.setUpdatedBy(d.getUpdatedBy()); po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private Examination toBatchDomain(ExamBatchPO po) {
        return Examination.builder().id(po.getId()).semesterId(po.getSemesterId()).batchName(po.getBatchName())
                .examType(po.getExamType()).startDate(po.getStartDate()).endDate(po.getEndDate())
                .status(po.getStatus()).remark(po.getRemark()).createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt()).updatedBy(po.getUpdatedBy()).updatedAt(po.getUpdatedAt()).build();
    }

    private ExamArrangementPO toArrangementPO(ExamArrangement d) {
        ExamArrangementPO po = new ExamArrangementPO();
        po.setId(d.getId()); po.setBatchId(d.getBatchId()); po.setCourseId(d.getCourseId());
        po.setExamDate(d.getExamDate()); po.setStartTime(d.getStartTime()); po.setEndTime(d.getEndTime());
        po.setStatus(d.getStatus()); po.setRemark(d.getRemark()); po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private ExamArrangement toArrangementDomain(ExamArrangementPO po) {
        return ExamArrangement.builder().id(po.getId()).batchId(po.getBatchId()).courseId(po.getCourseId())
                .examDate(po.getExamDate()).startTime(po.getStartTime()).endTime(po.getEndTime())
                .status(po.getStatus()).remark(po.getRemark()).createdAt(po.getCreatedAt()).build();
    }

    private ExamRoomPO toRoomPO(ExamRoom d) {
        ExamRoomPO po = new ExamRoomPO();
        po.setId(d.getId()); po.setArrangementId(d.getArrangementId()); po.setClassroomId(d.getClassroomId());
        po.setCapacity(d.getCapacity()); po.setActualCount(d.getActualCount());
        return po;
    }

    private ExamRoom toRoomDomain(ExamRoomPO po) {
        return ExamRoom.builder().id(po.getId()).arrangementId(po.getArrangementId())
                .classroomId(po.getClassroomId()).capacity(po.getCapacity()).actualCount(po.getActualCount()).build();
    }

    private ExamInvigilatorPO toInvigilatorPO(ExamInvigilator d) {
        ExamInvigilatorPO po = new ExamInvigilatorPO();
        po.setId(d.getId()); po.setRoomId(d.getRoomId()); po.setTeacherId(d.getTeacherId()); po.setIsMain(d.getIsMain());
        return po;
    }

    private ExamInvigilator toInvigilatorDomain(ExamInvigilatorPO po) {
        return ExamInvigilator.builder().id(po.getId()).roomId(po.getRoomId())
                .teacherId(po.getTeacherId()).isMain(po.getIsMain()).build();
    }
}
