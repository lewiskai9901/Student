package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.asset.model.aggregate.AssetBorrow;
import com.school.management.domain.asset.model.valueobject.BorrowStatus;
import com.school.management.domain.asset.model.valueobject.BorrowType;
import com.school.management.domain.asset.model.valueobject.ReturnCondition;
import com.school.management.domain.asset.repository.AssetBorrowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产借用仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetBorrowRepositoryImpl implements AssetBorrowRepository {

    private final AssetBorrowMapper borrowMapper;

    @Override
    public void save(AssetBorrow borrow) {
        AssetBorrowPO po = toPO(borrow);
        if (borrow.getId() == null) {
            borrowMapper.insert(po);
            borrow.setId(po.getId());
        } else {
            borrowMapper.updateById(po);
        }
    }

    @Override
    public Optional<AssetBorrow> findById(Long id) {
        AssetBorrowPO po = borrowMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AssetBorrow> findByBorrowNo(String borrowNo) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetBorrowPO::getBorrowNo, borrowNo);
        AssetBorrowPO po = borrowMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public String generateBorrowNo() {
        String prefix = "JY" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxNo = borrowMapper.selectMaxBorrowNo(prefix);

        int sequence = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            try {
                sequence = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }

        return prefix + String.format("%05d", sequence);
    }

    @Override
    public List<AssetBorrow> findByAssetIdAndStatus(Long assetId, BorrowStatus status) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetBorrowPO::getAssetId, assetId);
        if (status != null) {
            wrapper.eq(AssetBorrowPO::getStatus, status.getCode());
        }
        wrapper.orderByDesc(AssetBorrowPO::getCreatedAt);
        return borrowMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUnreturnedBorrow(Long assetId) {
        return borrowMapper.countUnreturnedByAssetId(assetId) > 0;
    }

    @Override
    public List<AssetBorrow> findByBorrowerId(Long borrowerId) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetBorrowPO::getBorrowerId, borrowerId);
        wrapper.orderByDesc(AssetBorrowPO::getCreatedAt);
        return borrowMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetBorrow> findOverdue() {
        return borrowMapper.selectOverdue(LocalDate.now()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetBorrow> findNearExpiry(int days) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(days);
        return borrowMapper.selectNearExpiry(today, targetDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetBorrow> findAll(BorrowType borrowType, BorrowStatus status, Long borrowerId,
                                     String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = buildQueryWrapper(borrowType, status, borrowerId, keyword);
        wrapper.orderByDesc(AssetBorrowPO::getCreatedAt);
        wrapper.last("LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize);
        return borrowMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(BorrowType borrowType, BorrowStatus status, Long borrowerId, String keyword) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = buildQueryWrapper(borrowType, status, borrowerId, keyword);
        return borrowMapper.selectCount(wrapper);
    }

    @Override
    public List<AssetBorrow> findByStatus(BorrowStatus status) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetBorrowPO::getStatus, status.getCode());
        wrapper.orderByDesc(AssetBorrowPO::getCreatedAt);
        return borrowMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<AssetBorrowPO> buildQueryWrapper(BorrowType borrowType, BorrowStatus status,
                                                                 Long borrowerId, String keyword) {
        LambdaQueryWrapper<AssetBorrowPO> wrapper = new LambdaQueryWrapper<>();
        if (borrowType != null) {
            wrapper.eq(AssetBorrowPO::getBorrowType, borrowType.getCode());
        }
        if (status != null) {
            wrapper.eq(AssetBorrowPO::getStatus, status.getCode());
        }
        if (borrowerId != null) {
            wrapper.eq(AssetBorrowPO::getBorrowerId, borrowerId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(AssetBorrowPO::getBorrowNo, keyword)
                    .or().like(AssetBorrowPO::getAssetName, keyword)
                    .or().like(AssetBorrowPO::getAssetCode, keyword)
                    .or().like(AssetBorrowPO::getBorrowerName, keyword));
        }
        return wrapper;
    }

    private AssetBorrowPO toPO(AssetBorrow domain) {
        AssetBorrowPO po = new AssetBorrowPO();
        po.setId(domain.getId());
        po.setBorrowNo(domain.getBorrowNo());
        po.setBorrowType(domain.getBorrowType() != null ? domain.getBorrowType().getCode() : null);
        po.setAssetId(domain.getAssetId());
        po.setAssetCode(domain.getAssetCode());
        po.setAssetName(domain.getAssetName());
        po.setQuantity(domain.getQuantity());
        po.setBorrowerId(domain.getBorrowerId());
        po.setBorrowerName(domain.getBorrowerName());
        po.setBorrowerDept(domain.getBorrowerDept());
        po.setBorrowerPhone(domain.getBorrowerPhone());
        po.setBorrowDate(domain.getBorrowDate());
        po.setExpectedReturnDate(domain.getExpectedReturnDate());
        po.setActualReturnDate(domain.getActualReturnDate());
        po.setReturnCondition(domain.getReturnCondition() != null ? domain.getReturnCondition().getCode() : null);
        po.setReturnRemark(domain.getReturnRemark());
        po.setReturnerId(domain.getReturnerId());
        po.setReturnerName(domain.getReturnerName());
        po.setPurpose(domain.getPurpose());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setOperatorId(domain.getOperatorId());
        po.setOperatorName(domain.getOperatorName());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private AssetBorrow toDomain(AssetBorrowPO po) {
        AssetBorrow borrow = AssetBorrow.builder()
                .borrowNo(po.getBorrowNo())
                .borrowType(po.getBorrowType() != null ? BorrowType.fromCode(po.getBorrowType()) : null)
                .assetId(po.getAssetId())
                .assetCode(po.getAssetCode())
                .assetName(po.getAssetName())
                .quantity(po.getQuantity())
                .borrowerId(po.getBorrowerId())
                .borrowerName(po.getBorrowerName())
                .borrowerDept(po.getBorrowerDept())
                .borrowerPhone(po.getBorrowerPhone())
                .borrowDate(po.getBorrowDate())
                .expectedReturnDate(po.getExpectedReturnDate())
                .actualReturnDate(po.getActualReturnDate())
                .returnCondition(po.getReturnCondition() != null ? ReturnCondition.fromCode(po.getReturnCondition()) : null)
                .returnRemark(po.getReturnRemark())
                .returnerId(po.getReturnerId())
                .returnerName(po.getReturnerName())
                .purpose(po.getPurpose())
                .status(po.getStatus() != null ? BorrowStatus.fromCode(po.getStatus()) : null)
                .operatorId(po.getOperatorId())
                .operatorName(po.getOperatorName())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
        // id is inherited from AggregateRoot, not included in Lombok @Builder
        borrow.setId(po.getId());
        return borrow;
    }
}
