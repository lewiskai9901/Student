package com.school.management.service.rating.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.rating.RatingResultQueryDTO;
import com.school.management.dto.rating.RatingResultVO;
import com.school.management.entity.Class;
import com.school.management.entity.rating.RatingChangeLog;
import com.school.management.entity.rating.RatingConfig;
import com.school.management.entity.rating.RatingResult;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.rating.RatingChangeLogMapper;
import com.school.management.mapper.rating.RatingConfigMapper;
import com.school.management.mapper.rating.RatingResultMapper;
import com.school.management.service.rating.RatingResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评级结果服务实现
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingResultServiceImpl extends ServiceImpl<RatingResultMapper, RatingResult>
        implements RatingResultService {

    private final RatingConfigMapper configMapper;
    private final RatingChangeLogMapper changeLogMapper;
    private final ClassMapper classMapper;

    @Override
    @Transactional
    public void approveResult(Long resultId, boolean approved, Long userId) {
        log.info("审核评级结果: resultId={}, approved={}, userId={}", resultId, approved, userId);

        RatingResult result = this.getById(resultId);
        if (result == null) {
            throw new BusinessException("评级结果不存在");
        }

        if (!"PENDING_APPROVAL".equals(result.getStatus())) {
            throw new BusinessException("只能审核待审核状态的评级结果");
        }

        String oldStatus = result.getStatus();
        String newStatus = approved ? "PUBLISHED" : "DRAFT";

        result.setStatus(newStatus);
        result.setApprovedBy(userId);
        result.setApprovedAt(LocalDateTime.now());

        if (approved) {
            // 获取配置，判断是否自动发布
            RatingConfig config = configMapper.selectById(result.getRatingConfigId());
            if (config != null && config.getAutoPublish() == 1) {
                result.setPublishedBy(userId);
                result.setPublishedAt(LocalDateTime.now());
            }
        }

        this.updateById(result);

        // 记录日志
        logChange(result, approved ? "APPROVED" : "REJECTED",
                approved ? "审核通过" : "审核驳回", userId, oldStatus, newStatus);

        log.info("评级结果审核完成: resultId={}, approved={}", resultId, approved);
    }

    @Override
    @Transactional
    public void batchApproveResults(List<Long> resultIds, boolean approved, Long userId) {
        log.info("批量审核评级结果: count={}, approved={}", resultIds.size(), approved);

        for (Long resultId : resultIds) {
            try {
                approveResult(resultId, approved, userId);
            } catch (Exception e) {
                log.error("审核评级结果失败: resultId={}", resultId, e);
                // 继续处理其他结果
            }
        }

        log.info("批量审核完成");
    }

    @Override
    @Transactional
    public void publishResult(Long resultId, Long userId) {
        log.info("发布评级结果: resultId={}, userId={}", resultId, userId);

        RatingResult result = this.getById(resultId);
        if (result == null) {
            throw new BusinessException("评级结果不存在");
        }

        if ("PUBLISHED".equals(result.getStatus())) {
            throw new BusinessException("评级结果已发布");
        }

        String oldStatus = result.getStatus();
        result.setStatus("PUBLISHED");
        result.setPublishedBy(userId);
        result.setPublishedAt(LocalDateTime.now());
        this.updateById(result);

        // 记录日志
        logChange(result, "PUBLISHED", "发布评级", userId, oldStatus, "PUBLISHED");

        log.info("评级结果发布成功: resultId={}", resultId);
    }

    @Override
    @Transactional
    public void batchPublishResults(List<Long> resultIds, Long userId) {
        log.info("批量发布评级结果: count={}", resultIds.size());

        for (Long resultId : resultIds) {
            try {
                publishResult(resultId, userId);
            } catch (Exception e) {
                log.error("发布评级结果失败: resultId={}", resultId, e);
                // 继续处理其他结果
            }
        }

        log.info("批量发布完成");
    }

    @Override
    @Transactional
    public void revokeResult(Long resultId, Long userId) {
        log.info("撤销发布评级结果: resultId={}, userId={}", resultId, userId);

        RatingResult result = this.getById(resultId);
        if (result == null) {
            throw new BusinessException("评级结果不存在");
        }

        if (!"PUBLISHED".equals(result.getStatus())) {
            throw new BusinessException("只能撤销已发布的评级结果");
        }

        String oldStatus = result.getStatus();
        result.setStatus("DRAFT");
        result.setPublishedBy(null);
        result.setPublishedAt(null);
        this.updateById(result);

        // 记录日志
        logChange(result, "REVOKED", "撤销发布", userId, oldStatus, "DRAFT");

        log.info("评级结果撤销成功: resultId={}", resultId);
    }

    @Override
    public RatingResultVO getResultDetail(Long resultId) {
        RatingResult result = this.getById(resultId);
        if (result == null) {
            throw new BusinessException("评级结果不存在");
        }

        return convertToVO(result);
    }

    @Override
    public Page<RatingResultVO> getResultPage(RatingResultQueryDTO query) {
        Page<RatingResult> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<RatingResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(query.getCheckPlanId() != null, RatingResult::getCheckPlanId, query.getCheckPlanId())
                .eq(query.getRatingConfigId() != null, RatingResult::getRatingConfigId, query.getRatingConfigId())
                .eq(query.getClassId() != null, RatingResult::getClassId, query.getClassId())
                .eq(query.getPeriodType() != null, RatingResult::getPeriodType, query.getPeriodType())
                .ge(query.getPeriodStartFrom() != null, RatingResult::getPeriodStart, query.getPeriodStartFrom())
                .le(query.getPeriodStartTo() != null, RatingResult::getPeriodStart, query.getPeriodStartTo())
                .eq(query.getAwarded() != null, RatingResult::getAwarded, query.getAwarded())
                .eq(query.getStatus() != null, RatingResult::getStatus, query.getStatus())
                .orderByDesc(RatingResult::getPeriodStart)
                .orderByAsc(RatingResult::getRanking);

        Page<RatingResult> result = this.page(page, queryWrapper);

        Page<RatingResultVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<RatingResultVO> getClassRatingHistory(Long classId, Long ratingConfigId) {
        LambdaQueryWrapper<RatingResult> query = new LambdaQueryWrapper<>();
        query.eq(RatingResult::getClassId, classId)
                .eq(ratingConfigId != null, RatingResult::getRatingConfigId, ratingConfigId)
                .orderByDesc(RatingResult::getPeriodStart);

        List<RatingResult> results = this.list(query);
        return results.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 记录变更日志
     */
    private void logChange(RatingResult result, String changeType, String reason,
                          Long operatorId, String oldStatus, String newStatus) {
        try {
            RatingChangeLog log = new RatingChangeLog();
            log.setRatingResultId(result.getId());
            log.setChangeType(changeType);
            log.setChangeReason(reason);
            log.setOldStatus(oldStatus);
            log.setNewStatus(newStatus);
            log.setOperatorId(operatorId);
            changeLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录变更日志失败", e);
            // 日志记录失败不影响主流程
        }
    }

    /**
     * 转换为VO
     */
    private RatingResultVO convertToVO(RatingResult result) {
        RatingResultVO vo = new RatingResultVO();
        BeanUtils.copyProperties(result, vo);

        // 设置文本
        vo.setPeriodTypeText(getPeriodTypeText(result.getPeriodType()));
        vo.setStatusText(getStatusText(result.getStatus()));
        vo.setPeriodText(formatPeriodText(result));

        // 加载评级配置信息
        RatingConfig config = configMapper.selectById(result.getRatingConfigId());
        if (config != null) {
            vo.setRatingName(config.getRatingName());
            vo.setIcon(config.getIcon());
            vo.setColor(config.getColor());
        }

        // 加载班级名称
        if (result.getClassId() != null) {
            Class classEntity = classMapper.selectById(result.getClassId());
            if (classEntity != null) {
                vo.setClassName(classEntity.getClassName());
            }
        }

        return vo;
    }

    /**
     * 格式化周期文本
     */
    private String formatPeriodText(RatingResult result) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String start = result.getPeriodStart().format(formatter);
        String end = result.getPeriodEnd().format(formatter);

        switch (result.getPeriodType()) {
            case "DAILY":
                return start;
            case "WEEKLY":
                return start + " ~ " + end;
            case "MONTHLY":
                return result.getPeriodStart().format(DateTimeFormatter.ofPattern("yyyy年MM月"));
            default:
                return start + " ~ " + end;
        }
    }

    /**
     * 获取周期类型文本
     */
    private String getPeriodTypeText(String type) {
        switch (type) {
            case "DAILY": return "日评级";
            case "WEEKLY": return "周评级";
            case "MONTHLY": return "月评级";
            default: return type;
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "DRAFT": return "草稿";
            case "PENDING_APPROVAL": return "待审核";
            case "PUBLISHED": return "已发布";
            default: return status;
        }
    }
}
