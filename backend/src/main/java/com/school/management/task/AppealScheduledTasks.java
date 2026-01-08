package com.school.management.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.entity.Student;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.service.ApprovalFlowService;
import com.school.management.service.CheckItemAppealService;
import com.school.management.service.ClassSizeSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 申诉系统定时任务
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppealScheduledTasks {

    private final CheckItemAppealService appealService;
    private final ApprovalFlowService approvalFlowService;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final ClassSizeSnapshotService snapshotService;

    /**
     * 处理公示期结束的申诉
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void processPublicityEnd() {
        log.info("开始执行定时任务: 处理公示期结束的申诉");

        try {
            int count = appealService.processPublicityEnd();
            log.info("公示期结束处理完成: 处理{}个申诉", count);
        } catch (Exception e) {
            log.error("处理公示期结束失败", e);
        }
    }

    /**
     * 检查审批超时
     * 每小时执行一次
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void checkApprovalTimeout() {
        log.info("开始执行定时任务: 检查审批超时");

        try {
            int count = approvalFlowService.checkTimeout();
            log.info("审批超时检查完成: 处理{}个超时审批", count);
        } catch (Exception e) {
            log.error("检查审批超时失败", e);
        }
    }

    /**
     * 同步班级人数
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncClassStudentCount() {
        log.info("开始执行定时任务: 同步班级人数");

        try {
            // 查询所有班级
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.management.entity.Class> classWrapper =
                    new LambdaQueryWrapper<>();
            classWrapper.eq(com.school.management.entity.Class::getDeleted, 0);
            java.util.List<com.school.management.entity.Class> classList = classMapper.selectList(classWrapper);

            int updatedCount = 0;
            int totalClasses = classList.size();

            // 遍历每个班级,统计实际学生数
            for (com.school.management.entity.Class clazz : classList) {
                // 统计该班级的学生数(未删除且状态为在读的学生)
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getClassId, clazz.getId())
                        .eq(Student::getDeleted, 0);
                // 如果Student实体有status字段,可以添加状态过滤
                // .eq(Student::getStatus, 1);  // 1=在读

                Long actualCount = studentMapper.selectCount(studentWrapper);

                // 如果实际人数与记录的人数不一致,则更新
                if (clazz.getStudentCount() == null || !clazz.getStudentCount().equals(actualCount.intValue())) {
                    int oldCount = clazz.getStudentCount() != null ? clazz.getStudentCount() : 0;
                    classMapper.updateStudentCount(clazz.getId(), actualCount.intValue());
                    updatedCount++;
                    log.debug("更新班级人数: classId={}, className={}, {} -> {}",
                            clazz.getId(), clazz.getClassName(), oldCount, actualCount);
                }
            }

            log.info("班级人数同步完成: 检查{}个班级, 更新{}个班级", totalClasses, updatedCount);
        } catch (Exception e) {
            log.error("同步班级人数失败", e);
        }
    }

    /**
     * 创建每日人数快照
     * 每天晚上11点执行
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void createDailySnapshot() {
        log.info("开始执行定时任务: 创建每日人数快照");

        try {
            int count = snapshotService.createDailySnapshot();
            log.info("每日人数快照创建完成: 创建{}个快照", count);
        } catch (Exception e) {
            log.error("创建每日人数快照失败", e);
        }
    }

    /**
     * 清理过期数据
     * 每周日凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 ? * SUN")
    public void cleanExpiredData() {
        log.info("开始执行定时任务: 清理过期数据");

        try {
            // 清理6个月前的审计日志
            // auditLogService.cleanExpiredLogs(180);

            // 清理1年前的人数快照(保留已使用的)
            // snapshotService.cleanExpiredSnapshots(365);

            log.info("过期数据清理完成");
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
}
