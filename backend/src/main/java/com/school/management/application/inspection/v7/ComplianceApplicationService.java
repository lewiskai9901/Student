package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.compliance.ComplianceClause;
import com.school.management.domain.inspection.model.v7.compliance.ComplianceStandard;
import com.school.management.domain.inspection.model.v7.compliance.ItemComplianceMapping;
import com.school.management.domain.inspection.repository.v7.ComplianceClauseRepository;
import com.school.management.domain.inspection.repository.v7.ComplianceStandardRepository;
import com.school.management.domain.inspection.repository.v7.ItemComplianceMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceApplicationService {

    private final ComplianceStandardRepository standardRepository;
    private final ComplianceClauseRepository clauseRepository;
    private final ItemComplianceMappingRepository mappingRepository;

    // ========== ComplianceStandard ==========

    @Transactional
    public ComplianceStandard createStandard(String standardCode, String standardName,
                                              String issuingBody, LocalDate effectiveDate,
                                              String version, String description,
                                              Long createdBy) {
        ComplianceStandard standard = ComplianceStandard.reconstruct(ComplianceStandard.builder()
                .standardCode(standardCode)
                .standardName(standardName)
                .issuingBody(issuingBody)
                .effectiveDate(effectiveDate)
                .standardVersion(version)
                .description(description));
        return standardRepository.save(standard);
    }

    @Transactional
    public ComplianceStandard updateStandard(Long id, String standardName,
                                              String issuingBody, LocalDate effectiveDate,
                                              String version, String description) {
        ComplianceStandard standard = standardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("合规标准不存在: " + id));
        standard.update(standardName, issuingBody, effectiveDate, version, description);
        return standardRepository.save(standard);
    }

    public ComplianceStandard getStandardById(Long id) {
        return standardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("合规标准不存在: " + id));
    }

    public List<ComplianceStandard> getAllStandards() {
        return standardRepository.findAll();
    }

    @Transactional
    public void deleteStandard(Long id) {
        clauseRepository.deleteByStandardId(id);
        standardRepository.deleteById(id);
    }

    // ========== ComplianceClause ==========

    @Transactional
    public ComplianceClause createClause(Long standardId, String clauseNumber,
                                          String clauseTitle, String clauseContent,
                                          Long parentClauseId, Integer sortOrder) {
        // 验证标准存在
        standardRepository.findById(standardId)
                .orElseThrow(() -> new IllegalArgumentException("合规标准不存在: " + standardId));
        ComplianceClause clause = ComplianceClause.reconstruct(ComplianceClause.builder()
                .standardId(standardId)
                .clauseNumber(clauseNumber)
                .clauseTitle(clauseTitle)
                .clauseContent(clauseContent)
                .parentClauseId(parentClauseId)
                .sortOrder(sortOrder));
        return clauseRepository.save(clause);
    }

    public List<ComplianceClause> getClausesByStandardId(Long standardId) {
        return clauseRepository.findByStandardId(standardId);
    }

    @Transactional
    public void deleteClause(Long id) {
        clauseRepository.deleteById(id);
    }

    // ========== ItemComplianceMapping ==========

    @Transactional
    public ItemComplianceMapping createMapping(Long templateItemId, Long clauseId,
                                                String coverageLevel, String notes) {
        ItemComplianceMapping mapping = ItemComplianceMapping.reconstruct(ItemComplianceMapping.builder()
                .templateItemId(templateItemId)
                .clauseId(clauseId)
                .coverageLevel(coverageLevel)
                .notes(notes));
        return mappingRepository.save(mapping);
    }

    public List<ItemComplianceMapping> getMappingsByItemId(Long templateItemId) {
        return mappingRepository.findByItemId(templateItemId);
    }

    public List<ItemComplianceMapping> getMappingsByClauseId(Long clauseId) {
        return mappingRepository.findByClauseId(clauseId);
    }

    @Transactional
    public void deleteMapping(Long id) {
        mappingRepository.deleteById(id);
    }

    @Transactional
    public void deleteMappingsByItemId(Long templateItemId) {
        mappingRepository.deleteByItemId(templateItemId);
    }
}
