package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.scoring.ScoreDimension;
import com.school.management.domain.inspection.model.v7.scoring.ScoringProfile;
import com.school.management.domain.inspection.model.v7.template.InspTemplate;
import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
import com.school.management.domain.inspection.repository.v7.InspTemplateRepository;
import com.school.management.domain.inspection.repository.v7.ScoreDimensionRepository;
import com.school.management.domain.inspection.repository.v7.ScoringProfileRepository;
import com.school.management.domain.inspection.repository.v7.TemplateModuleRefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @deprecated V62: 模块引用功能已被统一分区模型的引用分区(refSectionId)替代。
 */
@Deprecated
@Slf4j
@RequiredArgsConstructor
@Service
public class TemplateModuleRefApplicationService {

    private final TemplateModuleRefRepository moduleRefRepository;
    private final InspTemplateRepository templateRepository;
    private final ScoringProfileRepository scoringProfileRepository;
    private final ScoreDimensionRepository dimensionRepository;

    @Transactional
    public TemplateModuleRef addModuleRef(Long compositeTemplateId, Long moduleTemplateId,
                                           Integer sortOrder, Integer weight) {
        templateRepository.findById(compositeTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("父模板不存在: " + compositeTemplateId));
        templateRepository.findById(moduleTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("子模板不存在: " + moduleTemplateId));

        if (compositeTemplateId.equals(moduleTemplateId)) {
            throw new IllegalArgumentException("模板不能引用自身");
        }

        detectCircularReference(compositeTemplateId, moduleTemplateId);

        TemplateModuleRef ref = TemplateModuleRef.create(compositeTemplateId, moduleTemplateId, sortOrder, weight);
        TemplateModuleRef saved = moduleRefRepository.save(ref);

        // Auto-set parent template targetType to COMPOSITE
        InspTemplate parentTemplate = templateRepository.findById(compositeTemplateId).orElse(null);
        if (parentTemplate != null && parentTemplate.getTargetType() != TargetType.COMPOSITE) {
            parentTemplate.setTargetType(TargetType.COMPOSITE);
            templateRepository.save(parentTemplate);
        }

        // Auto-create MODULE dimension in scoring profile
        autoCreateModuleDimension(compositeTemplateId, moduleTemplateId, weight, sortOrder);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<TemplateModuleRef> listModuleRefs(Long compositeTemplateId) {
        return moduleRefRepository.findByCompositeTemplateId(compositeTemplateId);
    }

    @Transactional
    public TemplateModuleRef updateModuleRef(Long refId, Integer weight, String overrideConfig) {
        TemplateModuleRef ref = moduleRefRepository.findById(refId)
                .orElseThrow(() -> new IllegalArgumentException("模块引用不存在: " + refId));
        if (weight != null) {
            ref.updateWeight(weight);
        }
        return moduleRefRepository.save(ref);
    }

    @Transactional
    public void removeModuleRef(Long refId) {
        TemplateModuleRef ref = moduleRefRepository.findById(refId)
                .orElseThrow(() -> new IllegalArgumentException("模块引用不存在: " + refId));

        // Auto-remove corresponding MODULE dimension
        autoRemoveModuleDimension(ref.getCompositeTemplateId(), ref.getModuleTemplateId());

        moduleRefRepository.deleteById(refId);

        // If no more module refs, revert targetType from COMPOSITE to ORG
        List<TemplateModuleRef> remaining = moduleRefRepository.findByCompositeTemplateId(ref.getCompositeTemplateId());
        remaining.removeIf(r -> r.getId().equals(refId)); // exclude the one being deleted
        if (remaining.isEmpty()) {
            InspTemplate parentTemplate = templateRepository.findById(ref.getCompositeTemplateId()).orElse(null);
            if (parentTemplate != null && parentTemplate.getTargetType() == TargetType.COMPOSITE) {
                parentTemplate.setTargetType(TargetType.ORG);
                templateRepository.save(parentTemplate);
            }
        }
    }

    @Transactional
    public void reorderModuleRefs(Long compositeTemplateId, List<Long> refIdsInOrder) {
        List<TemplateModuleRef> refs = moduleRefRepository.findByCompositeTemplateId(compositeTemplateId);
        Map<Long, TemplateModuleRef> refMap = new HashMap<>();
        for (TemplateModuleRef ref : refs) {
            refMap.put(ref.getId(), ref);
        }
        for (int i = 0; i < refIdsInOrder.size(); i++) {
            TemplateModuleRef ref = refMap.get(refIdsInOrder.get(i));
            if (ref != null) {
                ref.reorder(i);
                moduleRefRepository.save(ref);
            }
        }
    }

    private void autoCreateModuleDimension(Long compositeTemplateId, Long moduleTemplateId, Integer weight, Integer sortOrder) {
        ScoringProfile profile = scoringProfileRepository.findBySectionId(compositeTemplateId).orElse(null);
        if (profile == null) return;

        // Check if dimension already exists
        List<ScoreDimension> dims = dimensionRepository.findByScoringProfileId(profile.getId());
        for (ScoreDimension dim : dims) {
            if ("MODULE".equals(dim.getSourceType()) && moduleTemplateId.equals(dim.getModuleTemplateId())) {
                return; // already exists
            }
        }

        String moduleName = templateRepository.findById(moduleTemplateId)
                .map(InspTemplate::getTemplateName)
                .orElse("子模板 #" + moduleTemplateId);

        ScoreDimension newDim = ScoreDimension.reconstruct(ScoreDimension.builder()
                .scoringProfileId(profile.getId())
                .dimensionCode("MOD_" + moduleTemplateId)
                .dimensionName(moduleName)
                .weight(weight != null ? weight : 0)
                .sourceType("MODULE")
                .moduleTemplateId(moduleTemplateId)
                .sortOrder(sortOrder != null ? sortOrder : 0));
        dimensionRepository.save(newDim);
    }

    private void autoRemoveModuleDimension(Long compositeTemplateId, Long moduleTemplateId) {
        ScoringProfile profile = scoringProfileRepository.findBySectionId(compositeTemplateId).orElse(null);
        if (profile == null) return;

        List<ScoreDimension> dims = dimensionRepository.findByScoringProfileId(profile.getId());
        for (ScoreDimension dim : dims) {
            if ("MODULE".equals(dim.getSourceType()) && moduleTemplateId.equals(dim.getModuleTemplateId())) {
                dimensionRepository.deleteById(dim.getId());
                return;
            }
        }
    }

    private void detectCircularReference(Long compositeTemplateId, Long moduleTemplateId) {
        Set<Long> visited = new HashSet<>();
        visited.add(compositeTemplateId);
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(moduleTemplateId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (visited.contains(current)) {
                throw new IllegalArgumentException("检测到循环引用: 模板 " + moduleTemplateId + " 直接或间接引用了模板 " + compositeTemplateId);
            }
            visited.add(current);
            List<TemplateModuleRef> children = moduleRefRepository.findByCompositeTemplateId(current);
            for (TemplateModuleRef child : children) {
                queue.add(child.getModuleTemplateId());
            }
        }
    }
}
