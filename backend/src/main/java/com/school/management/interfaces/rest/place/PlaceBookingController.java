package com.school.management.interfaces.rest.place;

import com.school.management.application.place.PlaceBookingApplicationService;
import com.school.management.application.place.PlaceBookingApplicationService.BookingDTO;
import com.school.management.application.place.PlaceBookingApplicationService.CreateBookingCommand;
import com.school.management.application.place.PlaceBookingApplicationService.SeatAssignmentCommand;
import com.school.management.application.place.PlaceBookingApplicationService.SeatAssignmentDTO;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/place-bookings")
@RequiredArgsConstructor
@Tag(name = "场所预订管理")
public class PlaceBookingController {

    private final PlaceBookingApplicationService bookingService;

    @PostMapping
    @Operation(summary = "创建预订")
    public Result<BookingDTO> createBooking(@RequestBody CreateBookingRequest request) {
        if (request.getPlaceId() == null) {
            return Result.error("场所ID不能为空");
        }
        CreateBookingCommand cmd = new CreateBookingCommand();
        cmd.setTitle(request.getTitle());
        cmd.setStartTime(request.getStartTime());
        cmd.setEndTime(request.getEndTime());
        cmd.setAttendeeIds(request.getAttendeeIds());
        cmd.setRemark(request.getRemark());
        return Result.success(bookingService.createBooking(request.getPlaceId(), cmd));
    }

    @GetMapping
    @Operation(summary = "查询预订列表")
    public Result<List<BookingDTO>> getBookings(
            @RequestParam Long placeId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return Result.success(bookingService.getPlaceBookings(placeId, activeOnly));
    }

    @GetMapping("/my")
    @Operation(summary = "我的预订")
    public Result<List<BookingDTO>> getMyBookings() {
        return Result.success(bookingService.getMyBookings());
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消预订")
    public Result<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return Result.success();
    }

    // ==================== 排座管理 ====================

    @PutMapping("/{id}/seating")
    @Operation(summary = "保存排座")
    public Result<List<SeatAssignmentDTO>> saveSeating(
            @PathVariable Long id,
            @RequestBody List<SeatAssignmentRequest> assignments
    ) {
        List<SeatAssignmentCommand> commands = assignments.stream()
                .map(req -> {
                    SeatAssignmentCommand cmd = new SeatAssignmentCommand();
                    cmd.setPositionNo(req.getPositionNo());
                    cmd.setUserId(req.getUserId());
                    cmd.setUserName(req.getUserName());
                    return cmd;
                })
                .toList();
        return Result.success(bookingService.saveSeatingArrangement(id, commands));
    }

    @GetMapping("/{id}/seating")
    @Operation(summary = "查询排座")
    public Result<List<SeatAssignmentDTO>> getSeating(@PathVariable Long id) {
        return Result.success(bookingService.getSeatingArrangement(id));
    }

    @DeleteMapping("/{id}/seating")
    @Operation(summary = "清除排座")
    public Result<Void> clearSeating(@PathVariable Long id) {
        bookingService.clearSeatingArrangement(id);
        return Result.success();
    }

    // ==================== Request ====================

    @Data
    public static class CreateBookingRequest {
        private Long placeId;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<Long> attendeeIds;
        private String remark;
    }

    @Data
    public static class SeatAssignmentRequest {
        private String positionNo;
        private Long userId;
        private String userName;
    }
}
