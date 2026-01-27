package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.Class;
import com.school.management.entity.ClassSizeSnapshot;
import com.school.management.entity.ClassSizeStandard;
import com.school.management.entity.Semester;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.ClassSizeSnapshotMapper;
import com.school.management.mapper.ClassSizeStandardMapper;
import com.school.management.service.ClassSizeSnapshotService;
import com.school.management.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级人数快照服务实现类
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSizeSnapshotServiceImpl extends ServiceImpl<ClassSizeSnapshotMapper, ClassSizeSnapshot>
        implements ClassSizeSnapshotService {

    private final ClassSizeSnapshotMapper snapshotMapper;
    private final ClassMapper classMapper;
    private final ClassSizeStandardMapper standardMapper;
    private final SemesterService semesterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ClassSizeSnapshot> createSnapshotsForCheck(Long recordId, List<Long> classIds) {
        log.info("为检查记录创建人数快照: recordId={}, classIds.size={}", recordId, classIds.size());

        LocalDate snapshotDate = LocalDate.now();
        List<ClassSizeSnapshot> snapshots = new ArrayList<>();

        for (Long classId : classIds) {
            // 检查是否已存在快照
            ClassSizeSnapshot existing = snapshotMapper.selectOne(
                    new LambdaQueryWrapper<ClassSizeSnapshot>()
                            .eq(ClassSizeSnapshot::getRecordId, recordId)
                            .eq(ClassSizeSnapshot::getClassId, classId)
                            .last("LIMIT 1")
            );

            if (existing != null) {
                log.debug("快照已存在,跳过: classId={}", classId);
                snapshots.add(existing);
                continue;
            }

            // 创建新快照
            ClassSizeSnapshot snapshot = createSnapshot(classId, snapshotDate, recordId, "CHECK");
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }

        log.info("人数快照创建完成: 共{}个", snapshots.size());
        return snapshots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassSizeSnapshot createSnapshot(Long classId, LocalDate snapshotDate,
                                             Long recordId, String source) {
        log.debug("创建班级人数快照: classId={}, date={}, source={}", classId, snapshotDate, source);

        // 1. 获取班级信息
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            log.error("班级不存在: classId={}", classId);
            return null;
        }

        // 2. 获取标准人数
        Integer standardSize = getStandardSize(clazz);

        // 3. 创建快照
        ClassSizeSnapshot snapshot = new ClassSizeSnapshot();
        snapshot.setRecordId(recordId);
        snapshot.setClassId(classId);
        // snapshot.setGradeId(clazz.getGradeLevel()); // ClassSizeSnapshot没有gradeId字段
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setStudentCount(clazz.getStudentCount() != null ? clazz.getStudentCount() : 0);
        snapshot.setStandardSize(standardSize);
        snapshot.setSnapshotSource(source);
        snapshot.setIsUsed(0);
        snapshot.setUsageCount(0);
        snapshot.setCreatedAt(LocalDateTime.now());

        // 4. 保存快照
        if (save(snapshot)) {
            log.debug("快照创建成功: id={}", snapshot.getId());
            return snapshot;
        }

        log.error("快照创建失败");
        return null;
    }

    @Override
    public ClassSizeSnapshot ensureSnapshot(Long classId, LocalDate snapshotDate) {
        // 先查询是否存在
        ClassSizeSnapshot snapshot = getSnapshotByDate(classId, snapshotDate);

        if (snapshot != null) {
            return snapshot;
        }

        // 不存在则创建
        return createSnapshot(classId, snapshotDate, null, "MANUAL");
    }

    @Override
    public ClassSizeSnapshot getSnapshotByDate(Long classId, LocalDate snapshotDate) {
        return snapshotMapper.selectByClassAndDate(classId, snapshotDate);
    }

    @Override
    public ClassSizeSnapshot getLatestSnapshot(Long classId) {
        return snapshotMapper.selectLatestByClass(classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createDailySnapshot() {
        log.info("开始创建每日人数快照");

        LocalDate today = LocalDate.now();

        // 查询所有正常状态的班级
        List<com.school.management.entity.Class> classes = classMapper.selectList(
                new LambdaQueryWrapper<com.school.management.entity.Class>()
                        .eq(com.school.management.entity.Class::getDeleted, 0)
        );

        int count = 0;
        for (com.school.management.entity.Class clazz : classes) {
            // 检查今天是否已创建快照
            ClassSizeSnapshot existing = getSnapshotByDate(clazz.getId(), today);
            if (existing != null) {
                continue;
            }

            // 创建快照
            ClassSizeSnapshot snapshot = createSnapshot(
                    clazz.getId(), today, null, "SCHEDULED"
            );

            if (snapshot != null) {
                count++;
            }
        }

        log.info("每日人数快照创建完成: 共{}个", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredSnapshots(int daysToKeep) {
        log.info("开始清理过期快照: 保留{}天", daysToKeep);

        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);

        // 删除过期且未使用的快照
        int deleted = baseMapper.delete(
                new LambdaQueryWrapper<ClassSizeSnapshot>()
                        .lt(ClassSizeSnapshot::getSnapshotDate, cutoffDate)
                        .eq(ClassSizeSnapshot::getIsUsed, 0)
        );

        log.info("过期快照清理完成: 删除{}个", deleted);
        return deleted;
    }

    /**
     * 获取班级的标准人数
     * 按优先级查询: 学期+部门+年级 > 学期+部门 > 学期 > 默认40
     *
     * @param clazz 班级实体
     * @return 标准人数
     */
    private Integer getStandardSize(Class clazz) {
        try {
            // 尝试查询标准人数(按优先级)
            // 获取当前学期ID
            Long semesterId = getCurrentSemesterId();

            ClassSizeStandard standard = standardMapper.selectStandardSize(
                    semesterId,
                    clazz.getOrgUnitId(),
                    clazz.getGradeLevel()
            );

            if (standard != null && standard.getStandardSize() != null) {
                log.debug("获取到班级标准人数: classId={}, orgUnitId={}, gradeLevel={}, semesterId={}, standardSize={}",
                        clazz.getId(), clazz.getOrgUnitId(), clazz.getGradeLevel(), semesterId, standard.getStandardSize());
                return standard.getStandardSize();
            }

            // 如果没有找到配置,使用默认值40
            log.debug("未找到标准人数配置,使用默认值40: classId={}", clazz.getId());
            return 40;

        } catch (Exception e) {
            log.warn("获取标准人数失败,使用默认值40: classId={}", clazz.getId(), e);
            return 40;
        }
    }

    /**
     * 获取当前学期ID
     *
     * @return 当前学期ID，如果没有设置当前学期则返回null
     */
    private Long getCurrentSemesterId() {
        try {
            Semester currentSemester = semesterService.getCurrentSemester();
            if (currentSemester != null) {
                return currentSemester.getId();
            }
        } catch (Exception e) {
            log.warn("获取当前学期失败: {}", e.getMessage());
        }
        return null;
    }
}
