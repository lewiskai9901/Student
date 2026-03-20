package com.school.management.infrastructure.scoring;

import com.school.management.domain.inspection.service.FormulaEvaluator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * GraalVM Polyglot JS 公式求值器
 *
 * 使用共享 Engine 提升性能，每次求值创建隔离 Context 保证安全性。
 * 公式示例: "score * quantity * (baseline / population)"
 */
@Component
public class GraalVmFormulaEvaluator implements FormulaEvaluator {

    private static final Logger log = LoggerFactory.getLogger(GraalVmFormulaEvaluator.class);
    private static final int MAX_FORMULA_LENGTH = 4096;
    private static final long TIMEOUT_MS = 5000;

    private final Engine engine;

    public GraalVmFormulaEvaluator() {
        this.engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    @Override
    public BigDecimal evaluate(String formula, Map<String, Object> variables) {
        if (formula == null || formula.isBlank()) {
            return BigDecimal.ZERO;
        }
        if (formula.length() > MAX_FORMULA_LENGTH) {
            throw new IllegalArgumentException("公式长度超过限制: " + MAX_FORMULA_LENGTH);
        }

        try (Context context = Context.newBuilder("js")
                .engine(engine)
                .allowAllAccess(false)
                .build()) {

            // 绑定变量到 JS 上下文
            Value bindings = context.getBindings("js");
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof BigDecimal) {
                        bindings.putMember(entry.getKey(), ((BigDecimal) val).doubleValue());
                    } else {
                        bindings.putMember(entry.getKey(), val);
                    }
                }
            }

            // 提供内置数学函数
            context.eval("js", "var sqrt = Math.sqrt; var abs = Math.abs; var min = Math.min; var max = Math.max; var pow = Math.pow; var round = Math.round; var floor = Math.floor; var ceil = Math.ceil;");

            // 执行公式
            Value result = context.eval("js", formula);

            if (result.isNull() || !result.isNumber()) {
                log.warn("公式返回非数值结果: formula={}, result={}", formula, result);
                return BigDecimal.ZERO;
            }

            double d = result.asDouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                log.warn("公式返回 NaN/Infinity: formula={}", formula);
                return BigDecimal.ZERO;
            }

            return BigDecimal.valueOf(d).setScale(10, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("公式执行失败: formula={}, variables={}", formula, variables, e);
            throw new IllegalArgumentException("公式执行失败: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (engine != null) {
            engine.close();
        }
    }
}
