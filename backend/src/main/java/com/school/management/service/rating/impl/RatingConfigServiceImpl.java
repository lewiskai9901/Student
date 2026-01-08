package com.school.management.service.rating.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.rating.*;
import com.school.management.entity.CheckCategory;
import com.school.management.entity.DeductionItem;
import com.school.management.entity.rating.RatingConfig;
import com.school.management.entity.rating.RatingConfigVersion;
import com.school.management.entity.rating.RatingRankingSource;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckCategoryMapper;
import com.school.management.mapper.DeductionItemMapper;
import com.school.management.mapper.rating.RatingConfigMapper;
import com.school.management.mapper.rating.RatingConfigVersionMapper;
import com.school.management.mapper.rating.RatingRankingSourceMapper;
import com.school.management.service.rating.RatingConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评级配置服务实现
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingConfigServiceImpl extends ServiceImpl<RatingConfigMapper, RatingConfig>
        implements RatingConfigService {

    private final RatingRankingSourceMapper rankingSourceMapper;
    private final RatingConfigVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final CheckCategoryMapper checkCategoryMapper;
    private final DeductionItemMapper deductionItemMapper;

    @Override
    @Transactional
    public Long createConfig(RatingConfigCreateDTO dto, Long userId) {
        log.info("创建评级配置: ratingName={}, checkPlanId={}", dto.getRatingName(), dto.getCheckPlanId());

        // 验证权重总和
        validateWeights(dto.getRankingSources());

        // 创建配置
        RatingConfig config = new RatingConfig();
        BeanUtils.copyProperties(dto, config);
        config.setCreatedBy(userId);
        config.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        this.save(config);

        // 创建数据源
        saveSources(config.getId(), dto.getRankingSources());

        // 保存版本
        saveVersion(config, userId, "创建配置");

        log.info("评级配置创建成功: id={}", config.getId());
        return config.getId();
    }

    @Override
    @Transactional
    public void updateConfig(RatingConfigUpdateDTO dto, Long userId) {
        log.info("更新评级配置: id={}, ratingName={}", dto.getId(), dto.getRatingName());

        // 验证配置是否存在
        RatingConfig config = this.getById(dto.getId());
        if (config == null) {
            throw new BusinessException("评级配置不存在");
        }

        // 验证权重总和
        validateWeights(dto.getRankingSources());

        // 更新配置
        BeanUtils.copyProperties(dto, config, "id", "checkPlanId", "ratingType", "createdBy", "createdAt");
        this.updateById(config);

        // 删除旧数据源
        LambdaQueryWrapper<RatingRankingSource> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(RatingRankingSource::getRatingConfigId, config.getId());
        rankingSourceMapper.delete(deleteQuery);

        // 创建新数据源
        saveSources(config.getId(), dto.getRankingSources());

        // 保存版本
        saveVersion(config, userId, dto.getChangeDescription() != null ? dto.getChangeDescription() : "更新配置");

        log.info("评级配置更新成功: id={}", config.getId());
    }

    @Override
    @Transactional
    public void deleteConfig(Long configId) {
        log.info("删除评级配置: id={}", configId);

        // 验证配置是否存在
        RatingConfig config = this.getById(configId);
        if (config == null) {
            throw new BusinessException("评级配置不存在");
        }

        // 删除配置（逻辑删除）
        this.removeById(configId);

        // 删除数据源
        LambdaQueryWrapper<RatingRankingSource> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(RatingRankingSource::getRatingConfigId, configId);
        rankingSourceMapper.delete(deleteQuery);

        log.info("评级配置删除成功: id={}", configId);
    }

    @Override
    @Transactional
    public void toggleEnabled(Long configId, boolean enabled) {
        log.info("切换评级配置启用状态: id={}, enabled={}", configId, enabled);

        RatingConfig config = this.getById(configId);
        if (config == null) {
            throw new BusinessException("评级配置不存在");
        }

        config.setEnabled(enabled ? 1 : 0);
        this.updateById(config);

        log.info("评级配置状态更新成功: id={}, enabled={}", configId, enabled);
    }

    @Override
    public RatingConfigVO getConfigDetail(Long configId) {
        RatingConfig config = this.getById(configId);
        if (config == null) {
            throw new BusinessException("评级配置不存在");
        }

        return convertToVO(config);
    }

    @Override
    public Page<RatingConfigVO> getConfigPage(RatingConfigQueryDTO query) {
        Page<RatingConfig> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<RatingConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(query.getCheckPlanId() != null, RatingConfig::getCheckPlanId, query.getCheckPlanId())
                .like(query.getRatingName() != null, RatingConfig::getRatingName, query.getRatingName())
                .eq(query.getRatingType() != null, RatingConfig::getRatingType, query.getRatingType())
                .eq(query.getEnabled() != null, RatingConfig::getEnabled, query.getEnabled())
                .orderByAsc(RatingConfig::getPriority)
                .orderByDesc(RatingConfig::getCreatedAt);

        Page<RatingConfig> result = this.page(page, queryWrapper);

        Page<RatingConfigVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<RatingConfigVO> getConfigsByPlan(Long checkPlanId) {
        LambdaQueryWrapper<RatingConfig> query = new LambdaQueryWrapper<>();
        query.eq(RatingConfig::getCheckPlanId, checkPlanId)
                .orderByAsc(RatingConfig::getPriority)
                .orderByAsc(RatingConfig::getSortOrder);

        List<RatingConfig> configs = this.list(query);
        return configs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 验证权重总和
     */
    private void validateWeights(List<RatingRankingSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) {
            throw new BusinessException("排名数据源不能为空");
        }

        if (sources.size() > 5) {
            throw new BusinessException("最多支持5个数据源组合");
        }

        // 如果是多数据源，验证权重总和
        if (sources.size() > 1) {
            double totalWeight = sources.stream()
                    .mapToDouble(s -> s.getWeight() != null ? s.getWeight().doubleValue() : 0)
                    .sum();

            if (Math.abs(totalWeight - 1.0) > 0.001) {
                throw new BusinessException("组合排名时，权重总和必须为1.0，当前为: " + totalWeight);
            }
        }
    }

    /**
     * 保存数据源
     */
    private void saveSources(Long configId, List<RatingRankingSourceDTO> sources) {
        for (int i = 0; i < sources.size(); i++) {
            RatingRankingSourceDTO sourceDTO = sources.get(i);
            RatingRankingSource source = new RatingRankingSource();
            BeanUtils.copyProperties(sourceDTO, source);
            source.setRatingConfigId(configId);
            source.setSortOrder(i);
            rankingSourceMapper.insert(source);
        }
    }

    /**
     * 保存版本
     */
    private void saveVersion(RatingConfig config, Long userId, String changeDescription) {
        try {
            // 获取当前最大版本号
            LambdaQueryWrapper<RatingConfigVersion> versionQuery = new LambdaQueryWrapper<>();
            versionQuery.eq(RatingConfigVersion::getRatingConfigId, config.getId())
                    .orderByDesc(RatingConfigVersion::getVersion)
                    .last("LIMIT 1");
            RatingConfigVersion lastVersion = versionMapper.selectOne(versionQuery);

            int newVersion = lastVersion != null ? lastVersion.getVersion() + 1 : 1;

            // 创建版本记录
            RatingConfigVersion version = new RatingConfigVersion();
            version.setRatingConfigId(config.getId());
            version.setVersion(newVersion);
            version.setConfigSnapshot(objectMapper.writeValueAsString(config));
            version.setChangeDescription(changeDescription);
            version.setCreatedBy(userId);
            versionMapper.insert(version);

            log.info("保存配置版本: configId={}, version={}", config.getId(), newVersion);
        } catch (Exception e) {
            log.error("保存配置版本失败", e);
            // 版本保存失败不影响主流程
        }
    }

    /**
     * 转换为VO
     */
    private RatingConfigVO convertToVO(RatingConfig config) {
        RatingConfigVO vo = new RatingConfigVO();
        BeanUtils.copyProperties(config, vo);

        // 设置文本
        vo.setRatingTypeText(getRatingTypeText(config.getRatingType()));
        vo.setDivisionMethodText(getDivisionMethodText(config.getDivisionMethod()));

        // 加载数据源
        LambdaQueryWrapper<RatingRankingSource> sourceQuery = new LambdaQueryWrapper<>();
        sourceQuery.eq(RatingRankingSource::getRatingConfigId, config.getId())
                .orderByAsc(RatingRankingSource::getSortOrder);
        List<RatingRankingSource> sources = rankingSourceMapper.selectList(sourceQuery);

        vo.setRankingSources(sources.stream()
                .map(this::convertSourceToVO)
                .collect(Collectors.toList()));

        // 获取当前版本号
        LambdaQueryWrapper<RatingConfigVersion> versionQuery = new LambdaQueryWrapper<>();
        versionQuery.eq(RatingConfigVersion::getRatingConfigId, config.getId())
                .orderByDesc(RatingConfigVersion::getVersion)
                .last("LIMIT 1");
        RatingConfigVersion lastVersion = versionMapper.selectOne(versionQuery);
        vo.setCurrentVersion(lastVersion != null ? lastVersion.getVersion() : 0);

        return vo;
    }

    /**
     * 转换数据源为VO
     */
    private RatingRankingSourceVO convertSourceToVO(RatingRankingSource source) {
        RatingRankingSourceVO vo = new RatingRankingSourceVO();
        BeanUtils.copyProperties(source, vo);

        vo.setSourceTypeText(getSourceTypeText(source.getSourceType()));
        vo.setMissingDataStrategyText(getMissingDataStrategyText(source.getMissingDataStrategy()));

        // 加载数据源名称
        if (source.getSourceId() != null) {
            String sourceType = source.getSourceType();
            if ("CATEGORY".equals(sourceType)) {
                CheckCategory category = checkCategoryMapper.selectById(source.getSourceId());
                if (category != null) {
                    vo.setSourceName(category.getCategoryName());
                }
            } else if ("DEDUCTION_ITEM".equals(sourceType)) {
                DeductionItem item = deductionItemMapper.selectById(source.getSourceId());
                if (item != null) {
                    vo.setSourceName(item.getItemName());
                }
            }
        } else if ("TOTAL_SCORE".equals(source.getSourceType())) {
            vo.setSourceName("总分");
        }

        return vo;
    }

    /**
     * 获取评级类型文本
     */
    private String getRatingTypeText(String type) {
        switch (type) {
            case "DAILY": return "日评级";
            case "WEEKLY": return "周评级";
            case "MONTHLY": return "月评级";
            default: return type;
        }
    }

    /**
     * 获取划分方式文本
     */
    private String getDivisionMethodText(String method) {
        switch (method) {
            case "TOP_N": return "前N名";
            case "TOP_PERCENT": return "前N%";
            case "BOTTOM_N": return "后N名";
            case "BOTTOM_PERCENT": return "后N%";
            default: return method;
        }
    }

    /**
     * 获取数据源类型文本
     */
    private String getSourceTypeText(String type) {
        switch (type) {
            case "TOTAL_SCORE": return "总分";
            case "CATEGORY": return "分类得分";
            case "DEDUCTION_ITEM": return "扣分项得分";
            default: return type;
        }
    }

    /**
     * 获取缺失数据策略文本
     */
    private String getMissingDataStrategyText(String strategy) {
        switch (strategy) {
            case "ZERO": return "视为0分";
            case "SKIP": return "跳过不计";
            default: return strategy;
        }
    }
}
