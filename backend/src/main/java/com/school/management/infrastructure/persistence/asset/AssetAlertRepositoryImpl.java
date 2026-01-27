package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.asset.model.entity.AssetAlert;
import com.school.management.domain.asset.model.valueobject.AlertLevel;
import com.school.management.domain.asset.model.valueobject.AlertType;
import com.school.management.domain.asset.repository.AssetAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产预警仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetAlertRepositoryImpl implements AssetAlertRepository {

    private final AssetAlertMapper mapper;

    @Override
    public void save(AssetAlert alert) {
        AssetAlertPO po = toPO(alert);
        if (po.getId() == null) {
            mapper.insert(po);
            alert.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public Optional<AssetAlert> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(this::toDomain);
    }

    @Override
    public List<AssetAlert> findUnreadByUserId(Long userId) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getNotifyUserId, userId)
               .eq(AssetAlertPO::getIsRead, false)
               .orderByDesc(AssetAlertPO::getAlertTime);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetAlert> findUnhandledByUserId(Long userId) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getNotifyUserId, userId)
               .eq(AssetAlertPO::getIsHandled, false)
               .orderByDesc(AssetAlertPO::getAlertTime);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetAlert> findByAssetId(Long assetId) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getAssetId, assetId)
               .orderByDesc(AssetAlertPO::getAlertTime);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetAlert> findByBorrowId(Long borrowId) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getBorrowId, borrowId)
               .orderByDesc(AssetAlertPO::getAlertTime);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAssetIdAndType(Long assetId, AlertType type) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getAssetId, assetId)
               .eq(AssetAlertPO::getAlertType, type.getCode())
               .eq(AssetAlertPO::getIsHandled, false);
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByBorrowIdAndType(Long borrowId, AlertType type) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getBorrowId, borrowId)
               .eq(AssetAlertPO::getAlertType, type.getCode())
               .eq(AssetAlertPO::getIsHandled, false);
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public int countUnreadByUserId(Long userId) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getNotifyUserId, userId)
               .eq(AssetAlertPO::getIsRead, false);
        return Math.toIntExact(mapper.selectCount(wrapper));
    }

    @Override
    public int countUnhandled() {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getIsHandled, false);
        return Math.toIntExact(mapper.selectCount(wrapper));
    }

    @Override
    public int countUnhandledByType(AlertType type) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetAlertPO::getAlertType, type.getCode())
               .eq(AssetAlertPO::getIsHandled, false);
        return Math.toIntExact(mapper.selectCount(wrapper));
    }

    @Override
    public List<AssetAlert> findByCondition(
            AlertType type,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId,
            int offset,
            int limit
    ) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = buildConditionWrapper(type, isRead, isHandled, notifyUserId);
        wrapper.orderByDesc(AssetAlertPO::getAlertTime)
               .last("LIMIT " + offset + ", " + limit);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countByCondition(
            AlertType type,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId
    ) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = buildConditionWrapper(type, isRead, isHandled, notifyUserId);
        return Math.toIntExact(mapper.selectCount(wrapper));
    }

    private LambdaQueryWrapper<AssetAlertPO> buildConditionWrapper(
            AlertType type,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId
    ) {
        LambdaQueryWrapper<AssetAlertPO> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(AssetAlertPO::getAlertType, type.getCode());
        }
        if (isRead != null) {
            wrapper.eq(AssetAlertPO::getIsRead, isRead);
        }
        if (isHandled != null) {
            wrapper.eq(AssetAlertPO::getIsHandled, isHandled);
        }
        if (notifyUserId != null) {
            wrapper.eq(AssetAlertPO::getNotifyUserId, notifyUserId);
        }
        return wrapper;
    }

    // ============ 转换方法 ============

    private AssetAlert toDomain(AssetAlertPO po) {
        AssetAlert alert = new AssetAlert();
        alert.setId(po.getId());
        alert.setAlertType(AlertType.fromCode(po.getAlertType()));
        alert.setAssetId(po.getAssetId());
        alert.setAssetCode(po.getAssetCode());
        alert.setAssetName(po.getAssetName());
        alert.setBorrowId(po.getBorrowId());
        alert.setAlertContent(po.getAlertContent());
        alert.setAlertLevel(AlertLevel.fromCode(po.getAlertLevel()));
        alert.setRead(Boolean.TRUE.equals(po.getIsRead()));
        alert.setHandled(Boolean.TRUE.equals(po.getIsHandled()));
        alert.setHandleRemark(po.getHandleRemark());
        alert.setHandleTime(po.getHandleTime());
        alert.setHandlerId(po.getHandlerId());
        alert.setHandlerName(po.getHandlerName());
        alert.setNotifyUserId(po.getNotifyUserId());
        alert.setNotifyUserName(po.getNotifyUserName());
        alert.setAlertTime(po.getAlertTime());
        alert.setExpireTime(po.getExpireTime());
        alert.setCreatedAt(po.getCreatedAt());
        alert.setUpdatedAt(po.getUpdatedAt());
        return alert;
    }

    private AssetAlertPO toPO(AssetAlert alert) {
        AssetAlertPO po = new AssetAlertPO();
        po.setId(alert.getId());
        po.setAlertType(alert.getAlertType().getCode());
        po.setAssetId(alert.getAssetId());
        po.setAssetCode(alert.getAssetCode());
        po.setAssetName(alert.getAssetName());
        po.setBorrowId(alert.getBorrowId());
        po.setAlertContent(alert.getAlertContent());
        po.setAlertLevel(alert.getAlertLevel().getCode());
        po.setIsRead(alert.isRead());
        po.setIsHandled(alert.isHandled());
        po.setHandleRemark(alert.getHandleRemark());
        po.setHandleTime(alert.getHandleTime());
        po.setHandlerId(alert.getHandlerId());
        po.setHandlerName(alert.getHandlerName());
        po.setNotifyUserId(alert.getNotifyUserId());
        po.setNotifyUserName(alert.getNotifyUserName());
        po.setAlertTime(alert.getAlertTime());
        po.setExpireTime(alert.getExpireTime());
        return po;
    }
}
