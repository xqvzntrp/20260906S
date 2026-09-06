import java.nio.file.Path;
import java.util.*;

public final class ValidateCapsule {

    private ValidateCapsule() {
    }

    public static void main(
            String[] args) {

        if (args.length != 1) {
            System.err.println(
                    "usage: java ValidateCapsule <capsule-root>");
            System.exit(2);
        }

        Path root =
                Path.of(args[0]);

        try {
            FileCapsuleParser parser =
                    new FileCapsuleParser();

            RawModel.Capsule capsule =
                    parser.parse(root);

            Map<String,RawModel.RelationDocument> relations =
                    new LinkedHashMap<>();

            for (RawModel.Relation relation :
                    capsule.relations) {

                RawModel.RelationDocument document =
                        parser.relation(
                                relation,
                                root);

                if (!relation.id.equals(
                        document.relationId)) {

                    throw new IllegalArgumentException(
                            "relation declaration/document id mismatch: " +
                            relation.id +
                            " != " +
                            document.relationId);
                }

                relations.put(
                        relation.id,
                        document);
            }

            Map<String,RawSchema> schemas =
                    new LinkedHashMap<>();

            for (RawModel.Input input :
                    capsule.inputs) {

                if (schemas.containsKey(
                        input.schemaPath)) {
                    continue;
                }

                schemas.put(
                        input.schemaPath,
                        parser.schema(
                                input.schemaPath,
                                root));
            }

            for (RawModel.Output output :
                    capsule.outputs) {

                if (schemas.containsKey(
                        output.schemaPath)) {
                    continue;
                }

                schemas.put(
                        output.schemaPath,
                        parser.schema(
                                output.schemaPath,
                                root));
            }

            CapsuleNormalizer normalizer =
                    new CapsuleNormalizer();

            Normalizer.NormalizationResult result =
                    normalizer.normalize(
                            capsule,
                            relations,
                            schemas);

            System.out.println(
                    "V2 VALID: " +
                    result.capsule.id);

            System.out.println(
                    "relations=" +
                    result.capsule.relations.size());

            System.out.println(
                    "outputs=" +
                    result.capsule.outputs.size());

            System.out.println(
                    "verification_expectations=" +
                    result.verification.expectations.size());

        } catch (RuntimeException ex) {
            System.err.println(
                    "V2 INVALID: " +
                    ex.getMessage());

            System.exit(1);
        }
    }
}
