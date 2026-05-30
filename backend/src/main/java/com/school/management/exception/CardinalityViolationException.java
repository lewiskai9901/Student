package com.school.management.exception;

/**
 * 关系基数约束违反 (cardinality violation).
 *
 * <p>当 forceGrant 尝试新增一条会超出关系类型 cardinality 上限的关系时抛出:
 * <ul>
 *   <li>maxPerSubject — 每主体上限 (如 member=1 每用户唯一归属)</li>
 *   <li>maxPerResource — 每资源上限 (如 admin=1 每组织唯一主管理员)</li>
 * </ul>
 *
 * <p>语义上是"数据已存在/冲突", 复用 {@link ErrorCode#DATA_ALREADY_EXISTS}。
 */
public class CardinalityViolationException extends AccessDomainException {

    public CardinalityViolationException(String message) {
        super(ErrorCode.DATA_ALREADY_EXISTS, message);
    }
}
