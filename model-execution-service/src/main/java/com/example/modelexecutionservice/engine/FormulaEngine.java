package com.example.modelexecutionservice.engine;

import java.util.Map;

/**
 * Pluggable expression evaluator used by the execution orchestrator to compute
 * derived fields and outputs from a model definition.
 *
 * Typical usage:
 *   Map<String, Object> ctx = Map.of(
 *       "principal", 10000,
 *       "rate", 0.12,
 *       "term", 36
 *   );
 *   BigDecimal emi = engine.evaluateAs("pmt(rate/12, term, -principal)", ctx, BigDecimal.class);
 *
 * Implementations should be side‑effect free and thread‑safe, or document otherwise.
 */
public interface FormulaEngine {

    /**
     * Evaluate an expression string with the provided context.
     *
     * @param expression expression text (e.g., "min(a + b, 100)")
     * @param context    variables available to the expression
     * @return raw result (caller may cast)
     * @throws FormulaEvaluationException on parse/eval errors
     */
    Object evaluate(String expression, Map<String, Object> context) throws FormulaEvaluationException;

    /**
     * Evaluate an expression string and coerce the result to the requested type.
     *
     * @param expression expression text
     * @param context    variables available to the expression
     * @param type       desired return type
     * @return result coerced to the requested type
     * @param <T>        generic type parameter
     * @throws FormulaEvaluationException on parse/eval errors or incompatible type coercion
     */
    <T> T evaluateAs(String expression, Map<String, Object> context, Class<T> type) throws FormulaEvaluationException;
}
