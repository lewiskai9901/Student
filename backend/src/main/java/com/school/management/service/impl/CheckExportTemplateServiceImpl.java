package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.export.*;
import com.school.management.entity.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.CheckExportTemplateService;
import com.school.management.service.ClassWeightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查导出模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckExportTemplateServiceImpl implements CheckExportTemplateService {

    private final CheckExportTemplateMapper templateMapper;
    private final CheckPlanMapper checkPlanMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckDetailMapper dailyCheckDetailMapper;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final DepartmentMapper departmentMapper;
    private final GradeMapper gradeMapper;
    private final DeductionItemMapper deductionItemMapper;
    private final CheckCategoryMapper checkCategoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final DocumentGeneratorService documentGeneratorService;
    private final ClassWeightService classWeightService;

    // 用于存储导出时的记录，供生成Word/Excel使用
    private final ThreadLocal<List<ExportPreviewDTO.StudentRecordDTO>> exportRecordsHolder = new ThreadLocal<>();
    private final ThreadLocal<ExportTemplateDTO.TableConfig> tableConfigHolder = new ThreadLocal<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTemplateDTO createTemplate(Long planId, ExportTemplateRequest request, Long operatorId) {
        log.info("创建导出模板, planId: {}, templateName: {}", planId, request.getTemplateName());

        // 验证检查计划存在
        CheckPlan plan = checkPlanMapper.selectById(planId);
        if (plan == null || plan.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查计划不存在");
        }

        CheckExportTemplate template = new CheckExportTemplate();
        template.setPlanId(planId);
        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setOutputFormat(request.getOutputFormat());
        template.setGroupBy(request.getGroupBy() != null ? request.getGroupBy() : "department,grade,class");
        template.setSortBy(request.getSortBy() != null ? request.getSortBy() : "studentNo");
        template.setDocumentTemplate(request.getDocumentTemplate());
        template.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        template.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        template.setCreatedBy(operatorId);

        // 序列化配置为JSON
        try {
            template.setFilterConfig(objectMapper.writeValueAsString(request.getFilterConfig()));
            // 优先使用tables数组，向后兼容tableConfig
            if (request.getTables() != null && !request.getTables().isEmpty()) {
                template.setTableConfig(objectMapper.writeValueAsString(request.getTables()));
            } else if (request.getTableConfig() != null) {
                template.setTableConfig(objectMapper.writeValueAsString(request.getTableConfig()));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "配置序列化失败");
        }

        templateMapper.insert(template);
        return convertToDTO(template, plan.getPlanName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTemplateDTO updateTemplate(Long templateId, ExportTemplateRequest request) {
        log.info("更新导出模板, templateId: {}", templateId);

        CheckExportTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出模板不存在");
        }

        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setOutputFormat(request.getOutputFormat());
        if (request.getGroupBy() != null) {
            template.setGroupBy(request.getGroupBy());
        }
        if (request.getSortBy() != null) {
            template.setSortBy(request.getSortBy());
        }
        template.setDocumentTemplate(request.getDocumentTemplate());
        if (request.getSortOrder() != null) {
            template.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }

        try {
            template.setFilterConfig(objectMapper.writeValueAsString(request.getFilterConfig()));
            // 优先使用tables数组，向后兼容tableConfig
            if (request.getTables() != null && !request.getTables().isEmpty()) {
                template.setTableConfig(objectMapper.writeValueAsString(request.getTables()));
            } else if (request.getTableConfig() != null) {
                template.setTableConfig(objectMapper.writeValueAsString(request.getTableConfig()));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "配置序列化失败");
        }

        templateMapper.updateById(template);

        CheckPlan plan = checkPlanMapper.selectById(template.getPlanId());
        return convertToDTO(template, plan != null ? plan.getPlanName() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        log.info("删除导出模板, templateId: {}", templateId);
        CheckExportTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出模板不存在");
        }
        templateMapper.deleteById(templateId);
    }

    @Override
    public ExportTemplateDTO getTemplate(Long templateId) {
        CheckExportTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出模板不存在");
        }
        CheckPlan plan = checkPlanMapper.selectById(template.getPlanId());
        return convertToDTO(template, plan != null ? plan.getPlanName() : null);
    }

    @Override
    public List<ExportTemplateDTO> getTemplatesByPlanId(Long planId) {
        CheckPlan plan = checkPlanMapper.selectById(planId);
        String planName = plan != null ? plan.getPlanName() : null;

        LambdaQueryWrapper<CheckExportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckExportTemplate::getPlanId, planId)
                .eq(CheckExportTemplate::getDeleted, 0)
                .orderByAsc(CheckExportTemplate::getSortOrder)
                .orderByDesc(CheckExportTemplate::getCreatedAt);

        return templateMapper.selectList(wrapper).stream()
                .map(t -> convertToDTO(t, planName))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExportTemplateDTO> getTemplatesForCheck(Long checkId) {
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日常检查不存在");
        }

        if (dailyCheck.getPlanId() == null) {
            return Collections.emptyList();
        }

        return getTemplatesByPlanId(dailyCheck.getPlanId());
    }

    @Override
    public ExportPreviewDTO getExportPreview(Long checkId, ExportRequest request) {
        log.info("获取导出预览, checkId: {}, templateId: {}", checkId, request.getTemplateId());

        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null || dailyCheck.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日常检查不存在");
        }

        CheckExportTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null || template.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出模板不存在");
        }

        // 解析筛选配置
        ExportTemplateDTO.FilterConfig filterConfig = parseFilterConfig(template.getFilterConfig());
        ExportTemplateDTO.TableConfig tableConfig = parseTableConfig(template.getTableConfig());
        List<ExportTemplateDTO.TableConfig> tables = parseTables(template.getTableConfig());

        // 如果请求中指定了轮次，使用请求中的轮次覆盖模板配置
        if (request.getCheckRounds() != null && !request.getCheckRounds().isEmpty()) {
            filterConfig.setCheckRounds(request.getCheckRounds());
        }

        // 查询符合条件的扣分记录
        List<ExportPreviewDTO.StudentRecordDTO> records = queryExportRecords(
                checkId, filterConfig, request.getClassIds());

        // 统计信息
        ExportPreviewDTO preview = new ExportPreviewDTO();
        preview.setCheckDate(dailyCheck.getCheckDate().toString());
        preview.setCheckName(dailyCheck.getCheckName());
        preview.setTotalCount(records.size());

        // 按班级统计
        Map<Long, List<ExportPreviewDTO.StudentRecordDTO>> byClass = records.stream()
                .collect(Collectors.groupingBy(ExportPreviewDTO.StudentRecordDTO::getClassId));
        preview.setClassCount(byClass.size());

        // 按部门统计
        Set<Long> departments = records.stream()
                .map(ExportPreviewDTO.StudentRecordDTO::getOrgUnitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        preview.setOrgUnitCount(departments.size());

        // 班级统计列表
        List<ExportPreviewDTO.ClassStatDTO> classStats = new ArrayList<>();
        byClass.forEach((classId, classRecords) -> {
            ExportPreviewDTO.StudentRecordDTO first = classRecords.get(0);
            ExportPreviewDTO.ClassStatDTO stat = new ExportPreviewDTO.ClassStatDTO();
            stat.setClassId(classId);
            stat.setClassName(first.getClassName());
            stat.setOrgUnitId(first.getOrgUnitId());
            stat.setOrgUnitName(first.getOrgUnitName());
            stat.setGradeId(first.getGradeId());
            stat.setGradeName(first.getGradeName());
            stat.setStudentCount(classRecords.size());
            classStats.add(stat);
        });
        // 按部门、年级、班级排序
        classStats.sort(Comparator
                .comparing(ExportPreviewDTO.ClassStatDTO::getOrgUnitName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.ClassStatDTO::getGradeName, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExportPreviewDTO.ClassStatDTO::getClassName, Comparator.nullsLast(String::compareTo)));
        preview.setClassStats(classStats);

        // 分组数据
        List<ExportPreviewDTO.OrgUnitGroupDTO> groupedData = groupRecords(records);
        preview.setGroupedData(groupedData);

        // 渲染HTML预览（支持多表格）
        String renderedHtml = renderDocument(template, dailyCheck, records, tableConfig, tables);
        preview.setRenderedHtml(renderedHtml);

        return preview;
    }

    @Override
    public byte[] exportFile(Long checkId, ExportRequest request) {
        log.info("导出文件, checkId: {}, templateId: {}", checkId, request.getTemplateId());

        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        if (dailyCheck == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日常检查不存在");
        }

        CheckExportTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出模板不存在");
        }

        ExportTemplateDTO.FilterConfig filterConfig = parseFilterConfig(template.getFilterConfig());
        ExportTemplateDTO.TableConfig tableConfig = parseTableConfig(template.getTableConfig());
        List<ExportTemplateDTO.TableConfig> tables = parseTables(template.getTableConfig());

        // 如果请求中指定了轮次，使用请求中的轮次覆盖模板配置
        if (request.getCheckRounds() != null && !request.getCheckRounds().isEmpty()) {
            filterConfig.setCheckRounds(request.getCheckRounds());
        }

        List<ExportPreviewDTO.StudentRecordDTO> records = queryExportRecords(
                checkId, filterConfig, request.getClassIds());

        String renderedHtml = renderDocument(template, dailyCheck, records, tableConfig, tables);

        String format = StringUtils.hasText(request.getFormat()) ? request.getFormat() : template.getOutputFormat();

        // 设置ThreadLocal供Word生成使用
        exportRecordsHolder.set(records);
        tableConfigHolder.set(tableConfig);

        try {
            // 根据格式生成文件
            return switch (format.toUpperCase()) {
                case "PDF" -> generatePdf(renderedHtml);
                case "WORD" -> generateWord(renderedHtml);
                case "EXCEL" -> generateExcel(records, tableConfig);
                default -> throw new BusinessException(ResultCode.VALIDATION_ERROR, "不支持的导出格式: " + format);
            };
        } finally {
            // 清理ThreadLocal
            exportRecordsHolder.remove();
            tableConfigHolder.remove();
        }
    }

    @Override
    public String getExportFileName(Long checkId, ExportRequest request) {
        DailyCheck dailyCheck = dailyCheckMapper.selectById(checkId);
        CheckExportTemplate template = templateMapper.selectById(request.getTemplateId());

        String format = StringUtils.hasText(request.getFormat()) ? request.getFormat() : template.getOutputFormat();
        String extension = switch (format.toUpperCase()) {
            case "PDF" -> ".pdf";
            case "WORD" -> ".docx";
            case "EXCEL" -> ".xlsx";
            default -> ".pdf";
        };

        String datePart = dailyCheck.getCheckDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return template.getTemplateName() + "_" + datePart + extension;
    }

    // ========== 私有方法 ==========

    private ExportTemplateDTO convertToDTO(CheckExportTemplate template, String planName) {
        ExportTemplateDTO dto = new ExportTemplateDTO();
        dto.setId(template.getId());
        dto.setPlanId(template.getPlanId());
        dto.setPlanName(planName);
        dto.setTemplateName(template.getTemplateName());
        dto.setDescription(template.getDescription());
        dto.setOutputFormat(template.getOutputFormat());
        dto.setSortOrder(template.getSortOrder());
        dto.setStatus(template.getStatus());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setDocumentTemplate(template.getDocumentTemplate());
        dto.setFilterConfig(parseFilterConfig(template.getFilterConfig()));
        dto.setTableConfig(parseTableConfig(template.getTableConfig()));
        dto.setTables(parseTables(template.getTableConfig()));
        return dto;
    }

    private ExportTemplateDTO.FilterConfig parseFilterConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new ExportTemplateDTO.FilterConfig();
        }
        try {
            return objectMapper.readValue(json, ExportTemplateDTO.FilterConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("解析筛选配置失败: {}", e.getMessage());
            return new ExportTemplateDTO.FilterConfig();
        }
    }

    private ExportTemplateDTO.TableConfig parseTableConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new ExportTemplateDTO.TableConfig();
        }
        try {
            // 检查是否是数组格式（多表格配置）
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                // 数组格式：返回第一个表格作为向后兼容
                List<ExportTemplateDTO.TableConfig> tables = parseTables(json);
                return tables.isEmpty() ? new ExportTemplateDTO.TableConfig() : tables.get(0);
            }
            // 单表格对象格式
            return objectMapper.readValue(json, ExportTemplateDTO.TableConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("解析表格配置失败: {}", e.getMessage());
            return new ExportTemplateDTO.TableConfig();
        }
    }

    /**
     * 解析多表格配置
     */
    private List<ExportTemplateDTO.TableConfig> parseTables(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                // 数组格式
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ExportTemplateDTO.TableConfig.class));
            } else {
                // 单表格对象格式，包装成数组
                ExportTemplateDTO.TableConfig single = objectMapper.readValue(json, ExportTemplateDTO.TableConfig.class);
                List<ExportTemplateDTO.TableConfig> list = new ArrayList<>();
                list.add(single);
                return list;
            }
        } catch (JsonProcessingException e) {
            log.warn("解析多表格配置失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 查询导出记录
     */
    private List<ExportPreviewDTO.StudentRecordDTO> queryExportRecords(
            Long checkId,
            ExportTemplateDTO.FilterConfig filterConfig,
            List<Long> selectedClassIds) {

        // 查询扣分明细
        LambdaQueryWrapper<DailyCheckDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckDetail::getCheckId, checkId)
                .eq(DailyCheckDetail::getDeleted, 0);

        // 按扣分项筛选
        if (filterConfig.getDeductionItemIds() != null && !filterConfig.getDeductionItemIds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getDeductionItemId, filterConfig.getDeductionItemIds());
        }

        // 按轮次筛选
        if (filterConfig.getCheckRounds() != null && !filterConfig.getCheckRounds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getCheckRound, filterConfig.getCheckRounds());
        }

        // 按类别筛选
        if (filterConfig.getCategoryIds() != null && !filterConfig.getCategoryIds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getCategoryId, filterConfig.getCategoryIds());
        }

        // 按班级筛选
        if (selectedClassIds != null && !selectedClassIds.isEmpty()) {
            wrapper.in(DailyCheckDetail::getClassId, selectedClassIds);
        }

        List<DailyCheckDetail> details = dailyCheckDetailMapper.selectList(wrapper);

        // 收集所有班级ID
        Set<Long> classIds = details.stream()
                .map(DailyCheckDetail::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 查询班级信息
        Map<Long, com.school.management.entity.Class> classMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            classMapper.selectBatchIds(classIds).forEach(c -> classMap.put(c.getId(), c));
        }

        // 收集组织单元和年级ID
        Set<Long> orgUnitIds = new HashSet<>();
        Set<Long> gradeIds = new HashSet<>();
        classMap.values().forEach(c -> {
            if (c.getOrgUnitId() != null) orgUnitIds.add(c.getOrgUnitId());
            if (c.getGradeId() != null) gradeIds.add(c.getGradeId());
        });

        // 查询组织单元信息
        Map<Long, Department> departmentMap = new HashMap<>();
        if (!orgUnitIds.isEmpty()) {
            departmentMapper.selectBatchIds(orgUnitIds).forEach(d -> departmentMap.put(d.getId(), d));
        }

        // 查询年级信息
        Map<Long, Grade> gradeMap = new HashMap<>();
        if (!gradeIds.isEmpty()) {
            gradeMapper.selectBatchIds(gradeIds).forEach(g -> gradeMap.put(g.getId(), g));
        }

        // 收集类别ID和检查员ID
        Set<Long> categoryIds = details.stream()
                .map(DailyCheckDetail::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> checkerIds = details.stream()
                .map(DailyCheckDetail::getCheckerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 查询类别信息
        Map<Long, CheckCategory> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            checkCategoryMapper.selectBatchIds(categoryIds).forEach(c -> categoryMap.put(c.getId(), c));
        }

        // 查询检查员信息
        Map<Long, User> checkerMap = new HashMap<>();
        if (!checkerIds.isEmpty()) {
            userMapper.selectBatchIds(checkerIds).forEach(u -> checkerMap.put(u.getId(), u));
        }

        // 构建记录列表
        // 设计思想：以扣分项为核心
        // - 固定扣分/区间扣分：生成1条记录，不关联学生（学号、姓名为空）
        // - 按人数扣分：有N个学生就生成N条记录，每条记录关联一个学生
        List<ExportPreviewDTO.StudentRecordDTO> records = new ArrayList<>();

        for (DailyCheckDetail detail : details) {
            com.school.management.entity.Class classInfo = classMap.get(detail.getClassId());
            Department dept = classInfo != null ? departmentMap.get(classInfo.getOrgUnitId()) : null;
            Grade grade = classInfo != null ? gradeMap.get(classInfo.getGradeId()) : null;

            // 判断扣分模式：1=固定扣分, 2=按人数扣分, 3=区间扣分
            Integer deductMode = detail.getDeductMode() != null ? detail.getDeductMode() : 1;
            String deductModeStr = switch (deductMode) {
                case 2 -> "按人数扣分";
                case 3 -> "区间扣分";
                default -> "固定扣分";
            };

            // 设置关联类型
            Integer linkType = detail.getLinkType() != null ? detail.getLinkType() : 0;
            String relationType = switch (linkType) {
                case 1 -> "宿舍";
                case 2 -> "教室";
                default -> "无";
            };

            // 扣分分值
            double deductScore = detail.getDeductScore() != null ? detail.getDeductScore().doubleValue() : 0;
            // 获取班级加权系数
            java.math.BigDecimal weightFactor = classWeightService.getWeightFactor(
                    detail.getClassId(),
                    detail.getCheckTime() != null ? detail.getCheckTime().toLocalDate() : java.time.LocalDate.now()
            );
            double weight = weightFactor != null ? weightFactor.doubleValue() : 1.0;

            if (deductMode == 2 && StringUtils.hasText(detail.getStudentIds())) {
                // ========== 按人数扣分：为每个学生创建一条记录 ==========
                String[] studentIdArray = detail.getStudentIds().split(",");
                String[] studentNameArray = StringUtils.hasText(detail.getStudentNames())
                        ? detail.getStudentNames().split(",")
                        : new String[0];

                // 有效学生数量（这次扣分扣了多少人）
                int totalStudentCount = (int) java.util.Arrays.stream(studentIdArray)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .count();

                for (int i = 0; i < studentIdArray.length; i++) {
                    String studentIdStr = studentIdArray[i].trim();
                    if (studentIdStr.isEmpty()) continue;

                    ExportPreviewDTO.StudentRecordDTO record = new ExportPreviewDTO.StudentRecordDTO();

                    // 学生信息
                    try {
                        Long studentId = Long.parseLong(studentIdStr);
                        record.setStudentId(studentId);
                        record.setStudentName(i < studentNameArray.length ? studentNameArray[i].trim() : "");
                        // 查询学生学号
                        Student student = studentMapper.selectById(studentId);
                        if (student != null) {
                            record.setStudentNo(student.getStudentNo());
                            // 性别: 1=男, 2=女
                            Integer genderInt = student.getGender();
                            record.setGender(genderInt != null ? (genderInt == 1 ? "男" : genderInt == 2 ? "女" : "") : "");
                        }
                    } catch (Exception e) {
                        log.warn("解析学生信息失败: {}", e.getMessage());
                        record.setStudentName(i < studentNameArray.length ? studentNameArray[i].trim() : "");
                    }

                    // 班级/年级/部门信息
                    record.setClassId(detail.getClassId());
                    record.setClassName(classInfo != null ? classInfo.getClassName() : "");
                    record.setOrgUnitId(classInfo != null ? classInfo.getOrgUnitId() : null);
                    record.setOrgUnitName(dept != null ? dept.getDeptName() : "");
                    record.setGradeId(classInfo != null ? classInfo.getGradeId() : null);
                    record.setGradeName(grade != null ? grade.getGradeName() : "");
                    record.setHeadTeacher(classInfo != null ? classInfo.getTeacherName() : "");

                    // 扣分项信息
                    record.setDeductionItemId(detail.getDeductionItemId());
                    record.setDeductionItemName(detail.getDeductionItemName());
                    CheckCategory category = categoryMap.get(detail.getCategoryId());
                    record.setCategoryName(category != null ? category.getCategoryName() : "");
                    record.setDeductMode(deductModeStr);
                    record.setRelationType(relationType);
                    record.setRemark(detail.getRemark());
                    record.setCheckRound(detail.getCheckRound());
                    User checker = checkerMap.get(detail.getCheckerId());
                    record.setCheckerName(checker != null ? checker.getRealName() : "");

                    // 关联宿舍/教室信息
                    if ((linkType == 1 || linkType == 2) && StringUtils.hasText(detail.getLinkNo())) {
                        record.setRoomNo(detail.getLinkNo());
                    }

                    // 扣分计算：
                    // personCount = 这次扣分的总人数（所有来自同一次扣分的记录都显示相同的次数）
                    // 例如：迟到扣了张三、李四，则两条记录的次数都显示2
                    record.setPersonCount(totalStudentCount);
                    record.setDeductScore(deductScore);
                    // 扣分项原总扣分 = 扣分分值 * 总人数（这次扣分的总扣分）
                    record.setOriginalTotalScore(deductScore * totalStudentCount);
                    record.setWeightedDeductScore(deductScore * weight);
                    record.setTotalWeightedDeductScore(deductScore * weight * totalStudentCount);

                    records.add(record);
                }
            } else {
                // ========== 固定扣分/区间扣分：生成1条记录，不关联学生 ==========
                ExportPreviewDTO.StudentRecordDTO record = new ExportPreviewDTO.StudentRecordDTO();

                // 学生信息为空（固定扣分不关联具体学生）
                record.setStudentId(null);
                record.setStudentNo("");
                record.setStudentName("");
                record.setGender("");

                // 班级/年级/部门信息
                record.setClassId(detail.getClassId());
                record.setClassName(classInfo != null ? classInfo.getClassName() : "");
                record.setOrgUnitId(classInfo != null ? classInfo.getOrgUnitId() : null);
                record.setOrgUnitName(dept != null ? dept.getDeptName() : "");
                record.setGradeId(classInfo != null ? classInfo.getGradeId() : null);
                record.setGradeName(grade != null ? grade.getGradeName() : "");
                record.setHeadTeacher(classInfo != null ? classInfo.getTeacherName() : "");

                // 扣分项信息
                record.setDeductionItemId(detail.getDeductionItemId());
                record.setDeductionItemName(detail.getDeductionItemName());
                CheckCategory category = categoryMap.get(detail.getCategoryId());
                record.setCategoryName(category != null ? category.getCategoryName() : "");
                record.setDeductMode(deductModeStr);
                record.setRelationType(relationType);
                record.setRemark(detail.getRemark());
                record.setCheckRound(detail.getCheckRound());
                User checker = checkerMap.get(detail.getCheckerId());
                record.setCheckerName(checker != null ? checker.getRealName() : "");

                // 关联宿舍/教室信息
                if ((linkType == 1 || linkType == 2) && StringUtils.hasText(detail.getLinkNo())) {
                    record.setRoomNo(detail.getLinkNo());
                }

                // 扣分计算：固定扣分/区间扣分，personCount = 1（代表这1次扣分）
                record.setPersonCount(1);
                record.setDeductScore(deductScore);
                record.setOriginalTotalScore(deductScore);
                record.setWeightedDeductScore(deductScore * weight);
                record.setTotalWeightedDeductScore(deductScore * weight);

                records.add(record);
            }
        }

        // ========== 计算班级总分和检查类别总分 ==========
        // 1. 按班级分组计算班级总分
        Map<Long, Double> classOriginalScoreMap = new HashMap<>();
        Map<Long, Double> classWeightedScoreMap = new HashMap<>();
        for (ExportPreviewDTO.StudentRecordDTO record : records) {
            Long classId = record.getClassId();
            if (classId != null) {
                classOriginalScoreMap.merge(classId,
                    record.getOriginalTotalScore() != null ? record.getOriginalTotalScore() : 0.0, Double::sum);
                classWeightedScoreMap.merge(classId,
                    record.getTotalWeightedDeductScore() != null ? record.getTotalWeightedDeductScore() : 0.0, Double::sum);
            }
        }

        // 2. 按班级+检查类别分组计算检查类别总分
        Map<String, Double> categoryOriginalScoreMap = new HashMap<>();
        Map<String, Double> categoryWeightedScoreMap = new HashMap<>();
        for (ExportPreviewDTO.StudentRecordDTO record : records) {
            Long classId = record.getClassId();
            String categoryName = record.getCategoryName();
            if (classId != null && categoryName != null) {
                String key = classId + "_" + categoryName;
                categoryOriginalScoreMap.merge(key,
                    record.getOriginalTotalScore() != null ? record.getOriginalTotalScore() : 0.0, Double::sum);
                categoryWeightedScoreMap.merge(key,
                    record.getTotalWeightedDeductScore() != null ? record.getTotalWeightedDeductScore() : 0.0, Double::sum);
            }
        }

        // 3. 将计算结果回填到每条记录
        for (ExportPreviewDTO.StudentRecordDTO record : records) {
            Long classId = record.getClassId();
            String categoryName = record.getCategoryName();

            // 设置班级总分
            if (classId != null) {
                record.setClassOriginalScore(classOriginalScoreMap.getOrDefault(classId, 0.0));
                record.setClassWeightedScore(classWeightedScoreMap.getOrDefault(classId, 0.0));
            }

            // 设置检查类别总分
            if (classId != null && categoryName != null) {
                String key = classId + "_" + categoryName;
                record.setCategoryOriginalScore(categoryOriginalScoreMap.getOrDefault(key, 0.0));
                record.setCategoryWeightedScore(categoryWeightedScoreMap.getOrDefault(key, 0.0));
            }
        }

        // 排序
        records.sort(Comparator
                .comparing(ExportPreviewDTO.StudentRecordDTO::getOrgUnitName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getGradeName, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getClassName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getStudentNo, Comparator.nullsLast(String::compareTo)));

        return records;
    }

    /**
     * 按部门→年级→班级分组
     */
    private List<ExportPreviewDTO.OrgUnitGroupDTO> groupRecords(List<ExportPreviewDTO.StudentRecordDTO> records) {
        Map<Long, ExportPreviewDTO.OrgUnitGroupDTO> deptMap = new LinkedHashMap<>();

        for (ExportPreviewDTO.StudentRecordDTO record : records) {
            Long deptId = record.getOrgUnitId() != null ? record.getOrgUnitId() : 0L;
            Long gradeId = record.getGradeId() != null ? record.getGradeId() : 0L;
            Long classId = record.getClassId() != null ? record.getClassId() : 0L;

            // 获取或创建部门分组
            ExportPreviewDTO.OrgUnitGroupDTO deptGroup = deptMap.computeIfAbsent(deptId, id -> {
                ExportPreviewDTO.OrgUnitGroupDTO g = new ExportPreviewDTO.OrgUnitGroupDTO();
                g.setOrgUnitId(id);
                g.setOrgUnitName(record.getOrgUnitName());
                g.setTotalCount(0);
                g.setGrades(new ArrayList<>());
                return g;
            });

            // 获取或创建年级分组
            ExportPreviewDTO.GradeGroupDTO gradeGroup = deptGroup.getGrades().stream()
                    .filter(g -> g.getGradeId().equals(gradeId))
                    .findFirst()
                    .orElseGet(() -> {
                        ExportPreviewDTO.GradeGroupDTO g = new ExportPreviewDTO.GradeGroupDTO();
                        g.setGradeId(gradeId);
                        g.setGradeName(record.getGradeName());
                        g.setTotalCount(0);
                        g.setClasses(new ArrayList<>());
                        deptGroup.getGrades().add(g);
                        return g;
                    });

            // 获取或创建班级分组
            ExportPreviewDTO.ClassGroupDTO classGroup = gradeGroup.getClasses().stream()
                    .filter(c -> c.getClassId().equals(classId))
                    .findFirst()
                    .orElseGet(() -> {
                        ExportPreviewDTO.ClassGroupDTO c = new ExportPreviewDTO.ClassGroupDTO();
                        c.setClassId(classId);
                        c.setClassName(record.getClassName());
                        c.setTotalCount(0);
                        c.setStudents(new ArrayList<>());
                        gradeGroup.getClasses().add(c);
                        return c;
                    });

            // 添加学生记录
            classGroup.getStudents().add(record);
            classGroup.setTotalCount(classGroup.getTotalCount() + 1);
            gradeGroup.setTotalCount(gradeGroup.getTotalCount() + 1);
            deptGroup.setTotalCount(deptGroup.getTotalCount() + 1);
        }

        return new ArrayList<>(deptMap.values());
    }

    /**
     * 渲染文档HTML（支持多表格）
     */
    private String renderDocument(
            CheckExportTemplate template,
            DailyCheck dailyCheck,
            List<ExportPreviewDTO.StudentRecordDTO> records,
            ExportTemplateDTO.TableConfig tableConfig) {
        // 向后兼容：单表格模式
        return renderDocument(template, dailyCheck, records, tableConfig, null);
    }

    /**
     * 渲染文档HTML（支持多表格）
     */
    private String renderDocument(
            CheckExportTemplate template,
            DailyCheck dailyCheck,
            List<ExportPreviewDTO.StudentRecordDTO> records,
            ExportTemplateDTO.TableConfig tableConfig,
            List<ExportTemplateDTO.TableConfig> tables) {

        String documentHtml = template.getDocumentTemplate();
        if (!StringUtils.hasText(documentHtml)) {
            documentHtml = getDefaultDocumentTemplate();
        }

        // 替换变量
        String checkDate = dailyCheck.getCheckDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        String exportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        documentHtml = documentHtml
                .replace("{{checkDate}}", checkDate)
                .replace("{{checkName}}", dailyCheck.getCheckName() != null ? dailyCheck.getCheckName() : "")
                .replace("{{totalCount}}", String.valueOf(records.size()))
                .replace("{{exportDate}}", exportDate);

        // 处理多表格占位符
        if (tables != null && !tables.isEmpty()) {
            // 生成所有表格HTML
            StringBuilder allTablesHtml = new StringBuilder();
            for (int i = 0; i < tables.size(); i++) {
                ExportTemplateDTO.TableConfig tc = tables.get(i);
                String singleTableHtml = generateTableHtml(records, tc);

                // 替换 {{TABLE_N}} 占位符（N从1开始）
                documentHtml = documentHtml.replace("{{TABLE_" + (i + 1) + "}}", singleTableHtml);

                // 构建所有表格拼接
                if (allTablesHtml.length() > 0) {
                    allTablesHtml.append("<div style=\"margin-top:20px;\"></div>");
                }
                allTablesHtml.append(singleTableHtml);
            }

            // 替换 {{TABLE_ALL}} 占位符
            documentHtml = documentHtml.replace("{{TABLE_ALL}}", allTablesHtml.toString());

            // 向后兼容：{{TABLE}} 使用第一个表格
            if (documentHtml.contains("{{TABLE}}")) {
                String firstTableHtml = generateTableHtml(records, tables.get(0));
                documentHtml = documentHtml.replace("{{TABLE}}", firstTableHtml);
            }
        } else if (tableConfig != null) {
            // 单表格模式
            String tableHtml = generateTableHtml(records, tableConfig);
            documentHtml = documentHtml.replace("{{TABLE}}", tableHtml);
        }

        return wrapHtmlDocument(documentHtml);
    }

    /**
     * 生成表格HTML
     */
    private String generateTableHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            ExportTemplateDTO.TableConfig tableConfig) {

        if (records.isEmpty()) {
            return "<p>暂无数据</p>";
        }

        // 调试日志
        log.info("生成表格HTML - 记录数: {}", records.size());
        log.info("表格配置 - columns: {}", tableConfig.getColumns());
        log.info("表格配置 - mergeConfig: {}", tableConfig.getMergeConfig());
        if (tableConfig.getMergeConfig() != null) {
            log.info("合并配置 - enabled: {}, hierarchyLevels: {}",
                    tableConfig.getMergeConfig().getEnabled(),
                    tableConfig.getMergeConfig().getHierarchyLevels());
        }

        List<ExportTemplateDTO.ColumnConfig> columns = tableConfig.getColumns();
        if (columns == null || columns.isEmpty()) {
            // 默认列配置
            columns = Arrays.asList(
                    createColumn("index", "序号", 60),
                    createColumn("studentNo", "学号", 100),
                    createColumn("studentName", "姓名", 80),
                    createColumn("className", "班级", 120),
                    createColumn("deductionItemName", "扣分项", 120)
            );
        }

        // 获取样式配置
        String borderColor = tableConfig.getBorderColor() != null ? tableConfig.getBorderColor() : "#000";
        String borderWidth = tableConfig.getBorderWidth() != null ? tableConfig.getBorderWidth() : "1px";
        String borderStyle = tableConfig.getBorderStyle() != null ? tableConfig.getBorderStyle() : "solid";
        String border = borderWidth + " " + borderStyle + " " + borderColor;
        String headerBg = tableConfig.getHeaderBgColor() != null ? tableConfig.getHeaderBgColor() : "#fff";
        String headerText = tableConfig.getHeaderTextColor() != null ? tableConfig.getHeaderTextColor() : "#000";
        String headerFontWeight = tableConfig.getHeaderFontWeight() != null ? tableConfig.getHeaderFontWeight() : "bold";
        String headerFontSize = tableConfig.getHeaderFontSize() != null ? tableConfig.getHeaderFontSize() : "14px";
        String padding = tableConfig.getCellPadding() != null ? tableConfig.getCellPadding() : "8px";
        String fontSize = tableConfig.getFontSize() != null ? tableConfig.getFontSize() : "14px";
        String textAlign = tableConfig.getTextAlign() != null ? tableConfig.getTextAlign() : "center";
        String verticalAlign = tableConfig.getVerticalAlign() != null ? tableConfig.getVerticalAlign() : "middle";
        String lineHeight = tableConfig.getLineHeight() != null ? tableConfig.getLineHeight() : "1.5";
        boolean zebra = Boolean.TRUE.equals(tableConfig.getZebraStripes());
        String zebraColor = tableConfig.getZebraColor() != null ? tableConfig.getZebraColor() : "#f9f9f9";

        // 检查是否启用分组合并
        ExportTemplateDTO.MergeConfig mergeConfig = tableConfig.getMergeConfig();
        boolean enableMerge = mergeConfig != null && Boolean.TRUE.equals(mergeConfig.getEnabled())
                && mergeConfig.getHierarchyLevels() != null && !mergeConfig.getHierarchyLevels().isEmpty();

        StringBuilder html = new StringBuilder();

        // 表格标题
        if (Boolean.TRUE.equals(tableConfig.getShowTitle()) && StringUtils.hasText(tableConfig.getTitle())) {
            html.append("<div style=\"text-align:center;font-weight:bold;font-size:16px;margin-bottom:10px;\">")
                    .append(tableConfig.getTitle())
                    .append("</div>");
        }

        html.append("<table class=\"data-table\" style=\"width:100%;border-collapse:collapse;font-size:")
                .append(fontSize).append(";line-height:").append(lineHeight).append(";\">");

        // 表头 - 使用 alias 优先于 label
        html.append("<thead><tr>");
        for (ExportTemplateDTO.ColumnConfig col : columns) {
            String headerLabel = StringUtils.hasText(col.getAlias()) ? col.getAlias() : col.getLabel();
            html.append("<th style=\"border:").append(border).append(";padding:").append(padding)
                    .append(";background:").append(headerBg).append(";color:").append(headerText)
                    .append(";font-weight:").append(headerFontWeight).append(";font-size:").append(headerFontSize)
                    .append(";text-align:center;vertical-align:").append(verticalAlign).append(";\">")
                    .append(headerLabel)
                    .append("</th>");
        }
        html.append("</tr></thead>");

        html.append("<tbody>");

        if (enableMerge) {
            // 分组合并模式：层级列使用rowspan，数据列可选择是否合并
            List<String> hierarchyLevels = mergeConfig.getHierarchyLevels();
            boolean concatDataRows = Boolean.TRUE.equals(mergeConfig.getConcatDataRows());
            String separator = mergeConfig.getSeparator() != null ? mergeConfig.getSeparator() : "、";
            if ("\\n".equals(separator)) {
                separator = "<br/>";
            }

            if (concatDataRows) {
                // 同组数据合并成一行，层级列rowspan，数据列concat
                html.append(generateConcatModeHtml(records, columns, hierarchyLevels, separator,
                        border, padding, textAlign, verticalAlign, zebra, zebraColor));
            } else {
                // 每条数据一行，层级列使用rowspan合并
                html.append(generateRowspanModeHtml(records, columns, hierarchyLevels,
                        border, padding, textAlign, verticalAlign, zebra, zebraColor));
            }
        } else {
            // 简单模式 - 不分组，按原有逻辑
            Boolean showDept = tableConfig.getShowDepartmentHeader();
            Boolean showGrade = tableConfig.getShowGradeHeader();
            Boolean showClass = tableConfig.getShowClassHeader();

            String currentDept = null;
            String currentGrade = null;
            String currentClass = null;
            int index = 0;

            for (ExportPreviewDTO.StudentRecordDTO record : records) {
                // 部门分组头
                if (Boolean.TRUE.equals(showDept) && !Objects.equals(currentDept, record.getOrgUnitName())) {
                    currentDept = record.getOrgUnitName();
                    currentGrade = null;
                    currentClass = null;
                    html.append("<tr><td colspan=\"").append(columns.size())
                            .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";background:#e0e0e0;font-weight:bold;\">")
                            .append(currentDept != null ? currentDept : "未分配部门")
                            .append("</td></tr>");
                }

                // 年级分组头
                if (Boolean.TRUE.equals(showGrade) && !Objects.equals(currentGrade, record.getGradeName())) {
                    currentGrade = record.getGradeName();
                    currentClass = null;
                    html.append("<tr><td colspan=\"").append(columns.size())
                            .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";background:#eee;padding-left:20px;\">")
                            .append(currentGrade != null ? currentGrade : "未分配年级")
                            .append("</td></tr>");
                }

                // 班级分组头
                if (Boolean.TRUE.equals(showClass) && !Objects.equals(currentClass, record.getClassName())) {
                    currentClass = record.getClassName();
                    html.append("<tr><td colspan=\"").append(columns.size())
                            .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";background:#f5f5f5;padding-left:40px;\">")
                            .append(currentClass != null ? currentClass : "未分配班级")
                            .append("</td></tr>");
                }

                // 数据行
                index++;
                String rowBg = zebra && index % 2 == 0 ? zebraColor : "#fff";
                html.append("<tr style=\"background:").append(rowBg).append(";\">");
                for (ExportTemplateDTO.ColumnConfig col : columns) {
                    html.append("<td style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign).append(";\">");
                    html.append(getFieldValue(record, col.getField(), index));
                    html.append("</td>");
                }
                html.append("</tr>");
            }
        }

        html.append("</tbody></table>");
        return html.toString();
    }

    /**
     * 生成分组合并模式（rowspan）的HTML
     */
    private String generateRowspanModeHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            List<ExportTemplateDTO.ColumnConfig> columns,
            List<String> hierarchyLevels,
            String border, String padding, String textAlign, String verticalAlign,
            boolean zebra, String zebraColor) {

        StringBuilder html = new StringBuilder();

        // 计算每行每个层级字段的 rowspan
        int[][] rowspans = new int[records.size()][hierarchyLevels.size()];

        for (int levelIdx = 0; levelIdx < hierarchyLevels.size(); levelIdx++) {
            String level = hierarchyLevels.get(levelIdx);
            int i = 0;
            while (i < records.size()) {
                // 计算当前层级的 key（包含上级层级）
                String currentKey = buildHierarchyKey(records.get(i), hierarchyLevels, levelIdx);
                int span = 1;
                int j = i + 1;
                while (j < records.size()) {
                    String jKey = buildHierarchyKey(records.get(j), hierarchyLevels, levelIdx);
                    if (jKey.equals(currentKey)) {
                        rowspans[j][levelIdx] = 0; // 被合并的行标记为0
                        span++;
                        j++;
                    } else {
                        break;
                    }
                }
                rowspans[i][levelIdx] = span;
                i = j;
            }
        }

        // 计算序号的 rowspan（跟随第一层级）
        int[] indexRowspans = new int[records.size()];
        int indexCounter = 0;
        String lastFirstLevelValue = null;
        for (int i = 0; i < records.size(); i++) {
            String firstLevelValue = getFieldValue(records.get(i), hierarchyLevels.get(0), 0);
            if (!Objects.equals(firstLevelValue, lastFirstLevelValue)) {
                indexCounter++;
                lastFirstLevelValue = firstLevelValue;
            }
            indexRowspans[i] = rowspans[i][0]; // 序号跟随第一层级
        }

        // 生成行
        int displayIndex = 0;
        String lastFirstLevel = null;
        for (int rowIdx = 0; rowIdx < records.size(); rowIdx++) {
            ExportPreviewDTO.StudentRecordDTO record = records.get(rowIdx);
            String rowBg = zebra && rowIdx % 2 == 1 ? zebraColor : "#fff";
            html.append("<tr style=\"background:").append(rowBg).append(";\">");

            // 按原始列顺序渲染
            for (ExportTemplateDTO.ColumnConfig col : columns) {
                String field = col.getField();

                if ("index".equals(field)) {
                    // 序号列
                    int rowspan = indexRowspans[rowIdx];
                    if (rowspan > 0) {
                        String firstLevelValue = getFieldValue(record, hierarchyLevels.get(0), 0);
                        if (!Objects.equals(firstLevelValue, lastFirstLevel)) {
                            displayIndex++;
                            lastFirstLevel = firstLevelValue;
                        }
                        html.append("<td rowspan=\"").append(rowspan)
                                .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                                .append(";text-align:center;vertical-align:").append(verticalAlign)
                                .append(";background:#fafafa;font-weight:500;\">")
                                .append(displayIndex).append("</td>");
                    }
                } else if (hierarchyLevels.contains(field)) {
                    // 层级列
                    int levelIdx = hierarchyLevels.indexOf(field);
                    int rowspan = rowspans[rowIdx][levelIdx];
                    if (rowspan > 0) {
                        String value = getFieldValue(record, field, rowIdx + 1);
                        html.append("<td rowspan=\"").append(rowspan)
                                .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                                .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign)
                                .append(";background:#fafafa;font-weight:500;\">")
                                .append(value.isEmpty() ? "-" : value).append("</td>");
                    }
                } else {
                    // 数据列
                    String value = getFieldValue(record, field, rowIdx + 1);
                    html.append("<td style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign).append(";\">")
                            .append(value.isEmpty() ? "-" : value).append("</td>");
                }
            }

            html.append("</tr>");
        }

        return html.toString();
    }

    /**
     * 生成数据合并模式（concat）的HTML
     */
    private String generateConcatModeHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            List<ExportTemplateDTO.ColumnConfig> columns,
            List<String> hierarchyLevels,
            String separator,
            String border, String padding, String textAlign, String verticalAlign,
            boolean zebra, String zebraColor) {

        StringBuilder html = new StringBuilder();

        // 按层级分组
        Map<String, List<ExportPreviewDTO.StudentRecordDTO>> groups = new LinkedHashMap<>();
        for (ExportPreviewDTO.StudentRecordDTO record : records) {
            String key = buildFullHierarchyKey(record, hierarchyLevels);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        // 计算每组的层级 rowspan
        List<Map<String, Object>> groupInfos = new ArrayList<>();
        for (Map.Entry<String, List<ExportPreviewDTO.StudentRecordDTO>> entry : groups.entrySet()) {
            Map<String, Object> info = new HashMap<>();
            info.put("records", entry.getValue());
            info.put("levelValues", new HashMap<String, String>());
            info.put("levelRowspans", new HashMap<String, Integer>());

            ExportPreviewDTO.StudentRecordDTO first = entry.getValue().get(0);
            Map<String, String> levelValues = (Map<String, String>) info.get("levelValues");
            Map<String, Integer> levelRowspans = (Map<String, Integer>) info.get("levelRowspans");
            for (String level : hierarchyLevels) {
                levelValues.put(level, getFieldValue(first, level, 0));
                levelRowspans.put(level, 1);
            }
            groupInfos.add(info);
        }

        // 计算层级 rowspan
        for (int levelIdx = 0; levelIdx < hierarchyLevels.size(); levelIdx++) {
            String level = hierarchyLevels.get(levelIdx);
            int i = 0;
            while (i < groupInfos.size()) {
                String currentKey = buildGroupHierarchyKey(groupInfos.get(i), hierarchyLevels, levelIdx);
                int span = 1;
                int j = i + 1;
                while (j < groupInfos.size()) {
                    String jKey = buildGroupHierarchyKey(groupInfos.get(j), hierarchyLevels, levelIdx);
                    if (jKey.equals(currentKey)) {
                        ((Map<String, Integer>) groupInfos.get(j).get("levelRowspans")).put(level, 0);
                        span++;
                        j++;
                    } else {
                        break;
                    }
                }
                ((Map<String, Integer>) groupInfos.get(i).get("levelRowspans")).put(level, span);
                i = j;
            }
        }

        // 计算序号（跟随第一层级）
        int[] indexRowspans = new int[groupInfos.size()];
        int indexCounter = 0;
        String lastFirstLevel = null;
        for (int i = 0; i < groupInfos.size(); i++) {
            Map<String, String> levelValues = (Map<String, String>) groupInfos.get(i).get("levelValues");
            String firstLevelValue = levelValues.get(hierarchyLevels.get(0));
            if (!Objects.equals(firstLevelValue, lastFirstLevel)) {
                indexCounter++;
                lastFirstLevel = firstLevelValue;
            }
            Map<String, Integer> levelRowspans = (Map<String, Integer>) groupInfos.get(i).get("levelRowspans");
            indexRowspans[i] = levelRowspans.get(hierarchyLevels.get(0));
        }

        // 生成行
        int displayIndex = 0;
        String lastFirstLevelForIndex = null;
        for (int groupIdx = 0; groupIdx < groupInfos.size(); groupIdx++) {
            Map<String, Object> groupInfo = groupInfos.get(groupIdx);
            List<ExportPreviewDTO.StudentRecordDTO> groupRecords = (List<ExportPreviewDTO.StudentRecordDTO>) groupInfo.get("records");
            Map<String, String> levelValues = (Map<String, String>) groupInfo.get("levelValues");
            Map<String, Integer> levelRowspans = (Map<String, Integer>) groupInfo.get("levelRowspans");

            String rowBg = zebra && groupIdx % 2 == 1 ? zebraColor : "#fff";
            html.append("<tr style=\"background:").append(rowBg).append(";\">");

            for (ExportTemplateDTO.ColumnConfig col : columns) {
                String field = col.getField();

                if ("index".equals(field)) {
                    int rowspan = indexRowspans[groupIdx];
                    if (rowspan > 0) {
                        String firstLevelValue = levelValues.get(hierarchyLevels.get(0));
                        if (!Objects.equals(firstLevelValue, lastFirstLevelForIndex)) {
                            displayIndex++;
                            lastFirstLevelForIndex = firstLevelValue;
                        }
                        html.append("<td rowspan=\"").append(rowspan)
                                .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                                .append(";text-align:center;vertical-align:").append(verticalAlign)
                                .append(";background:#fafafa;font-weight:500;\">")
                                .append(displayIndex).append("</td>");
                    }
                } else if (hierarchyLevels.contains(field)) {
                    int rowspan = levelRowspans.get(field);
                    if (rowspan > 0) {
                        String value = levelValues.get(field);
                        html.append("<td rowspan=\"").append(rowspan)
                                .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                                .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign)
                                .append(";background:#fafafa;font-weight:500;\">")
                                .append(value == null || value.isEmpty() ? "-" : value).append("</td>");
                    }
                } else {
                    // 数据列 - 合并显示
                    Set<String> uniqueValues = new LinkedHashSet<>();
                    for (ExportPreviewDTO.StudentRecordDTO r : groupRecords) {
                        String v = getFieldValue(r, field, 0);
                        if (v != null && !v.isEmpty()) {
                            uniqueValues.add(v);
                        }
                    }
                    String value = String.join(separator, uniqueValues);
                    html.append("<td style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign).append(";\">")
                            .append(value.isEmpty() ? "-" : value).append("</td>");
                }
            }

            html.append("</tr>");
        }

        return html.toString();
    }

    /**
     * 构建层级 key（到指定层级）
     */
    private String buildHierarchyKey(ExportPreviewDTO.StudentRecordDTO record, List<String> hierarchyLevels, int toLevel) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i <= toLevel; i++) {
            if (i > 0) key.append("|");
            key.append(getFieldValue(record, hierarchyLevels.get(i), 0));
        }
        return key.toString();
    }

    /**
     * 构建完整层级 key
     */
    private String buildFullHierarchyKey(ExportPreviewDTO.StudentRecordDTO record, List<String> hierarchyLevels) {
        return buildHierarchyKey(record, hierarchyLevels, hierarchyLevels.size() - 1);
    }

    /**
     * 构建分组的层级 key
     */
    private String buildGroupHierarchyKey(Map<String, Object> groupInfo, List<String> hierarchyLevels, int toLevel) {
        Map<String, String> levelValues = (Map<String, String>) groupInfo.get("levelValues");
        StringBuilder key = new StringBuilder();
        for (int i = 0; i <= toLevel; i++) {
            if (i > 0) key.append("|");
            key.append(levelValues.get(hierarchyLevels.get(i)));
        }
        return key.toString();
    }

    private ExportTemplateDTO.ColumnConfig createColumn(String field, String label, int width) {
        ExportTemplateDTO.ColumnConfig col = new ExportTemplateDTO.ColumnConfig();
        col.setField(field);
        col.setLabel(label);
        col.setWidth(width);
        return col;
    }

    /**
     * 获取字段值（带序号）
     */
    private String getFieldValue(ExportPreviewDTO.StudentRecordDTO record, String field, int index) {
        if (record == null || field == null) return "";
        if ("index".equals(field)) {
            return String.valueOf(index);
        }
        return getFieldValue(record, field);
    }

    /**
     * 获取字段值（不带序号）
     */
    private String getFieldValue(ExportPreviewDTO.StudentRecordDTO record, String field) {
        if (record == null || field == null) return "";
        return switch (field) {
            case "studentNo" -> record.getStudentNo() != null ? record.getStudentNo() : "";
            case "studentName" -> record.getStudentName() != null ? record.getStudentName() : "";
            case "gender" -> record.getGender() != null ? record.getGender() : "";
            case "className" -> record.getClassName() != null ? record.getClassName() : "";
            case "gradeName" -> record.getGradeName() != null ? record.getGradeName() : "";
            case "orgUnitName" -> record.getOrgUnitName() != null ? record.getOrgUnitName() : "";
            case "headTeacher" -> record.getHeadTeacher() != null ? record.getHeadTeacher() : "";
            case "buildingName" -> record.getBuildingName() != null ? record.getBuildingName() : "";
            case "roomNo" -> record.getRoomNo() != null ? record.getRoomNo() : "";
            case "categoryName" -> record.getCategoryName() != null ? record.getCategoryName() : "";
            case "relationType" -> record.getRelationType() != null ? record.getRelationType() : "";
            case "deductionItemName" -> record.getDeductionItemName() != null ? record.getDeductionItemName() : "";
            case "deductMode" -> record.getDeductMode() != null ? record.getDeductMode() : "";
            case "deductScore" -> record.getDeductScore() != null ? String.valueOf(record.getDeductScore()) : "";
            case "originalTotalScore" -> record.getOriginalTotalScore() != null ? String.valueOf(record.getOriginalTotalScore()) : "";
            case "weightedDeductScore" -> record.getWeightedDeductScore() != null ? String.valueOf(record.getWeightedDeductScore()) : "";
            case "totalWeightedDeductScore" -> record.getTotalWeightedDeductScore() != null ? String.valueOf(record.getTotalWeightedDeductScore()) : "";
            case "categoryOriginalScore" -> record.getCategoryOriginalScore() != null ? String.valueOf(record.getCategoryOriginalScore()) : "";
            case "categoryWeightedScore" -> record.getCategoryWeightedScore() != null ? String.valueOf(record.getCategoryWeightedScore()) : "";
            case "classOriginalScore" -> record.getClassOriginalScore() != null ? String.valueOf(record.getClassOriginalScore()) : "";
            case "classWeightedScore" -> record.getClassWeightedScore() != null ? String.valueOf(record.getClassWeightedScore()) : "";
            case "remark" -> record.getRemark() != null ? record.getRemark() : "";
            case "checkerName" -> record.getCheckerName() != null ? record.getCheckerName() : "";
            case "personCount" -> record.getPersonCount() != null ? String.valueOf(record.getPersonCount()) : "1";
            default -> "";
        };
    }

    private String getDefaultDocumentTemplate() {
        return """
                <div style="text-align:center;margin-bottom:20px;">
                    <h2>学生违纪通报</h2>
                    <p>{{checkDate}}</p>
                </div>
                <p style="text-indent:2em;line-height:1.8;">
                    根据{{checkDate}}检查结果，以下 <strong>{{totalCount}}</strong> 名学生存在违纪行为，现予以通报批评：
                </p>
                <div style="margin:20px 0;">
                    {{TABLE}}
                </div>
                <p style="text-indent:2em;line-height:1.8;">
                    请以上学生引以为戒，严格遵守学校规章制度。
                </p>
                <div style="text-align:right;margin-top:40px;">
                    <p>学生处</p>
                    <p>{{exportDate}}</p>
                </div>
                """;
    }

    private String wrapHtmlDocument(String content) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: SimSun, serif; font-size: 14px; line-height: 1.6; padding: 40px; }
                        h2 { font-size: 20px; margin-bottom: 10px; }
                        table { margin: 0 auto; }
                        .data-table th, .data-table td { text-align: center; }
                    </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(content);
    }

    /**
     * 生成PDF
     */
    private byte[] generatePdf(String html) {
        log.info("生成PDF文件");
        return documentGeneratorService.htmlToPdf(html);
    }

    /**
     * 生成Word
     */
    private byte[] generateWord(String html) {
        log.info("生成Word文件");
        List<ExportPreviewDTO.StudentRecordDTO> records = exportRecordsHolder.get();
        ExportTemplateDTO.TableConfig tableConfig = tableConfigHolder.get();
        return documentGeneratorService.htmlToWord(html, records, tableConfig);
    }

    /**
     * 生成Excel
     */
    private byte[] generateExcel(List<ExportPreviewDTO.StudentRecordDTO> records, ExportTemplateDTO.TableConfig tableConfig) {
        log.info("生成Excel文件");
        return documentGeneratorService.generateExcel(records, tableConfig);
    }
}
