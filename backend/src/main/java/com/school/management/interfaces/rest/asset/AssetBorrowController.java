package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetBorrowApplicationService;
import com.school.management.application.asset.command.CreateBorrowCommand;
import com.school.management.application.asset.command.ReturnBorrowCommand;
import com.school.management.application.asset.query.AssetBorrowDTO;
import com.school.management.common.ApiResponse;
import com.school.management.common.PageResult;
import com.school.management.interfaces.rest.asset.dto.CreateBorrowRequest;
import com.school.management.interfaces.rest.asset.dto.ReturnBorrowRequest;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产借用控制器
 */
@Tag(name = "资产借用管理", description = "资产借用、领用、归还等操作")
@RestController
@RequestMapping("/v2/asset-borrows")
@RequiredArgsConstructor
public class AssetBorrowController {

    private final AssetBorrowApplicationService borrowService;
    private final JwtTokenService jwtTokenService;

    @Operation(summary = "创建借用/领用记录")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:borrow:create')")
    public ApiResponse<Long> createBorrow(@Valid @RequestBody CreateBorrowRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        CreateBorrowCommand command = CreateBorrowCommand.builder()
                .borrowType(request.getBorrowType())
                .assetId(request.getAssetId())
                .quantity(request.getQuantity())
                .borrowerId(request.getBorrowerId())
                .borrowerName(request.getBorrowerName())
                .borrowerDept(request.getBorrowerDept())
                .borrowerPhone(request.getBorrowerPhone())
                .expectedReturnDate(request.getExpectedReturnDate())
                .purpose(request.getPurpose())
                .operatorId(userId)
                .operatorName(userName)
                .build();

        Long id = borrowService.createBorrow(command);
        return ApiResponse.success(id);
    }

    @Operation(summary = "归还资产")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('asset:borrow:return')")
    public ApiResponse<Void> returnBorrow(
            @PathVariable Long id,
            @Valid @RequestBody ReturnBorrowRequest request
    ) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        ReturnBorrowCommand command = ReturnBorrowCommand.builder()
                .borrowId(id)
                .returnCondition(request.getReturnCondition())
                .returnRemark(request.getReturnRemark())
                .returnerId(userId)
                .returnerName(userName)
                .build();

        borrowService.returnBorrow(command);
        return ApiResponse.success();
    }

    @Operation(summary = "取消借用")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('asset:borrow:cancel')")
    public ApiResponse<Void> cancelBorrow(@PathVariable Long id) {
        borrowService.cancelBorrow(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取借用详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:borrow:list')")
    public ApiResponse<AssetBorrowDTO> getBorrow(@PathVariable Long id) {
        return ApiResponse.success(borrowService.getBorrow(id));
    }

    @Operation(summary = "分页查询借用记录")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:borrow:list')")
    public ApiResponse<PageResult<AssetBorrowDTO>> listBorrows(
            @RequestParam(required = false) Integer borrowType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return ApiResponse.success(
                borrowService.listBorrows(borrowType, status, borrowerId, keyword, pageNum, pageSize)
        );
    }

    @Operation(summary = "获取我的借用记录")
    @GetMapping("/my")
    public ApiResponse<List<AssetBorrowDTO>> getMyBorrows() {
        Long userId = jwtTokenService.getCurrentUserId();
        return ApiResponse.success(borrowService.getMyBorrows(userId));
    }

    @Operation(summary = "获取资产的借用历史")
    @GetMapping("/asset/{assetId}")
    @PreAuthorize("hasAuthority('asset:borrow:list')")
    public ApiResponse<List<AssetBorrowDTO>> getAssetBorrowHistory(@PathVariable Long assetId) {
        return ApiResponse.success(borrowService.getAssetBorrowHistory(assetId));
    }

    @Operation(summary = "获取已逾期的借用记录")
    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('asset:borrow:list')")
    public ApiResponse<List<AssetBorrowDTO>> getOverdueBorrows() {
        return ApiResponse.success(borrowService.getOverdueBorrows());
    }

    @Operation(summary = "获取借用统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('asset:borrow:list')")
    public ApiResponse<AssetBorrowApplicationService.BorrowStatistics> getStatistics() {
        return ApiResponse.success(borrowService.getStatistics());
    }
}
