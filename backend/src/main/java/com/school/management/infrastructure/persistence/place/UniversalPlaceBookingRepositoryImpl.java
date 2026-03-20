package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.place.model.entity.UniversalPlaceBooking;
import com.school.management.domain.place.repository.UniversalPlaceBookingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UniversalPlaceBookingRepositoryImpl implements UniversalPlaceBookingRepository {

    private final UniversalPlaceBookingMapper mapper;
    private final ObjectMapper objectMapper;

    public UniversalPlaceBookingRepositoryImpl(UniversalPlaceBookingMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UniversalPlaceBooking save(UniversalPlaceBooking booking) {
        UniversalPlaceBookingPO po = toPO(booking);
        if (po.getId() == null) {
            mapper.insert(po);
            booking.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return booking;
    }

    @Override
    public Optional<UniversalPlaceBooking> findById(Long id) {
        UniversalPlaceBookingPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UniversalPlaceBooking> findByPlaceId(Long placeId) {
        return mapper.findByPlaceId(placeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceBooking> findByPlaceIdAndTimeRange(Long placeId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.findByPlaceIdAndTimeRange(placeId, startTime, endTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceBooking> findActiveByPlaceId(Long placeId) {
        return mapper.findActiveByPlaceId(placeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceBooking> findByBookerId(Long bookerId) {
        return mapper.findByBookerId(bookerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceBooking> findActiveByBookerId(Long bookerId) {
        return mapper.findActiveByBookerId(bookerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasConflict(Long placeId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        return mapper.hasConflict(placeId, startTime, endTime, excludeBookingId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public List<UniversalPlaceBooking> findExpiredBookings(LocalDateTime beforeTime) {
        return mapper.findExpiredBookings(beforeTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void batchUpdateStatus(List<Long> ids, int status) {
        if (ids != null && !ids.isEmpty()) {
            mapper.batchUpdateStatus(ids, status);
        }
    }

    @Override
    public List<UniversalPlaceBooking> findPage(BookingQueryCriteria criteria, int page, int size) {
        Page<UniversalPlaceBookingPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UniversalPlaceBookingPO> wrapper = buildQueryWrapper(criteria);
        wrapper.orderByDesc(UniversalPlaceBookingPO::getStartTime);
        Page<UniversalPlaceBookingPO> result = mapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(BookingQueryCriteria criteria) {
        LambdaQueryWrapper<UniversalPlaceBookingPO> wrapper = buildQueryWrapper(criteria);
        return mapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<UniversalPlaceBookingPO> buildQueryWrapper(BookingQueryCriteria criteria) {
        LambdaQueryWrapper<UniversalPlaceBookingPO> wrapper = new LambdaQueryWrapper<>();
        if (criteria != null) {
            if (criteria.getPlaceId() != null) {
                wrapper.eq(UniversalPlaceBookingPO::getPlaceId, criteria.getPlaceId());
            }
            if (criteria.getBookerId() != null) {
                wrapper.eq(UniversalPlaceBookingPO::getBookerId, criteria.getBookerId());
            }
            if (criteria.getStatus() != null) {
                wrapper.eq(UniversalPlaceBookingPO::getStatus, criteria.getStatus());
            }
            if (criteria.getStartTimeFrom() != null) {
                wrapper.ge(UniversalPlaceBookingPO::getStartTime, criteria.getStartTimeFrom());
            }
            if (criteria.getStartTimeTo() != null) {
                wrapper.le(UniversalPlaceBookingPO::getStartTime, criteria.getStartTimeTo());
            }
        }
        return wrapper;
    }

    // ==================== PO ↔ Domain ====================

    private UniversalPlaceBookingPO toPO(UniversalPlaceBooking entity) {
        UniversalPlaceBookingPO po = new UniversalPlaceBookingPO();
        po.setId(entity.getId());
        po.setPlaceId(entity.getPlaceId());
        po.setBookerId(entity.getBookerId());
        po.setBookerName(entity.getBookerName());
        po.setTitle(entity.getTitle());
        po.setStartTime(entity.getStartTime());
        po.setEndTime(entity.getEndTime());
        po.setStatus(entity.getStatus());
        po.setRemark(entity.getRemark());

        // attendeeIds → JSON string
        if (entity.getAttendeeIds() != null && !entity.getAttendeeIds().isEmpty()) {
            try {
                po.setAttendees(objectMapper.writeValueAsString(entity.getAttendeeIds()));
            } catch (JsonProcessingException e) {
                po.setAttendees("[]");
            }
        }

        return po;
    }

    private UniversalPlaceBooking toDomain(UniversalPlaceBookingPO po) {
        List<Long> attendeeIds = Collections.emptyList();
        if (po.getAttendees() != null && !po.getAttendees().isBlank()) {
            try {
                attendeeIds = objectMapper.readValue(po.getAttendees(), new TypeReference<List<Long>>() {});
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        return UniversalPlaceBooking.builder()
                .id(po.getId())
                .placeId(po.getPlaceId())
                .bookerId(po.getBookerId())
                .bookerName(po.getBookerName())
                .title(po.getTitle())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .attendeeIds(attendeeIds)
                .status(po.getStatus())
                .remark(po.getRemark())
                .build();
    }
}
