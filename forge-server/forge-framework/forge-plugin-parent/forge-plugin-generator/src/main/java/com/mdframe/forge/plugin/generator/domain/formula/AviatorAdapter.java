package com.mdframe.forge.plugin.generator.domain.formula;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.CompileExpressionErrorException;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin adapter over the Aviator expression engine.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Compile expression strings into executable {@link Expression} objects</li>
 *   <li>Extract variable (dependency) names from expressions</li>
 *   <li>Parse error messages to extract human-readable error positions</li>
 * </ul>
 * <p>
 * Does NOT execute expressions with real data — that stays in Phase 2B.
 * This adapter is deliberately narrow; only compile-time features are exposed.
 */
public class AviatorAdapter {

    /** Regex to extract line:column from Aviator error messages. */
    private static final Pattern POSITION_PATTERN =
        Pattern.compile("at\\s+\\((?:(\\d+):(\\d+))\\)");

    static {
        // Disable Aviator's internal cache so each compile is fresh during dev/test.
        // In production (Phase 3+), enable with LRU for performance.
        AviatorEvaluator.setOption(Options.OPTIMIZE_LEVEL, AviatorEvaluator.EVAL);
    }

    /**
     * Compile an expression string. Returns the compiled expression on success,
     * or throws {@link FormulaCompileException} with position details on failure.
     *
     * @param expression raw Aviator expression string
     * @return compiled Expression ready for execution
     * @throws FormulaCompileException if syntax is invalid
     */
    public Expression compile(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new FormulaCompileException("Expression must not be blank", null, -1, -1);
        }
        try {
            return AviatorEvaluator.compile(expression, true);
        } catch (ExpressionSyntaxErrorException e) {
            throw FormulaCompileException.fromSyntaxError(e);
        } catch (CompileExpressionErrorException e) {
            throw FormulaCompileException.fromCompileError(e);
        } catch (Exception e) {
            // Catch-all for unexpected Aviator errors
            throw new FormulaCompileException(
                "Unexpected compile error: " + e.getMessage(), e, -1, -1);
        }
    }

    /**
     * Extract all variable names referenced in an expression.
     * <p>
     * Uses Aviator's built-in {@code getVariableNames()} after compilation.
     * This is the authoritative way to get dependencies — more reliable than regex.
     *
     * @param expression raw Aviator expression string
     * @return sorted list of variable names (never null)
     */
    public List<String> extractVariables(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Expression compiled = AviatorEvaluator.compile(expression, true);
            List<String> vars = new ArrayList<>(compiled.getVariableNames());
            Collections.sort(vars);
            return Collections.unmodifiableList(vars);
        } catch (Exception e) {
            // If expression is invalid, fall back to empty list;
            // validation will catch the error separately.
            return Collections.emptyList();
        }
    }

    /**
     * Validate expression syntax and return a structured result.
     * <p>
     * This is the primary entry point for syntax-only validation.
     * Does not check dependency correctness — that is ExpressionParser's job.
     *
     * @param expression raw Aviator expression string
     * @return validation result with success flag, error details, and variable list
     */
    public SyntaxValidationResult validate(String expression) {
        if (expression == null || expression.isBlank()) {
            return SyntaxValidationResult.error("Expression must not be blank", -1, -1);
        }
        try {
            Expression compiled = AviatorEvaluator.compile(expression, true);
            List<String> vars = new ArrayList<>(compiled.getVariableNames());
            Collections.sort(vars);
            return SyntaxValidationResult.success(vars);
        } catch (ExpressionSyntaxErrorException e) {
            return FormulaCompileException.fromSyntaxError(e).toValidationResult();
        } catch (CompileExpressionErrorException e) {
            return FormulaCompileException.fromCompileError(e).toValidationResult();
        } catch (Exception e) {
            return SyntaxValidationResult.error("Unexpected error: " + e.getMessage(), -1, -1);
        }
    }

    // ─── inner types ───

    /**
     * Structured result of expression syntax validation.
     */
    public static class SyntaxValidationResult {
        private final boolean valid;
        private final List<String> variables;
        private final String errorMessage;
        private final int errorLine;
        private final int errorColumn;

        private SyntaxValidationResult(boolean valid, List<String> variables,
                                        String errorMessage, int errorLine, int errorColumn) {
            this.valid = valid;
            this.variables = variables != null ? Collections.unmodifiableList(variables) : Collections.emptyList();
            this.errorMessage = errorMessage;
            this.errorLine = errorLine;
            this.errorColumn = errorColumn;
        }

        static SyntaxValidationResult success(List<String> variables) {
            return new SyntaxValidationResult(true, variables, null, -1, -1);
        }

        static SyntaxValidationResult error(String message, int line, int column) {
            return new SyntaxValidationResult(false, Collections.emptyList(), message, line, column);
        }

        public boolean isValid() { return valid; }
        public List<String> getVariables() { return variables; }
        public String getErrorMessage() { return errorMessage; }
        public int getErrorLine() { return errorLine; }
        public int getErrorColumn() { return errorColumn; }
        public boolean hasPosition() { return errorLine >= 0 && errorColumn >= 0; }

        @Override
        public String toString() {
            if (valid) return "SyntaxValidationResult{valid=true, vars=" + variables + "}";
            return "SyntaxValidationResult{valid=false, error='" + errorMessage
                + "', pos=(" + errorLine + ":" + errorColumn + ")}";
        }
    }
}