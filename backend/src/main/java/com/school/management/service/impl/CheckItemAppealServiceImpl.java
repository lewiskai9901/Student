package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.CheckItemAppealDTO;
import com.school.management.dto.request.AppealCreateRequest;
import com.school.management.dto.request.AppealReviewRequest;
import com.school.management.dto.vo.AppealListVO;
import com.school.management.dto.vo.AppealStatisticsVO;
import com.school.management.dto.vo.RankingChangeVO;
import com.school.management.entity.CheckItemAppeal;
import com.school.management.enums.AppealStatus;
import com.school.management.enums.AppealType;
import com.school.management.exception.BusinessException;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.entity.record.CheckRecordDeductionNew;
import com.school.management.mapper.CheckItemAppealMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.record.CheckRecordDeductionMapper;
import com.school.management.service.CheckItemAppealService;
import com.school.management.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CheckItemAppealServiceImpl extends ServiceImpl<CheckItemAppealMapper, CheckItemAppeal>
        implements CheckItemAppealService {

    private final CheckItemAppealMapper appealMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;
    private final CheckRecordDeductionMapper deductionMapper;
    private final SystemConfigService systemConfigService;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 量化检查基础分数配置键 */
    private static final String CONFIG_KEY_BASE_SCORE = "check.base.score";
    /** 默认基础分数 */
    private static final String DEFAULT_BASE_SCORE = "100";
    /** 等级阈值配置键 */
    private static final String CONFIG_KEY_RATING_A = "check.rating.threshold.a";
    private static final String CONFIG_KEY_RATING_B = "check.rating.threshold.b";
    private static final String CONFIG_KEY_RATING_C = "check.rating.threshold.c";
    /** 默认等级阈值 */
    private static final String DEFAULT_THRESHOLD_A = "95";
    private static final String DEFAULT_THRESHOLD_B = "85";
    private static final String DEFAULT_THRESHOLD_C = "70";
    /** 排名重算锁前缀 */
    private static final String RANKING_LOCK_PREFIX = "appeal:ranking:lock:";
    /** 排名重算锁超时时间(秒) */
    private static final long RANKING_LOCK_TIMEOUT = 30;

    public CheckItemAppealServiceImpl(
            CheckItemAppealMapper appealMapper,
            @Qualifier("newCheckRecordClassStatsMapper") CheckRecordClassStatsMapper classStatsMapper,
            @Qualifier("newCheckRecordDeductionMapper") CheckRecordDeductionMapper deductionMapper,
            SystemConfigService systemConfigService,
            RedisTemplate<String, Object> redisTemplate) {
        this.appealMapper = appealMapper;
        this.classStatsMapper = classStatsMapper;
        this.deductionMapper = deductionMapper;
        this.systemConfigService = systemConfigService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取量化检查基础分数
     * @return 基础分数，默认100
     */
    private BigDecimal getBaseScore() {
        String scoreStr = systemConfigService.getConfigValue(CONFIG_KEY_BASE_SCORE, DEFAULT_BASE_SCORE);
        try {
            return new BigDecimal(scoreStr);
        } catch (NumberFormatException e) {
            log.warn("配置的基础分数格式错误: {}, 使用默认值: {}", scoreStr, DEFAULT_BASE_SCORE);
            return new BigDecimal(DEFAULT_BASE_SCORE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckItemAppealDTO createAppeal(AppealCreateRequest request, Long appellantId) {
        log.info("创建申诉: itemId={}, appellantId={}", request.getItemId(), appellantId);

        // 从扣分明细中获取必要信息
        CheckRecordDeductionNew deduction = deductionMapper.selectById(request.getItemId());
        if (deduction == null) {
            throw new BusinessException("扣分明细不存在");
        }

        CheckItemAppeal appeal = new CheckItemAppeal();
        appeal.setAppealCode(generateAppealCode());
        appeal.setRecordId(request.getRecordId());
        appeal.setItemId(request.getItemId());

        // 从扣分明细获取班级和扣分信息
        appeal.setClassStatId(deduction.getClassStatId());
        appeal.setClassId(deduction.getClassId());
        appeal.setClassName(deduction.getClassName());
        // 从班级统计获取年级信息
        if (deduction.getClassStatId() != null) {
            CheckRecordClassStatsNew classStats = classStatsMapper.selectById(deduction.getClassStatId());
            if (classStats != null) {
                appeal.setGradeId(classStats.getGradeId());
            }
        }
        appeal.setCategoryId(deduction.getCategoryId());
        appeal.setCategoryName(deduction.getCategoryName());
        appeal.setItemName(deduction.getDeductionItemName());
        appeal.setOriginalScore(deduction.getActualScore());

        // 关联信息
        if (deduction.getLinkType() != null && deduction.getLinkType() > 0) {
            String linkTypeName = deduction.getLinkType() == 1 ? "宿舍" : "教室";
            appeal.setLinkInfo(linkTypeName + ":" + (deduction.getLinkName() != null ? deduction.getLinkName() : deduction.getLinkCode()));
        }
        appeal.setOriginalPhotoUrls(deduction.getPhotoUrls());
        appeal.setOriginalRemark(deduction.getRemark());

        // 申诉人信息
        appeal.setAppellantId(appellantId);
        appeal.setAppealReason(request.getReason());
        appeal.setExpectedScore(request.getExpectedScore());
        appeal.setAppealType(request.getAppealType() != null ? request.getAppealType() : 1);

        // Evidence IDs to URLs conversion if needed
        if (request.getEvidenceIds() != null && !request.getEvidenceIds().isEmpty()) {
            appeal.setEvidenceUrls(String.join(",", request.getEvidenceIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.toList())));
        }
        appeal.setStatus(AppealStatus.PENDING.getCode());
        appeal.setCurrentStep(1);
        appeal.setAppealTime(LocalDateTime.now());

        save(appeal);

        // 更新扣分明细的申诉状态为"申诉中"
        updateDeductionAppealStatus(request.getItemId(), 1, appeal.getId());

        return convertToDTO(appeal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckItemAppealDTO reviewAppeal(AppealReviewRequest request, Long approverId) {
        log.info("审核申诉: appealId={}, approverId={}, status={}", request.getAppealId(), approverId, request.getApprovalStatus());

        CheckItemAppeal appeal = getById(request.getAppealId());
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }

        appeal.setStatus(request.getApprovalStatus());
        appeal.setFinalReviewerId(approverId);
        appeal.setFinalReviewTime(LocalDateTime.now());
        appeal.setFinalReviewOpinion(request.getApprovalOpinion());

        if (request.getAdjustedScore() != null) {
            appeal.setAdjustedScore(request.getAdjustedScore());
            // 计算分数变化
            if (appeal.getOriginalScore() != null) {
                appeal.setScoreChange(appeal.getOriginalScore().subtract(request.getAdjustedScore()));
            }
        }

        updateById(appeal);

        // 更新扣分明细的申诉状态
        if (appeal.getItemId() != null) {
            // 审核通过(2)或驳回(3)
            int appealStatus = request.getApprovalStatus(); // 2=通过, 3=驳回
            updateDeductionAppealStatus(appeal.getItemId(), appealStatus, appeal.getId());

            // 如果通过且有调整分数，更新实际扣分
            if (request.getApprovalStatus() == 2 && request.getAdjustedScore() != null) {
                updateDeductionScore(appeal.getItemId(), request.getAdjustedScore());
                log.info("更新扣分明细: itemId={}, adjustedScore={}", appeal.getItemId(), request.getAdjustedScore());
            }
        }

        return convertToDTO(appeal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawAppeal(Long appealId, String reason, Long userId) {
        log.info("撤销申诉: appealId={}, userId={}", appealId, userId);

        CheckItemAppeal appeal = getById(appealId);
        if (appeal == null) {
            return false;
        }

        appeal.setStatus(AppealStatus.WITHDRAWN.getCode());
        boolean success = updateById(appeal);

        // 更新扣分明细的申诉状态为0(无申诉),这样可以重新申诉
        if (success && appeal.getItemId() != null) {
            updateDeductionAppealStatus(appeal.getItemId(), 0, null);
            log.info("已重置扣分明细申诉状态: itemId={}", appeal.getItemId());
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processPublicityEnd() {
        log.info("处理公示期结束的申诉");

        // 查询公示期已结束的申诉(status=6且publicityEndTime<当前时间)
        List<CheckItemAppeal> appeals = appealMapper.selectPublicityEndedAppeals(LocalDateTime.now());

        int count = 0;
        for (CheckItemAppeal appeal : appeals) {
            try {
                // 将状态从公示中更新为已生效
                appeal.setStatus(AppealStatus.EFFECTIVE.getCode());
                updateById(appeal);

                // 执行排名重算
                recalculateAfterAppeal(appeal);

                count++;
                log.info("申诉生效成功: appealId={}, appealCode={}", appeal.getId(), appeal.getAppealCode());
            } catch (Exception e) {
                log.error("申诉生效失败: appealId={}", appeal.getId(), e);
                // 继续处理其他申诉
            }
        }

        log.info("公示期结束处理完成,共处理{}条申诉", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RankingChangeVO processEffective(Long appealId) {
        log.info("申诉生效: appealId={}", appealId);
        CheckItemAppeal appeal = getById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        return recalculateAfterAppeal(appeal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RankingChangeVO recalculateAfterAppeal(CheckItemAppeal appeal) {
        log.info("申诉后重新计算排名: appealId={}, recordId={}, itemId={}",
                appeal.getId(), appeal.getRecordId(), appeal.getItemId());

        RankingChangeVO vo = new RankingChangeVO();

        // 1. 获取申诉涉及的班级信息
        Long classId = appeal.getClassId();
        Long recordId = appeal.getRecordId();
        Long gradeId = appeal.getGradeId();

        if (classId == null || recordId == null) {
            log.warn("申诉数据不完整: classId={}, recordId={}", classId, recordId);
            return vo;
        }

        // ⚠️ 分布式锁：防止同一检查记录的多个申诉并发重算排名
        String lockKey = RANKING_LOCK_PREFIX + recordId;
        Boolean acquired = false;
        try {
            acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, appeal.getId().toString(),
                    RANKING_LOCK_TIMEOUT, TimeUnit.SECONDS);

            if (acquired == null || !acquired) {
                log.warn("【并发冲突】无法获取排名重算锁: recordId={}, appealId={}", recordId, appeal.getId());
                throw new BusinessException("其他申诉正在处理中，请稍后重试");
            }

            log.info("获取排名重算锁成功: recordId={}, appealId={}", recordId, appeal.getId());
            return doRecalculateAfterAppeal(appeal, vo, classId, recordId, gradeId);
        } finally {
            // 释放锁
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    redisTemplate.delete(lockKey);
                    log.debug("释放排名重算锁: recordId={}", recordId);
                } catch (Exception e) {
                    log.error("释放排名重算锁失败: recordId={}", recordId, e);
                }
            }
        }
    }

    /**
     * 执行排名重算逻辑（在分布式锁保护下）
     */
    private RankingChangeVO doRecalculateAfterAppeal(CheckItemAppeal appeal, RankingChangeVO vo,
                                                      Long classId, Long recordId, Long gradeId) {

        vo.setClassId(classId);
        vo.setClassName(appeal.getClassName());
        vo.setGradeId(gradeId);

        // 2. 获取该班级当前的统计记录
        CheckRecordClassStatsNew classStat = classStatsMapper.selectOne(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .eq(CheckRecordClassStatsNew::getRecordId, recordId)
                        .eq(CheckRecordClassStatsNew::getClassId, classId)
        );
        if (classStat == null) {
            log.warn("未找到班级统计记录: recordId={}, classId={}", recordId, classId);
            return vo;
        }

        // 记录原始分数和排名
        BigDecimal originalScore = classStat.getTotalScore() != null ? classStat.getTotalScore() : BigDecimal.ZERO;
        Integer originalGradeRank = classStat.getGradeRanking();
        Integer originalSchoolRank = classStat.getOverallRanking();

        vo.setOriginalTotalScore(originalScore);
        vo.setOriginalGradeRank(originalGradeRank);
        vo.setOriginalSchoolRank(originalSchoolRank);

        // 3. 如果申诉有分数调整,更新扣分明细
        BigDecimal scoreChange = BigDecimal.ZERO;
        if (appeal.getAdjustedScore() != null && appeal.getItemId() != null) {
            CheckRecordDeductionNew deduction = deductionMapper.selectById(appeal.getItemId());
            if (deduction != null) {
                BigDecimal oldDeduct = deduction.getActualScore() != null ? deduction.getActualScore() : BigDecimal.ZERO;
                BigDecimal newDeduct = appeal.getAdjustedScore();

                // 更新扣分明细
                updateDeductionScore(appeal.getItemId(), newDeduct);
                updateDeductionAppealStatus(appeal.getItemId(), 2, appeal.getId()); // 2=已通过

                // 计算分数变化(扣分减少=总分增加)
                scoreChange = oldDeduct.subtract(newDeduct);
                log.info("扣分调整: itemId={}, 原扣分={}, 新扣分={}, 变化={}",
                        appeal.getItemId(), oldDeduct, newDeduct, scoreChange);
            }
        }

        // 4. 重新计算该班级的总分
        BigDecimal totalDeduct = sumDeductScoreByRecordAndClass(recordId, classId);
        BigDecimal baseScore = getBaseScore();
        BigDecimal newTotalScore = baseScore.subtract(totalDeduct);

        vo.setAdjustedTotalScore(newTotalScore);
        vo.setScoreChange(newTotalScore.subtract(originalScore));

        // 更新班级统计的总分
        classStat.setTotalScore(newTotalScore);

        // 5. 重新计算年级内排名
        List<CheckRecordClassStatsNew> gradeStats = classStatsMapper.selectList(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .eq(CheckRecordClassStatsNew::getRecordId, recordId)
                        .eq(CheckRecordClassStatsNew::getGradeId, gradeId)
                        .orderByDesc(CheckRecordClassStatsNew::getTotalScore)
        );
        recalculateRankings(gradeStats, true);

        // 找到该班级的新年级排名
        Integer newGradeRank = null;
        for (CheckRecordClassStatsNew stat : gradeStats) {
            if (stat.getClassId().equals(classId)) {
                newGradeRank = stat.getGradeRanking();
                break;
            }
        }
        vo.setAdjustedGradeRank(newGradeRank);
        vo.setGradeRankChange(originalGradeRank != null && newGradeRank != null
                ? originalGradeRank - newGradeRank : 0); // 排名减小=上升

        // 6. 重新计算全校排名
        List<CheckRecordClassStatsNew> allStats = classStatsMapper.selectList(
                new LambdaQueryWrapper<CheckRecordClassStatsNew>()
                        .eq(CheckRecordClassStatsNew::getRecordId, recordId)
                        .orderByDesc(CheckRecordClassStatsNew::getTotalScore)
        );
        recalculateRankings(allStats, false);

        // 找到该班级的新全校排名
        Integer newSchoolRank = null;
        for (CheckRecordClassStatsNew stat : allStats) {
            if (stat.getClassId().equals(classId)) {
                newSchoolRank = stat.getOverallRanking();
                break;
            }
        }
        vo.setAdjustedSchoolRank(newSchoolRank);
        vo.setSchoolRankChange(originalSchoolRank != null && newSchoolRank != null
                ? originalSchoolRank - newSchoolRank : 0);

        // 7. 统计受影响的其他班级
        List<RankingChangeVO.AffectedClassDTO> affectedClasses = new ArrayList<>();
        int affectedCount = 0;
        for (CheckRecordClassStatsNew stat : gradeStats) {
            if (!stat.getClassId().equals(classId)) {
                // 检查排名是否变化(这里简化处理,实际需要与更新前比较)
                // 由于排名已经重算,这里只记录当前排名
                affectedCount++;
            }
        }
        vo.setAffectedClassCount(affectedCount);
        vo.setAffectedClasses(affectedClasses);

        // 8. 更新班级统计的申诉计数
        int appealCount = (classStat.getAppealCount() != null ? classStat.getAppealCount() : 0) + 1;
        int appealPassed = (classStat.getAppealApproved() != null ? classStat.getAppealApproved() : 0) + 1;
        updateClassStatsAppealCount(classStat.getId(), appealCount, appealPassed, 0);

        // 9. 确定等级是否变化
        String originalRating = determineRating(originalScore);
        String newRating = determineRating(newTotalScore);
        vo.setOriginalRating(originalRating);
        vo.setAdjustedRating(newRating);
        vo.setRatingChanged(!originalRating.equals(newRating));

        // 10. 判断是否影响评优资格(假设A等级才能评优)
        vo.setAffectsEvaluation("A".equals(newRating) != "A".equals(originalRating));

        log.info("排名重算完成: classId={}, 分数变化={}, 年级排名变化={}, 全校排名变化={}",
                classId, vo.getScoreChange(), vo.getGradeRankChange(), vo.getSchoolRankChange());

        return vo;
    }

    /**
     * 重新计算排名(处理并列情况)
     *
     * @param stats 班级统计列表(已按分数降序排序)
     * @param isGradeRanking true=年级排名, false=全校排名
     */
    private void recalculateRankings(List<CheckRecordClassStatsNew> stats, boolean isGradeRanking) {
        if (stats == null || stats.isEmpty()) {
            return;
        }

        int rank = 1;
        int sameRankCount = 0;
        BigDecimal lastScore = null;

        for (int i = 0; i < stats.size(); i++) {
            CheckRecordClassStatsNew stat = stats.get(i);
            BigDecimal currentScore = stat.getTotalScore() != null ? stat.getTotalScore() : BigDecimal.ZERO;

            if (lastScore != null && currentScore.compareTo(lastScore) == 0) {
                // 分数相同,排名不变
                sameRankCount++;
            } else {
                // 分数不同,排名 = 当前位置 + 1
                rank = i + 1;
                sameRankCount = 0;
            }

            if (isGradeRanking) {
                stat.setGradeRanking(rank);
            } else {
                stat.setOverallRanking(rank);
            }

            // 更新数据库
            updateClassStatsScoreAndRanking(
                    stat.getId(),
                    stat.getTotalScore(),
                    stat.getOverallRanking(),
                    stat.getGradeRanking()
            );

            lastScore = currentScore;
        }
    }

    /**
     * 根据分数确定等级
     */
    private String determineRating(BigDecimal score) {
        if (score == null) {
            return "D";
        }
        BigDecimal thresholdA = getThreshold(CONFIG_KEY_RATING_A, DEFAULT_THRESHOLD_A);
        BigDecimal thresholdB = getThreshold(CONFIG_KEY_RATING_B, DEFAULT_THRESHOLD_B);
        BigDecimal thresholdC = getThreshold(CONFIG_KEY_RATING_C, DEFAULT_THRESHOLD_C);

        if (score.compareTo(thresholdA) >= 0) {
            return "A";
        } else if (score.compareTo(thresholdB) >= 0) {
            return "B";
        } else if (score.compareTo(thresholdC) >= 0) {
            return "C";
        } else {
            return "D";
        }
    }

    /**
     * 获取等级阈值配置
     */
    private BigDecimal getThreshold(String configKey, String defaultValue) {
        String value = systemConfigService.getConfigValue(configKey, defaultValue);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("等级阈值配置格式错误: key={}, value={}, 使用默认值: {}", configKey, value, defaultValue);
            return new BigDecimal(defaultValue);
        }
    }

    @Override
    public IPage<AppealListVO> listAppeals(Integer pageNum, Integer pageSize,
                                            Long gradeId, Long classId, Long appellantId,
                                            Integer status, Integer appealType,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            String keyword) {
        log.info("查询申诉列表: pageNum={}, pageSize={}, gradeId={}, classId={}, status={}, appealType={}, keyword={}",
                pageNum, pageSize, gradeId, classId, status, appealType, keyword);

        Page<CheckItemAppeal> page = new Page<>(pageNum, pageSize);

        // 使用带条件的分页查询方法
        IPage<CheckItemAppeal> result = appealMapper.selectAppealPageWithDetails(
                page, gradeId, classId, appellantId, status, appealType, startTime, endTime, keyword);

        // 转换为VO并设置额外字段
        List<AppealListVO> voList = result.getRecords().stream()
                .map(this::convertToListVOWithDetails)
                .collect(Collectors.toList());

        IPage<AppealListVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(voList);

        log.info("查询申诉列表完成: 总数={}", result.getTotal());
        return voPage;
    }

    /**
     * 转换为AppealListVO并设置额外计算字段
     */
    private AppealListVO convertToListVOWithDetails(CheckItemAppeal appeal) {
        AppealListVO vo = new AppealListVO();
        BeanUtils.copyProperties(appeal, vo);

        // 设置申诉人姓名
        vo.setApplicantName(appeal.getAppellantName());

        // 设置申诉原因和期望分数
        vo.setReason(appeal.getAppealReason());
        vo.setExpectedScore(appeal.getExpectedScore());

        // 设置是否超时(超过48小时未审批)
        if (AppealStatus.PENDING.getCode().equals(appeal.getStatus()) && appeal.getAppealTime() != null) {
            long hours = java.time.Duration.between(appeal.getAppealTime(), LocalDateTime.now()).toHours();
            vo.setIsTimeout(hours > 48);
        } else {
            vo.setIsTimeout(false);
        }

        // 设置是否公示中
        vo.setInPublicity(AppealStatus.IN_PUBLICITY.getCode().equals(appeal.getStatus()));

        // 设置公示剩余天数
        if (AppealStatus.IN_PUBLICITY.getCode().equals(appeal.getStatus()) && appeal.getPublicityEndTime() != null) {
            long days = java.time.Duration.between(LocalDateTime.now(), appeal.getPublicityEndTime()).toDays();
            vo.setPublicityRemainingDays((int) Math.max(0, days));
        }

        // 设置是否可撤销(只有待审核状态可撤销)
        vo.setCanWithdraw(AppealStatus.canWithdraw(appeal.getStatus()));

        // 设置总步骤数(默认2步审批)
        vo.setTotalSteps(2);

        return vo;
    }

    @Override
    public List<AppealListVO> listPendingAppeals(Long approverId) {
        log.info("查询待审核申诉列表: approverId={}", approverId);

        // 查询当前用户需要审核的申诉(status=1待审核,且当前审批人是approverId)
        List<CheckItemAppeal> appeals = appealMapper.selectPendingAppealsByApprover(approverId);

        // 使用包含详细信息的转换方法
        return appeals.stream()
                .map(this::convertToListVOWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppealListVO> listPublicityAppeals() {
        log.info("查询公示中的申诉列表");

        // 查询状态为6(公示中)的申诉
        List<CheckItemAppeal> appeals = appealMapper.selectPublicityAppeals();

        // 使用包含详细信息的转换方法
        return appeals.stream()
                .map(this::convertToListVOWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppealListVO> listAppealHistory(Long classId, Integer limit) {
        log.info("查询班级申诉历史: classId={}, limit={}", classId, limit);

        if (classId == null) {
            log.warn("班级ID不能为空");
            return new ArrayList<>();
        }

        // 使用Mapper中的方法查询班级申诉历史
        List<CheckItemAppeal> appeals = appealMapper.selectAppealHistoryByClass(classId, limit);

        List<AppealListVO> result = appeals.stream()
                .map(this::convertToListVOWithDetails)
                .collect(Collectors.toList());

        log.info("查询班级申诉历史完成: classId={}, 数量={}", classId, result.size());
        return result;
    }

    @Override
    public CheckItemAppealDTO getAppealDetail(Long appealId) {
        CheckItemAppeal appeal = getById(appealId);
        return appeal != null ? convertToDTO(appeal) : null;
    }

    @Override
    public CheckItemAppealDTO getAppealByCode(String appealCode) {
        CheckItemAppeal appeal = appealMapper.selectByAppealCode(appealCode);
        return appeal != null ? convertToDTO(appeal) : null;
    }

    @Override
    public String checkCanAppeal(Long itemId) {
        log.info("检查是否可以申诉: itemId={}", itemId);

        if (itemId == null) {
            return "扣分明细ID不能为空";
        }

        // 检查该扣分明细是否已有未完成的申诉
        // 排除的状态: 已撤销(4)、已驳回(3)、已过期(5)
        List<Integer> excludeStatuses = Arrays.asList(3, 4, 5);
        int existingCount = appealMapper.checkItemHasAppeal(itemId, excludeStatuses);

        if (existingCount > 0) {
            return "该扣分项已存在进行中的申诉,请勿重复申诉";
        }

        // 可以申诉
        return null;
    }

    @Override
    public String checkCanWithdraw(Long appealId, Long userId) {
        log.info("检查是否可以撤销申诉: appealId={}, userId={}", appealId, userId);

        if (appealId == null) {
            return "申诉ID不能为空";
        }

        CheckItemAppeal appeal = getById(appealId);
        if (appeal == null) {
            return "申诉不存在";
        }

        // 只有待审核状态可以撤销
        if (!AppealStatus.canWithdraw(appeal.getStatus())) {
            return "只有待审核状态的申诉可以撤销";
        }

        // 验证操作者是申诉发起人
        if (userId != null && !userId.equals(appeal.getAppellantId())) {
            return "只有申诉发起人可以撤销申诉";
        }

        // 可以撤销
        return null;
    }

    @Override
    public String checkCanReview(Long appealId, Long approverId) {
        log.info("检查是否可以审核申诉: appealId={}, approverId={}", appealId, approverId);

        if (appealId == null) {
            return "申诉ID不能为空";
        }

        CheckItemAppeal appeal = getById(appealId);
        if (appeal == null) {
            return "申诉不存在";
        }

        // 只有待审核状态可以审核
        if (!AppealStatus.canReview(appeal.getStatus())) {
            return "该申诉不在待审核状态";
        }

        // 审核人不能是申诉人
        if (approverId != null && approverId.equals(appeal.getAppellantId())) {
            return "不能审核自己发起的申诉";
        }

        // 可以审核
        return null;
    }

    @Override
    public AppealStatisticsVO getAppealStatistics(String scope, Long scopeId, String period) {
        log.info("获取申诉统计: scope={}, scopeId={}, period={}", scope, scopeId, period);

        AppealStatisticsVO vo = new AppealStatisticsVO();
        vo.setScope(scope);
        vo.setScopeId(scopeId);
        vo.setPeriod(period);

        // 根据范围和周期查询申诉数据
        List<CheckItemAppeal> appeals = appealMapper.selectAppealsByScope(scope, scopeId, period);

        if (appeals.isEmpty()) {
            // 返回初始化的空统计
            initEmptyStatistics(vo);
            return vo;
        }

        // 按状态统计
        Map<Integer, Long> statusCounts = appeals.stream()
                .collect(Collectors.groupingBy(CheckItemAppeal::getStatus, Collectors.counting()));

        vo.setTotalAppeals(appeals.size());
        vo.setPendingAppeals(statusCounts.getOrDefault(1, 0L).intValue());      // 待审核
        vo.setApprovedAppeals(statusCounts.getOrDefault(2, 0L).intValue());     // 审核通过
        vo.setRejectedAppeals(statusCounts.getOrDefault(3, 0L).intValue());     // 审核驳回
        vo.setWithdrawnAppeals(statusCounts.getOrDefault(4, 0L).intValue());    // 已撤销
        vo.setExpiredAppeals(statusCounts.getOrDefault(5, 0L).intValue());      // 已过期
        vo.setPublicityAppeals(statusCounts.getOrDefault(6, 0L).intValue());    // 公示中
        vo.setEffectiveAppeals(statusCounts.getOrDefault(7, 0L).intValue());    // 已生效

        // 计算审核通过率
        int processedCount = vo.getApprovedAppeals() + vo.getRejectedAppeals();
        if (processedCount > 0) {
            BigDecimal approvalRate = BigDecimal.valueOf(vo.getApprovedAppeals())
                    .divide(BigDecimal.valueOf(processedCount), 4, RoundingMode.HALF_UP);
            vo.setApprovalRate(approvalRate);
        } else {
            vo.setApprovalRate(BigDecimal.ZERO);
        }

        // 按申诉类型统计
        Map<String, Integer> appealTypeStats = new HashMap<>();
        Map<Integer, Long> typeCounts = appeals.stream()
                .filter(a -> a.getAppealType() != null)
                .collect(Collectors.groupingBy(CheckItemAppeal::getAppealType, Collectors.counting()));
        typeCounts.forEach((type, count) -> appealTypeStats.put(getAppealTypeName(type), count.intValue()));
        vo.setAppealTypeStats(appealTypeStats);

        // 按状态统计(字符串形式)
        Map<String, Integer> statusStats = new HashMap<>();
        statusStats.put("待审核", vo.getPendingAppeals());
        statusStats.put("审核通过", vo.getApprovedAppeals());
        statusStats.put("审核驳回", vo.getRejectedAppeals());
        statusStats.put("已撤销", vo.getWithdrawnAppeals());
        statusStats.put("已过期", vo.getExpiredAppeals());
        statusStats.put("公示中", vo.getPublicityAppeals());
        statusStats.put("已生效", vo.getEffectiveAppeals());
        vo.setStatusStats(statusStats);

        // 计算分数调整统计
        List<BigDecimal> scoreChanges = appeals.stream()
                .filter(a -> a.getScoreChange() != null && AppealStatus.EFFECTIVE.getCode().equals(a.getStatus())) // 只统计已生效的
                .map(CheckItemAppeal::getScoreChange)
                .collect(Collectors.toList());

        if (!scoreChanges.isEmpty()) {
            BigDecimal total = scoreChanges.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setTotalScoreAdjustment(total);
            vo.setAverageScoreAdjustment(total.divide(BigDecimal.valueOf(scoreChanges.size()), 2, RoundingMode.HALF_UP));
            vo.setMaxScoreAdjustment(Collections.max(scoreChanges));
            vo.setMinScoreAdjustment(Collections.min(scoreChanges));
        }

        // 计算平均审批耗时(已审批完成的)
        List<Double> approvalHours = appeals.stream()
                .filter(a -> a.getFinalReviewTime() != null && a.getAppealTime() != null)
                .map(a -> (double) java.time.Duration.between(a.getAppealTime(), a.getFinalReviewTime()).toHours())
                .collect(Collectors.toList());

        if (!approvalHours.isEmpty()) {
            double avgHours = approvalHours.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            vo.setAverageApprovalHours(avgHours);
        }

        log.info("申诉统计完成: 总数={}, 通过率={}%", vo.getTotalAppeals(),
                vo.getApprovalRate() != null ? vo.getApprovalRate().multiply(BigDecimal.valueOf(100)) : 0);
        return vo;
    }

    /**
     * 初始化空统计数据
     */
    private void initEmptyStatistics(AppealStatisticsVO vo) {
        vo.setTotalAppeals(0);
        vo.setPendingAppeals(0);
        vo.setApprovedAppeals(0);
        vo.setRejectedAppeals(0);
        vo.setWithdrawnAppeals(0);
        vo.setExpiredAppeals(0);
        vo.setPublicityAppeals(0);
        vo.setEffectiveAppeals(0);
        vo.setApprovalRate(BigDecimal.ZERO);
        vo.setAppealTypeStats(new HashMap<>());
        vo.setStatusStats(new HashMap<>());
    }

    /**
     * 获取申诉类型名称
     */
    private String getAppealTypeName(Integer type) {
        return AppealType.getDescByCode(type);
    }

    @Override
    public String generateAppealCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "APPEAL-" + date + "-" + System.currentTimeMillis() % 10000;
    }

    @Override
    public CheckItemAppealDTO convertToDTO(CheckItemAppeal appeal) {
        if (appeal == null) {
            return null;
        }
        CheckItemAppealDTO dto = new CheckItemAppealDTO();
        BeanUtils.copyProperties(appeal, dto);
        return dto;
    }

    @Override
    public AppealListVO convertToListVO(CheckItemAppeal appeal) {
        if (appeal == null) {
            return null;
        }
        AppealListVO vo = new AppealListVO();
        BeanUtils.copyProperties(appeal, vo);
        return vo;
    }

    @Override
    public List<CheckItemAppealDTO> convertToDTOList(List<CheckItemAppeal> appeals) {
        return appeals.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AppealListVO> convertToListVOList(List<CheckItemAppeal> appeals) {
        return appeals.stream().map(this::convertToListVO).collect(Collectors.toList());
    }

    // ========== 辅助方法：替代旧Mapper的方法 ==========

    /**
     * 更新扣分明细的申诉状态
     */
    private void updateDeductionAppealStatus(Long deductionId, Integer appealStatus, Long appealId) {
        CheckRecordDeductionNew deduction = deductionMapper.selectById(deductionId);
        if (deduction != null) {
            deduction.setAppealStatus(appealStatus);
            deduction.setAppealId(appealId);
            deductionMapper.updateById(deduction);
        }
    }

    /**
     * 更新扣分明细的实际扣分
     */
    private void updateDeductionScore(Long deductionId, BigDecimal newScore) {
        CheckRecordDeductionNew deduction = deductionMapper.selectById(deductionId);
        if (deduction != null) {
            deduction.setActualScore(newScore);
            deductionMapper.updateById(deduction);
        }
    }

    /**
     * 汇总某记录某班级的总扣分
     */
    private BigDecimal sumDeductScoreByRecordAndClass(Long recordId, Long classId) {
        List<CheckRecordDeductionNew> deductions = deductionMapper.selectList(
                new LambdaQueryWrapper<CheckRecordDeductionNew>()
                        .eq(CheckRecordDeductionNew::getRecordId, recordId)
                        .eq(CheckRecordDeductionNew::getClassId, classId)
        );
        return deductions.stream()
                .map(d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 更新班级统计的分数和排名
     */
    private void updateClassStatsScoreAndRanking(Long classStatId, BigDecimal totalScore, Integer ranking, Integer gradeRanking) {
        CheckRecordClassStatsNew classStat = classStatsMapper.selectById(classStatId);
        if (classStat != null) {
            classStat.setTotalScore(totalScore);
            classStat.setOverallRanking(ranking);
            classStat.setGradeRanking(gradeRanking);
            classStatsMapper.updateById(classStat);
        }
    }

    /**
     * 更新班级统计的申诉计数
     * 注：新版schema没有appealRejected字段
     */
    private void updateClassStatsAppealCount(Long classStatId, Integer appealCount, Integer appealApproved, Integer appealRejected) {
        CheckRecordClassStatsNew classStat = classStatsMapper.selectById(classStatId);
        if (classStat != null) {
            classStat.setAppealCount(appealCount);
            classStat.setAppealApproved(appealApproved);
            // appealRejected 字段在新版schema中不存在，忽略此参数
            classStatsMapper.updateById(classStat);
        }
    }
}
