import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Exact decimal numeric semantics for the capsule engine.
 *
 * Design goals:
 *
 * - Never use binary floating point for semantic NUMBER values.
 * - Use BigDecimal as the canonical runtime representation.
 * - Support Oracle-inspired precision and scale constraints.
 * - Preserve exact addition, subtraction, and multiplication.
 * - Give division a deterministic 38-digit working precision.
 * - Propagate null operands.
 * - Treat divide-by-zero as an evaluation error.
 *
 * This is an engine semantic layer, not an implementation of
 * Oracle Database's internal NUMBER storage format.
 */
public final class Numeric {

    /**
     * Oracle NUMBER supports up to 38 digits of decimal precision.
     */
    public static final int MAX_PRECISION = 38;

    /**
     * Oracle-compatible declared scale bounds.
     */
    public static final int MIN_SCALE = -84;
    public static final int MAX_SCALE = 127;

    /**
     * Used where an operation inherently requires rounding,
     * particularly non-terminating division.
     *
     * Addition/subtraction/multiplication remain exact and do not
     * automatically apply this context.
     */
    public static final MathContext DIVISION_CONTEXT =
            new MathContext(
                    MAX_PRECISION,
                    RoundingMode.HALF_UP);

    private Numeric() {
    }

    /**
     * Parse an exact decimal from text.
     */
    public static BigDecimal number(String text) {

        Objects.requireNonNull(text, "text");

        return new BigDecimal(text);
    }

    /**
     * Convert supported runtime numeric objects into BigDecimal.
     *
     * Float and Double are deliberately rejected. Their values have
     * already passed through binary floating-point representation,
     * which is not appropriate for exact semantic NUMBER values.
     */
    public static BigDecimal coerce(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Byte ||
            value instanceof Short ||
            value instanceof Integer ||
            value instanceof Long) {

            return new BigDecimal(
                    value.toString());
        }

        if (value instanceof Float ||
            value instanceof Double) {

            throw new IllegalArgumentException(
                    "binary floating-point value is not allowed for NUMBER: " +
                    value);
        }

        if (value instanceof String) {
            return number((String) value);
        }

        throw new IllegalArgumentException(
                "cannot coerce value to NUMBER: " +
                value.getClass().getName());
    }

    /**
     * Apply a declared NUMBER(p,s) constraint.
     *
     * precision:
     *   total significant decimal digits
     *
     * scale:
     *   digits to the right of the decimal point.
     *   Negative scale rounds to positions left of the decimal point.
     *
     * Rounding is HALF_UP when scale enforcement requires it.
     */
    public static BigDecimal enforce(
            BigDecimal value,
            Integer precision,
            Integer scale) {

        if (value == null) {
            return null;
        }

        validateDeclaration(
                precision,
                scale);

        BigDecimal result = value;

        if (scale != null) {
            result =
                    result.setScale(
                            scale,
                            RoundingMode.HALF_UP);
        }

        if (precision != null &&
            result.precision() > precision) {

            throw new ArithmeticException(
                    "NUMBER precision overflow: value=" +
                    result.toPlainString() +
                    " precision=" +
                    result.precision() +
                    " declared=" +
                    precision +
                    (scale == null
                            ? ""
                            : " scale=" + scale));
        }

        return result;
    }

    /**
     * Validate NUMBER(p,s) declaration metadata.
     */
    public static void validateDeclaration(
            Integer precision,
            Integer scale) {

        if (precision != null) {

            if (precision < 1 ||
                precision > MAX_PRECISION) {

                throw new IllegalArgumentException(
                        "NUMBER precision must be between 1 and " +
                        MAX_PRECISION +
                        ": " +
                        precision);
            }
        }

        if (scale != null) {

            if (scale < MIN_SCALE ||
                scale > MAX_SCALE) {

                throw new IllegalArgumentException(
                        "NUMBER scale must be between " +
                        MIN_SCALE +
                        " and " +
                        MAX_SCALE +
                        ": " +
                        scale);
            }
        }

        if (precision == null &&
            scale != null) {

            throw new IllegalArgumentException(
                    "NUMBER scale requires precision");
        }
    }

    /**
     * SQL-style null propagation.
     */
    public static BigDecimal add(
            BigDecimal left,
            BigDecimal right) {

        if (left == null || right == null) {
            return null;
        }

        return left.add(right);
    }

    /**
     * SQL-style null propagation.
     */
    public static BigDecimal subtract(
            BigDecimal left,
            BigDecimal right) {

        if (left == null || right == null) {
            return null;
        }

        return left.subtract(right);
    }

    /**
     * SQL-style null propagation.
     *
     * Multiplication remains exact.
     */
    public static BigDecimal multiply(
            BigDecimal left,
            BigDecimal right) {

        if (left == null || right == null) {
            return null;
        }

        return left.multiply(right);
    }

    /**
     * SQL-style null propagation.
     *
     * Division uses the engine's deterministic 38-digit decimal
     * working precision for non-terminating results.
     */
    public static BigDecimal divide(
            BigDecimal left,
            BigDecimal right) {

        if (left == null || right == null) {
            return null;
        }

        if (right.compareTo(BigDecimal.ZERO) == 0) {

            throw new ArithmeticException(
                    "NUMBER division by zero");
        }

        return left.divide(
                right,
                DIVISION_CONTEXT);
    }
}
