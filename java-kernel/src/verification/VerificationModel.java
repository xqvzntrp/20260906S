import java.util.*;

public final class VerificationModel {

    private VerificationModel() {
    }

    public static final class Plan {
        public final List<Expectation> expectations;

        public Plan(List<Expectation> expectations) {
            this.expectations = List.copyOf(
                    Objects.requireNonNull(expectations));
        }
    }

    public static final class Expectation {
        public final String outputId;
        public final String expectedPath;
        public final String schemaPath;
        public final SemanticModel.Schema schema;
        public final Comparison comparison;

        public Expectation(
                String outputId,
                String expectedPath,
                String schemaPath,
                SemanticModel.Schema schema,
                Comparison comparison) {

            this.outputId =
                    Objects.requireNonNull(outputId);

            this.expectedPath =
                    Objects.requireNonNull(expectedPath);

            this.schemaPath =
                    Objects.requireNonNull(schemaPath);

            this.schema =
                    Objects.requireNonNull(schema);

            this.comparison =
                    Objects.requireNonNull(comparison);
        }
    }

    public enum Comparison {
        TYPED
    }
}