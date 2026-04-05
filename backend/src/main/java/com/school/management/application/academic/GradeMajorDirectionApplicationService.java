package com.school.management.application.academic;

import com.school.management.application.academic.query.GradeMajorDirectionDTO;
import com.school.management.domain.student.model.CohortOpenedDirection;
import com.school.management.domain.student.repository.CohortOpenedDirectionRepository;
import com.school.management.domain.student.repository.CohortRepository;
import com.school.management.domain.academic.repository.MajorRepository;
import com.school.management.infrastructure.persistence.academic.GradeMajorDirectionMapper;
import com.school.management.infrastructure.persistence.academic.GradeMajorDirectionPO;
import com.school.management.infrastructure.persistence.student.CohortPersistenceMapper;
import com.school.management.infrastructure.persistence.student.CohortPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 年级-专业方向关联 应用服务
 */
@RequiredArgsConstructor
@Service
public class GradeMajorDirectionApplicationService {

    private final CohortOpenedDirectionRepository gradeOpenedDirectionRepository;
    private final CohortRepository gradeRepository;
    private final MajorRepository majorRepository;
    private final GradeMajorDirectionMapper gradeMajorDirectionMapper;
    private final CohortPersistenceMapper gradePersistenceMapper;

    /**
     * 根据学年（入学年份）获取年级-方向列表，含 JOIN 信息
     */
    @Transactional(readOnly = true)
    public List<GradeMajorDirectionDTO> getDirectionsByYear(Integer enrollmentYear) {
        CohortPO grade = gradePersistenceMapper.findByEnrollmentYear(enrollmentYear);
        if (grade == null) {
            return new ArrayList<>();
        }

        List<GradeMajorDirectionPO> poList = gradeMajorDirectionMapper.findByGradeIdWithJoin(grade.getId());
        return poList.stream().map(po -> toDTO(po, enrollmentYear)).collect(Collectors.toList());
    }

    /**
     * 根据专业方向ID获取关联的年级列表
     */
    @Transactional(readOnly = true)
    public List<GradeMajorDirectionDTO> getDirectionsByMajorDirectionId(Long directionId) {
        List<GradeMajorDirectionPO> poList = gradeMajorDirectionMapper.findByMajorDirectionId(directionId);
        return poList.stream().map(po -> toDTO(po, null)).collect(Collectors.toList());
    }

    /**
     * 根据学年和方向ID获取单条记录
     */
    @Transactional(readOnly = true)
    public GradeMajorDirectionDTO getByYearAndDirection(Integer enrollmentYear, Long directionId) {
        CohortPO grade = gradePersistenceMapper.findByEnrollmentYear(enrollmentYear);
        if (grade == null) {
            throw new IllegalArgumentException("年级不存在: " + enrollmentYear + "级");
        }

        GradeMajorDirectionPO po = gradeMajorDirectionMapper.findByGradeIdAndMajorDirectionId(grade.getId(), directionId);
        if (po == null) {
            throw new IllegalArgumentException("未找到该年级-方向关联");
        }
        return toDTO(po, enrollmentYear);
    }

    /**
     * 根据ID获取详情
     */
    @Transactional(readOnly = true)
    public GradeMajorDirectionDTO getById(Long id) {
        GradeMajorDirectionPO po = gradeMajorDirectionMapper.selectById(id);
        if (po == null) {
            throw new IllegalArgumentException("年级-方向关联不存在: " + id);
        }
        return toDTO(po, null);
    }

    /**
     * 为学年添加专业方向
     */
    @Transactional
    public GradeMajorDirectionDTO addDirectionToYear(Integer academicYear, Long majorDirectionId,
                                                      String remarks, Long createdBy) {
        CohortPO grade = gradePersistenceMapper.findByEnrollmentYear(academicYear);
        if (grade == null) {
            throw new IllegalArgumentException("年级不存在: " + academicYear + "级");
        }

        // 检查是否已存在
        if (gradeOpenedDirectionRepository.existsByGradeIdAndMajorDirectionId(grade.getId(), majorDirectionId)) {
            throw new IllegalArgumentException("该年级已开设此专业方向");
        }

        // 查找方向关联的专业信息
        var major = majorRepository.findByDirectionId(majorDirectionId)
                .orElseThrow(() -> new IllegalArgumentException("专业方向不存在: " + majorDirectionId));

        CohortOpenedDirection entity = CohortOpenedDirection.create(
                grade.getId(), majorDirectionId, null, null, remarks, createdBy);

        entity = gradeOpenedDirectionRepository.save(entity);

        // 更新冗余字段
        GradeMajorDirectionPO po = gradeMajorDirectionMapper.selectById(entity.getId());
        if (po != null) {
            po.setMajorId(major.getId());
            po.setMajorName(major.getMajorName());
            gradeMajorDirectionMapper.updateById(po);
        }

        return toDTO(gradeMajorDirectionMapper.selectById(entity.getId()), academicYear);
    }

    /**
     * 批量为学年添加专业方向
     */
    @Transactional
    public void batchAddDirectionsToYear(Integer academicYear, List<Long> directionIds, Long createdBy) {
        for (Long directionId : directionIds) {
            try {
                addDirectionToYear(academicYear, directionId, null, createdBy);
            } catch (IllegalArgumentException e) {
                // skip already existing ones
            }
        }
    }

    /**
     * 更新年级-方向配置
     */
    @Transactional
    public GradeMajorDirectionDTO updateGradeMajorDirection(Long id, String remarks, Long updatedBy) {
        CohortOpenedDirection entity = gradeOpenedDirectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("年级-方向关联不存在: " + id));
        entity.updatePlan(entity.getPlannedClasses(), entity.getPlannedStudents(), remarks, updatedBy);
        gradeOpenedDirectionRepository.save(entity);
        return toDTO(gradeMajorDirectionMapper.selectById(id), null);
    }

    /**
     * 删除年级-方向关联
     */
    @Transactional
    public void deleteGradeMajorDirection(Long id) {
        gradeOpenedDirectionRepository.deleteById(id);
    }

    /**
     * 批量删除
     */
    @Transactional
    public void batchDeleteGradeMajorDirections(List<Long> ids) {
        for (Long id : ids) {
            gradeOpenedDirectionRepository.deleteById(id);
        }
    }

    // ======================== Mapping ========================

    private GradeMajorDirectionDTO toDTO(GradeMajorDirectionPO po, Integer enrollmentYear) {
        if (po == null) return null;
        GradeMajorDirectionDTO dto = new GradeMajorDirectionDTO();
        dto.setId(po.getId());
        dto.setAcademicYear(enrollmentYear != null ? enrollmentYear : po.getEnrollmentYear());
        dto.setMajorDirectionId(po.getMajorDirectionId());
        dto.setMajorId(po.getMajorId());
        dto.setMajorCode(po.getMajorCode());
        dto.setDirectionCode(po.getDirectionCode());
        dto.setDirectionName(po.getDirectionName());
        dto.setMajorName(po.getMajorName());
        dto.setLevel(po.getLevel());
        dto.setYears(po.getYears());
        dto.setOrgUnitId(po.getOrgUnitId());
        dto.setIsSegmented(po.getIsSegmented());
        dto.setPhase1Level(po.getPhase1Level());
        dto.setPhase1Years(po.getPhase1Years());
        dto.setPhase2Level(po.getPhase2Level());
        dto.setPhase2Years(po.getPhase2Years());
        dto.setActualClassCount(po.getActualClassCount());
        dto.setRemarks(po.getRemarks());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }
}
