import java.math.*;
import java.time.*;
import java.util.*;

public final class PredicateEvaluator {

    private PredicateEvaluator() {
    }

    public enum Truth {
        TRUE,
        FALSE,
        UNKNOWN
    }

    public static int compareValues(
            Object left,
            Object right,
            SemanticModel.Type leftType,
            SemanticModel.Type rightType) {

        Objects.requireNonNull(leftType);
        Objects.requireNonNull(rightType);

        if (left == null || right == null) {
            throw new IllegalArgumentException(
                    "compareValues does not accept NULL");
        }

        if (!ExpressionNormalizerCompatible.compatible(
                leftType,
                rightType)) {

            throw new IllegalArgumentException(
                    "incompatible comparison types: " +
                    leftType + " and " + rightType);
        }

        return compareNonNull(
                left,
                right,
                leftType,
                rightType);
    }
    public static Truth compare(
            Object left,
            Object right,
            SemanticModel.Type leftType,
            String operator,
            SemanticModel.Type rightType) {

        Objects.requireNonNull(leftType);
        Objects.requireNonNull(rightType);
        Objects.requireNonNull(operator);

        if ("is_not_null".equals(operator)) {
            return truth(left != null);
        }

        if (left == null || right == null) {
            return Truth.UNKNOWN;
        }

        if (!ExpressionNormalizerCompatible.compatible(
                leftType,
                rightType)) {

            throw new IllegalArgumentException(
                    "incompatible comparison types: " +
                    leftType + " and " + rightType);
        }

        int comparison =
                compareNonNull(
                        left,
                        right,
                        leftType,
                        rightType);

        switch (operator) {
            case "=":
                return truth(comparison == 0);

            case "!=":
                return truth(comparison != 0);

            case ">":
                return truth(comparison > 0);

            case ">=":
                return truth(comparison >= 0);

            case "<":
                return truth(comparison < 0);

            case "<=":
                return truth(comparison <= 0);

            default:
                throw new IllegalArgumentException(
                        "unsupported comparison operator: " +
                        operator);
        }
    }

    public static Truth and(
            Truth left,
            Truth right) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        if (left == Truth.FALSE ||
            right == Truth.FALSE) {

            return Truth.FALSE;
        }

        if (left == Truth.UNKNOWN ||
            right == Truth.UNKNOWN) {

            return Truth.UNKNOWN;
        }

        return Truth.TRUE;
    }

    private static Truth truth(boolean value) {
        return value
                ? Truth.TRUE
                : Truth.FALSE;
    }

    private static int compareNonNull(
            Object left,
            Object right,
            SemanticModel.Type leftType,
            SemanticModel.Type rightType) {

        switch (leftType) {
            case TEXT:
                return ((String) left)
                        .compareTo((String) right);

            case BOOLEAN:
                return ((Boolean) left)
                        .compareTo((Boolean) right);

            case DATE:
                return ((LocalDate) left)
                        .compareTo((LocalDate) right);

            case INTEGER:
                if (rightType ==
                        SemanticModel.Type.INTEGER) {

                    return ((Number) left)
                            .longValue() < ((Number) right)
                            .longValue()
                            ? -1
                            : ((Number) left)
                                .longValue() > ((Number) right)
                                .longValue()
                                ? 1
                                : 0;
                }

                return new BigDecimal(
                            left.toString())
                        .compareTo(
                            new BigDecimal(
                                right.toString()));

            case DECIMAL:
                if (rightType ==
                        SemanticModel.Type.INTEGER) {

                    return new BigDecimal(
                            left.toString())
                        .compareTo(
                            BigDecimal.valueOf(
                                ((Number) right)
                                    .longValue()));
                }

                return new BigDecimal(
                            left.toString())
                    .compareTo(
                        new BigDecimal(
                            right.toString()));

            default:
                throw new IllegalArgumentException(
                        "unsupported comparison type: " +
                        leftType);
        }
    }

    /*
     * This adapter keeps the evaluator independent of
     * ExpressionNormalizer's implementation details.
     */
    private static final class ExpressionNormalizerCompatible {

        static boolean compatible(
                SemanticModel.Type left,
                SemanticModel.Type right) {

            if (left == right) {
                return true;
            }

            return isNumeric(left) &&
                   isNumeric(right);
        }

        private static boolean isNumeric(
                SemanticModel.Type type) {

            return type ==
                    SemanticModel.Type.INTEGER ||
                   type ==
                    SemanticModel.Type.DECIMAL;
        }
    }
}
