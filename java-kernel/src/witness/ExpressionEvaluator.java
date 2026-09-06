import java.math.BigDecimal;
import java.util.*;

public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    public static Object evaluate(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema,
            ExecutionModel.Row row) {

        Objects.requireNonNull(expression);
        Objects.requireNonNull(schema);
        Objects.requireNonNull(row);

        switch (expression.kind) {
            case COLUMN:
                return evaluateColumn(
                        expression,
                        schema,
                        row);

            case LITERAL:
                return expression.literal;

            case CONDITIONAL: {
                PredicateEvaluator.Truth truth =
                        evaluateCondition(
                                expression.condition,
                                schema,
                                row);

                SemanticModel.Expression selected =
                        truth ==
                                PredicateEvaluator.Truth.TRUE
                        ? expression.thenBranch
                        : expression.elseBranch;

                return evaluate(
                        selected,
                        schema,
                        row);
            }

            case FUNCTION:
                return evaluateScalarFunction(
                        expression,
                        schema,
                        row);

            default:
                throw new IllegalArgumentException(
                        "unsupported expression kind: " +
                        expression.kind);
        }
    }

    public static PredicateEvaluator.Truth evaluateCondition(
            SemanticModel.Condition condition,
            SemanticModel.Schema schema,
            ExecutionModel.Row row) {

        Objects.requireNonNull(condition);
        Objects.requireNonNull(schema);
        Objects.requireNonNull(row);

        Object left =
                evaluate(
                        condition.left,
                        schema,
                        row);

        Object right =
                evaluate(
                        condition.right,
                        schema,
                        row);

        SemanticModel.Type leftType =
                expressionType(
                        condition.left,
                        schema);

        SemanticModel.Type rightType =
                expressionType(
                        condition.right,
                        schema);

        return PredicateEvaluator.compare(
                left,
                right,
                leftType,
                condition.operator,
                rightType);
    }

    public static PredicateEvaluator.Truth evaluateJoinCondition(
            SemanticModel.Condition condition,
            SemanticModel.Schema leftSchema,
            ExecutionModel.Row leftRow,
            SemanticModel.Schema rightSchema,
            ExecutionModel.Row rightRow) {

        Objects.requireNonNull(condition);
        Objects.requireNonNull(leftSchema);
        Objects.requireNonNull(leftRow);
        Objects.requireNonNull(rightSchema);
        Objects.requireNonNull(rightRow);

        Object left =
                evaluate(
                        condition.left,
                        leftSchema,
                        leftRow);

        Object right =
                evaluate(
                        condition.right,
                        rightSchema,
                        rightRow);

        SemanticModel.Type leftType =
                expressionType(
                        condition.left,
                        leftSchema);

        SemanticModel.Type rightType =
                expressionType(
                        condition.right,
                        rightSchema);

        return PredicateEvaluator.compare(
                left,
                right,
                leftType,
                condition.operator,
                rightType);
    }


    public static SemanticModel.Type expressionType(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema) {

        Objects.requireNonNull(expression);
        Objects.requireNonNull(schema);

        switch (expression.kind) {
            case COLUMN:
                return column(
                        schema,
                        expression.column).type;

            case LITERAL:
                return expression.literalType;

            case CONDITIONAL:
                throw new IllegalArgumentException(
                        "conditional expression runtime type requires normalized result context");

            case FUNCTION:
                return scalarFunctionType(
                        expression,
                        schema);

            default:
                throw new IllegalArgumentException(
                        "unsupported expression kind: " +
                        expression.kind);
        }
    }

    private static Object evaluateScalarFunction(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema,
            ExecutionModel.Row row) {

        if (expression.function == null ||
            expression.function.isBlank()) {

            throw new IllegalArgumentException(
                    "scalar function has empty name");
        }

        String function =
                expression.function.trim()
                                   .toLowerCase(Locale.ROOT);

        if ("max".equals(function)) {
            return evaluateScalarMax(
                    expression,
                    schema,
                    row);
        }

        if (expression.arguments.size() != 2) {
            throw new IllegalArgumentException(
                    "scalar " + function +
                    " requires exactly two arguments");
        }

        Object leftValue =
                evaluate(
                        expression.arguments.get(0),
                        schema,
                        row);

        Object rightValue =
                evaluate(
                        expression.arguments.get(1),
                        schema,
                        row);

        BigDecimal left =
                Numeric.coerce(leftValue);

        BigDecimal right =
                Numeric.coerce(rightValue);

        switch (function) {
            case "add":
                return Numeric.add(
                        left,
                        right);

            case "subtract":
                return Numeric.subtract(
                        left,
                        right);

            case "multiply":
                return Numeric.multiply(
                        left,
                        right);

            case "divide":
                return Numeric.divide(
                        left,
                        right);

            default:
                throw new IllegalArgumentException(
                        "unsupported scalar function: " +
                        expression.function);
        }
    }

    private static SemanticModel.Type scalarFunctionType(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema) {

        if (expression.function == null ||
            expression.function.isBlank()) {

            throw new IllegalArgumentException(
                    "scalar function has empty name");
        }

        String function =
                expression.function.trim()
                                   .toLowerCase(Locale.ROOT);

        if ("max".equals(function)) {
            return scalarMaxType(
                    expression,
                    schema);
        }

        switch (function) {
            case "add":
            case "subtract":
            case "multiply":
            case "divide":
                if (expression.arguments.size() != 2) {
                    throw new IllegalArgumentException(
                            "scalar " + function +
                            " requires exactly two arguments");
                }

                SemanticModel.Type left =
                        expressionType(
                                expression.arguments.get(0),
                                schema);

                SemanticModel.Type right =
                        expressionType(
                                expression.arguments.get(1),
                                schema);

                if (!isNumeric(left) ||
                    !isNumeric(right)) {

                    throw new IllegalArgumentException(
                            "scalar " + function +
                            " requires numeric arguments");
                }

                return SemanticModel.Type.DECIMAL;

            default:
                throw new IllegalArgumentException(
                        "unsupported scalar function: " +
                        expression.function);
        }
    }

    private static Object evaluateScalarMax(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema,
            ExecutionModel.Row row) {

        SemanticModel.Type resultType =
                scalarMaxType(expression, schema);

        Object left = evaluate(expression.arguments.get(0), schema, row);
        Object right = evaluate(expression.arguments.get(1), schema, row);

        if (left == null) {
            return right == null
                    ? null
                    : scalarMaxResult(right, resultType);
        }

        if (right == null) {
            return scalarMaxResult(left, resultType);
        }

        int comparison = PredicateEvaluator.compareValues(
                left,
                right,
                expressionType(expression.arguments.get(0), schema),
                expressionType(expression.arguments.get(1), schema));

        return scalarMaxResult(
                comparison >= 0 ? left : right,
                resultType);
    }

    private static SemanticModel.Type scalarMaxType(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema) {

        if (expression.function == null ||
            !"max".equalsIgnoreCase(expression.function)) {

            throw new IllegalArgumentException(
                    "unsupported scalar function: " + expression.function);
        }

        if (expression.arguments.size() != 2) {
            throw new IllegalArgumentException(
                    "scalar max requires exactly two arguments");
        }

        SemanticModel.Type left =
                expressionType(expression.arguments.get(0), schema);

        SemanticModel.Type right =
                expressionType(expression.arguments.get(1), schema);

        if (!isNumeric(left) || !isNumeric(right)) {
            throw new IllegalArgumentException(
                    "scalar max requires numeric arguments");
        }

        return left == SemanticModel.Type.DECIMAL ||
               right == SemanticModel.Type.DECIMAL
                ? SemanticModel.Type.DECIMAL
                : SemanticModel.Type.INTEGER;
    }

    private static Object scalarMaxResult(
            Object value,
            SemanticModel.Type type) {

        if (type == SemanticModel.Type.DECIMAL &&
            !(value instanceof BigDecimal)) {

            return new BigDecimal(value.toString());
        }

        return value;
    }

    private static boolean isNumeric(SemanticModel.Type type) {
        return type == SemanticModel.Type.INTEGER ||
               type == SemanticModel.Type.DECIMAL;
    }

    private static Object evaluateColumn(
            SemanticModel.Expression expression,
            SemanticModel.Schema schema,
            ExecutionModel.Row row) {

        int index =
                columnIndex(
                        schema,
                        expression.column);

        return row.get(index);
    }

    private static int columnIndex(
            SemanticModel.Schema schema,
            String name) {

        for (int i = 0;
             i < schema.columns.size();
             i++) {

            if (schema.columns
                    .get(i)
                    .name
                    .equals(name)) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "expression references unavailable column: " +
                name);
    }

    private static SemanticModel.Column column(
            SemanticModel.Schema schema,
            String name) {

        for (SemanticModel.Column column :
                schema.columns) {

            if (column.name.equals(name)) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                "expression references unavailable column: " +
                name);
    }
}
