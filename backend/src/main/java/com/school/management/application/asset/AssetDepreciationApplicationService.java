package com.school.management.application.asset;

import com.school.management.application.asset.query.AssetDepreciationDTO;
import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.entity.AssetDepreciation;
import com.school.management.domain.asset.model.valueobject.CategoryType;
import com.school.management.domain.asset.model.valueobject.DepreciationMethod;
import com.school.management.domain.asset.repository.AssetDepreciationRepository;
import com.school.management.domain.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产折旧应用服务
 * 提供折旧计算、记录查询等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetDepreciationApplicationService {

    private final AssetRepository assetRepository;
    private final AssetDepreciationRepository depreciationRepository;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 计算单个资产的月折旧
     */
    @Transactional
    public AssetDepreciationDTO calculateDepreciation(Long assetId, String period) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("资产不存在"));

        // 检查是否已计提
        if (depreciationRepository.existsByAssetIdAndPeriod(assetId, period)) {
            throw new RuntimeException("该期间已计提折旧");
        }

        // 检查是否需要折旧
        if (asset.getDepreciationMethod() == null ||
                asset.getDepreciationMethod() == DepreciationMethod.NONE.getCode()) {
            throw new RuntimeException("该资产不计提折旧");
        }

        // 计算折旧
        AssetDepreciation record = doCalculateDepreciation(asset, period);
        depreciationRepository.save(record);

        // 更新资产净值
        asset.setNetValue(record.getEndingNetValue());
        asset.setAccumulatedDepreciation(record.getEndingAccumulatedDepreciation());
        assetRepository.save(asset);

        return toDTO(record);
    }

    /**
     * 批量计算所有需要折旧的资产
     */
    @Transactional
    public int calculateAllDepreciation(String period) {
        // 查询所有需要折旧的固定资产
        List<Asset> assets = assetRepository.findAll().stream()
                .filter(a -> a.getCategoryType() == CategoryType.FIXED_ASSET.getCode())
                .filter(a -> a.getDepreciationMethod() != null &&
                        a.getDepreciationMethod() != DepreciationMethod.NONE.getCode())
                .filter(a -> !depreciationRepository.existsByAssetIdAndPeriod(a.getId(), period))
                .collect(Collectors.toList());

        int count = 0;
        List<AssetDepreciation> records = new ArrayList<>();

        for (Asset asset : assets) {
            try {
                AssetDepreciation record = doCalculateDepreciation(asset, period);
                records.add(record);

                // 更新资产净值
                asset.setNetValue(record.getEndingNetValue());
                asset.setAccumulatedDepreciation(record.getEndingAccumulatedDepreciation());

                count++;
            } catch (Exception e) {
                log.error("计算资产 {} 折旧失败: {}", asset.getAssetCode(), e.getMessage());
            }
        }

        // 批量保存
        if (!records.isEmpty()) {
            depreciationRepository.saveAll(records);
            for (Asset asset : assets) {
                if (asset.getNetValue() != null) {
                    assetRepository.save(asset);
                }
            }
        }

        log.info("批量计提折旧完成，期间: {}, 处理资产数: {}", period, count);
        return count;
    }

    /**
     * 获取资产折旧历史
     */
    public List<AssetDepreciationDTO> getDepreciationHistory(Long assetId) {
        return depreciationRepository.findByAssetId(assetId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取资产折旧历史（分页）
     */
    public Map<String, Object> getDepreciationHistoryPage(Long assetId, int pageNum, int pageSize) {
        List<AssetDepreciationDTO> records = depreciationRepository
                .findByAssetIdWithPagination(assetId, pageNum, pageSize).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        int total = depreciationRepository.countByAssetId(assetId);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 获取某期间的折旧汇总
     */
    public Map<String, Object> getPeriodSummary(String period) {
        List<AssetDepreciation> records = depreciationRepository.findByPeriod(period);

        BigDecimal totalDepreciation = records.stream()
                .map(AssetDepreciation::getDepreciationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("period", period);
        summary.put("assetCount", records.size());
        summary.put("totalDepreciation", totalDepreciation);
        summary.put("records", records.stream().map(this::toDTO).collect(Collectors.toList()));
        return summary;
    }

    /**
     * 预览折旧计算结果（不保存）
     */
    public AssetDepreciationDTO previewDepreciation(Long assetId, String period) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("资产不存在"));

        if (asset.getDepreciationMethod() == null ||
                asset.getDepreciationMethod() == DepreciationMethod.NONE.getCode()) {
            throw new RuntimeException("该资产不计提折旧");
        }

        AssetDepreciation record = doCalculateDepreciation(asset, period);
        return toDTO(record);
    }

    /**
     * 获取折旧方法列表
     */
    public List<Map<String, Object>> getDepreciationMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();
        for (DepreciationMethod method : DepreciationMethod.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("code", method.getCode());
            m.put("name", method.getDescription());
            methods.add(m);
        }
        return methods;
    }

    // ==================== 私有方法 ====================

    /**
     * 执行折旧计算
     */
    private AssetDepreciation doCalculateDepreciation(Asset asset, String period) {
        BigDecimal originalValue = asset.getOriginalValue() != null ?
                asset.getOriginalValue() : BigDecimal.ZERO;
        BigDecimal residualValue = asset.getResidualValue() != null ?
                asset.getResidualValue() : BigDecimal.ZERO;
        BigDecimal accumulatedDepreciation = asset.getAccumulatedDepreciation() != null ?
                asset.getAccumulatedDepreciation() : BigDecimal.ZERO;

        int usefulLifeMonths = (asset.getUsefulLife() != null ? asset.getUsefulLife() : 60) * 12;

        // 计算已使用月数
        LocalDate purchaseDate = asset.getPurchaseDate() != null ?
                asset.getPurchaseDate() : LocalDate.now().minusMonths(1);
        YearMonth periodYM = YearMonth.parse(period, PERIOD_FORMATTER);
        int usedMonths = (int) ChronoUnit.MONTHS.between(
                YearMonth.from(purchaseDate),
                periodYM
        ) + 1;

        int remainingMonths = Math.max(0, usefulLifeMonths - usedMonths);

        // 计算本期折旧额
        BigDecimal depreciationAmount = calculateMonthlyDepreciation(
                DepreciationMethod.fromCode(asset.getDepreciationMethod()),
                originalValue,
                residualValue,
                accumulatedDepreciation,
                usefulLifeMonths,
                usedMonths,
                remainingMonths
        );

        // 确保净值不为负
        BigDecimal currentNetValue = originalValue.subtract(accumulatedDepreciation);
        BigDecimal minNetValue = residualValue;
        if (currentNetValue.subtract(depreciationAmount).compareTo(minNetValue) < 0) {
            depreciationAmount = currentNetValue.subtract(minNetValue);
            if (depreciationAmount.compareTo(BigDecimal.ZERO) < 0) {
                depreciationAmount = BigDecimal.ZERO;
            }
        }

        return AssetDepreciation.create(
                asset.getId(),
                asset.getAssetCode(),
                period,
                originalValue,
                accumulatedDepreciation,
                depreciationAmount,
                usedMonths,
                remainingMonths,
                asset.getDepreciationMethod()
        );
    }

    /**
     * 根据折旧方法计算月折旧额
     */
    private BigDecimal calculateMonthlyDepreciation(
            DepreciationMethod method,
            BigDecimal originalValue,
            BigDecimal residualValue,
            BigDecimal accumulatedDepreciation,
            int usefulLifeMonths,
            int usedMonths,
            int remainingMonths
    ) {
        if (remainingMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal depreciableAmount = originalValue.subtract(residualValue);
        BigDecimal netValue = originalValue.subtract(accumulatedDepreciation);

        switch (method) {
            case STRAIGHT_LINE:
                // 直线法：月折旧额 = (原值 - 残值) / 使用月数
                return depreciableAmount.divide(
                        BigDecimal.valueOf(usefulLifeMonths),
                        2,
                        RoundingMode.HALF_UP
                );

            case DOUBLE_DECLINING:
                // 双倍余额递减法
                int usefulLifeYears = usefulLifeMonths / 12;
                if (remainingMonths <= 24) {
                    // 最后两年转为直线法
                    return netValue.subtract(residualValue).divide(
                            BigDecimal.valueOf(remainingMonths),
                            2,
                            RoundingMode.HALF_UP
                    );
                }
                // 年折旧率 = 2 / 使用年限
                BigDecimal yearRate = BigDecimal.valueOf(2).divide(
                        BigDecimal.valueOf(usefulLifeYears),
                        6,
                        RoundingMode.HALF_UP
                );
                return netValue.multiply(yearRate).divide(
                        BigDecimal.valueOf(12),
                        2,
                        RoundingMode.HALF_UP
                );

            case SUM_OF_YEARS:
                // 年数总和法
                int yearsRemaining = (remainingMonths + 11) / 12;
                int yearsTotal = usefulLifeMonths / 12;
                int sumOfYears = yearsTotal * (yearsTotal + 1) / 2;
                BigDecimal yearDepreciation = depreciableAmount
                        .multiply(BigDecimal.valueOf(yearsRemaining))
                        .divide(BigDecimal.valueOf(sumOfYears), 6, RoundingMode.HALF_UP);
                return yearDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            case UNITS_OF_PRODUCTION:
                // 工作量法需要实际工作量，这里暂用直线法替代
                return depreciableAmount.divide(
                        BigDecimal.valueOf(usefulLifeMonths),
                        2,
                        RoundingMode.HALF_UP
                );

            case NONE:
            default:
                return BigDecimal.ZERO;
        }
    }

    private AssetDepreciationDTO toDTO(AssetDepreciation domain) {
        AssetDepreciationDTO dto = new AssetDepreciationDTO();
        dto.setId(domain.getId());
        dto.setAssetId(domain.getAssetId());
        dto.setAssetCode(domain.getAssetCode());
        dto.setDepreciationPeriod(domain.getDepreciationPeriod());
        dto.setBeginningValue(domain.getBeginningValue());
        dto.setBeginningAccumulatedDepreciation(domain.getBeginningAccumulatedDepreciation());
        dto.setBeginningNetValue(domain.getBeginningNetValue());
        dto.setDepreciationAmount(domain.getDepreciationAmount());
        dto.setEndingAccumulatedDepreciation(domain.getEndingAccumulatedDepreciation());
        dto.setEndingNetValue(domain.getEndingNetValue());
        dto.setUsedMonths(domain.getUsedMonths());
        dto.setRemainingMonths(domain.getRemainingMonths());
        dto.setDepreciationMethod(domain.getDepreciationMethod());
        dto.setDepreciationMethodName(DepreciationMethod.fromCode(domain.getDepreciationMethod()).getDescription());
        dto.setDepreciationDate(domain.getDepreciationDate());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setRemark(domain.getRemark());
        return dto;
    }
}
