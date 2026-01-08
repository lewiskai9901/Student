package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.export.ExportPreviewDTO;
import com.school.management.dto.export.ExportTemplateDTO;
import com.school.management.entity.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.NotificationReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通报生成服务实现
 *
 * @author system
 * @since 4.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationReportServiceImpl implements NotificationReportService {

    private final NotificationRecordMapper notificationRecordMapper;
    private final CheckExportTemplateMapper templateMapper;
    private final CheckPlanMapper checkPlanMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckDetailMapper dailyCheckDetailMapper;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final DepartmentMapper departmentMapper;
    private final GradeMapper gradeMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final DocumentGeneratorService documentGeneratorService;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationRecord generateNotification(
            Long planId,
            Long templateId,
            List<Long> dailyCheckIds,
            List<Integer> checkRounds,
            List<Long> deductionItemIds,
            String variableValues) {

        log.info("生成通报草稿: planId={}, templateId={}, dailyCheckIds={}, rounds={}, deductionItemIds={}",
                planId, templateId, dailyCheckIds, checkRounds, deductionItemIds);

        // 验证计划
        CheckPlan plan = checkPlanMapper.selectById(planId);
        if (plan == null || plan.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查计划不存在");
        }

        // 验证模板
        CheckExportTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出模板不存在");
        }

        // 验证日常检查
        if (dailyCheckIds == null || dailyCheckIds.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "请选择至少一个日常检查");
        }

        List<DailyCheck> dailyChecks = dailyCheckMapper.selectBatchIds(dailyCheckIds);
        if (dailyChecks.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日常检查不存在");
        }

        // 创建通报记录（草稿状态）
        NotificationRecord record = new NotificationRecord();
        record.setPlanId(planId);
        record.setTemplateId(templateId);
        record.setNotificationType(determineNotificationType(dailyCheckIds, checkRounds));
        record.setDailyCheckIds(toJsonArray(dailyCheckIds));
        record.setCheckRounds(toJsonArray(checkRounds));
        record.setVariableValues(variableValues);
        record.setStatus(NotificationRecord.STATUS_EDITING);
        record.setPublishStatus(NotificationRecord.PUBLISH_STATUS_DRAFT);
        record.setCreatedAt(LocalDateTime.now());

        // 保存记录
        notificationRecordMapper.insert(record);

        try {
            // 查询所有符合条件的记录（支持扣分项筛选）
            List<ExportPreviewDTO.StudentRecordDTO> allRecords = queryNotificationRecords(
                    dailyCheckIds, checkRounds, deductionItemIds, template);

            // 解析模板配置
            ExportTemplateDTO.TableConfig tableConfig = parseTableConfig(template.getTableConfig());

            // 合并同一学生的多次违纪（如果是多检查汇总）
            if (dailyCheckIds.size() > 1) {
                allRecords = mergeStudentRecords(allRecords);
            }

            // 生成标题
            String title = generateTitle(dailyChecks, checkRounds);
            record.setTitle(title);

            // 渲染文档（只生成HTML，不生成文件）
            String html = renderNotificationDocument(template, dailyChecks, allRecords, tableConfig, variableValues);
            record.setContentSnapshot(html);

            // 统计信息
            record.setTotalCount(allRecords.size());
            record.setTotalDeductionCount(allRecords.size());
            Set<Long> classIds = allRecords.stream()
                    .map(ExportPreviewDTO.StudentRecordDTO::getClassId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            record.setTotalClasses(classIds.size());

            // 草稿状态，不生成文件
            record.setStatus(NotificationRecord.STATUS_COMPLETED);

            notificationRecordMapper.updateById(record);

            log.info("通报草稿生成成功: recordId={}, title={}", record.getId(), title);
            return record;

        } catch (Exception e) {
            log.error("通报草稿生成失败", e);
            record.setStatus(NotificationRecord.STATUS_FAILED);
            record.setErrorMessage(e.getMessage());
            notificationRecordMapper.updateById(record);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "通报生成失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationRecord updateNotificationContent(Long id, String title, String contentHtml) {
        log.info("更新通报内容: id={}", id);

        NotificationRecord record = getNotificationById(id);

        // 只有草稿状态可以编辑
        if (record.getPublishStatus() != null && record.getPublishStatus() == NotificationRecord.PUBLISH_STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "已发布的通报不能编辑");
        }

        try {
            if (StringUtils.hasText(title)) {
                record.setTitle(title);
            }
            if (StringUtils.hasText(contentHtml)) {
                // 直接保存HTML内容，不做复杂转换
                // 如果内容已经包含完整的HTML结构，直接保存
                if (contentHtml.contains("<html") || contentHtml.contains("<!DOCTYPE")) {
                    record.setContentSnapshot(contentHtml);
                } else {
                    // 否则包装为完整HTML文档
                    record.setContentSnapshot(wrapHtmlDocument(contentHtml));
                }
            }
            record.setUpdatedAt(LocalDateTime.now());

            notificationRecordMapper.updateById(record);
            log.info("通报内容更新成功: id={}", id);
            return record;
        } catch (Exception e) {
            log.error("更新通报内容失败: id={}, error={}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "保存失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationRecord publishNotification(Long id) {
        log.info("发布通报: id={}", id);

        NotificationRecord record = getNotificationById(id);

        if (record.getPublishStatus() != null && record.getPublishStatus() == NotificationRecord.PUBLISH_STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "通报已发布");
        }

        record.setPublishStatus(NotificationRecord.PUBLISH_STATUS_PUBLISHED);
        record.setGeneratedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        notificationRecordMapper.updateById(record);
        log.info("通报发布成功: id={}", id);
        return record;
    }

    @Override
    public byte[] downloadNotification(Long id, String format) {
        log.info("下载通报: id={}, format={}", id, format);

        NotificationRecord record = getNotificationById(id);

        if (!StringUtils.hasText(record.getContentSnapshot())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "通报内容为空");
        }

        String fmt = format != null ? format.toUpperCase() : "PDF";
        String html = record.getContentSnapshot();

        // 按需生成文件
        return switch (fmt) {
            case "PDF" -> documentGeneratorService.htmlToPdf(html);
            case "WORD" -> {
                // 对于WORD格式，需要重新查询数据生成
                ExportTemplateDTO.TableConfig tableConfig = new ExportTemplateDTO.TableConfig();
                if (record.getTemplateId() != null) {
                    CheckExportTemplate template = templateMapper.selectById(record.getTemplateId());
                    if (template != null) {
                        tableConfig = parseTableConfig(template.getTableConfig());
                    }
                }
                yield documentGeneratorService.htmlToWord(html, Collections.emptyList(), tableConfig);
            }
            default -> documentGeneratorService.htmlToPdf(html);
        };
    }

    @Override
    public List<NotificationRecord> getNotificationHistory(Long planId) {
        LambdaQueryWrapper<NotificationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationRecord::getPlanId, planId)
                .orderByDesc(NotificationRecord::getCreatedAt);
        return notificationRecordMapper.selectList(wrapper);
    }

    @Override
    public NotificationRecord getNotificationById(Long id) {
        NotificationRecord record = notificationRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通报记录不存在");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationRecord regenerateNotification(Long recordId) {
        NotificationRecord oldRecord = getNotificationById(recordId);

        // 解析原记录的参数
        List<Long> dailyCheckIds = parseJsonArray(oldRecord.getDailyCheckIds(), Long.class);
        List<Integer> checkRounds = parseJsonArray(oldRecord.getCheckRounds(), Integer.class);
        List<Long> deductionItemIds = parseJsonArray(oldRecord.getDeductionItemIds(), Long.class);

        // 重新生成
        return generateNotification(
                oldRecord.getPlanId(),
                oldRecord.getTemplateId(),
                dailyCheckIds,
                checkRounds,
                deductionItemIds,
                oldRecord.getVariableValues()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long id) {
        NotificationRecord record = getNotificationById(id);

        // 删除文件
        if (StringUtils.hasText(record.getFilePath())) {
            try {
                Path filePath = Paths.get(uploadPath, record.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("删除通报文件失败: {}", e.getMessage());
            }
        }

        notificationRecordMapper.deleteById(id);
    }

    @Override
    public byte[] getNotificationFile(Long id) {
        NotificationRecord record = getNotificationById(id);

        if (record.getStatus() != NotificationRecord.STATUS_COMPLETED) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "通报尚未生成完成");
        }

        if (!StringUtils.hasText(record.getFilePath())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通报文件不存在");
        }

        try {
            Path filePath = Paths.get(uploadPath, record.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取通报文件失败", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "读取通报文件失败");
        }
    }

    // ========== 私有方法 ==========

    private String determineNotificationType(List<Long> dailyCheckIds, List<Integer> checkRounds) {
        if (dailyCheckIds.size() > 1) {
            return NotificationRecord.TYPE_MULTI_CHECK;
        } else if (checkRounds != null && checkRounds.size() > 1) {
            return NotificationRecord.TYPE_MULTI_ROUND;
        } else {
            return NotificationRecord.TYPE_SINGLE_CHECK;
        }
    }

    private List<ExportPreviewDTO.StudentRecordDTO> queryNotificationRecords(
            List<Long> dailyCheckIds,
            List<Integer> checkRounds,
            List<Long> deductionItemIds,
            CheckExportTemplate template) {

        ExportTemplateDTO.FilterConfig filterConfig = parseFilterConfig(template.getFilterConfig());

        // 使用请求的轮次覆盖模板配置
        if (checkRounds != null && !checkRounds.isEmpty()) {
            filterConfig.setCheckRounds(checkRounds);
        }

        List<ExportPreviewDTO.StudentRecordDTO> allRecords = new ArrayList<>();

        for (Long checkId : dailyCheckIds) {
            List<ExportPreviewDTO.StudentRecordDTO> records = queryExportRecords(checkId, filterConfig);
            allRecords.addAll(records);
        }

        // 按扣分项筛选
        if (deductionItemIds != null && !deductionItemIds.isEmpty()) {
            log.info("按扣分项筛选: deductionItemIds={}", deductionItemIds);
            allRecords = allRecords.stream()
                    .filter(r -> r.getDeductionItemId() != null && deductionItemIds.contains(r.getDeductionItemId()))
                    .collect(Collectors.toList());
            log.info("筛选后记录数: {}", allRecords.size());
        }

        return allRecords;
    }

    private List<ExportPreviewDTO.StudentRecordDTO> queryExportRecords(
            Long checkId,
            ExportTemplateDTO.FilterConfig filterConfig) {

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

        // 收集部门、年级和班主任ID
        Set<Long> departmentIds = new HashSet<>();
        Set<Long> gradeIds = new HashSet<>();
        Set<Long> teacherIds = new HashSet<>();
        classMap.values().forEach(c -> {
            if (c.getDepartmentId() != null) departmentIds.add(c.getDepartmentId());
            if (c.getGradeId() != null) gradeIds.add(c.getGradeId());
            if (c.getTeacherId() != null) teacherIds.add(c.getTeacherId());
        });

        // 查询部门信息
        Map<Long, Department> departmentMap = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            departmentMapper.selectBatchIds(departmentIds).forEach(d -> departmentMap.put(d.getId(), d));
        }

        // 查询年级信息
        Map<Long, Grade> gradeMap = new HashMap<>();
        if (!gradeIds.isEmpty()) {
            gradeMapper.selectBatchIds(gradeIds).forEach(g -> gradeMap.put(g.getId(), g));
        }

        // 查询班主任信息
        Map<Long, User> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            userMapper.selectBatchIds(teacherIds).forEach(u -> teacherMap.put(u.getId(), u));
        }

        // 构建记录列表
        List<ExportPreviewDTO.StudentRecordDTO> records = new ArrayList<>();

        for (DailyCheckDetail detail : details) {
            if (!StringUtils.hasText(detail.getStudentIds())) {
                continue;
            }

            String[] studentIdArray = detail.getStudentIds().split(",");
            String[] studentNameArray = StringUtils.hasText(detail.getStudentNames())
                    ? detail.getStudentNames().split(",")
                    : new String[0];

            com.school.management.entity.Class classInfo = classMap.get(detail.getClassId());
            Department dept = classInfo != null ? departmentMap.get(classInfo.getDepartmentId()) : null;
            Grade grade = classInfo != null ? gradeMap.get(classInfo.getGradeId()) : null;
            User teacher = classInfo != null && classInfo.getTeacherId() != null ? teacherMap.get(classInfo.getTeacherId()) : null;

            for (int i = 0; i < studentIdArray.length; i++) {
                String studentIdStr = studentIdArray[i].trim();
                if (studentIdStr.isEmpty()) continue;

                ExportPreviewDTO.StudentRecordDTO record = new ExportPreviewDTO.StudentRecordDTO();
                record.setStudentId(Long.parseLong(studentIdStr));
                record.setStudentName(i < studentNameArray.length ? studentNameArray[i].trim() : "");
                record.setClassId(detail.getClassId());
                record.setClassName(classInfo != null ? classInfo.getClassName() : "");
                record.setDepartmentId(classInfo != null ? classInfo.getDepartmentId() : null);
                record.setDepartmentName(dept != null ? dept.getDeptName() : "");
                record.setGradeId(classInfo != null ? classInfo.getGradeId() : null);
                record.setGradeName(grade != null ? grade.getGradeName() : "");
                record.setHeadTeacher(teacher != null ? teacher.getRealName() : "");
                record.setDeductionItemId(detail.getDeductionItemId());
                record.setDeductionItemName(detail.getDeductionItemName());
                record.setDeductScore(detail.getDeductScore() != null ? detail.getDeductScore().doubleValue() : 0);
                record.setRemark(detail.getRemark());
                record.setCheckRound(detail.getCheckRound());

                // 查询学生详细信息
                try {
                    Student student = studentMapper.selectById(record.getStudentId());
                    if (student != null) {
                        record.setStudentNo(student.getStudentNo());
                    }
                } catch (Exception e) {
                    log.warn("查询学生信息失败: {}", e.getMessage());
                }

                records.add(record);
            }
        }

        // 排序
        records.sort(Comparator
                .comparing(ExportPreviewDTO.StudentRecordDTO::getDepartmentName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getGradeName, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getClassName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getStudentNo, Comparator.nullsLast(String::compareTo)));

        return records;
    }

    /**
     * 合并同一学生的多次违纪记录
     * 用于多检查汇总时，显示违纪次数和累计扣分
     */
    private List<ExportPreviewDTO.StudentRecordDTO> mergeStudentRecords(
            List<ExportPreviewDTO.StudentRecordDTO> records) {

        // 按学生ID分组
        Map<Long, List<ExportPreviewDTO.StudentRecordDTO>> byStudent = records.stream()
                .collect(Collectors.groupingBy(ExportPreviewDTO.StudentRecordDTO::getStudentId));

        List<ExportPreviewDTO.StudentRecordDTO> mergedRecords = new ArrayList<>();

        for (Map.Entry<Long, List<ExportPreviewDTO.StudentRecordDTO>> entry : byStudent.entrySet()) {
            List<ExportPreviewDTO.StudentRecordDTO> studentRecords = entry.getValue();
            ExportPreviewDTO.StudentRecordDTO first = studentRecords.get(0);

            if (studentRecords.size() == 1) {
                mergedRecords.add(first);
            } else {
                // 合并记录
                ExportPreviewDTO.StudentRecordDTO merged = new ExportPreviewDTO.StudentRecordDTO();
                merged.setStudentId(first.getStudentId());
                merged.setStudentNo(first.getStudentNo());
                merged.setStudentName(first.getStudentName());
                merged.setClassId(first.getClassId());
                merged.setClassName(first.getClassName());
                merged.setDepartmentId(first.getDepartmentId());
                merged.setDepartmentName(first.getDepartmentName());
                merged.setGradeId(first.getGradeId());
                merged.setGradeName(first.getGradeName());

                // 统计违纪次数和累计扣分
                int violationCount = studentRecords.size();
                double totalDeduct = studentRecords.stream()
                        .mapToDouble(r -> r.getDeductScore() != null ? r.getDeductScore() : 0)
                        .sum();

                // 合并扣分项名称
                String items = studentRecords.stream()
                        .map(ExportPreviewDTO.StudentRecordDTO::getDeductionItemName)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .collect(Collectors.joining("、"));

                merged.setDeductionItemName(items + " (共" + violationCount + "次)");
                merged.setDeductScore(totalDeduct);
                merged.setRemark("违纪" + violationCount + "次，累计扣分" + totalDeduct);

                mergedRecords.add(merged);
            }
        }

        // 重新排序
        mergedRecords.sort(Comparator
                .comparing(ExportPreviewDTO.StudentRecordDTO::getDepartmentName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getGradeName, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getClassName, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExportPreviewDTO.StudentRecordDTO::getStudentNo, Comparator.nullsLast(String::compareTo)));

        return mergedRecords;
    }

    private String renderNotificationDocument(
            CheckExportTemplate template,
            List<DailyCheck> dailyChecks,
            List<ExportPreviewDTO.StudentRecordDTO> records,
            ExportTemplateDTO.TableConfig tableConfig,
            String variableValuesJson) {

        String documentHtml = template.getDocumentTemplate();
        if (!StringUtils.hasText(documentHtml)) {
            documentHtml = getDefaultDocumentTemplate();
        }

        // 生成表格HTML
        String tableHtml = generateTableHtml(records, tableConfig);

        // 计算日期范围
        String dateRange = dailyChecks.stream()
                .map(DailyCheck::getCheckDate)
                .sorted()
                .map(d -> d.format(DateTimeFormatter.ofPattern("MM月dd日")))
                .distinct()
                .collect(Collectors.joining("、"));

        String checkDate = dailyChecks.size() == 1
                ? dailyChecks.get(0).getCheckDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                : dateRange;

        String exportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        // 替换系统变量
        documentHtml = documentHtml
                .replace("{{checkDate}}", checkDate)
                .replace("{{checkName}}", dailyChecks.get(0).getCheckName() != null ? dailyChecks.get(0).getCheckName() : "")
                .replace("{{totalCount}}", String.valueOf(records.size()))
                .replace("{{exportDate}}", exportDate)
                .replace("{{TABLE}}", tableHtml);

        // 替换自定义变量
        if (StringUtils.hasText(variableValuesJson)) {
            try {
                Map<String, String> variables = objectMapper.readValue(variableValuesJson,
                        new TypeReference<Map<String, String>>() {});
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    documentHtml = documentHtml.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
            } catch (JsonProcessingException e) {
                log.warn("解析自定义变量失败: {}", e.getMessage());
            }
        }

        // 转换为XHTML兼容格式
        documentHtml = convertToXhtml(documentHtml);

        return wrapHtmlDocument(documentHtml);
    }

    /**
     * 将HTML5内容转换为XHTML兼容格式
     */
    private String convertToXhtml(String html) {
        if (html == null) return "";

        // 自闭合标签转换
        html = html.replaceAll("<br\\s*/?>", "<br />");
        html = html.replaceAll("<hr\\s*/?>", "<hr />");
        html = html.replaceAll("<img([^>]*[^/])>", "<img$1 />");
        html = html.replaceAll("<input([^>]*[^/])>", "<input$1 />");
        html = html.replaceAll("<meta([^>]*[^/])>", "<meta$1 />");
        html = html.replaceAll("<link([^>]*[^/])>", "<link$1 />");

        // 移除可能的HTML5 doctype和xml声明（因为会在wrapper中添加）
        html = html.replaceAll("<!DOCTYPE[^>]*>", "");
        html = html.replaceAll("<\\?xml[^>]*\\?>", "");

        // 移除html/head/body标签（只保留内容）
        html = html.replaceAll("<html[^>]*>", "");
        html = html.replaceAll("</html>", "");
        html = html.replaceAll("<head[^>]*>.*?</head>", "");
        html = html.replaceAll("<body[^>]*>", "");
        html = html.replaceAll("</body>", "");

        return html.trim();
    }

    private String generateTableHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            ExportTemplateDTO.TableConfig tableConfig) {

        if (records.isEmpty()) {
            return "<p>暂无数据</p>";
        }

        List<ExportTemplateDTO.ColumnConfig> columns = tableConfig.getColumns();
        if (columns == null || columns.isEmpty()) {
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

        // 获取分组列（mergeType = "group"），按 groupOrder 排序
        List<ExportTemplateDTO.ColumnConfig> groupColumns = columns.stream()
                .filter(col -> "group".equals(col.getMergeType()))
                .sorted((a, b) -> {
                    int orderA = a.getGroupOrder() != null ? a.getGroupOrder() : 999;
                    int orderB = b.getGroupOrder() != null ? b.getGroupOrder() : 999;
                    return orderA - orderB;
                })
                .collect(Collectors.toList());
        List<String> groupFields = groupColumns.stream()
                .map(ExportTemplateDTO.ColumnConfig::getField)
                .collect(Collectors.toList());

        // 检查是否有任何合并配置
        boolean hasMerge = columns.stream()
                .anyMatch(col -> "group".equals(col.getMergeType()) || "concat".equals(col.getMergeType()));

        StringBuilder html = new StringBuilder();
        html.append("<table class=\"data-table\" style=\"width:100%;border-collapse:collapse;font-size:")
                .append(fontSize).append(";line-height:").append(lineHeight).append(";\">");

        // 表头
        html.append("<thead><tr>");
        for (ExportTemplateDTO.ColumnConfig col : columns) {
            html.append("<th style=\"border:").append(border).append(";padding:").append(padding)
                    .append(";background:").append(headerBg).append(";color:").append(headerText)
                    .append(";font-weight:").append(headerFontWeight).append(";font-size:").append(headerFontSize)
                    .append(";text-align:center;vertical-align:").append(verticalAlign).append(";\">")
                    .append(col.getLabel())
                    .append("</th>");
        }
        html.append("</tr></thead>");

        html.append("<tbody>");

        if (hasMerge && !groupFields.isEmpty()) {
            // 有分组列时的合并模式
            html.append(generateMergedTableHtml(records, columns, groupFields,
                    border, padding, textAlign, verticalAlign, zebra, zebraColor));
        } else {
            // 简单模式
            Boolean showDept = tableConfig.getShowDepartmentHeader();
            Boolean showGrade = tableConfig.getShowGradeHeader();
            Boolean showClass = tableConfig.getShowClassHeader();

            String currentDept = null;
            String currentGrade = null;
            String currentClass = null;
            int index = 0;

            for (ExportPreviewDTO.StudentRecordDTO record : records) {
                if (Boolean.TRUE.equals(showDept) && !Objects.equals(currentDept, record.getDepartmentName())) {
                    currentDept = record.getDepartmentName();
                    currentGrade = null;
                    currentClass = null;
                    html.append("<tr><td colspan=\"").append(columns.size())
                            .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";background:#e0e0e0;font-weight:bold;\">")
                            .append(currentDept != null ? currentDept : "未分配部门")
                            .append("</td></tr>");
                }

                if (Boolean.TRUE.equals(showGrade) && !Objects.equals(currentGrade, record.getGradeName())) {
                    currentGrade = record.getGradeName();
                    currentClass = null;
                    html.append("<tr><td colspan=\"").append(columns.size())
                            .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";background:#eee;padding-left:20px;\">")
                            .append(currentGrade != null ? currentGrade : "未分配年级")
                            .append("</td></tr>");
                }

                if (Boolean.TRUE.equals(showClass) && !Objects.equals(currentClass, record.getClassName())) {
                    currentClass = record.getClassName();
                    html.append("<tr><td colspan=\"").append(columns.size())
                            .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";background:#f5f5f5;padding-left:40px;\">")
                            .append(currentClass != null ? currentClass : "未分配班级")
                            .append("</td></tr>");
                }

                index++;
                String rowBg = zebra && index % 2 == 0 ? zebraColor : "#fff";
                html.append("<tr style=\"background:").append(rowBg).append(";\">");
                for (ExportTemplateDTO.ColumnConfig col : columns) {
                    html.append("<td style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign).append(";\">");
                    // 支持组合字段和序号字段
                    if ("index".equals(col.getField())) {
                        html.append(index);
                    } else {
                        String value = getCompositeFieldValue(record, col);
                        html.append(value != null && !value.isEmpty() ? value : "-");
                    }
                    html.append("</td>");
                }
                html.append("</tr>");
            }
        }

        html.append("</tbody></table>");
        return html.toString();
    }

    /**
     * 生成合并表格HTML - 每列独立配置合并模式
     * 分组列：相同内容的单元格用rowspan合并
     * 数据合并列：按 groupByField 确定范围，拼接后用 rowspan
     * 普通列：每行独立显示
     * 支持组合字段
     */
    private String generateMergedTableHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            List<ExportTemplateDTO.ColumnConfig> columns,
            List<String> groupFields,
            String border, String padding, String textAlign, String verticalAlign,
            boolean zebra, String zebraColor) {

        StringBuilder html = new StringBuilder();
        int rowCount = records.size();

        // 创建字段到列配置的映射，用于获取组合字段配置
        Map<String, ExportTemplateDTO.ColumnConfig> columnMap = columns.stream()
                .collect(Collectors.toMap(ExportTemplateDTO.ColumnConfig::getField, c -> c, (a, b) -> a));

        // cellInfo[rowIdx][field] = CellInfo(rowspan, concatValue)
        // rowspan > 0 表示输出，rowspan = 0 表示被合并（跳过）
        Map<Integer, Map<String, CellInfo>> cellInfo = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            cellInfo.put(i, new HashMap<>());
        }

        // 对每个分组列计算rowspan（支持组合字段）
        for (String field : groupFields) {
            ExportTemplateDTO.ColumnConfig col = columnMap.get(field);
            int i = 0;
            while (i < rowCount) {
                String currentValue = col != null ? getCompositeFieldValue(records.get(i), col)
                        : getFieldValue(records.get(i), field);
                int span = 1;
                int j = i + 1;
                while (j < rowCount) {
                    String jValue = col != null ? getCompositeFieldValue(records.get(j), col)
                            : getFieldValue(records.get(j), field);
                    if (currentValue != null && currentValue.equals(jValue)) {
                        cellInfo.get(j).put(field, new CellInfo(0, null));
                        span++;
                        j++;
                    } else {
                        break;
                    }
                }
                cellInfo.get(i).put(field, new CellInfo(span, null));
                i = j;
            }
        }

        // 对每个数据合并列计算rowspan和拼接值（支持组合字段）
        for (ExportTemplateDTO.ColumnConfig col : columns) {
            if (!"concat".equals(col.getMergeType())) continue;

            String groupByField = col.getGroupByField();
            if (groupByField == null || groupByField.isEmpty()) {
                // 默认使用最后一个分组列
                groupByField = groupFields.isEmpty() ? null : groupFields.get(groupFields.size() - 1);
            }
            if (groupByField == null) continue;

            String separator = col.getSeparator() != null ? col.getSeparator() : "、";
            if ("\\n".equals(separator)) {
                separator = "<br/>";
            }

            final String finalGroupByField = groupByField;
            int i = 0;
            while (i < rowCount) {
                String groupValue = getFieldValue(records.get(i), finalGroupByField);
                Set<String> values = new LinkedHashSet<>();
                // 使用组合字段值
                String v = getCompositeFieldValue(records.get(i), col);
                if (v != null && !v.isEmpty()) values.add(v);

                int span = 1;
                int j = i + 1;
                while (j < rowCount) {
                    String jGroupValue = getFieldValue(records.get(j), finalGroupByField);
                    if (groupValue != null && groupValue.equals(jGroupValue)) {
                        // 使用组合字段值
                        String jv = getCompositeFieldValue(records.get(j), col);
                        if (jv != null && !jv.isEmpty()) values.add(jv);
                        cellInfo.get(j).put(col.getField(), new CellInfo(0, null));
                        span++;
                        j++;
                    } else {
                        break;
                    }
                }
                String concatValue = values.isEmpty() ? "-" : String.join(separator, values);
                cellInfo.get(i).put(col.getField(), new CellInfo(span, concatValue));
                i = j;
            }
        }

        // 渲染每行数据
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            ExportPreviewDTO.StudentRecordDTO row = records.get(rowIdx);
            String rowBg = zebra && rowIdx % 2 == 1 ? zebraColor : "#fff";
            html.append("<tr style=\"background:").append(rowBg).append(";\">");

            for (ExportTemplateDTO.ColumnConfig col : columns) {
                CellInfo info = cellInfo.get(rowIdx).get(col.getField());
                String mergeType = col.getMergeType();

                if ("group".equals(mergeType)) {
                    // 分组合并列（支持组合字段）
                    if (info != null && info.rowspan > 0) {
                        String value = getCompositeFieldValue(row, col);
                        html.append("<td rowspan=\"").append(info.rowspan)
                                .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                                .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign)
                                .append(";background:#fafafa;font-weight:500;\">")
                                .append(value != null && !value.isEmpty() ? value : "-")
                                .append("</td>");
                    }
                    // rowspan == 0 时不输出（被合并了）
                } else if ("concat".equals(mergeType)) {
                    // 数据合并列
                    if (info != null && info.rowspan > 0) {
                        html.append("<td rowspan=\"").append(info.rowspan)
                                .append("\" style=\"border:").append(border).append(";padding:").append(padding)
                                .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign)
                                .append(";\">")
                                .append(info.concatValue)
                                .append("</td>");
                    }
                    // rowspan == 0 时不输出（被合并了）
                } else {
                    // 普通列：正常显示每行数据（支持组合字段）
                    String value = getCompositeFieldValue(row, col);
                    html.append("<td style=\"border:").append(border).append(";padding:").append(padding)
                            .append(";text-align:").append(textAlign).append(";vertical-align:").append(verticalAlign)
                            .append(";\">")
                            .append(value != null && !value.isEmpty() ? value : "-")
                            .append("</td>");
                }
            }

            html.append("</tr>");
        }

        return html.toString();
    }

    // 辅助类：单元格信息
    private static class CellInfo {
        int rowspan;
        String concatValue;

        CellInfo(int rowspan, String concatValue) {
            this.rowspan = rowspan;
            this.concatValue = concatValue;
        }
    }

    // 辅助类：分组信息
    private static class GroupInfo {
        List<ExportPreviewDTO.StudentRecordDTO> rows;
        Map<String, Integer> fieldRowspans;

        GroupInfo(List<ExportPreviewDTO.StudentRecordDTO> rows, Map<String, Integer> fieldRowspans) {
            this.rows = rows;
            this.fieldRowspans = fieldRowspans;
        }
    }

    // 构建分组键
    private String buildGroupKey(ExportPreviewDTO.StudentRecordDTO record, List<String> groupFields) {
        return groupFields.stream()
                .map(f -> getFieldValue(record, f))
                .map(v -> v != null ? v : "")
                .collect(Collectors.joining("|"));
    }

    // 构建部分分组键（用于层级计算）
    private String buildPartialGroupKey(ExportPreviewDTO.StudentRecordDTO record, List<String> groupFields, int toIndex) {
        return groupFields.subList(0, toIndex).stream()
                .map(f -> getFieldValue(record, f))
                .map(v -> v != null ? v : "")
                .collect(Collectors.joining("|"));
    }

    private String generateRowspanModeHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            List<ExportTemplateDTO.ColumnConfig> columns,
            List<String> hierarchyLevels,
            String border, String padding, String textAlign, String verticalAlign,
            boolean zebra, String zebraColor) {

        StringBuilder html = new StringBuilder();
        int[][] rowspans = new int[records.size()][hierarchyLevels.size()];

        for (int levelIdx = 0; levelIdx < hierarchyLevels.size(); levelIdx++) {
            int i = 0;
            while (i < records.size()) {
                String currentKey = buildHierarchyKey(records.get(i), hierarchyLevels, levelIdx);
                int span = 1;
                int j = i + 1;
                while (j < records.size()) {
                    String jKey = buildHierarchyKey(records.get(j), hierarchyLevels, levelIdx);
                    if (jKey.equals(currentKey)) {
                        rowspans[j][levelIdx] = 0;
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

        int[] indexRowspans = new int[records.size()];
        String lastFirstLevelValue = null;
        for (int i = 0; i < records.size(); i++) {
            String firstLevelValue = getFieldValue(records.get(i), hierarchyLevels.get(0), 0);
            if (!Objects.equals(firstLevelValue, lastFirstLevelValue)) {
                lastFirstLevelValue = firstLevelValue;
            }
            indexRowspans[i] = rowspans[i][0];
        }

        int displayIndex = 0;
        String lastFirstLevel = null;
        for (int rowIdx = 0; rowIdx < records.size(); rowIdx++) {
            ExportPreviewDTO.StudentRecordDTO record = records.get(rowIdx);
            String rowBg = zebra && rowIdx % 2 == 1 ? zebraColor : "#fff";
            html.append("<tr style=\"background:").append(rowBg).append(";\">");

            for (ExportTemplateDTO.ColumnConfig col : columns) {
                String field = col.getField();

                if ("index".equals(field)) {
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

    private String generateConcatModeHtml(
            List<ExportPreviewDTO.StudentRecordDTO> records,
            List<ExportTemplateDTO.ColumnConfig> columns,
            List<String> hierarchyLevels,
            String separator,
            String border, String padding, String textAlign, String verticalAlign,
            boolean zebra, String zebraColor) {

        StringBuilder html = new StringBuilder();
        Map<String, List<ExportPreviewDTO.StudentRecordDTO>> groups = new LinkedHashMap<>();
        for (ExportPreviewDTO.StudentRecordDTO record : records) {
            String key = buildFullHierarchyKey(record, hierarchyLevels);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        List<Map<String, Object>> groupInfos = new ArrayList<>();
        for (Map.Entry<String, List<ExportPreviewDTO.StudentRecordDTO>> entry : groups.entrySet()) {
            Map<String, Object> info = new HashMap<>();
            info.put("records", entry.getValue());
            Map<String, String> levelValues = new HashMap<>();
            Map<String, Integer> levelRowspans = new HashMap<>();
            ExportPreviewDTO.StudentRecordDTO first = entry.getValue().get(0);
            for (String level : hierarchyLevels) {
                levelValues.put(level, getFieldValue(first, level, 0));
                levelRowspans.put(level, 1);
            }
            info.put("levelValues", levelValues);
            info.put("levelRowspans", levelRowspans);
            groupInfos.add(info);
        }

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

        int[] indexRowspans = new int[groupInfos.size()];
        String lastFirstLevel = null;
        for (int i = 0; i < groupInfos.size(); i++) {
            Map<String, String> levelValues = (Map<String, String>) groupInfos.get(i).get("levelValues");
            String firstLevelValue = levelValues.get(hierarchyLevels.get(0));
            if (!Objects.equals(firstLevelValue, lastFirstLevel)) {
                lastFirstLevel = firstLevelValue;
            }
            Map<String, Integer> levelRowspans = (Map<String, Integer>) groupInfos.get(i).get("levelRowspans");
            indexRowspans[i] = levelRowspans.get(hierarchyLevels.get(0));
        }

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

    private String buildHierarchyKey(ExportPreviewDTO.StudentRecordDTO record, List<String> hierarchyLevels, int toLevel) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i <= toLevel; i++) {
            if (i > 0) key.append("|");
            key.append(getFieldValue(record, hierarchyLevels.get(i), 0));
        }
        return key.toString();
    }

    private String buildFullHierarchyKey(ExportPreviewDTO.StudentRecordDTO record, List<String> hierarchyLevels) {
        return buildHierarchyKey(record, hierarchyLevels, hierarchyLevels.size() - 1);
    }

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
            case "departmentName" -> record.getDepartmentName() != null ? record.getDepartmentName() : "";
            case "headTeacher" -> record.getHeadTeacher() != null ? record.getHeadTeacher() : "";
            case "buildingName" -> record.getBuildingName() != null ? record.getBuildingName() : "";
            case "roomNo", "dormitoryNo" -> record.getRoomNo() != null ? record.getRoomNo() : "";
            case "categoryName" -> record.getCategoryName() != null ? record.getCategoryName() : "";
            case "deductionItemName" -> record.getDeductionItemName() != null ? record.getDeductionItemName() : "";
            case "deductScore" -> record.getDeductScore() != null ? String.valueOf(record.getDeductScore()) : "";
            case "remark" -> record.getRemark() != null ? record.getRemark() : "";
            case "checkerName" -> record.getCheckerName() != null ? record.getCheckerName() : "";
            case "personCount" -> record.getPersonCount() != null ? String.valueOf(record.getPersonCount()) : "";
            case "weightedDeductScore" -> record.getWeightedDeductScore() != null ? String.valueOf(record.getWeightedDeductScore()) : "";
            case "totalWeightedDeductScore" -> record.getTotalWeightedDeductScore() != null ? String.valueOf(record.getTotalWeightedDeductScore()) : "";
            default -> "";
        };
    }

    /**
     * 获取组合字段值
     */
    private String getCompositeFieldValue(ExportPreviewDTO.StudentRecordDTO record,
                                          ExportTemplateDTO.ColumnConfig col) {
        if (record == null || col == null) return "";
        if (!Boolean.TRUE.equals(col.getIsComposite()) || col.getCompositeFields() == null) {
            return getFieldValue(record, col.getField());
        }

        String separator = col.getCompositeSeparator() != null ? col.getCompositeSeparator() : " ";
        return col.getCompositeFields().stream()
                .map(f -> getFieldValue(record, f))
                .filter(v -> v != null && !v.isEmpty())
                .collect(Collectors.joining(separator));
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
        // 使用XHTML格式以便PDF生成器正确解析
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                    <style type="text/css">
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

    private byte[] generateFile(String html, String format,
                                 List<ExportPreviewDTO.StudentRecordDTO> records,
                                 ExportTemplateDTO.TableConfig tableConfig) {
        String fmt = format != null ? format.toUpperCase() : "PDF";
        return switch (fmt) {
            case "PDF" -> documentGeneratorService.htmlToPdf(html);
            case "WORD" -> documentGeneratorService.htmlToWord(html, records, tableConfig);
            case "EXCEL" -> documentGeneratorService.generateExcel(records, tableConfig);
            default -> documentGeneratorService.htmlToPdf(html);
        };
    }

    private String generateTitle(List<DailyCheck> dailyChecks, List<Integer> checkRounds) {
        if (dailyChecks.size() == 1) {
            DailyCheck check = dailyChecks.get(0);
            String title = check.getCheckName() + "通报";
            if (checkRounds != null && !checkRounds.isEmpty()) {
                title += "_轮次" + checkRounds.stream().map(String::valueOf).collect(Collectors.joining(","));
            }
            return title;
        } else {
            return "多检查汇总通报_" + dailyChecks.size() + "次检查";
        }
    }

    private String generateFileName(String title, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = switch (format != null ? format.toUpperCase() : "PDF") {
            case "WORD" -> ".docx";
            case "EXCEL" -> ".xlsx";
            default -> ".pdf";
        };
        return title + "_" + timestamp + extension;
    }

    private String saveFile(byte[] fileBytes, String fileName) throws IOException {
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = "notifications/" + dateDir + "/" + fileName;
        Path fullPath = Paths.get(uploadPath, relativePath);

        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, fileBytes);

        return relativePath;
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
            return objectMapper.readValue(json, ExportTemplateDTO.TableConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("解析表格配置失败: {}", e.getMessage());
            return new ExportTemplateDTO.TableConfig();
        }
    }

    private String toJsonArray(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parseJsonArray(String json, java.lang.Class<T> elementType) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            if (elementType == Long.class) {
                return (List<T>) objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            } else if (elementType == Integer.class) {
                return (List<T>) objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
            }
            return new ArrayList<>();
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
