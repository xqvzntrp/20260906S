import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class ExpressionNormalizer {

    private ExpressionNormalizer() {
    }

    public static final class TypedExpression {
        public final SemanticModel.Expression expression;
        public final SemanticModel.Type type;
        public final boolean nullable;

        public TypedExpression(
                SemanticModel.Expression expression,
                SemanticModel.Type type,
                boolean nullable) {

            this.expression =
                    Objects.requireNonNull(expression);

            this.type =
                    Objects.requireNonNull(type);

            this.nullable = nullable;
        }
    }

    public static TypedExpression column(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw) {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(raw);

        if (raw.kind !=
                RawModel.RawExpression.Kind.COLUMN) {

            throw new IllegalArgumentException(
                    "expected column expression");
        }

        if (raw.column == null ||
            raw.column.isBlank()) {

            throw new IllegalArgumentException(
                    "column expression has empty name");
        }

        SemanticModel.Column column =
                findColumn(
                        schema,
                        raw.column);

        return new TypedExpression(
                SemanticModel.Expression.column(
                        column.name),
                column.type,
                column.nullable);
    }

    public static TypedExpression literal(
            RawModel.RawExpression raw,
            SemanticModel.Type expectedType) {

        Objects.requireNonNull(raw);
        Objects.requireNonNull(expectedType);

        if (raw.kind !=
                RawModel.RawExpression.Kind.LITERAL) {

            throw new IllegalArgumentException(
                    "expected literal expression");
        }

        Object value =
                normalizeLiteralValue(
                        raw.value,
                        expectedType);

        return new TypedExpression(
                SemanticModel.Expression.literal(
                        value,
                        expectedType),
                expectedType,
                raw.value == null);
    }

    public static TypedExpression normalizeAgainst(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw,
            SemanticModel.Type expectedType) {

        Objects.requireNonNull(raw);

        switch (raw.kind) {
            case COLUMN: {
                TypedExpression result =
                        column(schema, raw);

                if (expectedType != null &&
                    !compatible(
                            result.type,
                            expectedType)) {

                    throw new IllegalArgumentException(
                            "column " + raw.column +
                            " has type " + result.type +
                            " but " + expectedType +
                            " was required");
                }

                return result;
            }

            case LITERAL:
                if (expectedType == null) {
                    throw new IllegalArgumentException(
                            "literal expression requires contextual type");
                }

                return literal(
                        raw,
                        expectedType);

            case CONDITIONAL: {
                TypedExpression result =
                        normalizeConditional(
                                schema,
                                raw);

                if (expectedType != null &&
                    !compatible(
                            result.type,
                            expectedType)) {

                    throw new IllegalArgumentException(
                            "conditional expression has type " +
                            result.type +
                            " but " + expectedType +
                            " was required");
                }

                return result;
            }

            case FUNCTION:
                return normalizeScalarFunction(
                        schema,
                        raw,
                        expectedType);

            default:
                throw new IllegalArgumentException(
                        "unsupported expression kind: " +
                        raw.kind);
        }
    }

    private static TypedExpression normalizeScalarFunction(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw,
            SemanticModel.Type expectedType) {

        if (raw.function == null ||
            raw.function.isBlank()) {

            throw new IllegalArgumentException(
                    "scalar function has empty name");
        }

        String function =
                raw.function.trim()
                            .toLowerCase(Locale.ROOT);

        if ("max".equals(function)) {
            return normalizeScalarMax(
                    schema,
                    raw,
                    expectedType);
        }

        switch (function) {
            case "add":
            case "subtract":
            case "multiply":
            case "divide":
                return normalizeArithmetic(
                        schema,
                        raw,
                        expectedType,
                        function);

            default:
                throw new IllegalArgumentException(
                        "unsupported scalar function: " +
                        raw.function);
        }
    }

    private static TypedExpression normalizeArithmetic(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw,
            SemanticModel.Type expectedType,
            String function) {

        if (raw.arguments.size() != 2) {
            throw new IllegalArgumentException(
                    "scalar " + function +
                    " requires exactly two arguments");
        }

        /*
         * Arithmetic is normalized through DECIMAL even when both
         * operands originate as INTEGER. This ensures exact
         * BigDecimal semantics and makes division deterministic.
         */
        TypedExpression left =
                normalizeAgainst(
                        schema,
                        raw.arguments.get(0),
                        SemanticModel.Type.DECIMAL);

        TypedExpression right =
                normalizeAgainst(
                        schema,
                        raw.arguments.get(1),
                        SemanticModel.Type.DECIMAL);

        if (!isNumeric(left.type) ||
            !isNumeric(right.type)) {

            throw new IllegalArgumentException(
                    "scalar " + function +
                    " requires numeric arguments");
        }

        SemanticModel.Type resultType =
                SemanticModel.Type.DECIMAL;

        if (expectedType != null &&
            !compatible(
                    resultType,
                    expectedType)) {

            throw new IllegalArgumentException(
                    "scalar " + function +
                    " has type " +
                    resultType +
                    " but " +
                    expectedType +
                    " was required");
        }

        return new TypedExpression(
                SemanticModel.Expression.function(
                        function,
                        List.of(
                                left.expression,
                                right.expression)),
                resultType,
                left.nullable || right.nullable);
    }

    private static TypedExpression normalizeScalarMax(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw,
            SemanticModel.Type expectedType) {

        if (raw.function == null ||
            !"max".equalsIgnoreCase(raw.function)) {

            throw new IllegalArgumentException(
                    "unsupported scalar function: " + raw.function);
        }

        if (raw.arguments.size() != 2) {
            throw new IllegalArgumentException(
                    "scalar max requires exactly two arguments");
        }

        RawModel.RawExpression leftRaw = raw.arguments.get(0);
        RawModel.RawExpression rightRaw = raw.arguments.get(1);

        TypedExpression left;
        TypedExpression right;

        if (leftRaw.kind == RawModel.RawExpression.Kind.LITERAL &&
            rightRaw.kind == RawModel.RawExpression.Kind.LITERAL) {

            SemanticModel.Type type;

            if (leftRaw.value == null && rightRaw.value == null) {
                if (expectedType == null) {
                    throw new IllegalArgumentException(
                            "scalar max NULL arguments require contextual type");
                }

                type = expectedType;

            } else if (leftRaw.value == null) {
                type = inferLiteralType(rightRaw.value);

            } else if (rightRaw.value == null) {
                type = inferLiteralType(leftRaw.value);

            } else {
                type = commonLiteralType(leftRaw, rightRaw);
            }

            left = literal(leftRaw, type);
            right = literal(rightRaw, type);

        } else if (leftRaw.kind == RawModel.RawExpression.Kind.LITERAL) {
            right = normalizeAgainst(schema, rightRaw, null);
            left = literal(leftRaw, right.type);

        } else if (rightRaw.kind == RawModel.RawExpression.Kind.LITERAL) {
            left = normalizeAgainst(schema, leftRaw, null);
            right = literal(rightRaw, left.type);

        } else {
            left = normalizeAgainst(schema, leftRaw, null);
            right = normalizeAgainst(schema, rightRaw, null);
        }

        if (!isNumeric(left.type) || !isNumeric(right.type)) {
            throw new IllegalArgumentException(
                    "scalar max requires numeric arguments");
        }

        SemanticModel.Type type = commonType(left.type, right.type);

        if (expectedType != null && !compatible(type, expectedType)) {
            throw new IllegalArgumentException(
                    "scalar max has type " + type +
                    " but " + expectedType + " was required");
        }

        return new TypedExpression(
                SemanticModel.Expression.function(
                        "max",
                        List.of(left.expression, right.expression)),
                type,
                left.nullable && right.nullable);
    }

    private static SemanticModel.Type inferLiteralType(
            Object value) {

        if (value instanceof String)
            return SemanticModel.Type.TEXT;

        if (value instanceof Boolean)
            return SemanticModel.Type.BOOLEAN;

        if (value instanceof Byte ||
            value instanceof Short ||
            value instanceof Integer ||
            value instanceof Long) {

            return SemanticModel.Type.INTEGER;
        }

        if (value instanceof Number)
            return SemanticModel.Type.DECIMAL;

        if (value == null) {
            throw new IllegalArgumentException(
                    "NULL literal requires contextual type");
        }

        throw new IllegalArgumentException(
                "unsupported literal value type: " +
                value.getClass().getName());
    }

    private static SemanticModel.Type commonLiteralType(
            RawModel.RawExpression left,
            RawModel.RawExpression right) {

        SemanticModel.Type leftType =
                inferLiteralType(left.value);

        SemanticModel.Type rightType =
                inferLiteralType(right.value);

        return commonType(
                leftType,
                rightType);
    }
    public static TypedExpression normalizeConditional(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw) {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(raw);

        if (raw.kind !=
                RawModel.RawExpression.Kind.CONDITIONAL) {

            throw new IllegalArgumentException(
                    "expected conditional expression");
        }

        if (raw.condition == null ||
            raw.thenBranch == null ||
            raw.elseBranch == null) {

            throw new IllegalArgumentException(
                    "conditional expression requires condition, then, and else");
        }

        SemanticModel.Condition condition =
                normalizeCondition(
                        schema,
                        raw.condition);

        boolean thenLiteral =
                raw.thenBranch.kind ==
                        RawModel.RawExpression.Kind.LITERAL;

        boolean elseLiteral =
                raw.elseBranch.kind ==
                        RawModel.RawExpression.Kind.LITERAL;

        TypedExpression thenExpression;
        TypedExpression elseExpression;

        if (thenLiteral && elseLiteral) {

            SemanticModel.Type type =
                    commonLiteralType(
                            raw.thenBranch,
                            raw.elseBranch);

            thenExpression =
                    literal(
                            raw.thenBranch,
                            type);

            elseExpression =
                    literal(
                            raw.elseBranch,
                            type);

        } else if (thenLiteral) {

            elseExpression =
                    normalizeConditionalBranch(
                            schema,
                            raw.elseBranch);

            thenExpression =
                    literal(
                            raw.thenBranch,
                            elseExpression.type);

        } else if (elseLiteral) {

            thenExpression =
                    normalizeConditionalBranch(
                            schema,
                            raw.thenBranch);

            elseExpression =
                    literal(
                            raw.elseBranch,
                            thenExpression.type);

        } else {

            thenExpression =
                    normalizeConditionalBranch(
                            schema,
                            raw.thenBranch);

            elseExpression =
                    normalizeConditionalBranch(
                            schema,
                            raw.elseBranch);
        }

        SemanticModel.Type resultType =
                commonType(
                        thenExpression.type,
                        elseExpression.type);

        boolean nullable =
                thenExpression.nullable ||
                elseExpression.nullable;

        return new TypedExpression(
                SemanticModel.Expression.conditional(
                        condition,
                        thenExpression.expression,
                        elseExpression.expression),
                resultType,
                nullable);
    }
    private static TypedExpression normalizeConditionalBranch(
            SemanticModel.Schema schema,
            RawModel.RawExpression raw) {

        if (raw.kind ==
                RawModel.RawExpression.Kind.COLUMN) {

            return column(schema, raw);
        }

        if (raw.kind ==
                RawModel.RawExpression.Kind.LITERAL) {

            throw new IllegalArgumentException(
                    "conditional literal branch requires contextual result type");
        }

        return normalizeAgainst(
                schema,
                raw,
                null);
    }

    private static void validateBooleanCondition(
            SemanticModel.Condition condition) {

        /*
         * Comparison predicates are boolean-valued.
         * The current condition representation therefore
         * establishes the required boolean result implicitly.
         */
        if (condition.left == null ||
            condition.right == null) {

            throw new IllegalArgumentException(
                    "conditional condition is incomplete");
        }
    }

    public static SemanticModel.Type commonType(
            SemanticModel.Type left,
            SemanticModel.Type right) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        if (left == right)
            return left;

        if (isNumeric(left) &&
            isNumeric(right)) {

            if (left == SemanticModel.Type.DECIMAL ||
                right == SemanticModel.Type.DECIMAL) {

                return SemanticModel.Type.DECIMAL;
            }

            return SemanticModel.Type.INTEGER;
        }

        throw new IllegalArgumentException(
                "incompatible conditional branch types: " +
                left + " and " + right);
    }

    public static SemanticModel.Condition normalizeCondition(
            SemanticModel.Schema schema,
            RawModel.Condition raw) {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(raw);

        if (raw.operator == null ||
            raw.operator.isBlank()) {

            throw new IllegalArgumentException(
                    "condition has empty operator");
        }

        TypedExpression left =
                normalizeAgainst(
                        schema,
                        raw.left,
                        null);

        if ("is_not_null".equals(raw.operator) &&
            (raw.right.kind !=
                    RawModel.RawExpression.Kind.LITERAL ||
             raw.right.value != null)) {

            throw new IllegalArgumentException(
                    "is_not_null requires NULL right operand");
        }

        TypedExpression right =
                normalizeAgainst(
                        schema,
                        raw.right,
                        left.type);

        validateComparison(
                raw.operator,
                left.type,
                right.type);

        return new SemanticModel.Condition(
                left.expression,
                raw.operator,
                right.expression);
    }

    public static void validateComparison(
            String operator,
            SemanticModel.Type left,
            SemanticModel.Type right) {

        Objects.requireNonNull(operator);
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        switch (operator) {
            case "=":
            case "!=":
            case ">":
            case ">=":
            case "<":
            case "<=":
            case "is_not_null":
                break;

            default:
                throw new IllegalArgumentException(
                        "unsupported comparison operator: " +
                        operator);
        }

        if (!compatible(left, right)) {
            throw new IllegalArgumentException(
                    "compares incompatible types: " +
                    left + " " + operator + " " + right);
        }
    }
    public static boolean compatible(
            SemanticModel.Type left,
            SemanticModel.Type right) {

        if (left == right)
            return true;

        return isNumeric(left) &&
               isNumeric(right);
    }

    private static boolean isNumeric(
            SemanticModel.Type type) {

        return type == SemanticModel.Type.INTEGER ||
               type == SemanticModel.Type.DECIMAL;
    }

    private static SemanticModel.Column findColumn(
            SemanticModel.Schema schema,
            String name) {

        SemanticModel.Column found = null;

        for (SemanticModel.Column column :
                schema.columns) {

            if (!column.name.equals(name))
                continue;

            if (found != null) {
                throw new IllegalArgumentException(
                        "ambiguous column: " + name);
            }

            found = column;
        }

        if (found == null) {
            throw new IllegalArgumentException(
                    "unknown column: " + name);
        }

        return found;
    }

    private static Object normalizeLiteralValue(
            Object raw,
            SemanticModel.Type type) {

        if (raw == null)
            return null;

        String text =
                String.valueOf(raw);

        try {
            switch (type) {
                case TEXT:
                    if (!(raw instanceof String)) {
                        throw new IllegalArgumentException(
                                "TEXT literal must be textual");
                    }

                    return raw;

                case INTEGER:
                    if (raw instanceof Byte ||
                        raw instanceof Short ||
                        raw instanceof Integer ||
                        raw instanceof Long) {

                        return Long.valueOf(
                                ((Number) raw)
                                        .longValue());
                    }

                    throw new IllegalArgumentException(
                            "INTEGER literal must be integral");

                case DECIMAL:
                    if (raw instanceof Number) {
                        return new BigDecimal(text);
                    }

                    throw new IllegalArgumentException(
                            "DECIMAL literal must be numeric");

                case BOOLEAN:
                    if (raw instanceof Boolean)
                        return raw;

                    throw new IllegalArgumentException(
                            "BOOLEAN literal must be boolean");

                case DATE:
                    if (!(raw instanceof String)) {
                        throw new IllegalArgumentException(
                                "DATE literal must be textual");
                    }

                    return LocalDate.parse(text);

                default:
                    throw new IllegalArgumentException(
                            "unsupported literal type: " +
                            type);
            }

        } catch (NumberFormatException |
                 DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "literal value cannot be represented as " +
                    type + ": " + text);
        }
    }
}
