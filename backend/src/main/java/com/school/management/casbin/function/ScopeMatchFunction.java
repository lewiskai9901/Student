package com.school.management.casbin.function;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.school.management.casbin.service.ScopeHierarchyCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Casbin 自定义范围匹配函数
 * 用于在策略匹配时判断请求的范围是否在策略允许的范围内
 *
 * 使用 Aviator 表达式引擎的 AbstractFunction
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class ScopeMatchFunction extends AbstractFunction {

    private static ScopeHierarchyCache hierarchyCacheInstance;

    private final ScopeHierarchyCache hierarchyCache;

    public ScopeMatchFunction(ScopeHierarchyCache hierarchyCache) {
        this.hierarchyCache = hierarchyCache;
        ScopeMatchFunction.hierarchyCacheInstance = hierarchyCache;
    }

    @Override
    public String getName() {
        return "scopeMatch";
    }

    /**
     * 执行范围匹配 (3个参数)
     *
     * @param env  环境变量
     * @param arg1 userId
     * @param arg2 requestScope
     * @param arg3 policyScope
     * @return 是否匹配
     */
    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        String userId = String.valueOf(arg1.getValue(env));
        String requestScope = String.valueOf(arg2.getValue(env));
        String policyScope = String.valueOf(arg3.getValue(env));

        boolean result = doMatch(userId, requestScope, policyScope);
        log.debug("scopeMatch({}, {}, {}) = {}", userId, requestScope, policyScope, result);

        return result ? AviatorBoolean.TRUE : AviatorBoolean.FALSE;
    }

    /**
     * 执行实际的匹配逻辑
     */
    private boolean doMatch(String userId, String requestScope, String policyScope) {
        ScopeHierarchyCache cache = hierarchyCacheInstance;
        if (cache == null) {
            cache = this.hierarchyCache;
        }

        // 1. 全局范围匹配所有
        if ("scope:*".equals(policyScope) || "*".equals(policyScope)) {
            return true;
        }

        // 2. 精确匹配
        if (requestScope.equals(policyScope)) {
            return true;
        }

        // 3. 通配符模式匹配
        // 例如: policyScope = "scope:dept:*" 匹配 requestScope = "scope:dept:1"
        if (policyScope.endsWith(":*")) {
            String prefix = policyScope.substring(0, policyScope.length() - 1);
            if (requestScope.startsWith(prefix)) {
                return true;
            }
        }

        if (cache == null) {
            log.warn("ScopeHierarchyCache is not available");
            return false;
        }

        // 4. 用户范围继承匹配
        // 检查用户是否被分配了包含请求范围的更大范围
        if (cache.hasUserScope(userId, policyScope)) {
            // 检查请求的范围是否在用户分配的范围之内
            if (cache.isScopeContained(policyScope, requestScope)) {
                return true;
            }
        }

        // 5. 层级匹配
        // 例如: requestScope = "scope:class:101", policyScope = "scope:grade:24"
        // 如果班级101属于24级，则匹配成功
        return cache.isChildOf(requestScope, policyScope);
    }
}
