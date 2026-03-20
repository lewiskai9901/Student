package com.school.management.application.place;

import com.school.management.domain.place.model.entity.BookingSeatAssignment;
import com.school.management.domain.place.model.entity.UniversalPlaceBooking;
import com.school.management.domain.place.repository.BookingSeatAssignmentRepository;
import com.school.management.domain.place.repository.UniversalPlaceBookingRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceBookingApplicationService {

    private final UniversalPlaceBookingRepository bookingRepository;
    private final UniversalPlaceRepository placeRepository;
    private final BookingSeatAssignmentRepository seatAssignmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingDTO createBooking(Long placeId, CreateBookingCommand cmd) {
        // Verify place exists
        placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在: " + placeId));

        // Get current user
        UserContext ctx = UserContextHolder.getContext();
        Long bookerId = ctx != null ? ctx.getUserId() : null;
        String bookerName = ctx != null ? ctx.getUsername() : null;
        if (bookerId == null) {
            throw new IllegalStateException("无法获取当前用户信息");
        }

        LocalDateTime startTime = cmd.getStartTime();
        LocalDateTime endTime = cmd.getEndTime();

        // Check conflict
        if (bookingRepository.hasConflict(placeId, startTime, endTime)) {
            throw new IllegalStateException("该时间段已有预订，存在冲突");
        }

        UniversalPlaceBooking booking = UniversalPlaceBooking.create(
                placeId, bookerId, bookerName,
                cmd.getTitle(), startTime, endTime
        );
        booking.setAttendeeIds(cmd.getAttendeeIds());
        booking.setRemark(cmd.getRemark());

        booking = bookingRepository.save(booking);
        return toDTO(booking);
    }

    @Transactional
    public void cancelBooking(Long id) {
        UniversalPlaceBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("预订不存在: " + id));

        UserContext ctx = UserContextHolder.getContext();
        if (ctx != null && !booking.getBookerId().equals(ctx.getUserId())) {
            // Could add admin role check here
        }

        booking.cancel();
        bookingRepository.save(booking);
    }

    public List<BookingDTO> getPlaceBookings(Long placeId, Boolean activeOnly) {
        List<UniversalPlaceBooking> bookings;
        if (Boolean.TRUE.equals(activeOnly)) {
            bookings = bookingRepository.findActiveByPlaceId(placeId);
        } else {
            bookings = bookingRepository.findByPlaceId(placeId);
        }
        return bookings.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BookingDTO> getMyBookings() {
        UserContext ctx = UserContextHolder.getContext();
        if (ctx == null || ctx.getUserId() == null) {
            throw new IllegalStateException("无法获取当前用户信息");
        }
        return bookingRepository.findByBookerId(ctx.getUserId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==================== 排座管理 ====================

    @Transactional
    public List<SeatAssignmentDTO> saveSeatingArrangement(Long bookingId, List<SeatAssignmentCommand> assignments) {
        UniversalPlaceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("预订不存在: " + bookingId));

        // Only allow seating for PENDING(1) or IN_USE(2) bookings
        if (booking.getStatus() != 1 && booking.getStatus() != 2) {
            throw new IllegalStateException("只能对待使用或使用中的预订进行排座");
        }

        // Clear old assignments
        seatAssignmentRepository.deleteByBookingId(bookingId);

        // Save new assignments
        List<SeatAssignmentDTO> result = new ArrayList<>();
        for (SeatAssignmentCommand cmd : assignments) {
            BookingSeatAssignment entity = BookingSeatAssignment.create(
                    bookingId, cmd.getPositionNo(), cmd.getUserId(), cmd.getUserName()
            );
            entity = seatAssignmentRepository.save(entity);
            result.add(toSeatDTO(entity));
        }
        return result;
    }

    public List<SeatAssignmentDTO> getSeatingArrangement(Long bookingId) {
        return seatAssignmentRepository.findByBookingId(bookingId).stream()
                .map(this::toSeatDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void clearSeatingArrangement(Long bookingId) {
        seatAssignmentRepository.deleteByBookingId(bookingId);
    }

    // ==================== Mapping ====================

    private BookingDTO toDTO(UniversalPlaceBooking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setPlaceId(booking.getPlaceId());
        dto.setBookerId(booking.getBookerId());
        dto.setBookerName(booking.getBookerName());
        dto.setTitle(booking.getTitle());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setAttendeeIds(booking.getAttendeeIds());
        dto.setStatus(booking.getStatus());
        dto.setRemark(booking.getRemark());

        // Populate attendee details
        List<Long> ids = booking.getAttendeeIds();
        if (ids != null && !ids.isEmpty()) {
            List<User> users = userRepository.findByIds(ids);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
            List<AttendeeInfo> attendees = new ArrayList<>();
            for (Long uid : ids) {
                User u = userMap.get(uid);
                if (u != null) {
                    AttendeeInfo info = new AttendeeInfo();
                    info.setUserId(u.getId());
                    info.setUsername(u.getUsername());
                    info.setRealName(u.getRealName());
                    attendees.add(info);
                }
            }
            dto.setAttendees(attendees);
        } else {
            dto.setAttendees(Collections.emptyList());
        }

        return dto;
    }

    private SeatAssignmentDTO toSeatDTO(BookingSeatAssignment entity) {
        SeatAssignmentDTO dto = new SeatAssignmentDTO();
        dto.setId(entity.getId());
        dto.setBookingId(entity.getBookingId());
        dto.setPositionNo(entity.getPositionNo());
        dto.setUserId(entity.getUserId());
        dto.setUserName(entity.getUserName());
        return dto;
    }

    // ==================== DTO ====================

    @Data
    public static class BookingDTO {
        private Long id;
        private Long placeId;
        private Long bookerId;
        private String bookerName;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<Long> attendeeIds;
        private Integer status;
        private String remark;
        private List<AttendeeInfo> attendees;
    }

    @Data
    public static class AttendeeInfo {
        private Long userId;
        private String username;
        private String realName;
    }

    @Data
    public static class SeatAssignmentDTO {
        private Long id;
        private Long bookingId;
        private String positionNo;
        private Long userId;
        private String userName;
    }

    @Data
    public static class SeatAssignmentCommand {
        private String positionNo;
        private Long userId;
        private String userName;
    }

    // ==================== Command ====================

    @Data
    public static class CreateBookingCommand {
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<Long> attendeeIds;
        private String remark;
    }
}
