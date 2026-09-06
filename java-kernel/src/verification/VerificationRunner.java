import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class VerificationRunner {

    private VerificationRunner() {
    }

    public static void verify(
            VerificationModel.Plan plan,
            Map<String,ExecutionModel.RelationData> actualOutputs) {

        Objects.requireNonNull(plan);
        Objects.requireNonNull(actualOutputs);

        for (VerificationModel.Expectation expectation :
                plan.expectations) {

            ExecutionModel.RelationData actual =
                    actualOutputs.get(
                            expectation.outputId);

            if (actual == null) {
                throw new IllegalArgumentException(
                        "verification failed for output " +
                        expectation.outputId +
                        ": actual output is unavailable");
            }

            if (expectation.comparison !=
                    VerificationModel.Comparison.TYPED) {

                throw new IllegalArgumentException(
                        "verification failed for output " +
                        expectation.outputId +
                        ": unsupported comparison mode " +
                        expectation.comparison);
            }

            byte[] expectedBytes;

            try {
                expectedBytes =
                        Files.readAllBytes(
                                Path.of(
                                        expectation.expectedPath));

            } catch (IOException ex) {
                throw new IllegalArgumentException(
                        "verification failed for output " +
                        expectation.outputId +
                        ": cannot read expected evidence: " +
                        expectation.expectedPath,
                        ex);
            }

            ExecutionModel.RelationData expected;

            try {
                expected =
                        ExpectedCsvReader.read(
                                expectedBytes,
                                expectation.schema);

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "verification failed for output " +
                        expectation.outputId +
                        ": invalid expected evidence: " +
                        ex.getMessage(),
                        ex);
            }

            Verifier.verifyTyped(
                    expectation.outputId,
                    actual,
                    expected);
        }
    }
}
