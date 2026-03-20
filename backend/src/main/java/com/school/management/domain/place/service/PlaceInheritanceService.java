package com.school.management.domain.place.service;

import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 场所继承逻辑服务
 * 负责计算NULL继承模型下的有效属性值
 * 对标: Kubernetes ConfigMap Inheritance Pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceInheritanceService {

    private final UniversalPlaceRepository placeRepository;

    /**
     * 递归计算有效组织单元ID
     * 规则：如果当前场所的orgUnitId为NULL，则从父级继承；否则使用当前值
     *
     * @param place 场所实体
     * @return 有效的组织单元ID（可能为NULL，如果整条继承链都没有显式设置）
     */
    public Long getEffectiveOrgUnitId(UniversalPlace place) {
        if (place == null) {
            return null;
        }

        // 如果当前场所显式设置了orgUnitId，直接返回
        if (place.getOrgUnitId() != null) {
            return place.getOrgUnitId();
        }

        // 如果当前场所未设置（NULL），且有父级，则递归查找父级的有效值
        if (place.getParentId() != null) {
            UniversalPlace parent = placeRepository.findById(place.getParentId()).orElse(null);
            if (parent != null) {
                return getEffectiveOrgUnitId(parent); // 递归调用
            }
        }

        // 没有父级或父级不存在，返回NULL
        return null;
    }

    /**
     * 判断场所的组织归属是否为继承状态
     *
     * @param place 场所实体
     * @return true表示继承父级，false表示显式设置
     */
    public boolean isOrgInherited(UniversalPlace place) {
        return place != null && place.getOrgUnitId() == null && place.getParentId() != null;
    }

    // ==================== 负责人继承 ====================

    /**
     * 递归计算有效负责人ID
     * 规则：如果当前场所的responsibleUserId为NULL，则从父级继承；否则使用当前值
     */
    public Long getEffectiveResponsibleUserId(UniversalPlace place) {
        if (place == null) {
            return null;
        }
        if (place.getResponsibleUserId() != null) {
            return place.getResponsibleUserId();
        }
        if (place.getParentId() != null) {
            UniversalPlace parent = placeRepository.findById(place.getParentId()).orElse(null);
            if (parent != null) {
                return getEffectiveResponsibleUserId(parent);
            }
        }
        return null;
    }

    /**
     * 判断场所的负责人是否为继承状态
     */
    public boolean isResponsibleInherited(UniversalPlace place) {
        return place != null && place.getResponsibleUserId() == null && place.getParentId() != null;
    }

    /**
     * 获取受当前场所组织变更影响的所有子孙场所（仅限继承状态的子孙）
     * 用于级联通知：当父场所的组织归属变更时，所有继承状态的子孙场所的有效组织归属也会变更
     *
     * @param placeId 当前场所ID
     * @return 受影响的子孙场所列表（仅包含org_unit_id为NULL的场所）
     */
    public List<UniversalPlace> getAffectedDescendants(Long placeId) {
        List<UniversalPlace> affected = new ArrayList<>();
        collectAffectedDescendants(placeId, affected);
        return affected;
    }

    /**
     * 递归收集受影响的子孙场所
     */
    private void collectAffectedDescendants(Long parentId, List<UniversalPlace> result) {
        // 查找所有子场所
        List<UniversalPlace> children = placeRepository.findByParentId(parentId);

        for (UniversalPlace child : children) {
            // 如果子场所的orgUnitId为NULL（继承状态），则添加到结果集
            if (child.getOrgUnitId() == null) {
                result.add(child);
                // 继续递归查找该子场所的子孙场所
                collectAffectedDescendants(child.getId(), result);
            }
            // 如果子场所显式设置了orgUnitId，则不继续递归（因为该分支已脱离继承链）
        }
    }

    /**
     * 计算从根节点到当前场所的继承链路径
     * 用于调试和审计：显示完整的继承路径
     *
     * @param place 场所实体
     * @return 继承路径列表（从根到当前节点）
     */
    public List<UniversalPlace> getInheritanceChain(UniversalPlace place) {
        List<UniversalPlace> chain = new ArrayList<>();
        buildInheritanceChain(place, chain);
        return chain;
    }

    /**
     * 递归构建继承链（倒序添加，最后反转）
     */
    private void buildInheritanceChain(UniversalPlace place, List<UniversalPlace> chain) {
        if (place == null) {
            return;
        }

        // 先添加当前节点
        chain.add(0, place);

        // 如果有父级，继续递归
        if (place.getParentId() != null) {
            UniversalPlace parent = placeRepository.findById(place.getParentId()).orElse(null);
            if (parent != null) {
                buildInheritanceChain(parent, chain);
            }
        }
    }

    /**
     * 验证继承链的一致性
     * 确保继承链中不存在循环引用或断裂
     *
     * @param place 场所实体
     * @return true表示继承链有效
     */
    public boolean validateInheritanceChain(UniversalPlace place) {
        if (place == null) {
            return false;
        }

        int maxDepth = 50; // 防止无限循环
        int depth = 0;
        Long currentId = place.getId();
        Long parentId = place.getParentId();

        while (parentId != null && depth < maxDepth) {
            // 检查是否存在循环引用
            if (currentId.equals(parentId)) {
                log.error("检测到继承链循环引用: placeId={}", place.getId());
                return false;
            }

            UniversalPlace parent = placeRepository.findById(parentId).orElse(null);
            if (parent == null) {
                log.error("继承链断裂: placeId={}, 父级ID={} 不存在", place.getId(), parentId);
                return false;
            }

            currentId = parent.getId();
            parentId = parent.getParentId();
            depth++;
        }

        if (depth >= maxDepth) {
            log.error("继承链过深: placeId={}, depth={}", place.getId(), depth);
            return false;
        }

        return true;
    }
}
