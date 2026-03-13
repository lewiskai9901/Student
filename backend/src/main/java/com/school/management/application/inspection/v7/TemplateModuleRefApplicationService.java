package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
import com.school.management.domain.inspection.repository.v7.InspTemplateRepository;
import com.school.management.domain.inspection.repository.v7.TemplateModuleRefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class TemplateModuleRefApplicationService {

    private final TemplateModuleRefRepository moduleRefRepository;
    private final InspTemplateRepository templateRepository;

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
        return moduleRefRepository.save(ref);
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
        moduleRefRepository.findById(refId)
                .orElseThrow(() -> new IllegalArgumentException("模块引用不存在: " + refId));
        moduleRefRepository.deleteById(refId);
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
