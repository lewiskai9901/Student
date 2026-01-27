package com.school.management.infrastructure.persistence.corrective;

import com.school.management.domain.corrective.model.*;
import com.school.management.domain.corrective.repository.AutoActionRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AutoActionRuleRepositoryImpl implements AutoActionRuleRepository {

    private final AutoActionRuleMapper mapper;

    public AutoActionRuleRepositoryImpl(AutoActionRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AutoActionRule save(AutoActionRule aggregate) {
        AutoActionRulePO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            mapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<AutoActionRule> findById(Long id) {
        AutoActionRulePO po = mapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(AutoActionRule aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            mapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public List<AutoActionRule> findEnabled() {
        return mapper.findEnabled().stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<AutoActionRule> findByRuleCode(String ruleCode) {
        AutoActionRulePO po = mapper.findByRuleCode(ruleCode);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public List<AutoActionRule> findByTriggerType(String triggerType) {
        return mapper.findByTriggerType(triggerType).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private AutoActionRulePO toPO(AutoActionRule domain) {
        AutoActionRulePO po = new AutoActionRulePO();
        po.setId(domain.getId());
        po.setRuleCode(domain.getRuleCode());
        po.setRuleName(domain.getRuleName());
        po.setTriggerType(domain.getTriggerType());
        po.setTriggerCondition(domain.getTriggerCondition());
        po.setSeverity(domain.getSeverity().name());
        po.setCategory(domain.getCategory().name());
        po.setDeadlineHours(domain.getDeadlineHours());
        po.setAutoAssign(domain.isAutoAssign());
        po.setEnabled(domain.isEnabled());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private AutoActionRule toDomain(AutoActionRulePO po) {
        return AutoActionRule.reconstruct()
                .id(po.getId())
                .ruleCode(po.getRuleCode())
                .ruleName(po.getRuleName())
                .triggerType(po.getTriggerType())
                .triggerCondition(po.getTriggerCondition())
                .severity(ActionSeverity.valueOf(po.getSeverity()))
                .category(ActionCategory.valueOf(po.getCategory()))
                .deadlineHours(po.getDeadlineHours() != null ? po.getDeadlineHours() : 72)
                .autoAssign(po.getAutoAssign() != null && po.getAutoAssign())
                .enabled(po.getEnabled() != null && po.getEnabled())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
