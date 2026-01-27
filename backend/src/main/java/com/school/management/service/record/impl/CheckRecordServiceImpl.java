package com.school.management.service.record.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.record.*;
import com.school.management.entity.CheckCategory;
import com.school.management.entity.CheckTemplate;
import com.school.management.entity.Class;
import com.school.management.entity.DailyCheck;
import com.school.management.entity.DailyCheckDetail;
import com.school.management.entity.User;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.entity.record.CheckRecordClassStatsNew;
import com.school.management.entity.record.CheckRecordCategoryStatsNew;
import com.school.management.entity.record.CheckRecordDeductionNew;
import com.school.management.enums.DailyCheckStatus;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.CheckCategoryMapper;
import com.school.management.mapper.CheckTemplateMapper;
import com.school.management.mapper.DailyCheckMapper;
import com.school.management.mapper.DailyCheckDetailMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.mapper.record.CheckRecordMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.record.CheckRecordCategoryStatsMapper;
import com.school.management.mapper.record.CheckRecordDeductionMapper;
import com.school.management.mapper.record.CheckRecordAppealMapper;
import com.school.management.mapper.TemplateCategoryMapper;
import com.school.management.mapper.DeductionItemMapper;
import com.school.management.mapper.DailyCheckTargetMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.entity.TemplateCategory;
import com.school.management.entity.DeductionItem;
import com.school.management.entity.DailyCheckTarget;
import com.school.management.entity.Department;
import com.school.management.service.record.CheckRecordService;
import com.school.management.annotation.DataPermission;
import com.school.management.service.DailyCheckWeightConfigService;
import com.school.management.dto.DailyCheckWeightConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.school.management.service.ClassWeightService;
import com.school.management.dto.ClassWeightResultDTO;
import com.school.management.event.CheckRecordPublishedEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 检查记录服务实现类（重构版）
 *
 * @author system
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckRecordServiceImpl implements CheckRecordService {

    private final CheckRecordMapper checkRecordMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;
    private final CheckRecordCategoryStatsMapper categoryStatsMapper;
    private final CheckRecordDeductionMapper deductionMapper;
    private final CheckRecordAppealMapper appealMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckDetailMapper dailyCheckDetailMapper;
    private final ClassMapper classMapper;
    private final UserMapper userMapper;
    private final CheckCategoryMapper checkCategoryMapper;
    private final CheckTemplateMapper checkTemplateMapper;
    private final ObjectMapper objectMapper;
    private final ClassWeightService classWeightService;
    private final StudentMapper studentMapper;
    private final TemplateCategoryMapper templateCategoryMapper;
    private final DeductionItemMapper deductionItemMapper;
    private final DailyCheckTargetMapper dailyCheckTargetMapper;
    private final DepartmentMapper departmentMapper;
    private final DailyCheckWeightConfigService dailyCheckWeightConfigService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @DataPermission(module = "check_record", classField = "classIds")
    public IPage<CheckRecordDTO> queryPage(CheckRecordQueryDTO query) {
        // 调试日志
        log.info("查询检查记录 - planId: {}, pageNum: {}, pageSize: {}, classIds: {}",
                query.getPlanId(), query.getPageNum(), query.getPageSize(), query.getClassIds());

        // 数据权限过滤由AOP自动处理
        Page<CheckRecordNew> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<CheckRecordNew> result = checkRecordMapper.selectRecordPage(
                page,
                query.getCheckName(),
                query.getCheckDateStart(),
                query.getCheckDateEnd(),
                query.getStatus(),
                query.getCheckType(),
                query.getPlanId(),
                query.getClassIds()
        );

        log.info("查询结果 - total: {}, records: {}", result.getTotal(), result.getRecords().size());
        return result.convert(this::convertToDTO);
    }

    @Override
    public CheckRecordDTO getById(Long recordId) {
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }
        return convertToDTO(record);
    }

    @Override
    public CheckRecordDTO getDetailById(Long recordId) {
        // 先获取检查记录实体以获取检查日期
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        CheckRecordDTO dto = convertToDTO(record);

        // 加载班级统计（包含扣分明细和加权信息）
        List<CheckRecordClassStatsDTO> classStats = getClassStatsListWithDeductions(recordId, record.getCheckDate());
        dto.setClassStats(classStats);

        return dto;
    }

    /**
     * 获取班级统计列表并加载扣分明细和加权信息
     */
    private List<CheckRecordClassStatsDTO> getClassStatsListWithDeductions(Long recordId, LocalDate checkDate) {
        List<CheckRecordClassStatsNew> stats = classStatsMapper.selectByRecordId(recordId);
        return stats.stream()
                .map(stat -> {
                    CheckRecordClassStatsDTO dto = convertClassStatsToDTO(stat);
                    // 加载扣分明细
                    List<CheckRecordDeductionNew> deductions = deductionMapper.selectByClassStatId(stat.getId());
                    List<CheckRecordDeductionDTO> deductionDTOs = deductions.stream()
                            .map(this::convertDeductionToDTO)
                            .collect(Collectors.toList());
                    dto.setDeductions(deductionDTOs);

                    // 加载类别统计（用于加权计算）
                    List<CheckRecordCategoryStatsNew> categoryStats = categoryStatsMapper.selectByClassStatId(stat.getId());
                    List<CheckRecordCategoryStatsDTO> categoryStatsDTOs = categoryStats.stream()
                            .map(this::convertCategoryStatsToDTO)
                            .collect(Collectors.toList());

                    // 将扣分明细按类别ID分组并添加到类别统计中
                    Map<Long, List<CheckRecordDeductionDTO>> deductionsByCategoryId = deductionDTOs.stream()
                            .filter(d -> d.getCategoryId() != null)
                            .collect(Collectors.groupingBy(CheckRecordDeductionDTO::getCategoryId));

                    for (CheckRecordCategoryStatsDTO catStat : categoryStatsDTOs) {
                        if (catStat.getCategoryId() != null) {
                            catStat.setDeductions(deductionsByCategoryId.getOrDefault(catStat.getCategoryId(), new ArrayList<>()));
                        }
                    }

                    dto.setCategoryStats(categoryStatsDTOs);
                    // 添加加权信息
                    enrichWithWeightInfo(dto, checkDate, recordId);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CheckRecordDTO getByDailyCheckId(Long dailyCheckId) {
        CheckRecordNew record = checkRecordMapper.selectByDailyCheckId(dailyCheckId);
        if (record == null) {
            return null;
        }
        return convertToDTO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckRecordNew generateFromDailyCheck(Long dailyCheckId, Long operatorId, String operatorName) {
        log.info("开始从日常检查生成检查记录, dailyCheckId={}", dailyCheckId);

        // 1. 检查日常检查是否存在
        DailyCheck dailyCheck = dailyCheckMapper.selectById(dailyCheckId);
        if (dailyCheck == null) {
            throw new BusinessException("日常检查不存在");
        }

        // 2. 检查是否已生成过记录
        CheckRecordNew existingRecord = checkRecordMapper.selectByDailyCheckId(dailyCheckId);
        if (existingRecord != null) {
            log.warn("该日常检查已生成过检查记录, recordId={}", existingRecord.getId());
            return existingRecord;
        }

        // 3. 获取所有打分明细
        LambdaQueryWrapper<DailyCheckDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(DailyCheckDetail::getCheckId, dailyCheckId)
                     .eq(DailyCheckDetail::getDeleted, 0);
        List<DailyCheckDetail> details = dailyCheckDetailMapper.selectList(detailWrapper);

        if (details.isEmpty()) {
            log.warn("没有打分记录，生成空检查记录");
        }

        // 4. 批量加载上下文数据（避免N+1查询）
        ConversionContext context = loadConversionContext(details);

        // 4.1 获取所有检查目标班级（即使没有扣分也要创建统计记录）
        LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(DailyCheckTarget::getCheckId, dailyCheckId)
                     .eq(DailyCheckTarget::getTargetType, 1);  // 1=班级
        List<DailyCheckTarget> targets = dailyCheckTargetMapper.selectList(targetWrapper);

        // 将目标班级加入context.classMap
        if (!targets.isEmpty()) {
            List<Long> targetClassIds = targets.stream()
                    .map(DailyCheckTarget::getTargetId)
                    .collect(Collectors.toList());
            List<Class> targetClasses = classMapper.selectBatchIds(targetClassIds);
            for (Class c : targetClasses) {
                if (!context.classMap.containsKey(c.getId())) {
                    context.classMap.put(c.getId(), c);
                    // 如果这个班级没有扣分记录，给它创建空的扣分列表
                    if (!context.classDetailsMap.containsKey(c.getId())) {
                        context.classDetailsMap.put(c.getId(), new ArrayList<>());
                    }
                }
            }
            log.info("检查目标包含{}个班级，总共{}个班级需要生成统计", targets.size(), context.classMap.size());
        }

        // 5. 获取模板名称
        String templateName = null;
        if (dailyCheck.getTemplateId() != null) {
            CheckTemplate template = checkTemplateMapper.selectById(dailyCheck.getTemplateId());
            if (template != null) {
                templateName = template.getTemplateName();
            }
        }

        // 6. 创建检查记录主表
        CheckRecordNew record = new CheckRecordNew();
        record.setRecordCode(generateRecordCode(dailyCheck.getCheckDate()));
        record.setDailyCheckId(dailyCheckId);
        record.setPlanId(dailyCheck.getPlanId());
        record.setCheckName(dailyCheck.getCheckName());
        record.setCheckDate(dailyCheck.getCheckDate());
        record.setCheckType(dailyCheck.getCheckType() != null ? dailyCheck.getCheckType() : CheckRecordNew.CHECK_TYPE_DAILY);
        record.setCheckerId(dailyCheck.getCheckerId() != null ? dailyCheck.getCheckerId() : operatorId);
        record.setCheckerName(dailyCheck.getCheckerName() != null ? dailyCheck.getCheckerName() : operatorName);
        record.setTemplateId(dailyCheck.getTemplateId());
        record.setTemplateName(templateName);
        record.setDescription(dailyCheck.getDescription());
        record.setStatus(CheckRecordNew.STATUS_PUBLISHED);
        record.setPublishTime(LocalDateTime.now());
        record.setSnapshotVersion(1);
        record.setSnapshotCreatedAt(LocalDateTime.now());
        record.setCreatedBy(operatorId);
        record.setDeleted(0);

        // 初始化申诉统计
        record.setTotalAppealCount(0);
        record.setAppealPendingCount(0);
        record.setAppealApprovedCount(0);
        record.setAppealRejectedCount(0);

        // 7. 按班级分组处理（只创建班级统计，类别统计和扣分明细在保存阶段创建）
        List<CheckRecordClassStatsNew> allClassStats = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int totalDeductionCount = details.size(); // 扣分明细数 = 原始检查明细数

        for (Map.Entry<Long, List<DailyCheckDetail>> entry : context.classDetailsMap.entrySet()) {
            Long classId = entry.getKey();
            List<DailyCheckDetail> classDetails = entry.getValue();

            Class classInfo = context.getClassMap().get(classId);
            if (classInfo == null) {
                log.warn("班级不存在, classId={}", classId);
                continue;
            }

            // 创建班级统计
            CheckRecordClassStatsNew classStat = createClassStatsNew(classInfo, classDetails, context);
            allClassStats.add(classStat);
            totalScore = totalScore.add(classStat.getTotalScore());
        }

        // 8. 设置统计信息
        record.setTotalClasses(allClassStats.size());
        record.setTotalDeductionCount(totalDeductionCount);
        record.setTotalDeductionScore(totalScore);

        if (!allClassStats.isEmpty()) {
            BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(allClassStats.size()), 2, RoundingMode.HALF_UP);
            record.setAvgScore(avgScore);

            BigDecimal maxScore = allClassStats.stream()
                    .map(CheckRecordClassStatsNew::getTotalScore)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal minScore = allClassStats.stream()
                    .map(CheckRecordClassStatsNew::getTotalScore)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            record.setMaxScore(maxScore);
            record.setMinScore(minScore);
        } else {
            record.setAvgScore(BigDecimal.ZERO);
            record.setMaxScore(BigDecimal.ZERO);
            record.setMinScore(BigDecimal.ZERO);
        }

        // 9. 保存主记录（先保存以获取ID）
        checkRecordMapper.insert(record);
        Long recordId = record.getId();

        // 10. 保存班级统计（先保存以获取ID）
        for (CheckRecordClassStatsNew stat : allClassStats) {
            stat.setRecordId(recordId);
            classStatsMapper.insert(stat);
        }

        // 11. 创建类别统计和扣分明细（需要班级统计ID）
        for (CheckRecordClassStatsNew classStat : allClassStats) {
            Long classId = classStat.getClassId();
            List<DailyCheckDetail> classDetails = context.classDetailsMap.get(classId);
            if (classDetails == null) continue;

            // 按类别分组
            Map<Long, List<DailyCheckDetail>> detailsByCategory = classDetails.stream()
                    .filter(d -> d.getCategoryId() != null)
                    .collect(Collectors.groupingBy(DailyCheckDetail::getCategoryId));

            Class classInfo = context.getClassMap().get(classId);

            for (Map.Entry<Long, List<DailyCheckDetail>> catEntry : detailsByCategory.entrySet()) {
                Long categoryId = catEntry.getKey();
                List<DailyCheckDetail> catDetails = catEntry.getValue();

                // 按轮次分组
                Map<Integer, List<DailyCheckDetail>> detailsByRound = catDetails.stream()
                        .collect(Collectors.groupingBy(d -> d.getCheckRound() != null ? d.getCheckRound() : 1));

                for (Map.Entry<Integer, List<DailyCheckDetail>> roundEntry : detailsByRound.entrySet()) {
                    Integer round = roundEntry.getKey();
                    List<DailyCheckDetail> roundDetails = roundEntry.getValue();

                    // 创建并保存类别统计
                    CheckRecordCategoryStatsNew categoryStat = createCategoryStatsNew(
                            classStat.getId(), classId, categoryId, round, roundDetails, context);
                    categoryStat.setRecordId(recordId);
                    categoryStatsMapper.insert(categoryStat);

                    // 创建并保存扣分明细
                    for (DailyCheckDetail detail : roundDetails) {
                        CheckRecordDeductionNew deduction = createDeductionNew(
                                classStat.getId(), categoryStat.getId(), detail, classInfo, context);
                        deduction.setRecordId(recordId);
                        deductionMapper.insert(deduction);
                    }
                }
            }
        }

        // 12. 计算排名
        calculateRankings(recordId, allClassStats);

        // 13. 更新日常检查状态为已发布
        dailyCheck.setStatus(DailyCheckStatus.PUBLISHED.getCode());
        dailyCheckMapper.updateById(dailyCheck);

        log.info("检查记录生成完成, recordId={}, dailyCheckId={}, classCount={}, deductionCount={}",
                recordId, dailyCheckId, allClassStats.size(), totalDeductionCount);

        // 14. 发布检查记录发布事件（用于自动触发评级等功能）
        eventPublisher.publishEvent(new CheckRecordPublishedEvent(this, recordId, dailyCheck.getPlanId()));

        return record;
    }

    /**
     * 生成记录编号
     */
    private String generateRecordCode(LocalDate checkDate) {
        String dateStr = checkDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天已有的记录数
        LambdaQueryWrapper<CheckRecordNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("DATE(check_date) = {0}", checkDate)
               .eq(CheckRecordNew::getDeleted, 0);
        long count = checkRecordMapper.selectCount(wrapper);
        return String.format("CR%s%03d", dateStr, count + 1);
    }

    /**
     * 加载转换上下文（批量查询，避免N+1）
     */
    private ConversionContext loadConversionContext(List<DailyCheckDetail> allDetails) {
        ConversionContext context = new ConversionContext();

        if (allDetails.isEmpty()) {
            return context;
        }

        // 按班级分组
        context.classDetailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(DailyCheckDetail::getClassId));
        Set<Long> classIds = context.classDetailsMap.keySet();

        log.info("共涉及{}个班级", classIds.size());

        // 批量查询班级信息
        if (!classIds.isEmpty()) {
            List<Class> classList = classMapper.selectBatchIds(classIds);
            context.classMap = classList.stream()
                    .collect(Collectors.toMap(Class::getId, c -> c));

            // 批量查询班主任信息
            Set<Long> teacherIds = classList.stream()
                    .map(Class::getTeacherId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!teacherIds.isEmpty()) {
                List<User> teacherList = userMapper.selectBatchIds(teacherIds);
                context.teacherMap = teacherList.stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
            }
        }

        // 批量查询所有用到的检查类别
        Set<Long> categoryIds = allDetails.stream()
                .map(DailyCheckDetail::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!categoryIds.isEmpty()) {
            List<CheckCategory> categoryList = checkCategoryMapper.selectBatchIds(categoryIds);
            context.categoryMap = categoryList.stream()
                    .collect(Collectors.toMap(CheckCategory::getId, c -> c));
        }

        // 批量查询组织单元信息
        if (!classIds.isEmpty()) {
            Set<Long> orgUnitIds = context.classMap.values().stream()
                    .map(Class::getOrgUnitId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!orgUnitIds.isEmpty()) {
                List<Department> departmentList = departmentMapper.selectBatchIds(orgUnitIds);
                context.departmentMap = departmentList.stream()
                        .collect(Collectors.toMap(Department::getId, d -> d));
            }
        }

        log.info("上下文加载完成: {}个班级, {}个班主任, {}个类别, {}个组织单元",
                context.classMap.size(), context.teacherMap.size(), context.categoryMap.size(), context.departmentMap.size());

        return context;
    }

    /**
     * 转换上下文类
     */
    private static class ConversionContext {
        Map<Long, List<DailyCheckDetail>> classDetailsMap = new HashMap<>();
        Map<Long, Class> classMap = new HashMap<>();
        Map<Long, User> teacherMap = new HashMap<>();
        Map<Long, CheckCategory> categoryMap = new HashMap<>();
        Map<Long, Department> departmentMap = new HashMap<>();

        public Map<Long, Class> getClassMap() { return classMap; }
        public Map<Long, User> getTeacherMap() { return teacherMap; }
        public Map<Long, CheckCategory> getCategoryMap() { return categoryMap; }
        public Map<Long, Department> getDepartmentMap() { return departmentMap; }
    }

    /**
     * 创建班级统计（新版）
     */
    private CheckRecordClassStatsNew createClassStatsNew(Class classInfo, List<DailyCheckDetail> details,
                                                          ConversionContext context) {
        CheckRecordClassStatsNew stats = new CheckRecordClassStatsNew();
        // recordId稍后设置

        // 班级信息
        stats.setClassId(classInfo.getId());
        stats.setClassName(classInfo.getClassName());
        stats.setGradeId(classInfo.getGradeLevel() != null ? Long.valueOf(classInfo.getGradeLevel()) : null);
        stats.setGradeName(classInfo.getGradeLevel() != null ? classInfo.getGradeLevel() + "级" : null);
        stats.setOrgUnitId(classInfo.getOrgUnitId());

        // 设置组织单元名称（原部门名称）
        if (classInfo.getOrgUnitId() != null) {
            Department department = context.getDepartmentMap().get(classInfo.getOrgUnitId());
            if (department != null) {
                stats.setOrgUnitName(department.getDeptName());
            }
        }
        // 班级人数：优先使用Class表的studentCount，若为空或0则从students表实时统计
        Integer classSize = classInfo.getStudentCount();
        if (classSize == null || classSize == 0) {
            classSize = studentMapper.countStudentsByClassId(classInfo.getId());
        }
        stats.setClassSize(classSize != null ? classSize : 0);

        // 班主任信息
        if (classInfo.getTeacherId() != null) {
            User teacher = context.getTeacherMap().get(classInfo.getTeacherId());
            if (teacher != null) {
                stats.setTeacherId(teacher.getId());
                stats.setTeacherName(teacher.getRealName());
                stats.setTeacherPhone(teacher.getPhone());
            }
        }

        // 计算分数
        stats.setDeductionCount(details.size());
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal hygieneScore = BigDecimal.ZERO;
        BigDecimal disciplineScore = BigDecimal.ZERO;
        BigDecimal attendanceScore = BigDecimal.ZERO;
        BigDecimal dormitoryScore = BigDecimal.ZERO;
        BigDecimal otherScore = BigDecimal.ZERO;
        int appealCount = 0;

        for (DailyCheckDetail detail : details) {
            BigDecimal score = detail.getDeductScore() != null ? detail.getDeductScore() : BigDecimal.ZERO;
            totalScore = totalScore.add(score);

            // 统计申诉
            if (detail.getAppealStatus() != null && detail.getAppealStatus() > 0) {
                appealCount++;
            }

            // 根据类别分类统计
            if (detail.getCategoryId() != null) {
                CheckCategory category = context.getCategoryMap().get(detail.getCategoryId());
                if (category != null) {
                    String categoryName = category.getCategoryName();
                    if (categoryName != null) {
                        if (categoryName.contains("卫生")) {
                            hygieneScore = hygieneScore.add(score);
                        } else if (categoryName.contains("纪律")) {
                            disciplineScore = disciplineScore.add(score);
                        } else if (categoryName.contains("考勤") || categoryName.contains("早操")) {
                            attendanceScore = attendanceScore.add(score);
                        } else if (categoryName.contains("宿舍")) {
                            dormitoryScore = dormitoryScore.add(score);
                        } else {
                            otherScore = otherScore.add(score);
                        }
                    }
                }
            }
        }

        stats.setTotalScore(totalScore);
        stats.setHygieneScore(hygieneScore);
        stats.setDisciplineScore(disciplineScore);
        stats.setAttendanceScore(attendanceScore);
        stats.setDormitoryScore(dormitoryScore);
        stats.setOtherScore(otherScore);
        stats.setAppealCount(appealCount);
        stats.setAppealApproved(0);
        stats.setAppealPending(0);

        return stats;
    }

    /**
     * 创建类别统计（新版）
     */
    private CheckRecordCategoryStatsNew createCategoryStatsNew(Long classStatId, Long classId,
                                                                Long categoryId, Integer round,
                                                                List<DailyCheckDetail> details,
                                                                ConversionContext context) {
        CheckRecordCategoryStatsNew stats = new CheckRecordCategoryStatsNew();
        // recordId稍后设置
        stats.setClassStatId(classStatId);
        stats.setClassId(classId);
        stats.setCategoryId(categoryId);
        stats.setCheckRound(round);
        stats.setDeductionCount(details.size());

        // 获取类别信息
        CheckCategory category = context.getCategoryMap().get(categoryId);
        if (category != null) {
            stats.setCategoryName(category.getCategoryName());
            stats.setCategoryType(getCategoryType(category.getCategoryName()));
        }

        // 计算总分
        BigDecimal totalScore = details.stream()
                .map(d -> d.getDeductScore() != null ? d.getDeductScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalScore(totalScore);

        return stats;
    }

    /**
     * 创建扣分明细（新版）
     */
    private CheckRecordDeductionNew createDeductionNew(Long classStatId, Long categoryStatId,
                                                        DailyCheckDetail detail, Class classInfo,
                                                        ConversionContext context) {
        CheckRecordDeductionNew deduction = new CheckRecordDeductionNew();
        // recordId稍后设置
        deduction.setClassStatId(classStatId);
        deduction.setCategoryStatId(categoryStatId);
        deduction.setDailyCheckDetailId(detail.getId());

        // 班级信息
        deduction.setClassId(detail.getClassId());
        deduction.setClassName(classInfo.getClassName());

        // 类别信息
        deduction.setCategoryId(detail.getCategoryId());
        CheckCategory category = context.getCategoryMap().get(detail.getCategoryId());
        if (category != null) {
            deduction.setCategoryName(category.getCategoryName());
        }
        deduction.setCheckRound(detail.getCheckRound() != null ? detail.getCheckRound() : 1);

        // 扣分项信息
        deduction.setDeductionItemId(detail.getDeductionItemId());
        deduction.setDeductionItemName(detail.getDeductionItemName());

        // 扣分模式与计算
        deduction.setDeductMode(detail.getDeductMode() != null ? detail.getDeductMode() : 1);
        deduction.setActualScore(detail.getDeductScore() != null ? detail.getDeductScore() : BigDecimal.ZERO);

        // 涉及人员
        deduction.setPersonCount(detail.getPersonCount() != null ? detail.getPersonCount() : 0);
        deduction.setStudentIds(detail.getStudentIds());
        deduction.setStudentNames(detail.getStudentNames());

        // 关联对象
        deduction.setLinkType(detail.getLinkType() != null ? detail.getLinkType() : 0);
        deduction.setLinkId(detail.getLinkId());
        deduction.setLinkCode(detail.getLinkNo());

        // 证据材料 - 统一使用photoUrls字段
        String photoUrls = detail.getPhotoUrls();
        if (photoUrls == null || photoUrls.isEmpty()) {
            photoUrls = detail.getImages();
        }
        deduction.setPhotoUrls(photoUrls);
        if (photoUrls != null && !photoUrls.isEmpty()) {
            try {
                List<?> urls = objectMapper.readValue(photoUrls, List.class);
                deduction.setPhotoCount(urls.size());
            } catch (Exception e) {
                deduction.setPhotoCount(0);
            }
        } else {
            deduction.setPhotoCount(0);
        }

        // 备注 - 统一使用remark字段
        String remark = detail.getRemark();
        if (remark == null || remark.isEmpty()) {
            remark = detail.getDescription();
        }
        deduction.setRemark(remark);

        // 检查人信息
        deduction.setCheckerId(detail.getCheckerId());
        deduction.setCheckTime(detail.getCheckTime());

        // 申诉状态初始化
        deduction.setAppealStatus(detail.getAppealStatus() != null ? detail.getAppealStatus() : 0);

        // 修订信息
        deduction.setIsRevised(detail.getIsRevised() != null ? detail.getIsRevised() : 0);
        if (detail.getIsRevised() != null && detail.getIsRevised() == 1) {
            deduction.setOriginalScore(detail.getOriginalScore());
            deduction.setRevisedScore(detail.getRevisedScore());
            deduction.setRevisionReason(detail.getRevisionReason());
        }

        return deduction;
    }

    /**
     * 获取类别类型
     */
    private String getCategoryType(String categoryName) {
        if (categoryName == null) return "OTHER";
        if (categoryName.contains("卫生")) return "HYGIENE";
        if (categoryName.contains("纪律")) return "DISCIPLINE";
        if (categoryName.contains("考勤") || categoryName.contains("早操")) return "ATTENDANCE";
        if (categoryName.contains("宿舍")) return "DORMITORY";
        return "OTHER";
    }

    /**
     * 计算排名
     */
    private void calculateRankings(Long recordId, List<CheckRecordClassStatsNew> allClassStats) {
        if (allClassStats.isEmpty()) return;

        // 全校排名（按扣分从低到高）
        List<CheckRecordClassStatsNew> sortedByScore = new ArrayList<>(allClassStats);
        sortedByScore.sort(Comparator.comparing(CheckRecordClassStatsNew::getTotalScore));

        // 计算平均分
        BigDecimal avgScore = allClassStats.stream()
                .map(CheckRecordClassStatsNew::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allClassStats.size()), 2, RoundingMode.HALF_UP);

        for (int i = 0; i < sortedByScore.size(); i++) {
            CheckRecordClassStatsNew stat = sortedByScore.get(i);
            stat.setOverallRanking(i + 1);

            // 计算等级
            if (stat.getTotalScore().compareTo(BigDecimal.ZERO) == 0) {
                stat.setScoreLevel(CheckRecordClassStatsNew.LEVEL_EXCELLENT);
            } else if (stat.getTotalScore().compareTo(new BigDecimal("3")) < 0) {
                stat.setScoreLevel(CheckRecordClassStatsNew.LEVEL_GOOD);
            } else if (stat.getTotalScore().compareTo(new BigDecimal("5")) < 0) {
                stat.setScoreLevel(CheckRecordClassStatsNew.LEVEL_AVERAGE);
            } else {
                stat.setScoreLevel(CheckRecordClassStatsNew.LEVEL_POOR);
            }

            // 与平均分差值
            stat.setVsAvgDiff(stat.getTotalScore().subtract(avgScore));

            classStatsMapper.updateById(stat);
        }

        // 年级内排名
        Map<Long, List<CheckRecordClassStatsNew>> gradeMap = allClassStats.stream()
                .filter(s -> s.getGradeId() != null)
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getGradeId));

        for (List<CheckRecordClassStatsNew> gradeStats : gradeMap.values()) {
            gradeStats.sort(Comparator.comparing(CheckRecordClassStatsNew::getTotalScore));
            for (int i = 0; i < gradeStats.size(); i++) {
                CheckRecordClassStatsNew stat = gradeStats.get(i);
                stat.setGradeRanking(i + 1);
                classStatsMapper.updateById(stat);
            }
        }

        // 组织单元内排名（原院系内排名）
        Map<Long, List<CheckRecordClassStatsNew>> deptMap = allClassStats.stream()
                .filter(s -> s.getOrgUnitId() != null)
                .collect(Collectors.groupingBy(CheckRecordClassStatsNew::getOrgUnitId));

        for (List<CheckRecordClassStatsNew> deptStats : deptMap.values()) {
            deptStats.sort(Comparator.comparing(CheckRecordClassStatsNew::getTotalScore));
            for (int i = 0; i < deptStats.size(); i++) {
                CheckRecordClassStatsNew stat = deptStats.get(i);
                stat.setOrgUnitRanking(i + 1);
                classStatsMapper.updateById(stat);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long recordId) {
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        record.setStatus(CheckRecordNew.STATUS_ARCHIVED);
        record.setArchiveTime(LocalDateTime.now());
        checkRecordMapper.updateById(record);

        log.info("检查记录已归档: recordId={}", recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long recordId) {
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        // 逻辑删除
        record.setDeleted(1);
        checkRecordMapper.updateById(record);

        log.info("检查记录已删除: recordId={}", recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculate(Long recordId, String reason) {
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        // 重新计算班级统计
        List<CheckRecordClassStatsNew> classStats = classStatsMapper.selectByRecordId(recordId);
        for (CheckRecordClassStatsNew classStat : classStats) {
            classStatsMapper.recalculateScore(classStat.getId());
        }

        // 重新计算类别统计
        List<CheckRecordCategoryStatsNew> categoryStats = categoryStatsMapper.selectByRecordId(recordId);
        for (CheckRecordCategoryStatsNew categoryStat : categoryStats) {
            categoryStatsMapper.recalculateScore(categoryStat.getId());
        }

        // 重新计算主记录
        checkRecordMapper.recalculateTotalScore(recordId);

        // 更新排名
        classStatsMapper.updateRankings(recordId);

        // 更新申诉统计
        checkRecordMapper.updateAppealStats(recordId);
        for (CheckRecordClassStatsNew classStat : classStats) {
            classStatsMapper.updateAppealStats(classStat.getId());
        }

        // 更新元数据
        record.setLastRecalcAt(LocalDateTime.now());
        record.setRecalcReason(reason);
        record.setSnapshotVersion(record.getSnapshotVersion() + 1);
        checkRecordMapper.updateById(record);

        log.info("检查记录已重新计算: recordId={}, reason={}", recordId, reason);
    }

    @Override
    public List<CheckRecordClassStatsDTO> getClassStatsList(Long recordId) {
        // 获取检查记录以获取检查日期
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            return new java.util.ArrayList<>();
        }

        List<CheckRecordClassStatsNew> stats = classStatsMapper.selectByRecordId(recordId);
        return stats.stream()
                .map(stat -> {
                    CheckRecordClassStatsDTO dto = convertClassStatsToDTO(stat);
                    // 添加加权信息
                    enrichWithWeightInfo(dto, record.getCheckDate(), recordId);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CheckRecordClassStatsDTO getClassStatsDetail(Long classStatId) {
        CheckRecordClassStatsNew stats = classStatsMapper.selectById(classStatId);
        if (stats == null) {
            throw new BusinessException("班级统计不存在");
        }

        CheckRecordClassStatsDTO dto = convertClassStatsToDTO(stats);

        // 加载类别统计
        List<CheckRecordCategoryStatsNew> categoryStats = categoryStatsMapper.selectByClassStatId(classStatId);
        dto.setCategoryStats(categoryStats.stream()
                .map(this::convertCategoryStatsToDTO)
                .collect(Collectors.toList()));

        // 加载扣分明细
        List<CheckRecordDeductionNew> deductions = deductionMapper.selectByClassStatId(classStatId);
        dto.setDeductions(deductions.stream()
                .map(this::convertDeductionToDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    @Override
    public List<CheckRecordClassStatsDTO> getClassStatsByOrgUnit(Long recordId, Long orgUnitId) {
        List<CheckRecordClassStatsNew> stats = classStatsMapper.selectByOrgUnit(recordId, orgUnitId);
        return stats.stream()
                .map(this::convertClassStatsToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CheckRecordClassStatsDTO> getClassStatsByGrade(Long recordId, Long gradeId) {
        List<CheckRecordClassStatsNew> stats = classStatsMapper.selectByGrade(recordId, gradeId);
        return stats.stream()
                .map(this::convertClassStatsToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CheckRecordDeductionDTO> getDeductionList(Long recordId, Long classId) {
        List<CheckRecordDeductionNew> deductions;
        if (classId != null) {
            deductions = deductionMapper.selectByRecordAndClass(recordId, classId);
        } else {
            deductions = deductionMapper.selectByRecordId(recordId);
        }
        return deductions.stream()
                .map(this::convertDeductionToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CheckRecordDeductionDTO getDeductionById(Long deductionId) {
        CheckRecordDeductionNew deduction = deductionMapper.selectById(deductionId);
        if (deduction == null) {
            throw new BusinessException("扣分明细不存在");
        }
        return convertDeductionToDTO(deduction);
    }

    // ==================== 私有方法 ====================

    private CheckRecordDTO convertToDTO(CheckRecordNew entity) {
        CheckRecordDTO dto = new CheckRecordDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置类型名称
        dto.setCheckTypeName(entity.getCheckType() == CheckRecordNew.CHECK_TYPE_DAILY ? "日常检查" : "专项检查");
        dto.setStatusName(entity.getStatus() == CheckRecordNew.STATUS_PUBLISHED ? "已发布" : "已归档");

        return dto;
    }

    private CheckRecordClassStatsDTO convertClassStatsToDTO(CheckRecordClassStatsNew entity) {
        CheckRecordClassStatsDTO dto = new CheckRecordClassStatsDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private CheckRecordCategoryStatsDTO convertCategoryStatsToDTO(CheckRecordCategoryStatsNew entity) {
        CheckRecordCategoryStatsDTO dto = new CheckRecordCategoryStatsDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置类别类型名称
        switch (entity.getCategoryType()) {
            case "HYGIENE" -> dto.setCategoryTypeName("卫生");
            case "DISCIPLINE" -> dto.setCategoryTypeName("纪律");
            case "ATTENDANCE" -> dto.setCategoryTypeName("考勤");
            case "DORMITORY" -> dto.setCategoryTypeName("宿舍");
            default -> dto.setCategoryTypeName("其他");
        }

        return dto;
    }

    private CheckRecordDeductionDTO convertDeductionToDTO(CheckRecordDeductionNew entity) {
        CheckRecordDeductionDTO dto = new CheckRecordDeductionDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置扣分模式名称
        switch (entity.getDeductMode()) {
            case 1 -> dto.setDeductModeName("固定扣分");
            case 2 -> dto.setDeductModeName("按人数扣分");
            case 3 -> dto.setDeductModeName("区间扣分");
            default -> dto.setDeductModeName("未知");
        }

        // 设置关联类型名称
        switch (entity.getLinkType()) {
            case 0 -> dto.setLinkTypeName("无");
            case 1 -> dto.setLinkTypeName("宿舍");
            case 2 -> dto.setLinkTypeName("教室");
            default -> dto.setLinkTypeName("未知");
        }

        // 设置申诉状态名称
        switch (entity.getAppealStatus()) {
            case 0 -> dto.setAppealStatusName("未申诉");
            case 1 -> dto.setAppealStatusName("申诉中");
            case 2 -> dto.setAppealStatusName("已通过");
            case 3 -> dto.setAppealStatusName("已驳回");
            default -> dto.setAppealStatusName("未知");
        }

        // 解析学生ID列表
        if (entity.getStudentIds() != null && !entity.getStudentIds().isEmpty()) {
            dto.setStudentIdList(Arrays.stream(entity.getStudentIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }

        // 解析学生姓名列表
        if (entity.getStudentNames() != null && !entity.getStudentNames().isEmpty()) {
            dto.setStudentNameList(Arrays.asList(entity.getStudentNames().split(",")));
        }

        // 解析照片URL列表
        if (entity.getPhotoUrls() != null && !entity.getPhotoUrls().isEmpty()) {
            try {
                dto.setPhotoUrlList(objectMapper.readValue(entity.getPhotoUrls(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            } catch (Exception e) {
                log.warn("解析照片URL失败: {}", entity.getPhotoUrls(), e);
            }
        }

        dto.setIsRevised(entity.getIsRevised() != null && entity.getIsRevised() == 1);

        return dto;
    }

    @Override
    public List<CheckRecordClassStatsDTO> getClassRankingWithWeight(Long recordId, String sortBy, Long orgUnitId, Integer gradeLevel) {
        // 1. 获取检查记录信息
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        // 2. 获取班级统计列表
        List<CheckRecordClassStatsNew> stats = classStatsMapper.selectByRecordId(recordId);

        // 3. 筛选组织单元和年级
        if (orgUnitId != null) {
            stats = stats.stream()
                    .filter(s -> orgUnitId.equals(s.getOrgUnitId()))
                    .collect(Collectors.toList());
        }
        if (gradeLevel != null) {
            stats = stats.stream()
                    .filter(s -> s.getGradeId() != null && s.getGradeId().intValue() == gradeLevel)
                    .collect(Collectors.toList());
        }

        // 4. 转换为DTO并加载类别统计和计算加权分数
        List<CheckRecordClassStatsDTO> dtoList = stats.stream()
                .map(stat -> {
                    CheckRecordClassStatsDTO dto = convertClassStatsToDTO(stat);

                    // 加载类别统计（动态类别）
                    List<CheckRecordCategoryStatsNew> categoryStats = categoryStatsMapper.selectByClassStatId(stat.getId());
                    dto.setCategoryStats(categoryStats.stream()
                            .map(this::convertCategoryStatsToDTO)
                            .collect(Collectors.toList()));

                    // 计算加权分数（包括各类别的加权分数）
                    enrichWithWeightInfo(dto, record.getCheckDate(), recordId);

                    return dto;
                })
                .collect(Collectors.toList());

        // 5. 排序
        if ("original".equals(sortBy)) {
            // 按原始扣分排序（扣分少的排前面）
            dtoList.sort(Comparator.comparing(CheckRecordClassStatsDTO::getTotalScore));
        } else {
            // 默认按加权分数排序
            dtoList.sort(Comparator.comparing(dto ->
                    dto.getWeightedTotalScore() != null ? dto.getWeightedTotalScore() : dto.getTotalScore()
            ));
        }

        // 6. 计算排名
        for (int i = 0; i < dtoList.size(); i++) {
            CheckRecordClassStatsDTO dto = dtoList.get(i);
            if ("original".equals(sortBy)) {
                dto.setOverallRanking(i + 1);
            } else {
                dto.setWeightedOverallRanking(i + 1);
            }
        }

        return dtoList;
    }

    /**
     * 为班级统计DTO添加加权信息
     * 支持多配置加权：不同分类可以使用不同的加权配置
     */
    private void enrichWithWeightInfo(CheckRecordClassStatsDTO dto, LocalDate checkDate, Long recordId) {
        try {
            // 1. 获取检查记录关联的日常检查ID
            CheckRecordNew record = checkRecordMapper.selectById(recordId);
            Long dailyCheckId = record != null ? record.getDailyCheckId() : null;

            // 2. 获取多配置加权列表
            List<DailyCheckWeightConfigDTO> multiConfigs = null;
            if (dailyCheckId != null) {
                multiConfigs = dailyCheckWeightConfigService.getConfigsByDailyCheckId(dailyCheckId);
            }

            boolean hasMultiConfig = multiConfigs != null && multiConfigs.size() > 1;
            dto.setMultiConfigEnabled(hasMultiConfig);

            if (hasMultiConfig) {
                // ========== 多配置加权模式 ==========
                enrichWithMultiWeightConfig(dto, checkDate, recordId, multiConfigs);
            } else {
                // ========== 单配置加权模式（原有逻辑）==========
                enrichWithSingleWeightConfig(dto, checkDate, recordId);
            }
        } catch (Exception e) {
            log.warn("计算加权分数失败, classId={}", dto.getClassId(), e);
            setNoWeightResult(dto);
        }
    }

    /**
     * 多配置加权模式
     */
    private void enrichWithMultiWeightConfig(CheckRecordClassStatsDTO dto, LocalDate checkDate, Long recordId,
                                              List<DailyCheckWeightConfigDTO> multiConfigs) {
        log.info("多配置加权模式: classId={}, configCount={}", dto.getClassId(), multiConfigs.size());

        // 获取班级人数
        Integer classSize = dto.getClassSize();
        if (classSize == null || classSize == 0) {
            var classEntity = classMapper.selectById(dto.getClassId());
            if (classEntity != null) {
                classSize = classEntity.getStudentCount();
                dto.setClassSize(classSize);
            }
        }

        // 构建分类ID到配置的映射
        Map<Long, DailyCheckWeightConfigDTO> categoryConfigMap = new HashMap<>();
        DailyCheckWeightConfigDTO defaultConfig = null;

        for (DailyCheckWeightConfigDTO config : multiConfigs) {
            if (config.getIsDefault() != null && config.getIsDefault()) {
                defaultConfig = config;
            }
            if (config.getApplyCategoryIds() != null) {
                for (Long categoryId : config.getApplyCategoryIds()) {
                    categoryConfigMap.put(categoryId, config);
                }
            }
        }

        final DailyCheckWeightConfigDTO finalDefaultConfig = defaultConfig;
        final Integer finalClassSize = classSize;

        // 为每个配置计算加权系数和扣分
        List<CheckRecordClassStatsDTO.MultiWeightConfigInfo> multiWeightInfos = new ArrayList<>();
        BigDecimal weightedTotalScore = BigDecimal.ZERO;
        // 跟踪已处理的分类ID，避免同一分类被多个配置重复计算
        Set<Long> processedCategoryIds = new HashSet<>();

        for (DailyCheckWeightConfigDTO config : multiConfigs) {
            CheckRecordClassStatsDTO.MultiWeightConfigInfo info = new CheckRecordClassStatsDTO.MultiWeightConfigInfo();
            info.setConfigId(config.getWeightConfigId());
            info.setConfigName(config.getWeightConfigName());
            info.setColorCode(config.getColorCode());
            info.setColorName(config.getColorName());
            info.setIsDefault(config.getIsDefault());
            info.setApplyCategoryIds(config.getApplyCategoryIds());
            info.setApplyCategoryNames(config.getApplyCategoryNames());
            info.setWeightMode(config.getWeightMode());
            info.setWeightModeDesc(config.getWeightModeDesc());
            info.setStandardSizeMode(config.getStandardSizeMode());
            info.setStandardSizeModeDesc(config.getStandardSizeModeDesc());
            info.setMinWeight(config.getMinWeight());
            info.setMaxWeight(config.getMaxWeight());

            // 计算标准人数
            Integer standardSize = config.getCalculatedStandardSize();
            info.setStandardSize(standardSize);

            // 计算加权系数
            BigDecimal weightFactor = BigDecimal.ONE;
            if (finalClassSize != null && finalClassSize > 0 && standardSize != null && standardSize > 0) {
                String weightMode = config.getWeightMode();
                if ("STANDARD".equals(weightMode)) {
                    // 标准人数模式: 权重 = 标准人数 / 实际人数
                    weightFactor = BigDecimal.valueOf(standardSize)
                            .divide(BigDecimal.valueOf(finalClassSize), 4, RoundingMode.HALF_UP);
                } else if ("PER_CAPITA".equals(weightMode)) {
                    // 人均模式: 权重 = 实际人数 / 标准人数
                    weightFactor = BigDecimal.valueOf(finalClassSize)
                            .divide(BigDecimal.valueOf(standardSize), 4, RoundingMode.HALF_UP);
                }
                // 应用上下限
                if (config.getMinWeight() != null && weightFactor.compareTo(config.getMinWeight()) < 0) {
                    weightFactor = config.getMinWeight();
                }
                if (config.getMaxWeight() != null && weightFactor.compareTo(config.getMaxWeight()) > 0) {
                    weightFactor = config.getMaxWeight();
                }
            }
            info.setWeightFactor(weightFactor);

            // 计算该配置应用的分类总扣分（跳过已被其他配置处理的分类）
            BigDecimal configOriginalScore = BigDecimal.ZERO;
            if (dto.getCategoryStats() != null && config.getApplyCategoryIds() != null) {
                for (CheckRecordCategoryStatsDTO catStat : dto.getCategoryStats()) {
                    Long catId = catStat.getCategoryId();
                    // 如果该分类已被其他配置处理，则跳过
                    if (processedCategoryIds.contains(catId)) {
                        continue;
                    }
                    if (config.getApplyCategoryIds().contains(catId)) {
                        if (catStat.getTotalScore() != null) {
                            configOriginalScore = configOriginalScore.add(catStat.getTotalScore());
                            // 同时更新该分类的加权分数
                            BigDecimal weightedCatScore = catStat.getTotalScore()
                                    .multiply(weightFactor)
                                    .setScale(2, RoundingMode.HALF_UP);
                            catStat.setWeightedTotalScore(weightedCatScore);
                            // 标记该分类已处理
                            processedCategoryIds.add(catId);
                        }
                    }
                }
            }
            // 如果是默认配置，也计算未分配到其他配置的分类
            if (config.getIsDefault() != null && config.getIsDefault()) {
                if (dto.getCategoryStats() != null) {
                    for (CheckRecordCategoryStatsDTO catStat : dto.getCategoryStats()) {
                        Long catId = catStat.getCategoryId();
                        // 如果该分类已被其他配置处理，则跳过
                        if (processedCategoryIds.contains(catId)) {
                            continue;
                        }
                        if (!categoryConfigMap.containsKey(catId)) {
                            if (catStat.getTotalScore() != null) {
                                configOriginalScore = configOriginalScore.add(catStat.getTotalScore());
                                BigDecimal weightedCatScore = catStat.getTotalScore()
                                        .multiply(weightFactor)
                                        .setScale(2, RoundingMode.HALF_UP);
                                catStat.setWeightedTotalScore(weightedCatScore);
                                // 标记该分类已处理
                                processedCategoryIds.add(catId);
                            }
                        }
                    }
                }
            }

            info.setOriginalScore(configOriginalScore);
            BigDecimal configWeightedScore = configOriginalScore.multiply(weightFactor).setScale(2, RoundingMode.HALF_UP);
            info.setWeightedScore(configWeightedScore);
            weightedTotalScore = weightedTotalScore.add(configWeightedScore);

            multiWeightInfos.add(info);
        }

        dto.setMultiWeightConfigs(multiWeightInfos);
        dto.setWeightEnabled(true);
        dto.setWeightedTotalScore(weightedTotalScore);

        // 使用默认配置的加权系数作为主加权系数
        if (defaultConfig != null) {
            Integer defaultStandardSize = defaultConfig.getCalculatedStandardSize();
            dto.setStandardSize(defaultStandardSize);
            for (CheckRecordClassStatsDTO.MultiWeightConfigInfo info : multiWeightInfos) {
                if (info.getIsDefault() != null && info.getIsDefault()) {
                    dto.setWeightFactor(info.getWeightFactor());
                    break;
                }
            }
        } else if (!multiWeightInfos.isEmpty()) {
            dto.setWeightFactor(multiWeightInfos.get(0).getWeightFactor());
            dto.setStandardSize(multiWeightInfos.get(0).getStandardSize());
        }

        log.info("多配置加权完成: classId={}, originalTotal={}, weightedTotal={}",
                dto.getClassId(), dto.getTotalScore(), weightedTotalScore);
    }

    /**
     * 单配置加权模式（原有逻辑）
     */
    private void enrichWithSingleWeightConfig(CheckRecordClassStatsDTO dto, LocalDate checkDate, Long recordId) {
        ClassWeightResultDTO weightResult = classWeightService.calculateWeightedScore(
                dto.getClassId(),
                dto.getTotalScore(),
                checkDate,
                recordId
        );

        if (weightResult != null && weightResult.getWeightApplied() != null && weightResult.getWeightApplied()) {
            dto.setWeightEnabled(weightResult.getWeightApplied());
            dto.setWeightFactor(weightResult.getWeightFactor());
            dto.setStandardSize(weightResult.getStandardClassSize());
            dto.setWeightMode(weightResult.getWeightMode());
            dto.setWeightModeDesc(weightResult.getWeightModeDesc());
            dto.setWeightedTotalScore(weightResult.getWeightedScore());
            dto.setWeightCalculationDetails(weightResult.getCalculationNote());

            // 为动态类别统计计算加权分数
            BigDecimal factor = weightResult.getWeightFactor();
            if (factor != null && dto.getCategoryStats() != null) {
                for (CheckRecordCategoryStatsDTO catStat : dto.getCategoryStats()) {
                    if (catStat.getTotalScore() != null) {
                        BigDecimal weightedCatScore = catStat.getTotalScore()
                                .multiply(factor)
                                .setScale(2, RoundingMode.HALF_UP);
                        catStat.setWeightedTotalScore(weightedCatScore);
                    }
                }
            }

            // 计算各类别的加权分数（兼容旧版固定字段）
            if (factor != null && dto.getHygieneScore() != null) {
                dto.setWeightedHygieneScore(dto.getHygieneScore().multiply(factor).setScale(2, RoundingMode.HALF_UP));
            }
            if (factor != null && dto.getDisciplineScore() != null) {
                dto.setWeightedDisciplineScore(dto.getDisciplineScore().multiply(factor).setScale(2, RoundingMode.HALF_UP));
            }
            if (factor != null && dto.getAttendanceScore() != null) {
                dto.setWeightedAttendanceScore(dto.getAttendanceScore().multiply(factor).setScale(2, RoundingMode.HALF_UP));
            }
            if (factor != null && dto.getDormitoryScore() != null) {
                dto.setWeightedDormitoryScore(dto.getDormitoryScore().multiply(factor).setScale(2, RoundingMode.HALF_UP));
            }
            if (factor != null && dto.getOtherScore() != null) {
                dto.setWeightedOtherScore(dto.getOtherScore().multiply(factor).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            setNoWeightResult(dto);
        }
    }

    /**
     * 设置无加权结果
     */
    private void setNoWeightResult(CheckRecordClassStatsDTO dto) {
        dto.setWeightEnabled(false);
        dto.setWeightFactor(BigDecimal.ONE);
        dto.setWeightedTotalScore(dto.getTotalScore());

        // 动态类别的加权分数等于原始分数
        if (dto.getCategoryStats() != null) {
            for (CheckRecordCategoryStatsDTO catStat : dto.getCategoryStats()) {
                catStat.setWeightedTotalScore(catStat.getTotalScore());
            }
        }
    }

    @Override
    public WeightConfigDetailDTO getWeightConfigDetail(Long recordId) {
        // 1. 获取检查记录
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        // 2. 获取关联的日常检查
        DailyCheck dailyCheck = dailyCheckMapper.selectById(record.getDailyCheckId());
        if (dailyCheck == null) {
            throw new BusinessException("关联的日常检查不存在");
        }

        WeightConfigDetailDTO dto = new WeightConfigDetailDTO();

        // 3. 解析加权配置快照
        String configSnapshot = dailyCheck.getWeightConfigSnapshot();
        boolean weightEnabled = dailyCheck.getEnableWeight() != null && dailyCheck.getEnableWeight() == 1;
        dto.setWeightEnabled(weightEnabled);

        if (!weightEnabled || configSnapshot == null || configSnapshot.isEmpty()) {
            // 未启用加权
            dto.setConfigName("未配置");
            dto.setWeightMode("NONE");
            dto.setWeightModeDesc("不加权");
            dto.setWeightModeExplanation("本次检查未启用班级人数加权功能，所有班级使用原始扣分计算排名。");
            dto.setFormulaDescription("最终扣分 = 原始扣分（不进行加权调整）");
            return dto;
        }

        try {
            // 4. 解析配置快照JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = objectMapper.readValue(configSnapshot, Map.class);

            dto.setConfigId(dailyCheck.getWeightConfigId());
            dto.setConfigName((String) configMap.getOrDefault("configName", "加权配置"));
            dto.setConfigCode((String) configMap.get("configCode"));

            // 加权模式
            String weightMode = (String) configMap.getOrDefault("weightMode", "STANDARD");
            dto.setWeightMode(weightMode);
            dto.setWeightModeDesc(getWeightModeDesc(weightMode));
            dto.setWeightModeExplanation(getWeightModeExplanation(weightMode));

            // 标准人数模式
            String standardSizeMode = (String) configMap.getOrDefault("standardSizeMode", "FIXED");
            dto.setStandardSizeMode(standardSizeMode);
            dto.setStandardSizeModeDesc(getStandardSizeModeDesc(standardSizeMode));

            // 标准人数 - 根据模式计算
            Integer standardSize = dailyCheck.getCustomStandardSize();
            if (standardSize == null) {
                // 根据标准人数模式计算
                if ("TARGET_AVERAGE".equals(standardSizeMode)) {
                    // 从检查目标班级计算平均人数
                    standardSize = calculateTargetAverageSize(dailyCheck.getId());
                    log.info("TARGET_AVERAGE模式: 从检查目标计算得到标准人数={}", standardSize);
                } else {
                    // FIXED模式或其他模式，使用配置中的固定值
                    Object ss = configMap.get("standardSize");
                    if (ss instanceof Number) {
                        standardSize = ((Number) ss).intValue();
                    }
                }
            }
            dto.setStandardSize(standardSize);

            // 权重限制
            Object minWeight = configMap.get("minWeight");
            Object maxWeight = configMap.get("maxWeight");
            Object enableLimit = configMap.get("enableWeightLimit");

            if (minWeight instanceof Number) {
                dto.setMinWeight(new BigDecimal(minWeight.toString()));
            }
            if (maxWeight instanceof Number) {
                dto.setMaxWeight(new BigDecimal(maxWeight.toString()));
            }
            dto.setEnableWeightLimit(enableLimit != null && (enableLimit.equals(1) || enableLimit.equals(true)));

            // 分段规则
            if ("SEGMENT".equals(weightMode)) {
                Object segmentRules = configMap.get("segmentRules");
                if (segmentRules instanceof String) {
                    dto.setSegmentRules(parseSegmentRules((String) segmentRules));
                } else if (segmentRules instanceof List) {
                    dto.setSegmentRules(convertSegmentRules((List<?>) segmentRules));
                }
            }

            // 配置说明
            dto.setDescription((String) configMap.get("description"));

            // 计算公式说明
            dto.setFormulaDescription(getFormulaDescription(weightMode, standardSize));
            dto.setCalculationExample(getCalculationExample(weightMode, standardSize));

            // 5. 获取各班级的加权系数
            List<CheckRecordClassStatsNew> classStats = classStatsMapper.selectByRecordId(recordId);
            List<WeightConfigDetailDTO.ClassWeightFactor> classFactors = new ArrayList<>();

            for (CheckRecordClassStatsNew stat : classStats) {
                WeightConfigDetailDTO.ClassWeightFactor factor = new WeightConfigDetailDTO.ClassWeightFactor();
                factor.setClassId(stat.getClassId());
                factor.setClassName(stat.getClassName());
                factor.setOriginalScore(stat.getTotalScore());

                // 获取班级人数
                Integer classSize = stat.getClassSize();
                if (classSize == null || classSize == 0) {
                    // 尝试通过ClassMapper获取班级详情来获取学生数量
                    var classResponse = classMapper.selectClassResponseById(stat.getClassId());
                    if (classResponse != null) {
                        classSize = classResponse.getStudentCount();
                    }
                }
                factor.setActualSize(classSize);
                factor.setStandardSize(standardSize);

                // 计算加权系数
                if (classSize != null && classSize > 0 && standardSize != null && standardSize > 0) {
                    BigDecimal wf = calculateWeightFactor(weightMode, classSize, standardSize, dto.getSegmentRules());

                    // 应用权重限制
                    if (dto.getEnableWeightLimit() != null && dto.getEnableWeightLimit()) {
                        if (dto.getMinWeight() != null && wf.compareTo(dto.getMinWeight()) < 0) {
                            wf = dto.getMinWeight();
                        }
                        if (dto.getMaxWeight() != null && wf.compareTo(dto.getMaxWeight()) > 0) {
                            wf = dto.getMaxWeight();
                        }
                    }

                    factor.setWeightFactor(wf);
                    if (stat.getTotalScore() != null) {
                        factor.setWeightedScore(stat.getTotalScore().multiply(wf).setScale(2, RoundingMode.HALF_UP));
                    }
                    factor.setCalculationNote(String.format("标准人数(%d) / 实际人数(%d) = %.4f",
                            standardSize, classSize, wf.doubleValue()));
                } else {
                    factor.setWeightFactor(BigDecimal.ONE);
                    factor.setWeightedScore(stat.getTotalScore());
                    factor.setCalculationNote("人数信息不完整，使用原始分数");
                }

                classFactors.add(factor);
            }

            // 按加权系数排序
            classFactors.sort(Comparator.comparing(WeightConfigDetailDTO.ClassWeightFactor::getWeightFactor));
            dto.setClassWeightFactors(classFactors);

            // 6. 检查并加载多加权配置
            loadMultiWeightConfigs(dto, dailyCheck.getId(), classStats, standardSize, weightMode);

        } catch (Exception e) {
            log.error("解析加权配置快照失败: recordId={}", recordId, e);
            dto.setConfigName("配置解析失败");
            dto.setWeightMode("NONE");
            dto.setWeightModeDesc("不加权");
            dto.setWeightModeExplanation("加权配置快照解析失败，请联系管理员。");
        }

        return dto;
    }

    /**
     * 加载多加权配置信息
     */
    private void loadMultiWeightConfigs(WeightConfigDetailDTO dto, Long dailyCheckId,
                                         List<CheckRecordClassStatsNew> classStats,
                                         Integer defaultStandardSize, String defaultWeightMode) {
        try {
            List<DailyCheckWeightConfigDTO> multiConfigs = dailyCheckWeightConfigService.getConfigsByDailyCheckId(dailyCheckId);

            if (multiConfigs != null && !multiConfigs.isEmpty()) {
                dto.setMultiConfigEnabled(true);

                List<WeightConfigDetailDTO.MultiWeightConfig> multiConfigList = new ArrayList<>();
                for (DailyCheckWeightConfigDTO config : multiConfigs) {
                    WeightConfigDetailDTO.MultiWeightConfig multiConfig = new WeightConfigDetailDTO.MultiWeightConfig();
                    multiConfig.setConfigId(config.getId());
                    multiConfig.setWeightConfigId(config.getWeightConfigId());
                    multiConfig.setConfigName(config.getWeightConfigName());
                    multiConfig.setColorCode(config.getColorCode());
                    multiConfig.setColorName(config.getColorName());
                    multiConfig.setApplyScope(config.getApplyScope());
                    multiConfig.setApplyCategoryIds(config.getApplyCategoryIds());
                    multiConfig.setApplyCategoryNames(config.getApplyCategoryNames());
                    multiConfig.setWeightMode(config.getWeightMode());
                    multiConfig.setWeightModeDesc(config.getWeightModeDesc());
                    multiConfig.setStandardSizeMode(config.getStandardSizeMode());
                    multiConfig.setStandardSizeModeDesc(config.getStandardSizeModeDesc());
                    multiConfig.setCalculatedStandardSize(config.getCalculatedStandardSize());
                    multiConfig.setMinWeight(config.getMinWeight());
                    multiConfig.setMaxWeight(config.getMaxWeight());
                    multiConfig.setIsDefault(config.getIsDefault());
                    multiConfig.setPriority(config.getPriority());
                    multiConfig.setFormulaDescription(getFormulaDescription(
                            config.getWeightMode() != null ? config.getWeightMode() : defaultWeightMode,
                            config.getCalculatedStandardSize() != null ? config.getCalculatedStandardSize() : defaultStandardSize
                    ));
                    multiConfigList.add(multiConfig);
                }
                dto.setMultiConfigs(multiConfigList);

                // 更新班级加权系数中的配置信息
                updateClassFactorsWithMultiConfig(dto.getClassWeightFactors(), dailyCheckId, classStats);
            } else {
                dto.setMultiConfigEnabled(false);
            }
        } catch (Exception e) {
            log.warn("加载多加权配置失败: dailyCheckId={}", dailyCheckId, e);
            dto.setMultiConfigEnabled(false);
        }
    }

    /**
     * 更新班级加权系数中的多配置信息
     */
    private void updateClassFactorsWithMultiConfig(List<WeightConfigDetailDTO.ClassWeightFactor> classFactors,
                                                    Long dailyCheckId,
                                                    List<CheckRecordClassStatsNew> classStats) {
        if (classFactors == null || classFactors.isEmpty()) return;

        for (int i = 0; i < classFactors.size() && i < classStats.size(); i++) {
            WeightConfigDetailDTO.ClassWeightFactor factor = classFactors.get(i);
            // 这里可以根据班级的分类扣分情况，确定使用哪个配置
            // 暂时使用默认配置的颜色
            DailyCheckWeightConfigDTO defaultConfig = dailyCheckWeightConfigService.getApplicableConfig(
                    dailyCheckId, null, null
            );
            if (defaultConfig != null) {
                factor.setWeightConfigId(defaultConfig.getWeightConfigId());
                factor.setColorCode(defaultConfig.getColorCode());
            }
        }
    }

    private String getWeightModeDesc(String weightMode) {
        return switch (weightMode) {
            case "STANDARD" -> "标准人数折算";
            case "PER_CAPITA" -> "人均扣分";
            case "SEGMENT" -> "分段加权";
            case "NONE" -> "不加权";
            default -> "未知模式";
        };
    }

    private String getWeightModeExplanation(String weightMode) {
        return switch (weightMode) {
            case "STANDARD" -> "根据班级实际人数与标准人数的比例计算加权系数。人数多的班级管理难度大，扣分会相应减少。公式：加权系数 = 标准人数 / 实际人数";
            case "PER_CAPITA" -> "将扣分按人均计算后再乘以标准人数。公式：加权分数 = (原始扣分 / 实际人数) × 标准人数";
            case "SEGMENT" -> "根据班级人数所在区间使用预设的加权系数。不同人数区间对应不同的加权系数。";
            case "NONE" -> "不进行加权调整，直接使用原始扣分。";
            default -> "未知的加权模式。";
        };
    }

    private String getStandardSizeModeDesc(String mode) {
        return switch (mode) {
            case "FIXED" -> "固定标准人数";
            case "TARGET_AVERAGE" -> "目标班级平均人数";
            case "RANGE_AVERAGE" -> "范围内平均人数";
            case "CUSTOM" -> "自定义规则";
            default -> "未知模式";
        };
    }

    private String getFormulaDescription(String weightMode, Integer standardSize) {
        return switch (weightMode) {
            case "STANDARD" -> String.format("加权扣分 = 原始扣分 × (标准人数%s / 实际人数)",
                    standardSize != null ? "(" + standardSize + ")" : "");
            case "PER_CAPITA" -> String.format("加权扣分 = (原始扣分 / 实际人数) × 标准人数%s",
                    standardSize != null ? "(" + standardSize + ")" : "");
            case "SEGMENT" -> "加权扣分 = 原始扣分 × 分段系数（根据人数区间确定）";
            default -> "最终扣分 = 原始扣分";
        };
    }

    private String getCalculationExample(String weightMode, Integer standardSize) {
        int std = standardSize != null ? standardSize : 40;
        return switch (weightMode) {
            case "STANDARD" -> String.format("示例：班级有50人，原始扣分10分\n加权扣分 = 10 × (%d/50) = %.2f分",
                    std, 10.0 * std / 50);
            case "PER_CAPITA" -> String.format("示例：班级有50人，原始扣分10分\n加权扣分 = (10/50) × %d = %.2f分",
                    std, 10.0 / 50 * std);
            case "SEGMENT" -> "示例：班级有50人落入[41-50]区间，系数0.9\n加权扣分 = 10 × 0.9 = 9分";
            default -> "示例：原始扣分10分，最终扣分 = 10分";
        };
    }

    private List<WeightConfigDetailDTO.SegmentRule> parseSegmentRules(String json) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = objectMapper.readValue(json, List.class);
            return convertSegmentRules(rules);
        } catch (Exception e) {
            log.warn("解析分段规则失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private List<WeightConfigDetailDTO.SegmentRule> convertSegmentRules(List<?> rules) {
        List<WeightConfigDetailDTO.SegmentRule> result = new ArrayList<>();
        for (Object obj : rules) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rule = (Map<String, Object>) obj;
                WeightConfigDetailDTO.SegmentRule sr = new WeightConfigDetailDTO.SegmentRule();
                sr.setMinSize(getIntValue(rule.get("minSize")));
                sr.setMaxSize(getIntValue(rule.get("maxSize")));
                Object weight = rule.get("weight");
                if (weight instanceof Number) {
                    sr.setWeight(new BigDecimal(weight.toString()));
                }
                sr.setDescription(String.format("%d-%d人: 系数%.2f",
                        sr.getMinSize(), sr.getMaxSize(),
                        sr.getWeight() != null ? sr.getWeight().doubleValue() : 1.0));
                result.add(sr);
            }
        }
        return result;
    }

    /**
     * 计算检查目标班级的平均人数
     * 用于 TARGET_AVERAGE 标准人数模式
     *
     * @param dailyCheckId 日常检查ID
     * @return 目标班级的平均人数，如果无法计算则返回 null
     */
    private Integer calculateTargetAverageSize(Long dailyCheckId) {
        try {
            // 1. 获取检查目标班级列表 (target_type=1 表示班级)
            LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
            targetWrapper.eq(DailyCheckTarget::getCheckId, dailyCheckId)
                    .eq(DailyCheckTarget::getTargetType, 1);  // 1=班级
            List<DailyCheckTarget> targets = dailyCheckTargetMapper.selectList(targetWrapper);

            if (targets == null || targets.isEmpty()) {
                log.warn("日常检查ID={}没有班级类型的检查目标，无法计算TARGET_AVERAGE", dailyCheckId);
                return null;
            }

            // 2. 获取所有目标班级的人数
            List<Long> classIds = targets.stream()
                    .map(DailyCheckTarget::getTargetId)
                    .collect(Collectors.toList());

            int totalStudents = 0;
            int validClassCount = 0;

            for (Long classId : classIds) {
                Class classEntity = classMapper.selectById(classId);
                if (classEntity != null && classEntity.getStudentCount() != null && classEntity.getStudentCount() > 0) {
                    totalStudents += classEntity.getStudentCount();
                    validClassCount++;
                }
            }

            if (validClassCount == 0) {
                log.warn("日常检查ID={}的目标班级都没有有效的学生数量", dailyCheckId);
                return null;
            }

            // 3. 计算平均值（四舍五入）
            int averageSize = Math.round((float) totalStudents / validClassCount);
            log.info("TARGET_AVERAGE计算: 日常检查ID={}, 班级数={}, 总学生数={}, 平均人数={}",
                    dailyCheckId, validClassCount, totalStudents, averageSize);

            return averageSize;
        } catch (Exception e) {
            log.error("计算TARGET_AVERAGE标准人数失败: dailyCheckId={}", dailyCheckId, e);
            return null;
        }
    }

    private Integer getIntValue(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return null;
    }

    private BigDecimal calculateWeightFactor(String weightMode, Integer actualSize, Integer standardSize,
                                              List<WeightConfigDetailDTO.SegmentRule> segmentRules) {
        if (actualSize == null || actualSize <= 0 || standardSize == null || standardSize <= 0) {
            return BigDecimal.ONE;
        }

        return switch (weightMode) {
            case "STANDARD" -> new BigDecimal(standardSize)
                    .divide(new BigDecimal(actualSize), 4, RoundingMode.HALF_UP);
            case "PER_CAPITA" -> new BigDecimal(standardSize)
                    .divide(new BigDecimal(actualSize), 4, RoundingMode.HALF_UP);
            case "SEGMENT" -> {
                if (segmentRules != null) {
                    for (WeightConfigDetailDTO.SegmentRule rule : segmentRules) {
                        if (rule.getMinSize() != null && rule.getMaxSize() != null
                                && actualSize >= rule.getMinSize() && actualSize <= rule.getMaxSize()) {
                            yield rule.getWeight() != null ? rule.getWeight() : BigDecimal.ONE;
                        }
                    }
                }
                yield BigDecimal.ONE;
            }
            default -> BigDecimal.ONE;
        };
    }

    // ==================== 加权配置树形结构 ====================

    @Override
    public WeightConfigTreeDTO getWeightConfigTree(Long recordId) {
        // 1. 获取检查记录
        CheckRecordNew record = checkRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("检查记录不存在");
        }

        // 2. 获取关联的日常检查
        DailyCheck dailyCheck = dailyCheckMapper.selectById(record.getDailyCheckId());
        if (dailyCheck == null) {
            throw new BusinessException("关联的日常检查不存在");
        }

        WeightConfigTreeDTO tree = new WeightConfigTreeDTO();
        tree.setRecordId(recordId);
        tree.setCheckName(record.getCheckName());

        boolean weightEnabled = dailyCheck.getEnableWeight() != null && dailyCheck.getEnableWeight() == 1;
        tree.setWeightEnabled(weightEnabled);

        // 3. 解析全局加权配置
        String configSnapshot = dailyCheck.getWeightConfigSnapshot();
        WeightConfigTreeDTO.WeightConfigNode globalConfig = parseWeightConfigNode(configSnapshot, dailyCheck);
        tree.setGlobalConfig(globalConfig);

        // 3.1 加载多加权配置（从daily_check_weight_configs表）
        List<DailyCheckWeightConfigDTO> multiConfigs = dailyCheckWeightConfigService.getConfigsByDailyCheckId(dailyCheck.getId());
        boolean multiConfigEnabled = multiConfigs != null && multiConfigs.size() > 1;
        tree.setMultiConfigEnabled(multiConfigEnabled);

        // 转换多配置到DTO
        if (multiConfigs != null && !multiConfigs.isEmpty()) {
            List<WeightConfigTreeDTO.MultiWeightScheme> schemes = multiConfigs.stream()
                    .map(this::convertToMultiWeightScheme)
                    .collect(Collectors.toList());
            tree.setMultiWeightSchemes(schemes);
            log.info("加载多加权配置: dailyCheckId={}, configCount={}", dailyCheck.getId(), schemes.size());
        }

        // 构建分类ID到配置的映射
        Map<Long, DailyCheckWeightConfigDTO> categoryConfigMap = new HashMap<>();
        DailyCheckWeightConfigDTO defaultConfig = null;
        if (multiConfigs != null) {
            for (DailyCheckWeightConfigDTO mc : multiConfigs) {
                if (mc.getIsDefault() != null && mc.getIsDefault()) {
                    defaultConfig = mc;
                }
                if (mc.getApplyCategoryIds() != null) {
                    for (Long categoryId : mc.getApplyCategoryIds()) {
                        categoryConfigMap.put(categoryId, mc);
                    }
                }
            }
        }
        final DailyCheckWeightConfigDTO finalDefaultConfig = defaultConfig;

        // 4. 获取所有实际扣分记录，用于统计
        List<CheckRecordClassStatsNew> classStats = classStatsMapper.selectByRecordId(recordId);
        List<CheckRecordDeductionNew> allDeductions = deductionMapper.selectByRecordId(recordId);

        // 按类别ID分组扣分记录
        Map<Long, List<CheckRecordDeductionNew>> deductionsByCategoryId = allDeductions.stream()
                .filter(d -> d.getCategoryId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getCategoryId));

        // 按扣分项ID分组扣分记录
        Map<Long, List<CheckRecordDeductionNew>> deductionsByItemId = allDeductions.stream()
                .filter(d -> d.getDeductionItemId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getDeductionItemId));

        // 5. 从检查模板获取完整的类别和扣分项定义
        Long templateId = dailyCheck.getTemplateId();
        List<WeightConfigTreeDTO.CategoryWeightNode> categoryNodes = new ArrayList<>();

        if (templateId != null) {
            // 获取模板关联的所有类别
            LambdaQueryWrapper<TemplateCategory> tcWrapper = new LambdaQueryWrapper<>();
            tcWrapper.eq(TemplateCategory::getTemplateId, templateId)
                    .orderByAsc(TemplateCategory::getSortOrder);
            List<TemplateCategory> templateCategories = templateCategoryMapper.selectList(tcWrapper);

            for (TemplateCategory tc : templateCategories) {
                // 获取类别详情
                CheckCategory category = checkCategoryMapper.selectById(tc.getCategoryId());
                if (category == null) continue;

                WeightConfigTreeDTO.CategoryWeightNode categoryNode = new WeightConfigTreeDTO.CategoryWeightNode();
                categoryNode.setCategoryId(tc.getCategoryId());
                categoryNode.setCategoryName(category.getCategoryName());
                categoryNode.setCategoryCode(category.getCategoryCode());
                categoryNode.setCategoryType(category.getCategoryType());

                // 类别加权配置：新架构下加权配置在CheckPlan层级，类别继承全局配置
                categoryNode.setWeightEnabled(weightEnabled);
                categoryNode.setInherited(true);
                categoryNode.setInheritSource("全局配置");
                categoryNode.setEffectiveConfig(globalConfig);

                // 6. 获取该类别下所有扣分项定义
                List<DeductionItem> items = deductionItemMapper.selectEnabledByTypeId(tc.getCategoryId());
                List<WeightConfigTreeDTO.ItemWeightNode> itemNodes = new ArrayList<>();

                for (DeductionItem item : items) {
                    WeightConfigTreeDTO.ItemWeightNode itemNode = new WeightConfigTreeDTO.ItemWeightNode();
                    itemNode.setItemId(item.getId());
                    itemNode.setItemName(item.getItemName());
                    itemNode.setItemCode("ITEM_" + item.getId());

                    // 扣分项加权配置
                    boolean itemWeightEnabled = item.getEnableWeight() != null && item.getEnableWeight() == 1;
                    if (itemWeightEnabled && item.getWeightConfigId() != null) {
                        itemNode.setWeightEnabled(true);
                        itemNode.setInherited(false);
                        itemNode.setInheritSource(null);
                    } else if (!categoryNode.getInherited()) {
                        itemNode.setWeightEnabled(categoryNode.getWeightEnabled());
                        itemNode.setInherited(true);
                        itemNode.setInheritSource("类别配置");
                    } else {
                        itemNode.setWeightEnabled(weightEnabled);
                        itemNode.setInherited(true);
                        itemNode.setInheritSource("全局配置");
                    }
                    itemNode.setEffectiveConfig(globalConfig);

                    // 扣分模式
                    itemNode.setDeductMode(getDeductModeString(item.getDeductMode()));
                    itemNode.setDeductModeDesc(getDeductModeDesc(item.getDeductMode()));
                    itemNode.setBaseScore(item.getFixedScore() != null ? item.getFixedScore() : item.getBaseScore());

                    // 统计实际扣分
                    List<CheckRecordDeductionNew> itemDeductions = deductionsByItemId.getOrDefault(item.getId(), new ArrayList<>());
                    itemNode.setDeductionCount(itemDeductions.size());
                    BigDecimal itemOriginalTotal = itemDeductions.stream()
                            .map(CheckRecordDeductionNew::getActualScore)
                            .filter(s -> s != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    itemNode.setOriginalScore(itemOriginalTotal);
                    itemNode.setWeightedScore(itemOriginalTotal);

                    itemNodes.add(itemNode);
                }

                categoryNode.setItems(itemNodes);

                // 计算类别扣分统计
                List<CheckRecordDeductionNew> categoryDeductions = deductionsByCategoryId.getOrDefault(tc.getCategoryId(), new ArrayList<>());
                WeightConfigTreeDTO.CategoryDeductionSummary summary = new WeightConfigTreeDTO.CategoryDeductionSummary();
                summary.setItemCount(itemNodes.size());
                summary.setDeductionCount(categoryDeductions.size());
                BigDecimal originalTotal = categoryDeductions.stream()
                        .map(CheckRecordDeductionNew::getActualScore)
                        .filter(s -> s != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                summary.setOriginalScore(originalTotal);
                summary.setWeightedScore(originalTotal);
                categoryNode.setDeductionSummary(summary);

                categoryNodes.add(categoryNode);
            }
        }

        // 如果没有从模板获取到类别，回退到从扣分记录中提取
        if (categoryNodes.isEmpty() && !allDeductions.isEmpty()) {
            Set<String> processedCategories = new HashSet<>();
            for (CheckRecordDeductionNew deduction : allDeductions) {
                String categoryName = deduction.getCategoryName() != null ? deduction.getCategoryName() : "其他";
                if (processedCategories.contains(categoryName)) {
                    continue;
                }
                processedCategories.add(categoryName);

                WeightConfigTreeDTO.CategoryWeightNode categoryNode = new WeightConfigTreeDTO.CategoryWeightNode();
                categoryNode.setCategoryId(deduction.getCategoryId());
                categoryNode.setCategoryName(categoryName);
                categoryNode.setCategoryCode(deduction.getCategoryId() != null ? "CAT_" + deduction.getCategoryId() : null);
                categoryNode.setWeightEnabled(weightEnabled);
                categoryNode.setInherited(true);
                categoryNode.setInheritSource("全局配置");
                categoryNode.setEffectiveConfig(globalConfig);

                List<CheckRecordDeductionNew> categoryDeductions = allDeductions.stream()
                        .filter(d -> categoryName.equals(d.getCategoryName() != null ? d.getCategoryName() : "其他"))
                        .collect(Collectors.toList());
                List<WeightConfigTreeDTO.ItemWeightNode> itemNodes = buildItemNodes(categoryDeductions, weightEnabled, globalConfig);
                categoryNode.setItems(itemNodes);

                WeightConfigTreeDTO.CategoryDeductionSummary summary = new WeightConfigTreeDTO.CategoryDeductionSummary();
                summary.setItemCount(itemNodes.size());
                summary.setDeductionCount(categoryDeductions.size());
                BigDecimal originalTotal = categoryDeductions.stream()
                        .map(CheckRecordDeductionNew::getActualScore)
                        .filter(s -> s != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                summary.setOriginalScore(originalTotal);
                summary.setWeightedScore(originalTotal);
                categoryNode.setDeductionSummary(summary);

                categoryNodes.add(categoryNode);
            }
        }

        tree.setCategories(categoryNodes);

        // 7. 构建班级加权系数汇总
        List<WeightConfigTreeDTO.ClassWeightSummary> classWeightSummaries = buildClassWeightSummaries(
                classStats, globalConfig, dailyCheck, categoryNodes);
        tree.setClassWeightSummary(classWeightSummaries);

        return tree;
    }

    private WeightConfigTreeDTO.WeightConfigNode parseWeightConfigNode(String configSnapshot, DailyCheck dailyCheck) {
        WeightConfigTreeDTO.WeightConfigNode node = new WeightConfigTreeDTO.WeightConfigNode();

        if (configSnapshot == null || configSnapshot.isEmpty()) {
            node.setConfigName("未配置");
            node.setWeightMode("NONE");
            node.setWeightModeDesc("不加权");
            node.setFormulaDescription("最终扣分 = 原始扣分");
            return node;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = objectMapper.readValue(configSnapshot, Map.class);

            node.setConfigId(dailyCheck.getWeightConfigId());
            node.setConfigName((String) configMap.getOrDefault("configName", "加权配置"));
            node.setConfigCode((String) configMap.get("configCode"));

            String weightMode = (String) configMap.getOrDefault("weightMode", "STANDARD");
            node.setWeightMode(weightMode);
            node.setWeightModeDesc(getWeightModeDesc(weightMode));

            String standardSizeMode = (String) configMap.getOrDefault("standardSizeMode", "FIXED");
            node.setStandardSizeMode(standardSizeMode);
            node.setStandardSizeModeDesc(getStandardSizeModeDesc(standardSizeMode));

            // 标准人数 - 根据模式计算
            Integer standardSize = dailyCheck.getCustomStandardSize();
            if (standardSize == null) {
                // 根据标准人数模式计算
                if ("TARGET_AVERAGE".equals(standardSizeMode)) {
                    // 从检查目标班级计算平均人数
                    standardSize = calculateTargetAverageSize(dailyCheck.getId());
                    log.debug("TreeDTO TARGET_AVERAGE模式: 从检查目标计算得到标准人数={}", standardSize);
                }
                // 如果仍然为null，使用配置中的固定值
                if (standardSize == null) {
                    Object ss = configMap.get("standardSize");
                    if (ss instanceof Number) {
                        standardSize = ((Number) ss).intValue();
                    }
                }
            }
            node.setStandardSize(standardSize);

            Object minWeight = configMap.get("minWeight");
            Object maxWeight = configMap.get("maxWeight");
            Object enableLimit = configMap.get("enableWeightLimit");

            if (minWeight instanceof Number) {
                node.setMinWeight(new BigDecimal(minWeight.toString()));
            }
            if (maxWeight instanceof Number) {
                node.setMaxWeight(new BigDecimal(maxWeight.toString()));
            }
            node.setEnableWeightLimit(enableLimit != null && (enableLimit.equals(1) || enableLimit.equals(true)));

            // 分段规则
            if ("SEGMENT".equals(weightMode)) {
                Object segmentRules = configMap.get("segmentRules");
                if (segmentRules instanceof String) {
                    node.setSegmentRules(parseTreeSegmentRules((String) segmentRules));
                } else if (segmentRules instanceof List) {
                    node.setSegmentRules(convertTreeSegmentRules((List<?>) segmentRules));
                }
            }

            node.setFormulaDescription(getFormulaDescription(weightMode, standardSize));

        } catch (Exception e) {
            log.error("解析加权配置快照失败", e);
            node.setConfigName("配置解析失败");
            node.setWeightMode("NONE");
            node.setWeightModeDesc("不加权");
        }

        return node;
    }

    private List<WeightConfigTreeDTO.ItemWeightNode> buildItemNodes(
            List<CheckRecordDeductionNew> deductions,
            boolean parentWeightEnabled,
            WeightConfigTreeDTO.WeightConfigNode parentConfig) {

        // 按扣分项分组
        Map<Long, List<CheckRecordDeductionNew>> byItemId = deductions.stream()
                .filter(d -> d.getDeductionItemId() != null)
                .collect(Collectors.groupingBy(CheckRecordDeductionNew::getDeductionItemId));

        List<WeightConfigTreeDTO.ItemWeightNode> items = new ArrayList<>();

        for (Map.Entry<Long, List<CheckRecordDeductionNew>> entry : byItemId.entrySet()) {
            List<CheckRecordDeductionNew> itemDeductions = entry.getValue();
            CheckRecordDeductionNew firstDeduction = itemDeductions.get(0);

            WeightConfigTreeDTO.ItemWeightNode itemNode = new WeightConfigTreeDTO.ItemWeightNode();
            itemNode.setItemId(firstDeduction.getDeductionItemId());
            itemNode.setItemName(firstDeduction.getDeductionItemName());
            itemNode.setItemCode(firstDeduction.getDeductionItemCode());

            // 默认继承类别配置
            itemNode.setWeightEnabled(parentWeightEnabled);
            itemNode.setInherited(true);
            itemNode.setInheritSource("继承自类别配置");
            itemNode.setEffectiveConfig(parentConfig);

            itemNode.setBaseScore(firstDeduction.getBaseScore());
            itemNode.setDeductMode(getDeductModeString(firstDeduction.getDeductMode()));
            itemNode.setDeductModeDesc(getDeductModeDesc(firstDeduction.getDeductMode()));

            itemNode.setDeductionCount(itemDeductions.size());
            BigDecimal originalTotal = itemDeductions.stream()
                    .map(CheckRecordDeductionNew::getActualScore)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            itemNode.setOriginalScore(originalTotal);
            itemNode.setWeightedScore(originalTotal); // 简化，实际需要按班级计算

            items.add(itemNode);
        }

        return items;
    }

    private String getDeductModeString(Integer mode) {
        if (mode == null) return "UNKNOWN";
        return switch (mode) {
            case 1 -> "FIXED_DEDUCT";
            case 2 -> "PER_PERSON_DEDUCT";
            case 3 -> "SCORE_RANGE";
            default -> "UNKNOWN";
        };
    }

    private String getDeductModeDesc(Integer mode) {
        if (mode == null) return "未知";
        return switch (mode) {
            case 1 -> "固定扣分";
            case 2 -> "按人数扣分";
            case 3 -> "区间扣分";
            default -> "未知";
        };
    }

    private List<WeightConfigTreeDTO.ClassWeightSummary> buildClassWeightSummaries(
            List<CheckRecordClassStatsNew> classStats,
            WeightConfigTreeDTO.WeightConfigNode globalConfig,
            DailyCheck dailyCheck,
            List<WeightConfigTreeDTO.CategoryWeightNode> categories) {

        List<WeightConfigTreeDTO.ClassWeightSummary> summaries = new ArrayList<>();

        for (CheckRecordClassStatsNew stat : classStats) {
            WeightConfigTreeDTO.ClassWeightSummary summary = new WeightConfigTreeDTO.ClassWeightSummary();
            summary.setClassId(stat.getClassId());
            summary.setClassName(stat.getClassName());

            // 获取班级人数
            Integer classSize = stat.getClassSize();
            if (classSize == null || classSize == 0) {
                var classResponse = classMapper.selectClassResponseById(stat.getClassId());
                if (classResponse != null) {
                    classSize = classResponse.getStudentCount();
                }
            }
            summary.setActualSize(classSize);
            summary.setStandardSize(globalConfig.getStandardSize());

            // 计算全局加权系数
            if (classSize != null && classSize > 0 && globalConfig.getStandardSize() != null && globalConfig.getStandardSize() > 0) {
                BigDecimal factor = calculateWeightFactor(
                        globalConfig.getWeightMode(),
                        classSize,
                        globalConfig.getStandardSize(),
                        convertToDetailSegmentRules(globalConfig.getSegmentRules())
                );
                summary.setGlobalWeightFactor(factor);
            } else {
                summary.setGlobalWeightFactor(BigDecimal.ONE);
            }

            summary.setOriginalTotalScore(stat.getTotalScore());

            // 计算加权后总分
            if (stat.getTotalScore() != null && summary.getGlobalWeightFactor() != null) {
                summary.setWeightedTotalScore(stat.getTotalScore()
                        .multiply(summary.getGlobalWeightFactor())
                        .setScale(2, RoundingMode.HALF_UP));
            }

            // 构建各类别的加权系数（当前简化为使用全局系数）
            List<WeightConfigTreeDTO.CategoryWeightFactor> categoryFactors = new ArrayList<>();
            for (WeightConfigTreeDTO.CategoryWeightNode category : categories) {
                WeightConfigTreeDTO.CategoryWeightFactor catFactor = new WeightConfigTreeDTO.CategoryWeightFactor();
                catFactor.setCategoryId(category.getCategoryId());
                catFactor.setCategoryName(category.getCategoryName());
                catFactor.setWeightFactor(summary.getGlobalWeightFactor()); // 继承全局系数
                // 原始分和加权分需要从扣分明细计算
                catFactor.setOriginalScore(BigDecimal.ZERO);
                catFactor.setWeightedScore(BigDecimal.ZERO);
                categoryFactors.add(catFactor);
            }
            summary.setCategoryFactors(categoryFactors);

            summaries.add(summary);
        }

        return summaries;
    }

    private List<WeightConfigTreeDTO.SegmentRule> parseTreeSegmentRules(String rulesJson) {
        List<WeightConfigTreeDTO.SegmentRule> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rulesList = objectMapper.readValue(rulesJson, List.class);
            for (Map<String, Object> rule : rulesList) {
                WeightConfigTreeDTO.SegmentRule sr = new WeightConfigTreeDTO.SegmentRule();
                sr.setMinSize(getIntValue(rule.get("minSize")));
                sr.setMaxSize(getIntValue(rule.get("maxSize")));
                Object weight = rule.get("weight");
                if (weight instanceof Number) {
                    sr.setWeight(new BigDecimal(weight.toString()));
                }
                sr.setDescription(String.format("%d-%d人: 系数%.2f",
                        sr.getMinSize(), sr.getMaxSize(),
                        sr.getWeight() != null ? sr.getWeight().doubleValue() : 1.0));
                result.add(sr);
            }
        } catch (Exception e) {
            log.warn("解析分段规则失败: {}", rulesJson, e);
        }
        return result;
    }

    private List<WeightConfigTreeDTO.SegmentRule> convertTreeSegmentRules(List<?> rulesList) {
        List<WeightConfigTreeDTO.SegmentRule> result = new ArrayList<>();
        for (Object rule : rulesList) {
            if (rule instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleMap = (Map<String, Object>) rule;
                WeightConfigTreeDTO.SegmentRule sr = new WeightConfigTreeDTO.SegmentRule();
                sr.setMinSize(getIntValue(ruleMap.get("minSize")));
                sr.setMaxSize(getIntValue(ruleMap.get("maxSize")));
                Object weight = ruleMap.get("weight");
                if (weight instanceof Number) {
                    sr.setWeight(new BigDecimal(weight.toString()));
                }
                sr.setDescription(String.format("%d-%d人: 系数%.2f",
                        sr.getMinSize(), sr.getMaxSize(),
                        sr.getWeight() != null ? sr.getWeight().doubleValue() : 1.0));
                result.add(sr);
            }
        }
        return result;
    }

    private List<WeightConfigDetailDTO.SegmentRule> convertToDetailSegmentRules(List<WeightConfigTreeDTO.SegmentRule> treeRules) {
        if (treeRules == null) return null;
        return treeRules.stream().map(tr -> {
            WeightConfigDetailDTO.SegmentRule dr = new WeightConfigDetailDTO.SegmentRule();
            dr.setMinSize(tr.getMinSize());
            dr.setMaxSize(tr.getMaxSize());
            dr.setWeight(tr.getWeight());
            dr.setDescription(tr.getDescription());
            return dr;
        }).collect(Collectors.toList());
    }

    /**
     * 将DailyCheckWeightConfigDTO转换为MultiWeightScheme
     */
    private WeightConfigTreeDTO.MultiWeightScheme convertToMultiWeightScheme(DailyCheckWeightConfigDTO config) {
        WeightConfigTreeDTO.MultiWeightScheme scheme = new WeightConfigTreeDTO.MultiWeightScheme();
        scheme.setId(config.getId());
        scheme.setWeightConfigId(config.getWeightConfigId());
        scheme.setConfigName(config.getWeightConfigName());
        scheme.setColorCode(config.getColorCode());
        scheme.setColorName(config.getColorName());
        scheme.setApplyScope(config.getApplyScope());
        scheme.setIsDefault(config.getIsDefault());
        scheme.setPriority(config.getPriority());
        scheme.setApplyCategoryIds(config.getApplyCategoryIds());
        scheme.setApplyCategoryNames(config.getApplyCategoryNames());
        scheme.setWeightMode(config.getWeightMode());
        scheme.setWeightModeDesc(config.getWeightModeDesc());
        scheme.setStandardSizeMode(config.getStandardSizeMode());
        scheme.setStandardSizeModeDesc(config.getStandardSizeModeDesc());
        scheme.setCalculatedStandardSize(config.getCalculatedStandardSize());
        scheme.setMinWeight(config.getMinWeight());
        scheme.setMaxWeight(config.getMaxWeight());

        // 生成公式说明
        String formula = getFormulaDescription(config.getWeightMode(), config.getCalculatedStandardSize());
        scheme.setFormulaDescription(formula);

        return scheme;
    }
}
