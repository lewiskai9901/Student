package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.school.management.domain.inspection.model.v7.compliance.ComplianceClause;
import com.school.management.domain.inspection.repository.v7.ComplianceClauseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ComplianceClauseRepositoryImpl implements ComplianceClauseRepository {

    private final ComplianceClauseMapper mapper;

    public ComplianceClauseRepositoryImpl(ComplianceClauseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ComplianceClause save(ComplianceClause clause) {
        ComplianceClausePO po = toPO(clause);
        if (clause.getId() == null) {
            mapper.insert(po);
            clause.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return clause;
    }

    @Override
    public Optional<ComplianceClause> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ComplianceClause> findByStandardId(Long standardId) {
        return mapper.findByStandardId(standardId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByStandardId(Long standardId) {
        mapper.softDeleteByStandardId(standardId);
    }

    private ComplianceClausePO toPO(ComplianceClause d) {
        ComplianceClausePO po = new ComplianceClausePO();
        po.setId(d.getId());
        po.setStandardId(d.getStandardId());
        po.setClauseNumber(d.getClauseNumber());
        po.setClauseTitle(d.getClauseTitle());
        po.setClauseContent(d.getClauseContent());
        po.setParentClauseId(d.getParentClauseId());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private ComplianceClause toDomain(ComplianceClausePO po) {
        return ComplianceClause.reconstruct(ComplianceClause.builder()
                .id(po.getId())
                .standardId(po.getStandardId())
                .clauseNumber(po.getClauseNumber())
                .clauseTitle(po.getClauseTitle())
                .clauseContent(po.getClauseContent())
                .parentClauseId(po.getParentClauseId())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt()));
    }
}
