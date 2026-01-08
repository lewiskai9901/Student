package com.school.management.util;

import com.school.management.common.result.ResultCode;
import com.school.management.exception.BusinessException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 断言工具类 - 用于减少服务层重复的验证代码
 *
 * @author system
 * @since 1.0.0
 */
public final class AssertUtil {

    private AssertUtil() {
        // 私有构造函数，防止实例化
    }

    /**
     * 断言对象不为空，否则抛出业务异常
     *
     * @param object  待检查的对象
     * @param message 错误消息
     * @throws BusinessException 如果对象为空
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言对象不为空，否则抛出指定错误码的业务异常
     *
     * @param object     待检查的对象
     * @param resultCode 错误码
     * @param message    错误消息
     * @throws BusinessException 如果对象为空
     */
    public static void notNull(Object object, ResultCode resultCode, String message) {
        if (object == null) {
            throw new BusinessException(resultCode, message);
        }
    }

    /**
     * 断言实体存在，否则抛出"资源不存在"异常
     *
     * @param entity     实体对象
     * @param entityName 实体名称（如"用户"、"班级"等）
     * @throws BusinessException 如果实体为空
     */
    public static void entityExists(Object entity, String entityName) {
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, entityName + "不存在");
        }
    }

    /**
     * 断言实体存在，否则抛出"资源不存在"异常（带ID信息）
     *
     * @param entity     实体对象
     * @param entityName 实体名称
     * @param id         实体ID
     * @throws BusinessException 如果实体为空
     */
    public static void entityExists(Object entity, String entityName, Object id) {
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND,
                String.format("%s不存在（ID: %s）", entityName, id));
        }
    }

    /**
     * 断言字符串不为空，否则抛出业务异常
     *
     * @param str     待检查的字符串
     * @param message 错误消息
     * @throws BusinessException 如果字符串为空
     */
    public static void hasText(String str, String message) {
        if (!StringUtils.hasText(str)) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言集合不为空，否则抛出业务异常
     *
     * @param collection 待检查的集合
     * @param message    错误消息
     * @throws BusinessException 如果集合为空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言条件为真，否则抛出业务异常
     *
     * @param condition 条件
     * @param message   错误消息
     * @throws BusinessException 如果条件为假
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言条件为真，否则抛出指定错误码的业务异常
     *
     * @param condition  条件
     * @param resultCode 错误码
     * @param message    错误消息
     * @throws BusinessException 如果条件为假
     */
    public static void isTrue(boolean condition, ResultCode resultCode, String message) {
        if (!condition) {
            throw new BusinessException(resultCode, message);
        }
    }

    /**
     * 断言条件为假，否则抛出业务异常
     *
     * @param condition 条件
     * @param message   错误消息
     * @throws BusinessException 如果条件为真
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言两个对象相等，否则抛出业务异常
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param message  错误消息
     * @throws BusinessException 如果不相等
     */
    public static void equals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言数值在指定范围内，否则抛出业务异常
     *
     * @param value   待检查的数值
     * @param min     最小值（包含）
     * @param max     最大值（包含）
     * @param message 错误消息
     * @throws BusinessException 如果数值超出范围
     */
    public static void inRange(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言数值为正数，否则抛出业务异常
     *
     * @param value   待检查的数值
     * @param message 错误消息
     * @throws BusinessException 如果数值非正
     */
    public static void positive(long value, String message) {
        if (value <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }

    /**
     * 断言数值非负，否则抛出业务异常
     *
     * @param value   待检查的数值
     * @param message 错误消息
     * @throws BusinessException 如果数值为负
     */
    public static void nonNegative(long value, String message) {
        if (value < 0) {
            throw new BusinessException(ResultCode.VALIDATE_ERROR, message);
        }
    }
}
