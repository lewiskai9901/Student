package com.school.management.application.academic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.academic.command.CreateMajorCommand;
import com.school.management.application.academic.command.CreateMajorDirectionCommand;
import com.school.management.application.academic.command.UpdateMajorCommand;
import com.school.management.application.academic.command.UpdateMajorDirectionCommand;
import com.school.management.application.academic.query.MajorDTO;
import com.school.management.application.academic.query.MajorDirectionDTO;
import com.school.management.domain.academic.model.Major;
import com.school.management.domain.academic.model.MajorDirection;
import com.school.management.domain.academic.model.MajorStatus;
import com.school.management.domain.academic.repository.MajorRepository;
import com.school.management.infrastructure.persistence.academic.MajorDirectionPO;
import com.school.management.infrastructure.persistence.academic.MajorDirectionPersistenceMapper;
import com.school.management.infrastructure.persistence.academic.MajorPO;
import com.school.management.infrastructure.persistence.academic.MajorPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 专业管理应用服务
 */
@RequiredArgsConstructor
@Service
public class MajorApplicationService {

    private final MajorRepository majorRepository;
    private final MajorPersistenceMapper majorMapper;
    private final MajorDirectionPersistenceMapper directionMapper;

    // ======================== 专业 CRUD ========================

    @Transactional(readOnly = true)
    public Page<MajorDTO> getMajorList(String name, String code, Long orgUnitId, Integer status,
                                        int pageNum, int pageSize) {
        Page<MajorPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MajorPO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(MajorPO::getMajorName, name);
        }
        if (StringUtils.hasText(code)) {
            wrapper.like(MajorPO::getMajorCode, code);
        }
        if (orgUnitId != null) {
            wrapper.eq(MajorPO::getOrgUnitId, orgUnitId);
        }
        if (status != null) {
            wrapper.eq(MajorPO::getStatus, status);
        }
        wrapper.orderByAsc(MajorPO::getId);

        Page<MajorPO> result = majorMapper.selectPage(page, wrapper);

        Page<MajorDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::poToMajorDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Transactional(readOnly = true)
    public List<MajorDTO> getAllEnabledMajors() {
        return majorRepository.findAllEnabled().stream()
                .map(this::toMajorDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MajorDTO> getMajorsByOrgUnit(Long orgUnitId) {
        return majorRepository.findByOrgUnitId(orgUnitId).stream()
                .map(this::toMajorDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MajorDTO getMajor(Long id) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("专业不存在: " + id));
        return toMajorDTO(major);
    }

    @Transactional
    public MajorDTO createMajor(CreateMajorCommand command) {
        if (majorRepository.existsByMajorCode(command.getMajorCode())) {
            throw new IllegalArgumentException("专业编码已存在: " + command.getMajorCode());
        }

        Major major = Major.create(
                command.getMajorCode(),
                command.getMajorName(),
                command.getOrgUnitId(),
                command.getDescription(),
                command.getCreatedBy()
        );

        // Set vocational fields if provided
        MajorStatus status = null;
        if (StringUtils.hasText(command.getMajorStatus())) {
            try {
                status = MajorStatus.valueOf(command.getMajorStatus());
            } catch (IllegalArgumentException ignored) {
            }
        }
        major.updateVocationalInfo(
                command.getMajorCategoryCode(),
                command.getEnrollmentTarget(),
                command.getEducationForm(),
                command.getLeadTeacherId(),
                command.getLeadTeacherName(),
                command.getApprovalYear(),
                status
        );

        major = majorRepository.save(major);
        return toMajorDTO(major);
    }

    @Transactional
    public MajorDTO updateMajor(Long id, UpdateMajorCommand command) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("专业不存在: " + id));

        major.updateInfo(command.getMajorName(), command.getDescription(), null, command.getUpdatedBy());

        if (command.getStatus() != null) {
            if (command.getStatus() == 1) {
                major.enable();
            } else {
                major.disable();
            }
        }

        // Update vocational fields
        MajorStatus majorStatus = null;
        if (StringUtils.hasText(command.getMajorStatus())) {
            try {
                majorStatus = MajorStatus.valueOf(command.getMajorStatus());
            } catch (IllegalArgumentException ignored) {
            }
        }
        major.updateVocationalInfo(
                command.getMajorCategoryCode(),
                command.getEnrollmentTarget(),
                command.getEducationForm(),
                command.getLeadTeacherId(),
                command.getLeadTeacherName(),
                command.getApprovalYear(),
                majorStatus
        );

        major = majorRepository.save(major);
        return toMajorDTO(major);
    }

