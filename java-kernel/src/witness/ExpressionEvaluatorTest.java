import java.math.*;
import java.util.*;

public final class ExpressionEvaluatorTest {

    public static void main(String[] args) {
        testColumnEvaluation();
        testLiteralEvaluation();
        testConditionTrue();
        testConditionFalse();
        testConditionUnknown();
        testNumericCondition();
        testConditionalTrueBranch();
        testConditionalFalseBranch();
        testConditionalUnknownUsesElseBranch();
        testScalarMaxEvaluation();
        testUnknownColumnRejected();

        System.out.println(
                "ExpressionEvaluatorTest PASSED");
    }

    private static SemanticModel.Schema schema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "category",
                                SemanticModel.Type.TEXT,
                                false),
                        new SemanticModel.Column(
                                "amount",
                                SemanticModel.Type.DECIMAL,
                                true),
                        new SemanticModel.Column(
                                "count",
                                SemanticModel.Type.INTEGER,
                                false)));
    }

    private static void testColumnEvaluation() {
        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                new BigDecimal("12.50"),
                                2L));

        Object value =
                ExpressionEvaluator.evaluate(
                        SemanticModel.Expression.column(
                                "category"),
                        schema(),
                        row);

        require(
                value.equals("A"),
                "column evaluation");
    }

    private static void testLiteralEvaluation() {
        Object value =
                ExpressionEvaluator.evaluate(
                        SemanticModel.Expression.literal(
                                100L,
                                SemanticModel.Type.INTEGER),
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        null,
                                        2L)));

        require(
                value.equals(100L),
                "literal evaluation");
    }

    private static void testConditionTrue() {
        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "category"),
                        "=",
                        SemanticModel.Expression.literal(
                                "A",
                                SemanticModel.Type.TEXT));

        PredicateEvaluator.Truth result =
                ExpressionEvaluator.evaluateCondition(
                        condition,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        null,
                                        2L)));

        require(
                result ==
                        PredicateEvaluator.Truth.TRUE,
                "condition TRUE");
    }

    private static void testConditionFalse() {
        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "category"),
                        "=",
                        SemanticModel.Expression.literal(
                                "A",
                                SemanticModel.Type.TEXT));

        PredicateEvaluator.Truth result =
                ExpressionEvaluator.evaluateCondition(
                        condition,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "B",
                                        null,
                                        2L)));

        require(
                result ==
                        PredicateEvaluator.Truth.FALSE,
                "condition FALSE");
    }

    private static void testConditionUnknown() {
        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "amount"),
                        ">=",
                        SemanticModel.Expression.literal(
                                new BigDecimal("10"),
                                SemanticModel.Type.DECIMAL));

        PredicateEvaluator.Truth result =
                ExpressionEvaluator.evaluateCondition(
                        condition,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        null,
                                        2L)));

        require(
                result ==
                        PredicateEvaluator.Truth.UNKNOWN,
                "condition UNKNOWN");
    }

    private static void testNumericCondition() {
        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "count"),
                        "=",
                        SemanticModel.Expression.literal(
                                new BigDecimal("2.0"),
                                SemanticModel.Type.DECIMAL));

        PredicateEvaluator.Truth result =
                ExpressionEvaluator.evaluateCondition(
                        condition,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        null,
                                        2L)));

        require(
                result ==
                        PredicateEvaluator.Truth.TRUE,
                "numeric condition");
    }

    private static SemanticModel.Expression conditionalExpression() {
        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "amount"),
                        ">=",
                        SemanticModel.Expression.literal(
                                new BigDecimal("10"),
                                SemanticModel.Type.DECIMAL));

        return SemanticModel.Expression.conditional(
                condition,
                SemanticModel.Expression.literal(
                        "HIGH",
                        SemanticModel.Type.TEXT),
                SemanticModel.Expression.literal(
                        "LOW",
                        SemanticModel.Type.TEXT));
    }

    private static void testConditionalTrueBranch() {
        Object value =
                ExpressionEvaluator.evaluate(
                        conditionalExpression(),
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        new BigDecimal("20"),
                                        2L)));

        require(
                value.equals("HIGH"),
                "conditional TRUE branch");
    }

    private static void testScalarMaxEvaluation() {
        SemanticModel.Expression integerMax =
                SemanticModel.Expression.function(
                        "max",
                        List.of(
                                SemanticModel.Expression.literal(
                                        2L,
                                        SemanticModel.Type.INTEGER),
                                SemanticModel.Expression.literal(
                                        5L,
                                        SemanticModel.Type.INTEGER)));

        require(
                ExpressionEvaluator.evaluate(
                        integerMax,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList("A", null, 2L)))
                .equals(5L),
                "scalar max INTEGER");

        SemanticModel.Expression decimalMax =
                SemanticModel.Expression.function(
                        "MAX",
                        List.of(
                                SemanticModel.Expression.literal(
                                        2L,
                                        SemanticModel.Type.INTEGER),
                                SemanticModel.Expression.literal(
                                        new BigDecimal("1.5"),
                                        SemanticModel.Type.DECIMAL)));

        require(
                ExpressionEvaluator.expressionType(
                        decimalMax,
                        schema()) == SemanticModel.Type.DECIMAL,
                "scalar max DECIMAL result type");

        require(
                ExpressionEvaluator.evaluate(
                        decimalMax,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList("A", null, 2L)))
                .equals(new BigDecimal("2")),
                "scalar max widens INTEGER result to DECIMAL");

        SemanticModel.Expression oneNull =
                SemanticModel.Expression.function(
                        "max",
                        List.of(
                                SemanticModel.Expression.literal(
                                        null,
                                        SemanticModel.Type.DECIMAL),
                                SemanticModel.Expression.literal(
                                        new BigDecimal("3.5"),
                                        SemanticModel.Type.DECIMAL)));

        require(
                ExpressionEvaluator.evaluate(
                        oneNull,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList("A", null, 2L)))
                .equals(new BigDecimal("3.5")),
                "scalar max one NULL");

        SemanticModel.Expression bothNull =
                SemanticModel.Expression.function(
                        "max",
                        List.of(
                                SemanticModel.Expression.literal(
                                        null,
                                        SemanticModel.Type.INTEGER),
                                SemanticModel.Expression.literal(
                                        null,
                                        SemanticModel.Type.INTEGER)));

        require(
                ExpressionEvaluator.evaluate(
                        bothNull,
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList("A", null, 2L))) == null,
                "scalar max both NULL");
    }

    private static void testConditionalFalseBranch() {
        Object value =
                ExpressionEvaluator.evaluate(
                        conditionalExpression(),
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        new BigDecimal("5"),
                                        2L)));

        require(
                value.equals("LOW"),
                "conditional FALSE branch");
    }

    private static void testConditionalUnknownUsesElseBranch() {
        Object value =
                ExpressionEvaluator.evaluate(
                        conditionalExpression(),
                        schema(),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        null,
                                        2L)));

        require(
                value.equals("LOW"),
                "conditional UNKNOWN uses ELSE branch");
    }
    private static void testUnknownColumnRejected() {
        try {
            ExpressionEvaluator.evaluate(
                    SemanticModel.Expression.column(
                            "missing"),
                    schema(),
                    new ExecutionModel.Row(
                            Arrays.asList(
                                    "A",
                                    null,
                                    2L)));

            throw new AssertionError(
                    "Expected unavailable column failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable column"),
                    "unknown column message");
        }
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
