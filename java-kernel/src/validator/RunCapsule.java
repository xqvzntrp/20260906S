import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RunCapsule {

    private RunCapsule() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(
                    "usage: java RunCapsule <capsule-root>");
            System.exit(2);
        }

        Path root = Path.of(args[0]).toAbsolutePath().normalize();

        try {
            FileCapsuleParser parser = new FileCapsuleParser();
            RawModel.Capsule rawCapsule = parser.parse(root);
            Map<String,RawModel.RelationDocument> relations =
                    loadRelations(parser, rawCapsule, root);
            Map<String,RawSchema> schemas =
                    loadSchemas(parser, rawCapsule, root);

            Normalizer.NormalizationResult normalized =
                    new CapsuleNormalizer().normalize(
                            rawCapsule,
                            relations,
                            schemas);

            Map<String,ExecutionModel.RelationData> inputs =
                    loadInputs(normalized.capsule, root);

            Map<String,ExecutionModel.RelationData> outputs =
                    new WitnessExecutor().executeCapsule(
                            normalized.capsule,
                            inputs);

            writeOutputs(normalized.capsule, outputs, root);

            VerificationRunner.verify(
                    rootedVerificationPlan(
                            normalized.verification,
                            root),
                    outputs);

            System.out.println(
                    "EXECUTION PASSED: " +
                    normalized.capsule.id);

            System.out.println(
                    "VERIFY PASSED: " +
                    normalized.verification.expectations.size() +
                    " outputs");

        } catch (RuntimeException ex) {
            System.err.println(
                    "EXECUTION FAILED: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static Map<String,RawModel.RelationDocument> loadRelations(
            FileCapsuleParser parser,
            RawModel.Capsule capsule,
            Path root) {

        Map<String,RawModel.RelationDocument> relations =
                new LinkedHashMap<>();

        for (RawModel.Relation relation : capsule.relations) {
            RawModel.RelationDocument document =
                    parser.relation(relation, root);

            if (!relation.id.equals(document.relationId)) {
                throw new IllegalArgumentException(
                        "relation declaration/document id mismatch: " +
                        relation.id + " != " + document.relationId);
            }

            relations.put(relation.id, document);
        }

        return relations;
    }

    private static Map<String,RawSchema> loadSchemas(
            FileCapsuleParser parser,
            RawModel.Capsule capsule,
            Path root) {

        Map<String,RawSchema> schemas = new LinkedHashMap<>();

        for (RawModel.Input input : capsule.inputs) {
            schemas.putIfAbsent(
                    input.schemaPath,
                    parser.schema(input.schemaPath, root));
        }

        for (RawModel.Output output : capsule.outputs) {
            schemas.putIfAbsent(
                    output.schemaPath,
                    parser.schema(output.schemaPath, root));
        }

        return schemas;
    }

    private static Map<String,ExecutionModel.RelationData> loadInputs(
            SemanticModel.Capsule capsule,
            Path root) {

        Map<String,ExecutionModel.RelationData> inputs =
                new LinkedHashMap<>();

        for (SemanticModel.Input input : capsule.inputs) {
            Path path = resolveInsideRoot(root, input.path);

            try {
                inputs.put(
                        input.id,
                        ExpectedCsvReader.read(
                                Files.readAllBytes(path),
                                input.schema));
            } catch (IOException ex) {
                throw new IllegalArgumentException(
                        "cannot read input " + input.id +
                        ": " + input.path,
                        ex);
            }
        }

        return inputs;
    }

    private static void writeOutputs(
            SemanticModel.Capsule capsule,
            Map<String,ExecutionModel.RelationData> outputs,
            Path root) {

        for (SemanticModel.Output output : capsule.outputs) {
            ExecutionModel.RelationData data = outputs.get(output.id);

            if (data == null) {
                throw new IllegalArgumentException(
                        "declared output is unavailable: " + output.id);
            }

            Path generatedPath =
                    resolveInsideRoot(
                            root,
                            output.generatedPath);

            OutputWriter.writeCsv(
                    generatedPath,
                    data);

            String fileName =
                    generatedPath.getFileName().toString();

            String schemaFileName =
                    fileName.endsWith(".csv")
                    ? fileName.substring(
                            0,
                            fileName.length() - 4) +
                            ".schema.json"
                    : fileName + ".schema.json";

            OutputWriter.writeSchemaJson(
                    generatedPath.resolveSibling(
                            schemaFileName),
                    data.schema);

            System.out.println("OUTPUT: " + output.id);
        }
    }

    private static VerificationModel.Plan rootedVerificationPlan(
            VerificationModel.Plan plan,
            Path root) {

        List<VerificationModel.Expectation> expectations =
                new ArrayList<>();

        for (VerificationModel.Expectation expectation : plan.expectations) {
            expectations.add(new VerificationModel.Expectation(
                    expectation.outputId,
                    resolveInsideRoot(root, expectation.expectedPath)
                            .toString(),
                    expectation.schemaPath,
                    expectation.schema,
                    expectation.comparison));
        }

        return new VerificationModel.Plan(expectations);
    }

    private static Path resolveInsideRoot(Path root, String relativePath) {
        Path path = root.resolve(relativePath).normalize();

        if (!path.startsWith(root)) {
            throw new IllegalArgumentException(
                    "path escapes capsule root: " + relativePath);
        }

        return path;
    }
}
