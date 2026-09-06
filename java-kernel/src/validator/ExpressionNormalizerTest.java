import java.math.BigDecimal;
import java.util.*;

public final class ExpressionNormalizerTest {

    private ExpressionNormalizerTest() {
    }

    public static void main(String[] args) {
        testColumnType();
        testUnknownColumn();
        testDecimalLiteralFromNumber();
        testTextLiteralRemainsText();
        testTextNotAcceptedAsDecimal();
        testLiteralRequiresContext();
        testConditionNormalization();
        testScalarMaxNormalization();
        testScalarMaxRejections();
        testIsNotNullConditionNormalization();
        testIsNotNullRequiresNullRightOperand();
        testConditionUnsupportedOperator();
        testConditionalInteger();
        testConditionalNumericWidening();
        testConditionalTypeMismatch();
        testConditionalTextLiterals();
        testConditionalIntegerLiterals();
        testConditionalNumericLiteralWidening();
        testConditionalLiteralMismatch();

        System.out.println(
                "ExpressionNormalizerTest PASSED");
    }

    private static SemanticModel.Schema schema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "order_id",
                                SemanticModel.Type.TEXT,
                                false),
                        new SemanticModel.Column(
                                "amount",
                                SemanticModel.Type.DECIMAL,
                                true)));
    }

    private static void testColumnType() {
        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.column(
                        schema(),
                        RawModel.RawExpression.column(
                                "amount"));

        require(
                result.type ==
                        SemanticModel.Type.DECIMAL,
                "column declared type");

        require(
                result.nullable,
                "column nullability");

        require(
                result.expression.kind ==
                        SemanticModel.Expression.Kind.COLUMN,
                "column expression kind");
    }

    private static void testUnknownColumn() {
        expectFailure(
                () -> ExpressionNormalizer.column(
                        schema(),
                        RawModel.RawExpression.column(
                                "missing")),
                "unknown column");
    }

    private static void testDecimalLiteralFromNumber() {
        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.literal(
                        RawModel.RawExpression.literal(100),
                        SemanticModel.Type.DECIMAL);

        require(
                result.type ==
                        SemanticModel.Type.DECIMAL,
                "decimal literal type");

        require(
                result.expression.literal
                        instanceof BigDecimal,
                "decimal representation");

        require(
                result.expression.literalType ==
                        SemanticModel.Type.DECIMAL,
                "semantic literal type");
    }

    private static void testTextLiteralRemainsText() {
        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.literal(
                        RawModel.RawExpression.literal(
                                "100"),
                        SemanticModel.Type.TEXT);

        require(
                result.type ==
                        SemanticModel.Type.TEXT,
                "text literal type");

        require(
                result.expression.literal.equals(
                        "100"),
                "text literal value");
    }

    private static void testTextNotAcceptedAsDecimal() {
        expectFailure(
                () -> ExpressionNormalizer.literal(
                        RawModel.RawExpression.literal(
                                "100"),
                        SemanticModel.Type.DECIMAL),
                "must be numeric");
    }

    private static void testLiteralRequiresContext() {
        expectFailure(
                () -> ExpressionNormalizer.normalizeAgainst(
                        schema(),
                        RawModel.RawExpression.literal(100),
                        null),
                "contextual type");
    }

    private static void testConditionNormalization() {
        RawModel.Condition raw =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "amount"),
                        ">=",
                        RawModel.RawExpression.literal(
                                100));

        SemanticModel.Condition result =
                ExpressionNormalizer.normalizeCondition(
                        schema(),
                        raw);

        require(
                result.left.kind ==
                        SemanticModel.Expression.Kind.COLUMN,
                "condition left kind");

        require(
                result.right.kind ==
                        SemanticModel.Expression.Kind.LITERAL,
                "condition right kind");

        require(
                result.right.literalType ==
                        SemanticModel.Type.DECIMAL,
                "condition contextual literal type");
    }

    private static void testConditionUnsupportedOperator() {
        RawModel.Condition raw =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "amount"),
                        "LIKE",
                        RawModel.RawExpression.literal(
                                100));

        expectFailure(
                () -> ExpressionNormalizer.normalizeCondition(
                        schema(),
                        raw),
                "unsupported comparison operator");
    }

    private static void testScalarMaxNormalization() {
        RawModel.RawExpression raw = scalarFunction(
                "MAX",
                List.of(
                        RawModel.RawExpression.literal(2),
                        RawModel.RawExpression.column("amount")));

        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.normalizeAgainst(
                        schema(), raw, null);

        require(
                result.expression.kind ==
                        SemanticModel.Expression.Kind.FUNCTION,
                "scalar max expression kind");

        require(
                result.expression.function.equals("max"),
                "scalar max canonical function name");

        require(
                result.expression.arguments.size() == 2,
                "scalar max argument count");

        require(
                result.type == SemanticModel.Type.DECIMAL,
                "scalar max widens INTEGER and DECIMAL");
    }

    private static void testScalarMaxRejections() {
        expectFailure(
                () -> ExpressionNormalizer.normalizeAgainst(
                        schema(),
                        scalarFunction(
                                "min",
                                List.of(
                                        RawModel.RawExpression.column("count"),
                                        RawModel.RawExpression.column("amount"))),
                        null),
                "unsupported scalar function");

        expectFailure(
                () -> ExpressionNormalizer.normalizeAgainst(
                        schema(),
                        scalarFunction(
                                "max",
                                List.of(
                                        RawModel.RawExpression.column("amount"))),
                        null),
                "requires exactly two arguments");

        expectFailure(
                () -> ExpressionNormalizer.normalizeAgainst(
                        schema(),
                        scalarFunction(
                                "max",
                                List.of(
                                        RawModel.RawExpression.column("order_id"),
                                        RawModel.RawExpression.column("order_id"))),
                        null),
                "requires numeric arguments");
    }

    private static RawModel.RawExpression scalarFunction(
            String function,
            List<RawModel.RawExpression> arguments) {

        return new RawModel.RawExpression(
                RawModel.RawExpression.Kind.FUNCTION,
                null,
                null,
                function,
                arguments,
                null,
                null,
                null);
    }

    private static void testIsNotNullConditionNormalization() {
        RawModel.Condition raw =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "amount"),
                        "is_not_null",
                        RawModel.RawExpression.literal(null));

        SemanticModel.Condition result =
                ExpressionNormalizer.normalizeCondition(
                        schema(),
                        raw);

        require(
                result.operator.equals("is_not_null"),
                "is_not_null operator retained");

        require(
                result.right.literal == null,
                "is_not_null NULL right operand");
    }

    private static void testIsNotNullRequiresNullRightOperand() {
        RawModel.Condition raw =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "amount"),
                        "is_not_null",
                        RawModel.RawExpression.literal(100));

        expectFailure(
                () -> ExpressionNormalizer.normalizeCondition(
                        schema(), raw),
                "requires NULL right operand");
    }
    private static SemanticModel.Schema conditionalSchema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "flag",
                                SemanticModel.Type.BOOLEAN,
                                false),
                        new SemanticModel.Column(
                                "amount",
                                SemanticModel.Type.DECIMAL,
                                false),
                        new SemanticModel.Column(
                                "count",
                                SemanticModel.Type.INTEGER,
                                false)));
    }

    private static void testConditionalInteger() {
        RawModel.RawExpression conditionLeft =
                RawModel.RawExpression.column("flag");

        /*
         * Equality against a boolean literal gives us the
         * boolean predicate required by the conditional.
         */
        RawModel.Condition condition =
                new RawModel.Condition(
                        conditionLeft,
                        "=",
                        RawModel.RawExpression.literal(true));

        RawModel.RawExpression raw =
                RawModel.RawExpression.conditional(
                        condition,
                        RawModel.RawExpression.column("count"),
                        RawModel.RawExpression.column("count"));

        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.normalizeConditional(
                        conditionalSchema(),
                        raw);

        require(
                result.type ==
                        SemanticModel.Type.INTEGER,
                "conditional integer result type");

        require(
                !result.nullable,
                "conditional integer nullability");

        require(
                result.expression.kind ==
                        SemanticModel.Expression.Kind.CONDITIONAL,
                "conditional expression kind");

        require(
                result.expression.condition != null,
                "conditional normalized condition");
    }

    private static void testConditionalNumericWidening() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column("flag"),
                        "=",
                        RawModel.RawExpression.literal(true));

        RawModel.RawExpression raw =
                RawModel.RawExpression.conditional(
                        condition,
                        RawModel.RawExpression.column("count"),
                        RawModel.RawExpression.column("amount"));

        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.normalizeConditional(
                        conditionalSchema(),
                        raw);

        require(
                result.type ==
                        SemanticModel.Type.DECIMAL,
                "conditional numeric widening");
    }

    private static void testConditionalTypeMismatch() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column("flag"),
                        "=",
                        RawModel.RawExpression.literal(true));

        RawModel.RawExpression raw =
                RawModel.RawExpression.conditional(
                        condition,
                        RawModel.RawExpression.column("count"),
                        RawModel.RawExpression.column("flag"));

        expectFailure(
                () -> ExpressionNormalizer.normalizeConditional(
                        conditionalSchema(),
                        raw),
                "incompatible conditional branch types");
    }
    private static RawModel.Condition trueCondition() {
        return new RawModel.Condition(
                RawModel.RawExpression.column("flag"),
                "=",
                RawModel.RawExpression.literal(true));
    }

    private static void testConditionalTextLiterals() {
        RawModel.RawExpression raw =
                RawModel.RawExpression.conditional(
                        trueCondition(),
                        RawModel.RawExpression.literal(
                                "MEETS_TARGET"),
                        RawModel.RawExpression.literal(
                                "BELOW_TARGET"));

        TypedResult result =
                typedConditional(raw);

        require(
                result.type == SemanticModel.Type.TEXT,
                "conditional text literal type");

        require(
                result.expression.thenBranch.literalType ==
                        SemanticModel.Type.TEXT,
                "conditional then TEXT");

        require(
                result.expression.elseBranch.literalType ==
                        SemanticModel.Type.TEXT,
                "conditional else TEXT");
    }

    private static void testConditionalIntegerLiterals() {
        TypedResult result =
                typedConditional(
                        RawModel.RawExpression.conditional(
                                trueCondition(),
                                RawModel.RawExpression.literal(100),
                                RawModel.RawExpression.literal(200)));

        require(
                result.type == SemanticModel.Type.INTEGER,
                "conditional integer literal type");
    }

    private static void testConditionalNumericLiteralWidening() {
        TypedResult result =
                typedConditional(
                        RawModel.RawExpression.conditional(
                                trueCondition(),
                                RawModel.RawExpression.literal(100),
                                RawModel.RawExpression.literal(200.5)));

        require(
                result.type == SemanticModel.Type.DECIMAL,
                "conditional numeric literal widening");
    }

    private static void testConditionalLiteralMismatch() {
        expectFailure(
                () -> ExpressionNormalizer.normalizeAgainst(
                        conditionalSchema(),
                        RawModel.RawExpression.conditional(
                                trueCondition(),
                                RawModel.RawExpression.literal(100),
                                RawModel.RawExpression.literal("200")),
                        null),
                "incompatible conditional branch types");
    }

    private static TypedResult typedConditional(
            RawModel.RawExpression raw) {

        ExpressionNormalizer.TypedExpression result =
                ExpressionNormalizer.normalizeAgainst(
                        conditionalSchema(),
                        raw,
                        null);

        return new TypedResult(
                result.expression,
                result.type);
    }

    private static final class TypedResult {
        final SemanticModel.Expression expression;
        final SemanticModel.Type type;

        TypedResult(
                SemanticModel.Expression expression,
                SemanticModel.Type type) {

            this.expression = expression;
            this.type = type;
        }
    }
    private static void expectFailure(
            Runnable action,
            String expectedText) {

        try {
            action.run();

            throw new AssertionError(
                    "Expected failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "failure message: " +
                    ex.getMessage());
        }
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition)
            throw new AssertionError(message);
    }
}
