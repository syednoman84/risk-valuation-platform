package com.example.modelexecutionservice.engine;

import org.mvel2.MVEL;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default FormulaEngine implementation backed by MVEL.
 *
 * Usage in expressions (examples):
 *   - fn.min(a + b, 100)
 *   - fn.round(principal * rate, 2)
 *   - fn.pmt(rate/12, term, -principal)            // payment per period
 *   - fn.pv(rate/12, term, payment, 0, 0)          // present value
 *   - fn.safeDiv(numerator, denominator)           // returns 0 if denom == 0
 *   - fn.toNumber("123.45")
 *
 * Note: Functions are exposed under variable "fn".
 */
public class DefaultFormulaEngine implements FormulaEngine {

    // Cache compiled expressions for performance
    private final Map<String, Serializable> compiledCache = new ConcurrentHashMap<>();

    private final MathContext mc;

    public DefaultFormulaEngine() {
        this.mc = new MathContext(20, RoundingMode.HALF_UP);
    }

    public DefaultFormulaEngine(MathContext mc) {
        this.mc = Objects.requireNonNull(mc);
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) throws FormulaEvaluationException {
        if (expression == null || expression.isBlank()) {
            throw new FormulaEvaluationException("Expression is null or blank");
        }
        try {
            // Merge user ctx + built-ins
            Map<String, Object> vars = new HashMap<>();
            if (context != null) vars.putAll(context);
            vars.putIfAbsent("fn", new Functions(mc)); // expose helpers under "fn"

            // For lookup expressions, disable all optimization
            if (expression.contains("lookup")) {
                // Completely disable optimization by setting system property
                String oldOptLevel = System.getProperty("mvel2.disable.jit");
                System.setProperty("mvel2.disable.jit", "true");
                try {
                    return MVEL.eval(expression, vars);
                } finally {
                    if (oldOptLevel != null) {
                        System.setProperty("mvel2.disable.jit", oldOptLevel);
                    } else {
                        System.clearProperty("mvel2.disable.jit");
                    }
                }
            }
            
            // Use compiled/cached version for other expressions
            final Serializable compiled = compiledCache.computeIfAbsent(expression, MVEL::compileExpression);
            return MVEL.executeExpression(compiled, vars);
        } catch (Exception ex) {
            throw new FormulaEvaluationException("Failed to evaluate expression: " + expression, ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluateAs(String expression, Map<String, Object> context, Class<T> type)
            throws FormulaEvaluationException {
        Object raw = evaluate(expression, context);
        if (raw == null) return null;
        if (type.isInstance(raw)) return (T) raw;

        // Simple coercions commonly needed in numeric work
        try {
            if (type == BigDecimal.class) {
                return (T) toBigDecimal(raw, mc);
            } else if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(toBigDecimal(raw, mc).doubleValue());
            } else if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(toBigDecimal(raw, mc).longValue());
            } else if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(toBigDecimal(raw, mc).intValue());
            } else if (type == String.class) {
                return (T) String.valueOf(raw);
            }
        } catch (Exception ce) {
            throw new FormulaEvaluationException("Type coercion failed to " + type.getSimpleName(), ce);
        }

        throw new FormulaEvaluationException("Unsupported coercion to " + type.getSimpleName());
    }

