import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class NumericPhase2Test {

    private static int assertions = 0;

    public static void main(String[] args)
            throws Exception {

        semanticColumnMetadata();
        rawColumnMetadata();
        schemaParserNumberMetadata();

        subtractEvaluation();
        divideEvaluation();
        mixedIntegerDecimal();
        nullPropagation();
        divideByZero();

        expressionNormalizerArithmetic();

        System.out.println(
                "NumericPhase2Test PASSED: " +
                assertions +
                " assertions");
    }

    private static void semanticColumnMetadata() {

        SemanticModel.Column column =
                new SemanticModel.Column(
                        "amount",
                        SemanticModel.Type.DECIMAL,
                        false,
                        18,
                        2);

        assertEquals(
                Integer.valueOf(18),
                column.precision);

        assertEquals(
                Integer.valueOf(2),
                column.scale);
    }

    private static void rawColumnMetadata() {

        RawSchema.Column column =
                new RawSchema.Column(
                        "amount",
                        "NUMBER",
                        false,
                        38,
                        10);

        assertEquals(
                Integer.valueOf(38),
                column.precision);

        assertEquals(
                Integer.valueOf(10),
                column.scale);
    }

    private static void schemaParserNumberMetadata()
            throws Exception {

        Path root =
                Files.createTempDirectory(
                        "number-schema-test");

        try {
            Path schema =
                    root.resolve("schema.json");

            Files.writeString(
                    schema,
                    """
                    {
                      "schema_id": "number_test",
                      "version": "1.0.0",
                      "relation": "number_test",
                      "columns": [
                        {
                          "name": "amount",
                          "type": "NUMBER",
                          "precision": 18,
                          "scale": 2,
                          "nullable": false
                        }
                      ]
                    }
                    """,
                    StandardCharsets.UTF_8);

            RawSchema parsed =
                    new FileCapsuleParser()
                            .schema(
                                    "schema.json",
                                    root);

            RawSchema.Column column =
                    parsed.columns.get(0);

            assertEquals(
                    "NUMBER",
                    column.type);

            assertEquals(
                    Integer.valueOf(18),
                    column.precision);

            assertEquals(
                    Integer.valueOf(2),
                    column.scale);

        } finally {
            deleteTree(root);
        }
    }

    private static void subtractEvaluation() {

        SemanticModel.Schema schema =
                decimalSchema();

        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        List.of(
                                new BigDecimal("27250"),
                                new BigDecimal("9700")));

        SemanticModel.Expression expression =
                SemanticModel.Expression.function(
                        "subtract",
                        List.of(
                                SemanticModel.Expression.column(
                                        "reported_amount"),
                                SemanticModel.Expression.column(
                                        "cloud_cost")));

        Object result =
                ExpressionEvaluator.evaluate(
                        expression,
                        schema,
                        row);

        assertDecimal(
                "17550",
                result);
    }

    private static void divideEvaluation() {

        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "contribution",
                                        SemanticModel.Type.DECIMAL,
                                        false),
                                new SemanticModel.Column(
                                        "reported_amount",
                                        SemanticModel.Type.DECIMAL,
                                        false)));

        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        List.of(
                                new BigDecimal("17550"),
                                new BigDecimal("27250")));

        SemanticModel.Expression expression =
                SemanticModel.Expression.function(
                        "divide",
                        List.of(
                                SemanticModel.Expression.column(
                                        "contribution"),
                                SemanticModel.Expression.column(
                                        "reported_amount")));

        BigDecimal result =
                (BigDecimal)
                        ExpressionEvaluator.evaluate(
                                expression,
                                schema,
                                row);

        assertEquals(
                "0.64403669724770642201834862385321100917",
                result.toPlainString());
    }

    private static void mixedIntegerDecimal() {

        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "count",
                                        SemanticModel.Type.INTEGER,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        false)));

        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        List.of(
                                2L,
                                new BigDecimal("10.50")));

        SemanticModel.Expression expression =
                SemanticModel.Expression.function(
                        "multiply",
                        List.of(
                                SemanticModel.Expression.column(
                                        "count"),
                                SemanticModel.Expression.column(
                                        "amount")));

        assertDecimal(
                "21.00",
                ExpressionEvaluator.evaluate(
                        expression,
                        schema,
                        row));
    }

    private static void nullPropagation() {

        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "left_value",
                                        SemanticModel.Type.DECIMAL,
                                        true),
                                new SemanticModel.Column(
                                        "right_value",
                                        SemanticModel.Type.DECIMAL,
                                        false)));

        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        java.util.Arrays.asList(
                                null,
                                new BigDecimal("5")));

        SemanticModel.Expression expression =
                SemanticModel.Expression.function(
                        "add",
                        List.of(
                                SemanticModel.Expression.column(
                                        "left_value"),
                                SemanticModel.Expression.column(
                                        "right_value")));

        Object result =
                ExpressionEvaluator.evaluate(
                        expression,
                        schema,
                        row);

        assertions++;

        if (result != null) {
            throw new AssertionError(
                    "expected NULL propagation");
        }
    }

    private static void divideByZero() {

        SemanticModel.Schema schema =
                decimalSchema();

        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        List.of(
                                new BigDecimal("10"),
                                BigDecimal.ZERO));

        SemanticModel.Expression expression =
                SemanticModel.Expression.function(
                        "divide",
                        List.of(
                                SemanticModel.Expression.column(
                                        "reported_amount"),
                                SemanticModel.Expression.column(
                                        "cloud_cost")));

        assertions++;

        try {
            ExpressionEvaluator.evaluate(
                    expression,
                    schema,
                    row);

            throw new AssertionError(
                    "expected division-by-zero failure");

        } catch (ArithmeticException expected) {
            // expected
        }
    }

    private static void expressionNormalizerArithmetic() {

        SemanticModel.Schema schema =
                decimalSchema();

        RawModel.RawExpression raw =
                new RawModel.RawExpression(
                        RawModel.RawExpression.Kind.FUNCTION,
                        null,
                        null,
                        "subtract",
                        List.of(
                                RawModel.RawExpression.column(
                                        "reported_amount"),
                                RawModel.RawExpression.column(
                                        "cloud_cost")),
                        null,
                        null,
                        null);

        ExpressionNormalizer.TypedExpression typed =
                ExpressionNormalizer.normalizeAgainst(
                        schema,
                        raw,
                        null);

        assertEquals(
                SemanticModel.Type.DECIMAL,
                typed.type);

        assertEquals(
                "subtract",
                typed.expression.function);
    }

    private static SemanticModel.Schema decimalSchema() {

        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "reported_amount",
                                SemanticModel.Type.DECIMAL,
                                false),
                        new SemanticModel.Column(
                                "cloud_cost",
                                SemanticModel.Type.DECIMAL,
                                false)));
    }

    private static void assertDecimal(
            String expected,
            Object actual) {

        assertions++;

        if (!(actual instanceof BigDecimal)) {
            throw new AssertionError(
                    "expected BigDecimal but got " +
                    (actual == null
                            ? "null"
                            : actual.getClass().getName()));
        }

        BigDecimal expectedValue =
                new BigDecimal(expected);

        if (expectedValue.compareTo(
                (BigDecimal) actual) != 0) {

            throw new AssertionError(
                    "expected " +
                    expected +
                    " but got " +
                    actual);
        }
    }

    private static void assertEquals(
            Object expected,
            Object actual) {

        assertions++;

        if (!java.util.Objects.equals(
                expected,
                actual)) {

            throw new AssertionError(
                    "expected " +
                    expected +
                    " but got " +
                    actual);
        }
    }

    private static void deleteTree(Path root)
            throws Exception {

        if (root == null ||
            !Files.exists(root)) {

            return;
        }

        try (var stream = Files.walk(root)) {
            stream.sorted(
                    java.util.Comparator.reverseOrder())
                  .forEach(path -> {
                      try {
                          Files.deleteIfExists(path);
                      } catch (Exception ex) {
                          throw new RuntimeException(ex);
                      }
                  });
        }
    }
}
