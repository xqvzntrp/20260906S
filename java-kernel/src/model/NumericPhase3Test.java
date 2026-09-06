import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NumericPhase3Test {

    private static int assertions = 0;

    public static void main(String[] args)
            throws Exception {

        rawDerivedMetadata();
        semanticDerivedMetadata();

        deriveRoundingBoundary();
        deriveOverflowBoundary();

        renamePreservesMetadata();

        aggregateMinPreservesMetadata();
        aggregateMaxPreservesMetadata();
        aggregateSumDropsConstraint();

        schemaEqualityIncludesPrecision();
        schemaEqualityIncludesScale();

        System.out.println(
                "NumericPhase3Test PASSED: " +
                assertions +
                " assertions");
    }

    private static void rawDerivedMetadata() {

        RawModel.DerivedColumn derived =
                new RawModel.DerivedColumn(
                        "amount",
                        RawModel.RawExpression.column(
                                "source_amount"),
                        18,
                        2);

        assertEquals(
                Integer.valueOf(18),
                derived.precision);

        assertEquals(
                Integer.valueOf(2),
                derived.scale);
    }

    private static void semanticDerivedMetadata() {

        SemanticModel.DerivedColumn derived =
                new SemanticModel.DerivedColumn(
                        "amount",
                        SemanticModel.Expression.column(
                                "source_amount"),
                        18,
                        2);

        assertEquals(
                Integer.valueOf(18),
                derived.precision);

        assertEquals(
                Integer.valueOf(2),
                derived.scale);
    }

    private static void deriveRoundingBoundary() {

        SemanticModel.Schema inputSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "reported_amount",
                                        SemanticModel.Type.DECIMAL,
                                        false),
                                new SemanticModel.Column(
                                        "cloud_cost",
                                        SemanticModel.Type.DECIMAL,
                                        false)));

        SemanticModel.Expression subtract =
                SemanticModel.Expression.function(
                        "subtract",
                        List.of(
                                SemanticModel.Expression.column(
                                        "reported_amount"),
                                SemanticModel.Expression.column(
                                        "cloud_cost")));

        SemanticModel.DerivedColumn contribution =
                new SemanticModel.DerivedColumn(
                        "contribution_amount",
                        subtract,
                        18,
                        2);

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                inputSchema.columns.get(0),
                                inputSchema.columns.get(1),
                                new SemanticModel.Column(
                                        "contribution_amount",
                                        SemanticModel.Type.DECIMAL,
                                        false,
                                        18,
                                        2)));

        SemanticModel.Step step =
                new SemanticModel.Step(
                        "derive_contribution",
                        SemanticModel.Operation.DERIVE,
                        outputSchema,
                        "source",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(contribution),
                        List.of(),
                        List.of(),
                        Map.of());

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        inputSchema,
                        List.of(
                                new ExecutionModel.Row(
                                        List.of(
                                                new BigDecimal(
                                                        "27.255"),
                                                new BigDecimal(
                                                        "9.70")))));

        Map<String,ExecutionModel.RelationData> available =
                new LinkedHashMap<>();

        available.put(
                "source",
                input);

        ExecutionModel.RelationData result =
                new WitnessExecutor()
                        .executeStep(
                                step,
                                available);

        assertDecimal(
                "17.56",
                result.rows.get(0).get(2));

        SemanticModel.Column column =
                result.schema.columns.get(2);

        assertEquals(
                Integer.valueOf(18),
                column.precision);

        assertEquals(
                Integer.valueOf(2),
                column.scale);
    }

    private static void deriveOverflowBoundary() {

        SemanticModel.Schema inputSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "left_value",
                                        SemanticModel.Type.DECIMAL,
                                        false),
                                new SemanticModel.Column(
                                        "right_value",
                                        SemanticModel.Type.DECIMAL,
                                        false)));

        SemanticModel.Expression subtract =
                SemanticModel.Expression.function(
                        "subtract",
                        List.of(
                                SemanticModel.Expression.column(
                                        "left_value"),
                                SemanticModel.Expression.column(
                                        "right_value")));

        SemanticModel.DerivedColumn derived =
                new SemanticModel.DerivedColumn(
                        "result_value",
                        subtract,
                        4,
                        2);

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                inputSchema.columns.get(0),
                                inputSchema.columns.get(1),
                                new SemanticModel.Column(
                                        "result_value",
                                        SemanticModel.Type.DECIMAL,
                                        false,
                                        4,
                                        2)));

        SemanticModel.Step step =
                new SemanticModel.Step(
                        "derive_overflow",
                        SemanticModel.Operation.DERIVE,
                        outputSchema,
                        "source",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(derived),
                        List.of(),
                        List.of(),
                        Map.of());

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        inputSchema,
                        List.of(
                                new ExecutionModel.Row(
                                        List.of(
                                                new BigDecimal(
                                                        "200"),
                                                new BigDecimal(
                                                        "76.55")))));

        Map<String,ExecutionModel.RelationData> available =
                new LinkedHashMap<>();

        available.put(
                "source",
                input);

        assertions++;

        try {
            new WitnessExecutor()
                    .executeStep(
                            step,
                            available);

            throw new AssertionError(
                    "expected NUMBER precision overflow");

        } catch (IllegalArgumentException expected) {

            if (!expected.getMessage()
                    .contains(
                            "violates declared NUMBER contract")) {

                throw new AssertionError(
                        "unexpected error: " +
                        expected.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void renamePreservesMetadata()
            throws Exception {

        SemanticModel.Column amount =
                new SemanticModel.Column(
                        "amount",
                        SemanticModel.Type.DECIMAL,
                        false,
                        18,
                        2);

        SemanticModel.Schema input =
                new SemanticModel.Schema(
                        List.of(amount));

        Method method =
                CapsuleNormalizer.class
                        .getDeclaredMethod(
                                "normalizeRenameSchema",
                                String.class,
                                String.class,
                                SemanticModel.Schema.class,
                                Map.class);

        method.setAccessible(true);

        SemanticModel.Schema result =
                (SemanticModel.Schema)
                        method.invoke(
                                null,
                                "TEST",
                                "rename",
                                input,
                                Map.of(
                                        "amount",
                                        "renamed_amount"));

        SemanticModel.Column renamed =
                result.columns.get(0);

        assertEquals(
                "renamed_amount",
                renamed.name);

        assertEquals(
                Integer.valueOf(18),
                renamed.precision);

        assertEquals(
                Integer.valueOf(2),
                renamed.scale);
    }

    private static void aggregateMinPreservesMetadata()
            throws Exception {

        SemanticModel.Column result =
                aggregateColumn("MIN");

        assertEquals(
                Integer.valueOf(18),
                result.precision);

        assertEquals(
                Integer.valueOf(2),
                result.scale);
    }

    private static void aggregateMaxPreservesMetadata()
            throws Exception {

        SemanticModel.Column result =
                aggregateColumn("MAX");

        assertEquals(
                Integer.valueOf(18),
                result.precision);

        assertEquals(
                Integer.valueOf(2),
                result.scale);
    }

    private static void aggregateSumDropsConstraint()
            throws Exception {

        SemanticModel.Column result =
                aggregateColumn("SUM");

        assertEquals(
                null,
                result.precision);

        assertEquals(
                null,
                result.scale);

        assertEquals(
                SemanticModel.Type.DECIMAL,
                result.type);
    }

    @SuppressWarnings("unchecked")
    private static SemanticModel.Column aggregateColumn(
            String function)
            throws Exception {

        SemanticModel.Column amount =
                new SemanticModel.Column(
                        "amount",
                        SemanticModel.Type.DECIMAL,
                        false,
                        18,
                        2);

        Map<String,SemanticModel.Column> columns =
                new LinkedHashMap<>();

        columns.put(
                amount.name,
                amount);

        RawModel.Measure measure =
                new RawModel.Measure(
                        "result",
                        function,
                        "amount");

        Method method =
                CapsuleNormalizer.class
                        .getDeclaredMethod(
                                "measureOutputColumn",
                                String.class,
                                String.class,
                                Map.class,
                                RawModel.Measure.class);

        method.setAccessible(true);

        return (SemanticModel.Column)
                method.invoke(
                        null,
                        "TEST",
                        "aggregate",
                        columns,
                        measure);
    }

    private static void schemaEqualityIncludesPrecision()
            throws Exception {

        SemanticModel.Schema left =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        false,
                                        18,
                                        2)));

        SemanticModel.Schema right =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        false,
                                        20,
                                        2)));

        assertFalse(
                sameSchema(
                        left,
                        right));
    }

    private static void schemaEqualityIncludesScale()
            throws Exception {

        SemanticModel.Schema left =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        false,
                                        18,
                                        2)));

        SemanticModel.Schema right =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        false,
                                        18,
                                        4)));

        assertFalse(
                sameSchema(
                        left,
                        right));
    }

    private static boolean sameSchema(
            SemanticModel.Schema left,
            SemanticModel.Schema right)
            throws Exception {

        Method method =
                WitnessExecutor.class
                        .getDeclaredMethod(
                                "sameSchema",
                                SemanticModel.Schema.class,
                                SemanticModel.Schema.class);

        method.setAccessible(true);

        return (Boolean)
                method.invoke(
                        null,
                        left,
                        right);
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
                            : actual.getClass()
                                    .getName()));
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

    private static void assertFalse(
            boolean actual) {

        assertions++;

        if (actual) {
            throw new AssertionError(
                    "expected false");
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
}