    private static BigDecimal toBigDecimal(Object o, MathContext mc) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return new BigDecimal(n.toString(), mc);
        if (o instanceof CharSequence cs) return new BigDecimal(cs.toString().trim(), mc);
        throw new IllegalArgumentException("Cannot convert to BigDecimal: " + o.getClass());
    }

    /**
     * Helper functions exposed to expressions under the variable name "fn".
     * Keep this class stateless; it is created per evaluation to ensure thread-safety of MathContext usage.
     */
    public static final class Functions {
        private final MathContext mc;

        public Functions(MathContext mc) {
            this.mc = mc;
        }

        /** min of two numbers */
        public BigDecimal min(Object a, Object b) {
            BigDecimal A = toBD(a);
            BigDecimal B = toBD(b);
            return A.min(B);
        }

        /** max of two numbers */
        public BigDecimal max(Object a, Object b) {
            BigDecimal A = toBD(a);
            BigDecimal B = toBD(b);
            return A.max(B);
        }

        /** round(x, scale) */
        public BigDecimal round(Object x, int scale) {
            return toBD(x).setScale(scale, RoundingMode.HALF_UP);
        }

        /** safe division: returns 0 if denominator == 0 */
        public BigDecimal safeDiv(Object numerator, Object denominator) {
            BigDecimal num = toBD(numerator);
            BigDecimal den = toBD(denominator);
            if (den.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            return num.divide(den, mc);
        }

        /** toNumber("123.45") or toNumber(any Number) */
        public BigDecimal toNumber(Object x) {
            return toBD(x);
        }

        /**
         * PMT function: payment for an annuity based on constant payments and a constant interest rate.
         *
         * @param ratePerPeriod e.g., annualRate/12
         * @param nper          number of periods
         * @param pv            present value (loan principal, typically positive; sign conventions apply)
         * @return payment per period (BigDecimal, sign follows Excel/finance conventions)
         */
        public BigDecimal pmt(Object ratePerPeriod, Object nper, Object pv) {
            return pmt(ratePerPeriod, nper, pv, BigDecimal.ZERO, 0);
        }

        /**
         * PMT with FV and type (0=end of period, 1=beginning).
         */
        public BigDecimal pmt(Object ratePerPeriod, Object nper, Object pv, Object fv, int type) {
            BigDecimal r = toBD(ratePerPeriod);
            BigDecimal n = toBD(nper);
            BigDecimal PV = toBD(pv);
            BigDecimal FV = toBD(fv);

            if (r.compareTo(BigDecimal.ZERO) == 0) {
                // simple: (pv + fv) / n, with sign handling
                BigDecimal payment = (PV.add(FV)).divide(n, mc).negate();
                return payment;
            } else {
                BigDecimal onePlusRpowN = pow(BigDecimal.ONE.add(r, mc), n.intValue());
                BigDecimal numerator = r.multiply(PV.multiply(onePlusRpowN, mc), mc)
                        .add(r.multiply(FV, mc), mc);
                BigDecimal denom = (type == 1)
                        ? (BigDecimal.ONE.add(r, mc).multiply(onePlusRpowN.subtract(BigDecimal.ONE, mc), mc))
                        : (onePlusRpowN.subtract(BigDecimal.ONE, mc));
                return numerator.divide(denom, mc).negate();
            }
        }

        /**
         * PV function: present value of an investment.
         *
         * @param ratePerPeriod interest rate per period
         * @param nper          number of periods
         * @param pmt           payment made each period (negative for outflow)
         * @param fv            future value
         * @param type          when payments are due: 0=end of period, 1=beginning
         */
        public BigDecimal pv(Object ratePerPeriod, Object nper, Object pmt, Object fv, int type) {
            BigDecimal r = toBD(ratePerPeriod);
            BigDecimal n = toBD(nper);
            BigDecimal PMT = toBD(pmt);
            BigDecimal FV = toBD(fv);

            if (r.compareTo(BigDecimal.ZERO) == 0) {
                // PV = - (FV + PMT * n)
                return (FV.add(PMT.multiply(n, mc), mc)).negate();
            } else {
                BigDecimal onePlusR = BigDecimal.ONE.add(r, mc);
                BigDecimal onePlusRpowN = pow(onePlusR, n.intValue());
                BigDecimal factor = (type == 1) ? onePlusR : BigDecimal.ONE;
                // PV = -( FV/(1+r)^n + PMT * factor * ( (1+r)^n - 1) / r )
                BigDecimal term1 = FV.divide(onePlusRpowN, mc);
                BigDecimal term2 = PMT.multiply(factor, mc)
                        .multiply(onePlusRpowN.subtract(BigDecimal.ONE, mc), mc)
                        .divide(r, mc);
                return (term1.add(term2, mc)).negate();
            }
        }

        private BigDecimal toBD(Object o) {
            if (o == null) return BigDecimal.ZERO;
            if (o instanceof BigDecimal bd) return bd;
            if (o instanceof Number n) return new BigDecimal(n.toString(), mc);
            if (o instanceof CharSequence cs) return new BigDecimal(cs.toString().trim(), mc);
            throw new IllegalArgumentException("Unsupported numeric type: " + o.getClass());
        }

        private BigDecimal pow(BigDecimal base, int exp) {
            if (exp < 0) {
                BigDecimal positive = pow(base, -exp);
                return BigDecimal.ONE.divide(positive, mc);
            }
            BigDecimal result = BigDecimal.ONE;
            BigDecimal b = base;
            int e = exp;
            while (e > 0) {
                if ((e & 1) == 1) {
                    result = result.multiply(b, mc);
                }
                e >>= 1;
                if (e > 0) b = b.multiply(b, mc);
            }
            return result;
        }
    }
}
