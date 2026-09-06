import java.util.Map;

public interface Normalizer {

    NormalizationResult normalize(
            RawModel.Capsule capsule,
            Map<String,RawModel.RelationDocument> relations,
            Map<String,RawSchema> schemas);

    final class NormalizationResult {
        public final SemanticModel.Capsule capsule;
        public final VerificationModel.Plan verification;

        public NormalizationResult(
                SemanticModel.Capsule capsule,
                VerificationModel.Plan verification) {

            this.capsule =
                    java.util.Objects.requireNonNull(capsule);

            this.verification =
                    java.util.Objects.requireNonNull(verification);
        }
    }
}