    @Transactional
    public void deleteMajor(Long id) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("专业不存在: " + id));
        majorRepository.delete(major);
    }

    // ======================== 专业方向 CRUD ========================

    @Transactional(readOnly = true)
    public List<MajorDirectionDTO> getDirectionsByMajor(Long majorId) {
        return majorRepository.findDirectionsByMajorId(majorId).stream()
                .map(d -> toDirectionDTO(d, majorId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MajorDirectionDTO> getAllDirections() {
        List<MajorDirectionPO> allDirections = directionMapper.selectList(
                new LambdaQueryWrapper<MajorDirectionPO>().orderByAsc(MajorDirectionPO::getId));
        return allDirections.stream().map(this::poToDirectionDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MajorDirectionDTO getDirection(Long id) {
        MajorDirection direction = majorRepository.findDirectionById(id)
                .orElseThrow(() -> new IllegalArgumentException("专业方向不存在: " + id));
        // find majorId from PO
        MajorDirectionPO po = directionMapper.selectById(id);
        return toDirectionDTO(direction, po != null ? po.getMajorId() : null);
    }

    @Transactional
    public MajorDirectionDTO createDirection(CreateMajorDirectionCommand command) {
        Major major = majorRepository.findById(command.getMajorId())
                .orElseThrow(() -> new IllegalArgumentException("专业不存在: " + command.getMajorId()));

        boolean isSegmented = command.getIsSegmented() != null && command.getIsSegmented() == 1;

        MajorDirection direction = major.addDirection(
                command.getDirectionCode(),
                command.getDirectionName(),
                command.getLevel(),
                command.getYears(),
                isSegmented,
                command.getPhase1Level(),
                command.getPhase1Years(),
                command.getPhase2Level(),
                command.getPhase2Years()
        );
        if (command.getRemarks() != null) {
            direction.setRemarks(command.getRemarks());
        }

        // Set vocational fields
        direction.updateVocationalInfo(
                command.getEnrollmentTarget(),
                command.getEducationForm(),
                command.getCertificateNames(),
                command.getTrainingStandard(),
                command.getCooperationEnterprise(),
                command.getMaxEnrollment(),
                command.getSortOrder()
        );

        majorRepository.save(major);
        return toDirectionDTO(direction, major.getId());
    }

    @Transactional
    public MajorDirectionDTO updateDirection(Long id, UpdateMajorDirectionCommand command) {
        Major major = majorRepository.findByDirectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("专业方向不存在: " + id));

        boolean isSegmented = command.getIsSegmented() != null && command.getIsSegmented() == 1;

        major.updateDirection(
                id,
                command.getDirectionName(),
                command.getLevel(),
                command.getYears(),
                isSegmented,
                command.getPhase1Level(),
                command.getPhase1Years(),
                command.getPhase2Level(),
                command.getPhase2Years()
        );

        MajorDirection direction = major.findDirectionById(id).orElse(null);
        if (direction != null) {
            if (command.getRemarks() != null) {
                direction.setRemarks(command.getRemarks());
            }
            direction.updateVocationalInfo(
                    command.getEnrollmentTarget(),
                    command.getEducationForm(),
                    command.getCertificateNames(),
                    command.getTrainingStandard(),
                    command.getCooperationEnterprise(),
                    command.getMaxEnrollment(),
                    command.getSortOrder()
            );
        }

        majorRepository.save(major);
        return toDirectionDTO(direction, major.getId());
    }

    @Transactional
    public void deleteDirection(Long id) {
        Major major = majorRepository.findByDirectionId(id)
                .orElseThrow(() -> new IllegalArgumentException("专业方向不存在: " + id));
        major.removeDirection(id);
        majorRepository.save(major);
    }

    // ======================== Mapping Helpers ========================

    private MajorDTO toMajorDTO(Major major) {
        MajorDTO dto = new MajorDTO();
        dto.setId(major.getId());
        dto.setMajorCode(major.getMajorCode());
        dto.setMajorName(major.getMajorName());
        dto.setOrgUnitId(major.getOrgUnitId());
        dto.setDescription(major.getDescription());
        dto.setStatus(major.isEnabled() != null && major.isEnabled() ? 1 : 0);
        dto.setStatusName(major.isEnabled() != null && major.isEnabled() ? "启用" : "禁用");
        dto.setMajorCategoryCode(major.getMajorCategoryCode());
        dto.setEnrollmentTarget(major.getEnrollmentTarget());
        dto.setEducationForm(major.getEducationForm());
        dto.setLeadTeacherId(major.getLeadTeacherId());
        dto.setLeadTeacherName(major.getLeadTeacherName());
        dto.setApprovalYear(major.getApprovalYear());
        dto.setMajorStatus(major.getMajorStatus() != null ? major.getMajorStatus().name() : null);
        dto.setCreatedAt(major.getCreatedAt());
        dto.setUpdatedAt(major.getUpdatedAt());
        return dto;
    }

    private MajorDTO poToMajorDTO(MajorPO po) {
        MajorDTO dto = new MajorDTO();
        dto.setId(po.getId());
        dto.setMajorCode(po.getMajorCode());
        dto.setMajorName(po.getMajorName());
        dto.setOrgUnitId(po.getOrgUnitId());
        dto.setDescription(po.getDescription());
        dto.setStatus(po.getStatus());
        dto.setStatusName(po.getStatus() != null && po.getStatus() == 1 ? "启用" : "禁用");
        dto.setMajorCategoryCode(po.getMajorCategoryCode());
        dto.setEnrollmentTarget(po.getEnrollmentTarget());
        dto.setEducationForm(po.getEducationForm());
        dto.setLeadTeacherId(po.getLeadTeacherId());
        dto.setLeadTeacherName(po.getLeadTeacherName());
        dto.setApprovalYear(po.getApprovalYear());
        dto.setMajorStatus(po.getMajorStatus());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }

    private MajorDirectionDTO toDirectionDTO(MajorDirection direction, Long majorId) {
        if (direction == null) return null;
        MajorDirectionDTO dto = new MajorDirectionDTO();
        dto.setId(direction.getId());
        dto.setMajorId(majorId);
        dto.setDirectionCode(direction.getDirectionCode());
        dto.setDirectionName(direction.getDirectionName());
        dto.setLevel(direction.getLevel());
        dto.setYears(direction.getYears());
        dto.setIsSegmented(direction.isSegmented() != null && direction.isSegmented() ? 1 : 0);
        dto.setPhase1Level(direction.getPhase1Level());
        dto.setPhase1Years(direction.getPhase1Years());
        dto.setPhase2Level(direction.getPhase2Level());
        dto.setPhase2Years(direction.getPhase2Years());
        dto.setRemarks(direction.getRemarks());
        dto.setLevelDisplay(direction.getLevelDisplay());
        dto.setYearsDisplay(direction.getYearsDisplay());
        dto.setEnrollmentTarget(direction.getEnrollmentTarget());
        dto.setEducationForm(direction.getEducationForm());
        dto.setCertificateNames(direction.getCertificateNames());
        dto.setTrainingStandard(direction.getTrainingStandard());
        dto.setCooperationEnterprise(direction.getCooperationEnterprise());
        dto.setMaxEnrollment(direction.getMaxEnrollment());
        dto.setSortOrder(direction.getSortOrder());
        dto.setCreatedAt(direction.getCreatedAt());
        dto.setUpdatedAt(direction.getUpdatedAt());
        return dto;
    }

    private MajorDirectionDTO poToDirectionDTO(MajorDirectionPO po) {
        MajorDirectionDTO dto = new MajorDirectionDTO();
        dto.setId(po.getId());
        dto.setMajorId(po.getMajorId());
        dto.setDirectionCode(po.getDirectionCode());
        dto.setDirectionName(po.getDirectionName());
        dto.setLevel(po.getLevel());
        dto.setYears(po.getYears());
        dto.setIsSegmented(po.getIsSegmented());
        dto.setPhase1Level(po.getPhase1Level());
        dto.setPhase1Years(po.getPhase1Years());
        dto.setPhase2Level(po.getPhase2Level());
        dto.setPhase2Years(po.getPhase2Years());
        dto.setRemarks(po.getRemarks());
        dto.setEnrollmentTarget(po.getEnrollmentTarget());
        dto.setEducationForm(po.getEducationForm());
        dto.setCertificateNames(po.getCertificateNames());
        dto.setTrainingStandard(po.getTrainingStandard());
        dto.setCooperationEnterprise(po.getCooperationEnterprise());
        dto.setMaxEnrollment(po.getMaxEnrollment());
        dto.setSortOrder(po.getSortOrder());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        // Compute display fields
        boolean segmented = po.getIsSegmented() != null && po.getIsSegmented() == 1;
        if (segmented) {
            String p1 = po.getPhase1Level() != null ? po.getPhase1Level() : "";
            String p2 = po.getPhase2Level() != null ? po.getPhase2Level() : "";
            dto.setLevelDisplay(p1 + " → " + p2);
            int py1 = po.getPhase1Years() != null ? po.getPhase1Years() : 0;
            int py2 = po.getPhase2Years() != null ? po.getPhase2Years() : 0;
            dto.setYearsDisplay(py1 + "+" + py2 + "年");
        } else {
            dto.setLevelDisplay(po.getLevel());
            dto.setYearsDisplay((po.getYears() != null ? po.getYears() : 0) + "年");
        }
        return dto;
    }
}
