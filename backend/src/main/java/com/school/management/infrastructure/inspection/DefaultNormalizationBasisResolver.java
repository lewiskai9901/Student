package com.school.management.infrastructure.inspection;

import com.school.management.application.access.RelationTypeRegistry;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.NormalizeBy;
import com.school.management.domain.inspection.service.NormalizationBasisResolver;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 归一化分母默认实现 — 从三个基础模块 (组织/用户/场所) 通用字段取 population.
 *
 * <p>通用核心: 全程只用 {@link TargetType} / {@link NormalizeBy} 枚举 +
 * access 模块的核心关系/类型常量 (member / org_unit / user), 不出现任何行业字面量
 * (班级/学生/教室). 行业插件如需特殊口径可实现 {@link NormalizationBasisResolver} 覆盖.
 *
 * <p>取数失败 (DB 抖动等) 绝不让评分崩溃: 一律 catch + warn + 回退分母 1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultNormalizationBasisResolver implements NormalizationBasisResolver {

    private final OrgUnitRepository orgUnitRepository;
    private final AccessRelationRepository accessRelationRepository;
    private final UniversalPlaceRepository placeRepository;

    @Override
    public int resolveDenominator(TargetType targetType, Long targetId, NormalizeBy normalizeBy) {
        // 无归一化维度 → 不摊平
        if (normalizeBy == null || normalizeBy == NormalizeBy.NONE) {
            return 1;
        }
        // 单体用户无规模问题
        if (targetType == TargetType.USER) {
            return 1;
        }
        if (targetId == null) {
            return 1;
        }
        try {
            if (targetType == TargetType.PLACE) {
                return resolvePlaceCapacity(targetId);
            }
            if (targetType == TargetType.ORG) {
                return resolveOrgPopulation(targetId, normalizeBy);
            }
            // ASSET / COMPOSITE 等暂无通用规模口径
            return 1;
        } catch (Exception e) {
            log.warn("归一化分母解析失败, 回退 1: targetType={}, targetId={}, normalizeBy={}, err={}",
                    targetType, targetId, normalizeBy, e.toString());
            return 1;
        }
    }

    private int resolvePlaceCapacity(Long placeId) {
        Optional<UniversalPlace> place = placeRepository.findById(placeId);
        if (place.isPresent()) {
            Integer capacity = place.get().getCapacity();
            if (capacity != null && capacity > 0) {
                return capacity;
            }
        }
        return 1;
    }

    private int resolveOrgPopulation(Long orgUnitId, NormalizeBy normalizeBy) {
        int population;
        switch (normalizeBy) {
            case PER_MEMBER:
                // 组织成员数 = access_relations 上 (org_unit, member, user) 活跃关系数
                List<Long> members = accessRelationRepository.findActiveSubjectIds(
                        RelationTypeRegistry.Types.ORG_UNIT, orgUnitId,
                        RelationTypeRegistry.Relations.MEMBER, RelationTypeRegistry.Types.USER);
                population = members == null ? 0 : members.size();
                break;
            case PER_PLACE:
                // 组织关联场所数
                List<UniversalPlace> places = placeRepository.findByOrgUnitId(orgUnitId);
                population = places == null ? 0 : places.size();
                break;
            case PER_SUB_ORG:
                // 直接子组织数
                population = (int) orgUnitRepository.countByParentId(orgUnitId);
                break;
            default:
                population = 0;
        }
        return population > 0 ? population : 1;
    }
}
