package com.school.management.infrastructure.schedule;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.domain.schedule.ScheduleCodeGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Default implementation of ScheduleCodeGenerator.
 * Generates policy codes in the format: SCH-yyyyMMdd-NNN
 */
@Service
public class DefaultScheduleCodeGenerator implements ScheduleCodeGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String generatePolicyCode() {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        int randomPart = Math.abs((int) (IdWorker.getId() % 1000));
        return String.format("SCH-%s-%03d", datePart, randomPart);
    }
}
