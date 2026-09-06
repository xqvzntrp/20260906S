import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class VerifierTest {

    public static void main(String[] args) {
        testIdenticalRelationsPass();
        testDecimalScaleDoesNotMatter();
        testNullEqualsNull();
        testTupleCountMismatch();
        testTupleOrderMismatch();
        testValueMismatch();
        testSchemaNameMismatch();
        testSchemaTypeMismatch();
        testSchemaNullabilityMismatch();

        System.out.println(
                "VerifierTest PASSED");
    }

    private static void testIdenticalRelationsPass() {
        ExecutionModel.RelationData data =
                relation(
                        schema(),
                        row(
                                "A",
                                7L,
                                new BigDecimal("1.23"),
                                true,
                                LocalDate.of(2026, 9, 4)));

        Verifier.verifyTyped(
                "output",
                data,
                data);
    }

    private static void testDecimalScaleDoesNotMatter() {
        ExecutionModel.RelationData actual =
                relation(
                        schema(),
                        row(
                                "A",
                                7L,
                                new BigDecimal("1.2300"),
                                true,
                                LocalDate.of(2026, 9, 4)));

        ExecutionModel.RelationData expected =
                relation(
                        schema(),
                        row(
                                "A",
                                7L,
                                new BigDecimal("1.23"),
                                true,
                                LocalDate.of(2026, 9, 4)));

        Verifier.verifyTyped(
                "output",
                actual,
                expected);
    }

    private static void testNullEqualsNull() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column(
                                        "value",
                                        SemanticModel.Type.TEXT,
                                        true)));

        Verifier.verifyTyped(
                "output",
                relation(
                        schema,
                        row((Object) null)),
                relation(
                        schema,
                        row((Object) null)));
    }

    private static void testTupleCountMismatch() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column(
                                        "value",
                                        SemanticModel.Type.TEXT,
                                        false)));

        expectFailure(
                relation(
                        schema,
                        row("A")),
                new ExecutionModel.RelationData(
                        schema,
                        List.of()),
                "tuple count");
    }

    private static void testTupleOrderMismatch() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column(
                                        "value",
                                        SemanticModel.Type.TEXT,
                                        false)));

        ExecutionModel.RelationData actual =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                row("A"),
                                row("B")));

        ExecutionModel.RelationData expected =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                row("B"),
                                row("A")));

        expectFailure(
                actual,
                expected,
                "tuple 1 column value differs");
    }

    private static void testValueMismatch() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column(
                                        "value",
                                        SemanticModel.Type.INTEGER,
                                        false)));

        expectFailure(
                relation(
                        schema,
                        row(1L)),
                relation(
                        schema,
                        row(2L)),
                "tuple 1 column value differs");
    }

    private static void testSchemaNameMismatch() {
        expectFailure(
                relation(
                        new SemanticModel.Schema(
                                List.of(
                                        column(
                                                "actual",
                                                SemanticModel.Type.TEXT,
                                                false))),
                        row("A")),
                relation(
                        new SemanticModel.Schema(
                                List.of(
                                        column(
                                                "expected",
                                                SemanticModel.Type.TEXT,
                                                false))),
                        row("A")),
                "name differs");
    }

    private static void testSchemaTypeMismatch() {
        expectFailure(
                relation(
                        new SemanticModel.Schema(
                                List.of(
                                        column(
                                                "value",
                                                SemanticModel.Type.TEXT,
                                                false))),
                        row("1")),
                relation(
                        new SemanticModel.Schema(
                                List.of(
                                        column(
                                                "value",
                                                SemanticModel.Type.INTEGER,
                                                false))),
                        row(1L)),
                "type differs");
    }

    private static void testSchemaNullabilityMismatch() {
        expectFailure(
                relation(
                        new SemanticModel.Schema(
                                List.of(
                                        column(
                                                "value",
                                                SemanticModel.Type.TEXT,
                                                false))),
                        row("A")),
                relation(
                        new SemanticModel.Schema(
                                List.of(
                                        column(
                                                "value",
                                                SemanticModel.Type.TEXT,
                                                true))),
                        row("A")),
                "nullability differs");
    }

    private static SemanticModel.Schema schema() {
        return new SemanticModel.Schema(
                List.of(
                        column("text", SemanticModel.Type.TEXT, false),
                        column("integer", SemanticModel.Type.INTEGER, false),
                        column("decimal", SemanticModel.Type.DECIMAL, false),
                        column("boolean", SemanticModel.Type.BOOLEAN, false),
                        column("date", SemanticModel.Type.DATE, false)));
    }

    private static SemanticModel.Column column(
            String name,
            SemanticModel.Type type,
            boolean nullable) {

        return new SemanticModel.Column(
                name,
                type,
                nullable);
    }

    private static ExecutionModel.Row row(
            Object... values) {

        return new ExecutionModel.Row(
                Arrays.asList(values));
    }

    private static ExecutionModel.RelationData relation(
            SemanticModel.Schema schema,
            ExecutionModel.Row... rows) {

        return new ExecutionModel.RelationData(
                schema,
                Arrays.asList(rows));
    }

    private static void expectFailure(
            ExecutionModel.RelationData actual,
            ExecutionModel.RelationData expected,
            String fragment) {

        try {
            Verifier.verifyTyped(
                    "test_output",
                    actual,
                    expected);

            throw new AssertionError(
                    "Expected verification failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "test_output"),
                    "failure identifies output");

            require(
                    ex.getMessage().contains(fragment),
                    "failure detail: " +
                    ex.getMessage());
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
