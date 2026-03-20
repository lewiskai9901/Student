package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.entity.BookingSeatAssignment;

import java.util.List;

public interface BookingSeatAssignmentRepository {

    BookingSeatAssignment save(BookingSeatAssignment assignment);

    List<BookingSeatAssignment> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);

    void deleteById(Long id);
}
