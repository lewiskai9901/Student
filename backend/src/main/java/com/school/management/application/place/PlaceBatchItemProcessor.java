package com.school.management.application.place;

import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批量Job单项处理器（独立Bean）
 * 解决同类内 @Transactional 不生效的问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBatchItemProcessor {

    private final UniversalPlaceRepository placeRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public void processAssignOrg(Long placeId, Long targetOrgUnitId, String reason) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在: " + placeId));

        place.assignOrganization(targetOrgUnitId, reason);
        placeRepository.save(place);

        place.getDomainEvents().forEach(eventPublisher::publish);
        place.clearDomainEvents();

        log.debug("已处理场所组织分配: placeId={}, targetOrgUnitId={}", placeId, targetOrgUnitId);
    }

    @Transactional
    public void processChangeStatus(Long placeId, PlaceStatus status, String reason) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在: " + placeId));

        place.changeStatus(status, reason);
        placeRepository.save(place);

        place.getDomainEvents().forEach(eventPublisher::publish);
        place.clearDomainEvents();

        log.debug("已处理场所状态变更: placeId={}, targetStatus={}", placeId, status);
    }

    @Transactional
    public void processAssignResponsible(Long placeId, Long userId, String reason) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在: " + placeId));

        place.assignResponsible(userId, reason);
        placeRepository.save(place);

        place.getDomainEvents().forEach(eventPublisher::publish);
        place.clearDomainEvents();

        log.debug("已处理场所负责人分配: placeId={}, targetResponsibleUserId={}", placeId, userId);
    }
}
