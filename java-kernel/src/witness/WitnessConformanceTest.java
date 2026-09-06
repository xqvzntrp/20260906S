import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class WitnessConformanceTest {

    private WitnessConformanceTest() {
    }

    public static void main(String[] args)
            throws Exception {
        testNormalizedDependencyAwareJoinExecutes();
        testNormalizedDependencyAwareJoinSerializesCanonicalCsv();
        testNormalizedDependencyAwareJoinWritesOutputFile();

        System.out.println(
                "WitnessConformanceTest PASSED");
    }

    private static void testNormalizedDependencyAwareJoinExecutes() {
        RawSchema ordersSchema =
                new RawSchema(
                        "orders",
                        "1.0.0",
                        "orders",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true)));

        RawSchema targetsSchema =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "join_key",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        RawSchema joinedSchema =
                new RawSchema(
                        "joined",
                        "1.0.0",
                        "joined",
                        List.of(
                                new RawSchema.Column(
                                        "join_key",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        RawModel.Input orders =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Input targets =
                new RawModel.Input(
                        "targets",
                        "data/targets.csv",
                        "data/targets.schema.json",
                        "csv");

        RawModel.Relation downstream =
                new RawModel.Relation(
                        "downstream",
                        "relations/downstream.json",
                        "json");

        RawModel.Relation upstream =
                new RawModel.Relation(
                        "upstream",
                        "relations/upstream.json",
                        "json");

        RawModel.Output output =
                new RawModel.Output(
                        "joined_output",
                        "downstream",
                        "generated/joined.csv",
                        "expected/joined.csv",
                        "expected/joined.schema.json",
                        "csv");

        /*
         * Downstream intentionally precedes its dependency.
         */
        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(
                                downstream,
                                upstream),
                        List.of(output));

        RawModel.Step rename =
                new RawModel.Step(
                        "renamed",
                        "rename",
                        "orders",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of(
                                "category",
                                "join_key"));

        RawModel.RelationDocument upstreamDocument =
                new RawModel.RelationDocument(
                        "upstream",
                        "upstream transform",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(rename),
                        "renamed");

        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "join_key"),
                        "=",
                        RawModel.RawExpression.column(
                                "join_key"));

        RawModel.Step join =
                new RawModel.Step(
                        "joined",
                        "join",
                        null,
                        "upstream",
                        "targets",
                        null,
                        List.of(condition),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument downstreamDocument =
                new RawModel.RelationDocument(
                        "downstream",
                        "dependency-aware join",
                        List.of(
                                new RawModel.Binding(
                                        "upstream",
                                        "relations/upstream.json"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(join),
                        "joined");

        Normalizer.NormalizationResult normalized =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "upstream",
                                upstreamDocument,
                                "downstream",
                                downstreamDocument),
                        Map.of(
                                "data/orders.schema.json",
                                ordersSchema,
                                "data/targets.schema.json",
                                targetsSchema,
                                "expected/joined.schema.json",
                                joinedSchema));

        /*
         * Runtime data deliberately contains:
         *
         * - two A rows on the left
         * - one B row
         * - one unmatched C row
         *
         * and two A targets on the right.
         *
         * This qualifies both dependency execution and JOIN
         * multiplicity/order.
         */
        SemanticModel.Schema normalizedOrdersSchema =
                normalized.capsule.inputs
                        .get(0)
                        .schema;

        SemanticModel.Schema normalizedTargetsSchema =
                normalized.capsule.inputs
                        .get(1)
                        .schema;

        ExecutionModel.RelationData ordersData =
                new ExecutionModel.RelationData(
                        normalizedOrdersSchema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("10"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new BigDecimal("20"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("30"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "C",
                                                new BigDecimal("40")))));

        ExecutionModel.RelationData targetsData =
                new ExecutionModel.RelationData(
                        normalizedTargetsSchema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("100"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("101"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new BigDecimal("200"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "D",
                                                new BigDecimal("400")))));

        Map<String,ExecutionModel.RelationData> outputs =
                new WitnessExecutor().executeCapsule(
                        normalized.capsule,
                        Map.of(
                                "orders",
                                ordersData,
                                "targets",
                                targetsData));

        require(
                outputs.size() == 1,
                "conformance output count");

        ExecutionModel.RelationData result =
                outputs.get(
                        "joined_output");

        require(
                result != null,
                "conformance declared output id");

        require(
                result.schema.columns.size() == 3,
                "conformance output width");

        require(
                result.schema.columns.get(0)
                        .name.equals("join_key"),
                "conformance output join key");

        require(
                result.schema.columns.get(1)
                        .name.equals("amount"),
                "conformance output amount");

        require(
                result.schema.columns.get(2)
                        .name.equals("target"),
                "conformance output target");

        /*
         * Left input order:
         *
         * A(10), B(20), A(30), C(40)
         *
         * Right encounter order:
         *
         * A(100), A(101), B(200), D(400)
         *
         * Inner JOIN therefore emits:
         *
         * A 10 100
         * A 10 101
         * B 20 200
         * A 30 100
         * A 30 101
         *
         * C has no match and is omitted.
         */
        require(
                result.rows.size() == 5,
                "conformance joined row count");

        requireRow(
                result.rows.get(0),
                "A",
                "10",
                "100",
                "conformance row 0");

        requireRow(
                result.rows.get(1),
                "A",
                "10",
                "101",
                "conformance row 1");

        requireRow(
                result.rows.get(2),
                "B",
                "20",
                "200",
                "conformance row 2");

        requireRow(
                result.rows.get(3),
                "A",
                "30",
                "100",
                "conformance row 3");

        requireRow(
                result.rows.get(4),
                "A",
                "30",
                "101",
                "conformance row 4");
    }

    private static void requireRow(
            ExecutionModel.Row row,
            String key,
            String amount,
            String target,
            String message) {

        require(
                row.size() == 3,
                message + " width");

        require(
                row.get(0).equals(key),
                message + " key");

        require(
                ((BigDecimal) row.get(1))
                        .compareTo(
                                new BigDecimal(amount)) == 0,
                message + " amount");

        require(
                ((BigDecimal) row.get(2))
                        .compareTo(
                                new BigDecimal(target)) == 0,
                message + " target");
    }

    private static void testNormalizedDependencyAwareJoinSerializesCanonicalCsv() {
        RawSchema ordersSchema =
                new RawSchema(
                        "orders",
                        "1.0.0",
                        "orders",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true)));

        RawSchema targetsSchema =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "join_key",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        RawSchema joinedSchema =
                new RawSchema(
                        "joined",
                        "1.0.0",
                        "joined",
                        List.of(
                                new RawSchema.Column(
                                        "join_key",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        RawModel.Input orders =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Input targets =
                new RawModel.Input(
                        "targets",
                        "data/targets.csv",
                        "data/targets.schema.json",
                        "csv");

        RawModel.Relation downstream =
                new RawModel.Relation(
                        "downstream",
                        "relations/downstream.json",
                        "json");

        RawModel.Relation upstream =
                new RawModel.Relation(
                        "upstream",
                        "relations/upstream.json",
                        "json");

        RawModel.Output output =
                new RawModel.Output(
                        "joined_output",
                        "downstream",
                        "generated/joined.csv",
                        "expected/joined.csv",
                        "expected/joined.schema.json",
                        "csv");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(
                                downstream,
                                upstream),
                        List.of(output));

        RawModel.Step rename =
                new RawModel.Step(
                        "renamed",
                        "rename",
                        "orders",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of(
                                "category",
                                "join_key"));

        RawModel.RelationDocument upstreamDocument =
                new RawModel.RelationDocument(
                        "upstream",
                        "upstream transform",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(rename),
                        "renamed");

        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "join_key"),
                        "=",
                        RawModel.RawExpression.column(
                                "join_key"));

        RawModel.Step join =
                new RawModel.Step(
                        "joined",
                        "join",
                        null,
                        "upstream",
                        "targets",
                        null,
                        List.of(condition),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument downstreamDocument =
                new RawModel.RelationDocument(
                        "downstream",
                        "dependency-aware join",
                        List.of(
                                new RawModel.Binding(
                                        "upstream",
                                        "relations/upstream.json"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(join),
                        "joined");

        Normalizer.NormalizationResult normalized =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "upstream",
                                upstreamDocument,
                                "downstream",
                                downstreamDocument),
                        Map.of(
                                "data/orders.schema.json",
                                ordersSchema,
                                "data/targets.schema.json",
                                targetsSchema,
                                "expected/joined.schema.json",
                                joinedSchema));

        SemanticModel.Schema ordersNormalized =
                normalized.capsule.inputs
                        .get(0)
                        .schema;

        SemanticModel.Schema targetsNormalized =
                normalized.capsule.inputs
                        .get(1)
                        .schema;

        ExecutionModel.RelationData ordersData =
                new ExecutionModel.RelationData(
                        ordersNormalized,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("10"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new BigDecimal("20"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("30")))));

        ExecutionModel.RelationData targetsData =
                new ExecutionModel.RelationData(
                        targetsNormalized,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("100"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("101"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new BigDecimal("200")))));

        ExecutionModel.RelationData result =
                new WitnessExecutor()
                        .executeCapsule(
                                normalized.capsule,
                                Map.of(
                                        "orders",
                                        ordersData,
                                        "targets",
                                        targetsData))
                        .get("joined_output");

        String csv =
                new String(
                        CsvSerializer.serialize(result),
                        StandardCharsets.UTF_8);

        String expected =
                "join_key,amount,target\n" +
                "A,10,100\n" +
                "A,10,101\n" +
                "B,20,200\n" +
                "A,30,100\n" +
                "A,30,101\n";

        require(
                csv.equals(expected),
                "canonical normalized-execution CSV");
    }
    private static void testNormalizedDependencyAwareJoinWritesOutputFile()
            throws Exception {

        Path tempDirectory =
                Files.createTempDirectory(
                        "v2-conformance-output-");

        Path outputPath =
                tempDirectory.resolve(
                        "nested").resolve(
                        "joined.csv");

        Path expectedPath =
                tempDirectory.resolve(
                        "expected").resolve(
                        "joined.csv");

        RawSchema ordersSchema =
                new RawSchema(
                        "orders",
                        "1.0.0",
                        "orders",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true)));

        RawSchema targetsSchema =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "join_key",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        RawSchema joinedSchema =
                new RawSchema(
                        "joined",
                        "1.0.0",
                        "joined",
                        List.of(
                                new RawSchema.Column(
                                        "join_key",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        RawModel.Input orders =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Input targets =
                new RawModel.Input(
                        "targets",
                        "data/targets.csv",
                        "data/targets.schema.json",
                        "csv");

        RawModel.Relation downstream =
                new RawModel.Relation(
                        "downstream",
                        "relations/downstream.json",
                        "json");

        RawModel.Relation upstream =
                new RawModel.Relation(
                        "upstream",
                        "relations/upstream.json",
                        "json");

        RawModel.Output output =
                new RawModel.Output(
                        "joined_output",
                        "downstream",
                        outputPath.toString(),
                        expectedPath.toString(),
                        "expected/joined.schema.json",
                        "csv");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(
                                downstream,
                                upstream),
                        List.of(output));

        RawModel.Step rename =
                new RawModel.Step(
                        "renamed",
                        "rename",
                        "orders",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of(
                                "category",
                                "join_key"));

        RawModel.RelationDocument upstreamDocument =
                new RawModel.RelationDocument(
                        "upstream",
                        "upstream transform",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(rename),
                        "renamed");

        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "join_key"),
                        "=",
                        RawModel.RawExpression.column(
                                "join_key"));

        RawModel.Step join =
                new RawModel.Step(
                        "joined",
                        "join",
                        null,
                        "upstream",
                        "targets",
                        null,
                        List.of(condition),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument downstreamDocument =
                new RawModel.RelationDocument(
                        "downstream",
                        "dependency-aware join",
                        List.of(
                                new RawModel.Binding(
                                        "upstream",
                                        "relations/upstream.json"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(join),
                        "joined");

        Normalizer.NormalizationResult normalized =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "upstream",
                                upstreamDocument,
                                "downstream",
                                downstreamDocument),
                        Map.of(
                                "data/orders.schema.json",
                                ordersSchema,
                                "data/targets.schema.json",
                                targetsSchema,
                                "expected/joined.schema.json",
                                joinedSchema));

        SemanticModel.Schema ordersNormalized =
                normalized.capsule.inputs
                        .get(0)
                        .schema;

        SemanticModel.Schema targetsNormalized =
                normalized.capsule.inputs
                        .get(1)
                        .schema;

        ExecutionModel.RelationData ordersData =
                new ExecutionModel.RelationData(
                        ordersNormalized,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("10"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new BigDecimal("20"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("30")))));

        ExecutionModel.RelationData targetsData =
                new ExecutionModel.RelationData(
                        targetsNormalized,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("100"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new BigDecimal("101"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new BigDecimal("200")))));

        String expected =
                "join_key,amount,target\n" +
                "A,10.00,100.0\n" +
                "A,10.0,101.00\n" +
                "B,20.000,200\n" +
                "A,30,100.000\n" +
                "A,30.00,101\n";

        Files.createDirectories(
                expectedPath.getParent());

        Files.write(
                expectedPath,
                expected.getBytes(
                        StandardCharsets.UTF_8));

        Map<String,ExecutionModel.RelationData> inputData =
                Map.of(
                        "orders",
                        ordersData,
                        "targets",
                        targetsData);

        WitnessExecutor executor =
                new WitnessExecutor();

        Map<String,ExecutionModel.RelationData> actualOutputs =
                executor.executeCapsule(
                        normalized.capsule,
                        inputData);

        executor.writeOutputs(
                normalized.capsule,
                inputData);

        require(
                Files.exists(outputPath),
                "declared output file exists");

        String generated =
                Files.readString(
                        outputPath,
                        StandardCharsets.UTF_8);

        String canonical =
                "join_key,amount,target\n" +
                "A,10,100\n" +
                "A,10,101\n" +
                "B,20,200\n" +
                "A,30,100\n" +
                "A,30,101\n";

        require(
                generated.equals(canonical),
                "declared output file contents");

        VerificationRunner.verify(
                normalized.verification,
                actualOutputs);
    }
    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
