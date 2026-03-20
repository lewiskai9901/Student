package com.school.management.domain.place.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatAssignment implements Entity<Long> {

    private Long id;
    private Long bookingId;
    private String positionNo;
    private Long userId;
    private String userName;

    public static BookingSeatAssignment create(Long bookingId, String positionNo,
                                                Long userId, String userName) {
        return BookingSeatAssignment.builder()
                .bookingId(bookingId)
                .positionNo(positionNo)
                .userId(userId)
                .userName(userName)
                .build();
    }
}
