package com.school.management.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 申诉编号生成器
 * 格式: AP + 日期(yyyyMMdd) + 4位流水号
 * 示例: AP202412290001
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Slf4j
@Component
public class AppealCodeGenerator {

    private static final String APPEAL_CODE_PREFIX = "AP";
    private static final String REDIS_KEY_PREFIX = "appeal:code:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 生成申诉编号
     *
     * @return 申诉编号
     */
    public String generateCode() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String prefix = APPEAL_CODE_PREFIX + dateStr;

        // 如果Redis可用,使用Redis生成流水号
        if (redisTemplate != null) {
            return generateCodeWithRedis(prefix, dateStr);
        }

        // 否则使用内存方式(不推荐生产环境)
        return generateCodeWithMemory(prefix);
    }

    /**
     * 使用Redis生成编号(推荐)
     */
    private String generateCodeWithRedis(String prefix, String dateStr) {
        String redisKey = REDIS_KEY_PREFIX + dateStr;

        try {
            // 原子递增
            Long sequence = redisTemplate.opsForValue().increment(redisKey);

            // 设置过期时间为2天
            if (sequence == 1) {
                redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
            }

            String code = prefix + String.format("%04d", sequence);
            log.debug("生成申诉编号(Redis): {}", code);
            return code;

        } catch (Exception e) {
            log.error("Redis生成编号失败,降级到内存方式", e);
            return generateCodeWithMemory(prefix);
        }
    }

    /**
     * 使用内存生成编号(备用方案)
     */
    private synchronized String generateCodeWithMemory(String prefix) {
        // 简单方式:使用时间戳的后4位作为流水号
        long timestamp = System.currentTimeMillis();
        int sequence = (int) (timestamp % 10000);

        String code = prefix + String.format("%04d", sequence);
        log.warn("生成申诉编号(内存): {} - 建议配置Redis以保证唯一性", code);
        return code;
    }

    /**
     * 验证申诉编号格式
     *
     * @param code 申诉编号
     * @return 是否有效
     */
    public boolean validate(String code) {
        if (code == null || code.length() != 14) {
            return false;
        }

        // AP + 8位日期 + 4位流水号
        if (!code.startsWith(APPEAL_CODE_PREFIX)) {
            return false;
        }

        String dateStr = code.substring(2, 10);
        String seqStr = code.substring(10);

        // 验证日期格式
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return false;
        }

        // 验证流水号格式
        try {
            Integer.parseInt(seqStr);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * 从申诉编号中提取日期
     *
     * @param code 申诉编号
     * @return 日期
     */
    public LocalDate extractDate(String code) {
        if (!validate(code)) {
            return null;
        }

        String dateStr = code.substring(2, 10);
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * 从申诉编号中提取流水号
     *
     * @param code 申诉编号
     * @return 流水号
     */
    public Integer extractSequence(String code) {
        if (!validate(code)) {
            return null;
        }

        String seqStr = code.substring(10);
        return Integer.parseInt(seqStr);
    }
}
