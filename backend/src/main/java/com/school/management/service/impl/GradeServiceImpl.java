package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.GradeDTO;
import com.school.management.dto.request.GradeCreateRequest;
import com.school.management.dto.request.GradeUpdateRequest;
import com.school.management.dto.vo.GradeStatisticsVO;
import com.school.management.entity.Grade;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckItemAppealMapper;
import com.school.management.mapper.record.CheckRecordClassStatsMapper;
import com.school.management.mapper.DailyCheckMapper;
import com.school.management.mapper.GradeMapper;
import com.school.management.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 年级服务实现类
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl extends ServiceImpl<GradeMapper, Grade> implements GradeService {

    private final GradeMapper gradeMapper;
    private final CheckItemAppealMapper appealMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final CheckRecordClassStatsMapper classStatsMapper;

    @Override
    @CacheEvict(value = {"grades", "grade"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public GradeDTO createGrade(GradeCreateRequest request) {
        log.info("创建年级: gradeName={}, gradeCode={}", request.getGradeName(), request.getGradeCode());

        // 1. 检查年级编码是否已存在
        int exists = gradeMapper.checkGradeCodeExists(request.getGradeCode(), null);
        if (exists > 0) {
            throw new BusinessException("年级编码已存在");
        }

        // 2. 创建年级实体
        Grade grade = new Grade();
        BeanUtils.copyProperties(request, grade);
        grade.setClassCount(0);
        grade.setStudentCount(0);
        grade.setAverageClassSize(0.0);
        grade.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        grade.setDeleted(0);
        grade.setCreatedAt(LocalDateTime.now());
        grade.setUpdatedAt(LocalDateTime.now());

        // 3. 保存年级
        if (!save(grade)) {
            throw new BusinessException("创建年级失败");
        }

        log.info("年级创建成功: gradeId={}, gradeCode={}", grade.getId(), grade.getGradeCode());
        return convertToDTO(grade);
    }

    @Override
    @CacheEvict(value = {"grades", "grade"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public GradeDTO updateGrade(GradeUpdateRequest request) {
        log.info("更新年级: gradeId={}", request.getId());

        // 1. 检查年级是否存在
        Grade grade = getById(request.getId());
        if (grade == null) {
            throw new BusinessException("年级不存在");
        }

        // 2. 更新字段
        if (request.getGradeName() != null) {
            grade.setGradeName(request.getGradeName());
        }
        if (request.getGradeDirectorId() != null) {
            grade.setGradeDirectorId(request.getGradeDirectorId());
        }
        if (request.getStandardClassSize() != null) {
            grade.setStandardClassSize(request.getStandardClassSize());
        }
        if (request.getStatus() != null) {
            grade.setStatus(request.getStatus());
        }
        grade.setUpdatedAt(LocalDateTime.now());

        // 3. 保存更新
        if (!updateById(grade)) {
            throw new BusinessException("更新年级失败");
        }

        log.info("年级更新成功: gradeId={}", grade.getId());
        return convertToDTO(grade);
    }

    @Override
    @CacheEvict(value = {"grades", "grade"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGrade(Long id) {
        log.info("删除年级: gradeId={}", id);

        // 1. 检查年级是否存在
        Grade grade = getById(id);
        if (grade == null) {
            throw new BusinessException("年级不存在");
        }

        // 2. 检查是否可以删除
        if (!checkCanDelete(id)) {
            throw new BusinessException("年级下有班级或学生，无法删除");
        }

        // 3. 使用MyBatis-Plus的removeById方法进行逻辑删除
        // 由于BaseEntity的deleted字段有@TableLogic注解,removeById会自动设置deleted=1
        boolean success = removeById(id);
        log.info("年级删除成功: gradeId={}", id);
        return success;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<GradeDTO> listGrades(Integer pageNum, Integer pageSize,
                                       Integer enrollmentYear, Integer status,
                                       String keyword) {
        Page<Grade> page = new Page<>(pageNum, pageSize);
        IPage<Grade> gradePage = gradeMapper.selectGradePageWithDetails(
                page, enrollmentYear, status, keyword
        );

        return gradePage.convert(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "grade", key = "#id")
    @Transactional(readOnly = true)
    public GradeDTO getGradeById(Long id) {
        log.debug("从数据库查询年级: {}", id);
        Grade grade = gradeMapper.selectGradeWithDetails(id);
        return convertToDTO(grade);
    }

    @Override
    @Cacheable(value = "grade", key = "'code:' + #gradeCode")
    @Transactional(readOnly = true)
    public GradeDTO getGradeByCode(String gradeCode) {
        log.debug("从数据库查询年级(按编码): {}", gradeCode);
        Grade grade = gradeMapper.selectByGradeCode(gradeCode);
        return convertToDTO(grade);
    }


    @Override
    @Cacheable(value = "grades", key = "'director:' + #directorId")
    @Transactional(readOnly = true)
    public List<GradeDTO> listGradesByDirector(Long directorId) {
        log.debug("从数据库查询年级(按主任ID): {}", directorId);
        List<Grade> grades = gradeMapper.selectByDirectorId(directorId);
        return convertToDTOList(grades);
    }

    @Override
    @CacheEvict(value = {"grades", "grade"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean syncGradeStatistics(Long gradeId) {
        log.info("同步年级统计: gradeId={}", gradeId);

        int updated;
        if (gradeId != null) {
            // 同步单个年级
            updated = gradeMapper.syncGradeStatistics(gradeId);
        } else {
            // 同步所有年级
            updated = gradeMapper.batchSyncGradeStatistics();
        }

        log.info("年级统计同步完成: 更新{}个年级", updated);
        return updated > 0;
    }

    @Override
    @CacheEvict(value = {"grades", "grade"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGradeDirector(Long gradeId, Long directorId) {
        log.info("更新年级主任: gradeId={}, directorId={}", gradeId, directorId);

        int updated = gradeMapper.updateGradeDirector(gradeId, directorId);
        return updated > 0;
    }

    @Override
    @CacheEvict(value = {"grades", "grade"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> gradeIds, Integer status) {
        log.info("批量更新年级状态: gradeIds.size={}, status={}", gradeIds.size(), status);

        if (gradeIds == null || gradeIds.isEmpty()) {
            return false;
        }

        int updated = gradeMapper.batchUpdateStatus(gradeIds, status);
        log.info("批量更新完成: 更新{}个年级", updated);
        return updated > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public GradeStatisticsVO getGradeStatistics(Long gradeId) {
        log.info("获取年级统计: gradeId={}", gradeId);

        GradeStatisticsVO statistics = new GradeStatisticsVO();
        statistics.setGradeId(gradeId);

        // 1. 基础信息
        Grade grade = getById(gradeId);
        if (grade == null) {
            log.warn("年级不存在: gradeId={}", gradeId);
            return statistics;
        }

        statistics.setGradeName(grade.getGradeName());
        statistics.setClassCount(grade.getClassCount());
        statistics.setStudentCount(grade.getStudentCount());
        statistics.setAverageClassSize(grade.getAverageClassSize());
        statistics.setStandardClassSize(grade.getStandardClassSize());

        // 2. 班级人数详情统计
        List<Map<String, Object>> classDetails = gradeMapper.selectClassDetailsForStatistics(gradeId);
        if (classDetails != null && !classDetails.isEmpty()) {
            int maxSize = 0;
            int minSize = Integer.MAX_VALUE;
            String maxClassName = null;
            String minClassName = null;
            double sumSquaredDiff = 0;
            double avgSize = grade.getAverageClassSize() != null ? grade.getAverageClassSize() : 0;

            for (Map<String, Object> classInfo : classDetails) {
                Object countObj = classInfo.get("studentCount");
                int studentCount = countObj != null ? ((Number) countObj).intValue() : 0;
                String className = (String) classInfo.get("className");

                if (studentCount > maxSize) {
                    maxSize = studentCount;
                    maxClassName = className;
                }
                if (studentCount < minSize) {
                    minSize = studentCount;
                    minClassName = className;
                }
                sumSquaredDiff += Math.pow(studentCount - avgSize, 2);
            }

            statistics.setMaxClassSize(maxSize);
            statistics.setMinClassSize(minSize == Integer.MAX_VALUE ? 0 : minSize);
            statistics.setMaxSizeClassName(maxClassName);
            statistics.setMinSizeClassName(minClassName);

            // 计算方差
            if (!classDetails.isEmpty()) {
                statistics.setClassSizeVariance(sumSquaredDiff / classDetails.size());
            }
        }

        // 3. 申诉统计
        LocalDateTime now = LocalDateTime.now();

        // 本月统计
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = now;
        Map<String, Object> monthlyStats = appealMapper.statisticsAppealsByGradeAndTime(gradeId, monthStart, monthEnd);
        if (monthlyStats != null) {
            Object totalObj = monthlyStats.get("totalCount");
            Object approvedObj = monthlyStats.get("approvedCount");
            Object adjustmentObj = monthlyStats.get("totalScoreAdjustment");

            int monthlyTotal = totalObj != null ? ((Number) totalObj).intValue() : 0;
            int monthlyApproved = approvedObj != null ? ((Number) approvedObj).intValue() : 0;

            statistics.setMonthlyAppealCount(monthlyTotal);
            statistics.setMonthlyApprovedCount(monthlyApproved);
            statistics.setMonthlyScoreAdjustment(adjustmentObj != null ?
                    new BigDecimal(adjustmentObj.toString()) : BigDecimal.ZERO);

            if (monthlyTotal > 0) {
                statistics.setMonthlyApprovalRate(
                        BigDecimal.valueOf(monthlyApproved)
                                .divide(BigDecimal.valueOf(monthlyTotal), 4, RoundingMode.HALF_UP)
                );
            } else {
                statistics.setMonthlyApprovalRate(BigDecimal.ZERO);
            }
        }

        // 本学期统计(约6个月)
        LocalDateTime semesterStart = now.minusMonths(6).withHour(0).withMinute(0).withSecond(0);
        Map<String, Object> semesterStats = appealMapper.statisticsAppealsByGradeAndTime(gradeId, semesterStart, now);
        if (semesterStats != null) {
            Object totalObj = semesterStats.get("totalCount");
            Object approvedObj = semesterStats.get("approvedCount");
            Object adjustmentObj = semesterStats.get("totalScoreAdjustment");

            int semesterTotal = totalObj != null ? ((Number) totalObj).intValue() : 0;
            int semesterApproved = approvedObj != null ? ((Number) approvedObj).intValue() : 0;

            statistics.setSemesterAppealCount(semesterTotal);
            statistics.setSemesterApprovedCount(semesterApproved);
            statistics.setSemesterScoreAdjustment(adjustmentObj != null ?
                    new BigDecimal(adjustmentObj.toString()) : BigDecimal.ZERO);

            if (semesterTotal > 0) {
                statistics.setSemesterApprovalRate(
                        BigDecimal.valueOf(semesterApproved)
                                .divide(BigDecimal.valueOf(semesterTotal), 4, RoundingMode.HALF_UP)
                );
            } else {
                statistics.setSemesterApprovalRate(BigDecimal.ZERO);
            }
        }

        // 4. 检查次数统计
        LocalDate today = LocalDate.now();

        // 本周统计
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        statistics.setWeeklyCheckCount(
                dailyCheckMapper.countChecksByGradeAndDateRange(gradeId, weekStart, today)
        );

        // 本月统计
        LocalDate monthStartDate = today.withDayOfMonth(1);
        statistics.setMonthlyCheckCount(
                dailyCheckMapper.countChecksByGradeAndDateRange(gradeId, monthStartDate, today)
        );

        // 本学期统计
        LocalDate semesterStartDate = today.minusMonths(6);
        statistics.setSemesterCheckCount(
                dailyCheckMapper.countChecksByGradeAndDateRange(gradeId, semesterStartDate, today)
        );

        // 5. 平均扣分统计
        BigDecimal avgDeductionPerClass = classStatsMapper.avgDeductionByGrade(gradeId);
        statistics.setAverageDeductionPerClass(avgDeductionPerClass != null ? avgDeductionPerClass : BigDecimal.ZERO);

        // 人均扣分
        if (grade.getStudentCount() != null && grade.getStudentCount() > 0 && avgDeductionPerClass != null) {
            int classCount = grade.getClassCount() != null ? grade.getClassCount() : 1;
            BigDecimal totalDeduction = avgDeductionPerClass.multiply(BigDecimal.valueOf(classCount));
            statistics.setAverageDeductionPerStudent(
                    totalDeduction.divide(BigDecimal.valueOf(grade.getStudentCount()), 4, RoundingMode.HALF_UP)
            );
        } else {
            statistics.setAverageDeductionPerStudent(BigDecimal.ZERO);
        }

        // 6. 加权配置状态(简化判断)
        statistics.setWeightEnabled(grade.getStandardClassSize() != null && grade.getStandardClassSize() > 0);

        log.info("年级统计完成: gradeId={}, classCount={}, appealCount={}",
                gradeId, statistics.getClassCount(), statistics.getMonthlyAppealCount());

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkCanDelete(Long gradeId) {
        // 检查年级下是否有班级
        int classCount = gradeMapper.countClassesByGradeId(gradeId);
        if (classCount > 0) {
            log.warn("年级下有{}个班级,无法删除", classCount);
            return false;
        }

        // 检查年级下是否有学生
        int studentCount = gradeMapper.countStudentsByGradeId(gradeId);
        if (studentCount > 0) {
            log.warn("年级下有{}个学生,无法删除", studentCount);
            return false;
        }

        return true;
    }

    @Override
    public GradeDTO convertToDTO(Grade grade) {
        if (grade == null) {
            return null;
        }

        GradeDTO dto = new GradeDTO();
        BeanUtils.copyProperties(grade, dto);
        return dto;
    }

    @Override
    public List<GradeDTO> convertToDTOList(List<Grade> grades) {
        if (grades == null) {
            return null;
        }
        return grades.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
