import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class VerificationRunnerTest {

    public static void main(String[] args)
            throws Exception {

        testTypedVerificationPasses();
        testDecimalScaleDifferencePasses();
        testValueMismatchFails();
        testMissingActualOutputFails();
        testMissingExpectedEvidenceFails();
        testMalformedExpectedEvidenceFails();

        System.out.println(
                "VerificationRunnerTest PASSED");
    }

    private static void testTypedVerificationPasses()
            throws Exception {

        SemanticModel.Schema schema =
                schema();

        Path expected =
                writeExpected(
                        "category,amount\n" +
                        "A,1.23\n" +
                        "B,\\N\n");

        VerificationRunner.verify(
                plan(expected, schema),
                Map.of(
                        "result",
                        new ExecutionModel.RelationData(
                                schema,
                                List.of(
                                        row(
                                                "A",
                                                new BigDecimal("1.23")),
                                        row(
                                                "B",
                                                null)))));
    }

    private static void testDecimalScaleDifferencePasses()
            throws Exception {

        SemanticModel.Schema schema =
                schema();

        Path expected =
                writeExpected(
                        "category,amount\n" +
                        "A,1.2300\n");

        VerificationRunner.verify(
                plan(expected, schema),
                Map.of(
                        "result",
                        new ExecutionModel.RelationData(
                                schema,
                                List.of(
                                        row(
                                                "A",
                                                new BigDecimal("1.23"))))));
    }

    private static void testValueMismatchFails()
            throws Exception {

        SemanticModel.Schema schema =
                schema();

        Path expected =
                writeExpected(
                        "category,amount\n" +
                        "A,1.23\n");

        expectFailure(
                plan(expected, schema),
                Map.of(
                        "result",
                        new ExecutionModel.RelationData(
                                schema,
                                List.of(
                                        row(
                                                "A",
                                                new BigDecimal("2.00"))))),
                "tuple 1 column amount differs");
    }

    private static void testMissingActualOutputFails()
            throws Exception {

        SemanticModel.Schema schema =
                schema();

        Path expected =
                writeExpected(
                        "category,amount\n");

        expectFailure(
                plan(expected, schema),
                Map.of(),
                "actual output is unavailable");
    }

    private static void testMissingExpectedEvidenceFails() {
        SemanticModel.Schema schema =
                schema();

        Path missing =
                Path.of(
                        "definitely-missing-v2-expected.csv");

        expectFailure(
                plan(missing, schema),
                Map.of(
                        "result",
                        new ExecutionModel.RelationData(
                                schema,
                                List.of())),
                "cannot read expected evidence");
    }

    private static void testMalformedExpectedEvidenceFails()
            throws Exception {

        SemanticModel.Schema schema =
                schema();

        Path expected =
                writeExpected(
                        "category,amount\r\n");

        expectFailure(
                plan(expected, schema),
                Map.of(
                        "result",
                        new ExecutionModel.RelationData(
                                schema,
                                List.of())),
                "invalid expected evidence");
    }

    private static VerificationModel.Plan plan(
            Path expected,
            SemanticModel.Schema schema) {

        return new VerificationModel.Plan(
                List.of(
                        new VerificationModel.Expectation(
                                "result",
                                expected.toString(),
                                "expected/result.schema.json",
                                schema,
                                VerificationModel.Comparison.TYPED)));
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
                                true)));
    }

    private static ExecutionModel.Row row(
            Object... values) {

        return new ExecutionModel.Row(
                Arrays.asList(values));
    }

    private static Path writeExpected(
            String text)
            throws Exception {

        Path path =
                Files.createTempFile(
                        "v2-expected-",
                        ".csv");

        Files.write(
                path,
                text.getBytes(
                        StandardCharsets.UTF_8));

        return path;
    }

    private static void expectFailure(
            VerificationModel.Plan plan,
            Map<String,ExecutionModel.RelationData> actual,
            String fragment) {

        try {
            VerificationRunner.verify(
                    plan,
                    actual);

            throw new AssertionError(
                    "Expected verification failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "result"),
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
