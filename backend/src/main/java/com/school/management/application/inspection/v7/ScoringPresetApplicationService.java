package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.ScoringPreset;
import com.school.management.domain.inspection.repository.v7.ScoringPresetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScoringPresetApplicationService {

    private final ScoringPresetRepository scoringPresetRepository;

    @Transactional
    public ScoringPreset createPreset(Long templateId, String presetName, String presetType,
                                       String itemValues, Long createdBy) {
        ScoringPreset preset = ScoringPreset.create(templateId, presetName, presetType,
                itemValues, createdBy);
        ScoringPreset saved = scoringPresetRepository.save(preset);
        log.info("Created scoring preset '{}' for template {}", presetName, templateId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScoringPreset> listByTemplateId(Long templateId) {
        return scoringPresetRepository.findByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public ScoringPreset getPreset(Long id) {
        return scoringPresetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分预设不存在: " + id));
    }

    /**
     * 应用预设 - 递增使用次数并返回预设数据供前端填充
     */
    @Transactional
    public ScoringPreset applyPreset(Long id) {
        ScoringPreset preset = scoringPresetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分预设不存在: " + id));
        preset.incrementUsageCount();
        scoringPresetRepository.save(preset);
        log.info("Applied scoring preset '{}', usage count: {}", preset.getPresetName(), preset.getUsageCount());
        return preset;
    }

    @Transactional
    public ScoringPreset updatePreset(Long id, String presetName, String itemValues) {
        ScoringPreset preset = scoringPresetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分预设不存在: " + id));
        if (presetName != null) {
            preset.updateName(presetName);
        }
        if (itemValues != null) {
            preset.updateItemValues(itemValues);
        }
        return scoringPresetRepository.save(preset);
    }

    @Transactional
    public void deletePreset(Long id) {
        scoringPresetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分预设不存在: " + id));
        scoringPresetRepository.deleteById(id);
        log.info("Deleted scoring preset {}", id);
    }
}
