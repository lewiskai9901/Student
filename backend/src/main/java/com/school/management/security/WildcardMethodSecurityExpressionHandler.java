package com.school.management.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

/**
 * 让 {@code @PreAuthorize}/{@code @PostAuthorize} 表达式识别超管通配权限 {@code "*"}。
 *
 * <p>实现方式: 委托父类构建标准的 {@link EvaluationContext}, 然后把其根对象替换为
 * {@link WildcardSecurityExpressionRoot} 包装版 — 该包装在 hasAuthority/hasRole 等判定
 * 失败时追加一次 {@code "*"} 通配检查。这样不复制父类的上下文装配逻辑 (parameter
 * discoverer / bean resolver / permission evaluator 全部沿用), 版本升级也不易碎。
 *
 * @see WildcardSecurityExpressionRoot
 */
public class WildcardMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
        EvaluationContext ctx = super.createEvaluationContext(authentication, mi);
        if (ctx instanceof StandardEvaluationContext sec
                && sec.getRootObject().getValue() instanceof MethodSecurityExpressionOperations ops
                && !(ops instanceof WildcardSecurityExpressionRoot)) {
            sec.setRootObject(new WildcardSecurityExpressionRoot(ops));
        }
        return ctx;
    }
}
