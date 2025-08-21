package com.example.modelexecutionservice.engine;

/**
 * Thrown when a model expression fails to parse or evaluate,
 * or when its result cannot be coerced to the requested type.
 */
public class FormulaEvaluationException extends RuntimeException {

    public FormulaEvaluationException() {
        super();
    }

    public FormulaEvaluationException(String message) {
        super(message);
    }

    public FormulaEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormulaEvaluationException(Throwable cause) {
        super(cause);
    }
}
