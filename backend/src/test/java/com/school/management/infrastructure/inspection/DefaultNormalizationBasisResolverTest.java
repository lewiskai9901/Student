package com.school.management.infrastructure.inspection;

import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.NormalizeBy;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 归一化分母解析器默认实现单测 — 纯 Mockito, 不启 Spring.
 */
@ExtendWith(MockitoExtension.class)
class DefaultNormalizationBasisResolverTest {

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @Mock
    private AccessRelationRepository accessRelationRepository;

    @Mock
    private UniversalPlaceRepository placeRepository;

    @InjectMocks
    private DefaultNormalizationBasisResolver resolver;

    @Test
    void org_perMember_returnsMemberCount() {
        when(accessRelationRepository.findActiveSubjectIds("org_unit", 10L, "member", "user"))
                .thenReturn(List.of(1L, 2L, 3L, 4L));

        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(4);
    }

    @Test
    void org_perSubOrg_returnsChildCount() {
        when(orgUnitRepository.countByParentId(10L)).thenReturn(6L);

        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.PER_SUB_ORG);

        assertThat(denom).isEqualTo(6);
    }

    @Test
    void org_perPlace_returnsAssociatedPlaceCount() {
        UniversalPlace p1 = new UniversalPlace();
        UniversalPlace p2 = new UniversalPlace();
        when(placeRepository.findByOrgUnitId(10L)).thenReturn(List.of(p1, p2));

        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.PER_PLACE);

        assertThat(denom).isEqualTo(2);
    }

    @Test
    void user_isAlwaysOne() {
        int denom = resolver.resolveDenominator(TargetType.USER, 99L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void none_isAlwaysOne() {
        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.NONE);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void nullNormalizeBy_isOne() {
        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, null);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void place_returnsCapacity() {
        UniversalPlace place = new UniversalPlace();
        place.setCapacity(45);
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));

        int denom = resolver.resolveDenominator(TargetType.PLACE, 7L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(45);
    }

    @Test
    void place_missingCapacity_fallsBackToOne() {
        UniversalPlace place = new UniversalPlace();
        place.setCapacity(null);
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));

        int denom = resolver.resolveDenominator(TargetType.PLACE, 7L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void place_notFound_fallsBackToOne() {
        when(placeRepository.findById(7L)).thenReturn(Optional.empty());

        int denom = resolver.resolveDenominator(TargetType.PLACE, 7L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void dataFetchThrows_fallsBackToOne() {
        when(accessRelationRepository.findActiveSubjectIds(anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db down"));

        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void nullTargetId_fallsBackToOne() {
        int denom = resolver.resolveDenominator(TargetType.ORG, null, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void org_memberCountZero_fallsBackToOne() {
        when(accessRelationRepository.findActiveSubjectIds("org_unit", 10L, "member", "user"))
                .thenReturn(List.of());

        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.PER_MEMBER);

        assertThat(denom).isEqualTo(1);
    }

    @Test
    void org_noneNormalizeBy_isOne_evenWithData() {
        lenient().when(orgUnitRepository.countByParentId(anyLong())).thenReturn(5L);

        int denom = resolver.resolveDenominator(TargetType.ORG, 10L, NormalizeBy.NONE);

        assertThat(denom).isEqualTo(1);
    }
}
