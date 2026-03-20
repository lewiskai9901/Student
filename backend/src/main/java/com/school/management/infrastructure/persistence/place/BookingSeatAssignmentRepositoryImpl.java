package com.school.management.infrastructure.persistence.place;

import com.school.management.domain.place.model.entity.BookingSeatAssignment;
import com.school.management.domain.place.repository.BookingSeatAssignmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BookingSeatAssignmentRepositoryImpl implements BookingSeatAssignmentRepository {

    private final BookingSeatAssignmentMapper mapper;

    public BookingSeatAssignmentRepositoryImpl(BookingSeatAssignmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BookingSeatAssignment save(BookingSeatAssignment assignment) {
        BookingSeatAssignmentPO po = toPO(assignment);
        if (po.getId() == null) {
            mapper.insert(po);
            assignment.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return assignment;
    }

    @Override
    public List<BookingSeatAssignment> findByBookingId(Long bookingId) {
        return mapper.findByBookingId(bookingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByBookingId(Long bookingId) {
        mapper.physicalDeleteByBookingId(bookingId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private BookingSeatAssignmentPO toPO(BookingSeatAssignment entity) {
        BookingSeatAssignmentPO po = new BookingSeatAssignmentPO();
        po.setId(entity.getId());
        po.setBookingId(entity.getBookingId());
        po.setPositionNo(entity.getPositionNo());
        po.setUserId(entity.getUserId());
        po.setUserName(entity.getUserName());
        return po;
    }

    private BookingSeatAssignment toDomain(BookingSeatAssignmentPO po) {
        return BookingSeatAssignment.builder()
                .id(po.getId())
                .bookingId(po.getBookingId())
                .positionNo(po.getPositionNo())
                .userId(po.getUserId())
                .userName(po.getUserName())
                .build();
    }
}
