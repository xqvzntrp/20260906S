import java.util.*;

public final class CapsuleNormalizerTest {

    private CapsuleNormalizerTest() {
    }

    public static void main(String[] args) {
        testValidSchema();
        testUnknownType();
        testDuplicateColumn();
        testEmptyColumnName();
        testInputBindingByPath();
        testInputBindingById();
        testExpectedBindingRejected();
        testUnknownBindingRejected();
        testRelationBindingResolved();
        testForwardRelationDependency();
        testRelationDependencyChain();
        testCyclicRelationDependencyRejected();
        testDependencyAwareJoin();
        testRenameSuccess();
        testRenameUnknownSourceRejected();
        testRenameDuplicateTargetRejected();
        testRenameUntouchedCollisionRejected();
        testFilterSuccess();
        testFilterUnknownColumnRejected();
        testFilterMissingWhereRejected();
        testFilterUnsupportedOperatorRejected();
        testFilterTextNumericRejected();
        testProjectSuccess();
        testProjectUnknownColumnRejected();
        testProjectDuplicateColumnRejected();
        testProjectEmptyRejected();
        testDeriveSuccess();
        testDeriveEmptyRejected();
        testDeriveCollisionRejected();
        testDeriveDuplicateRejected();
        testDeriveUnknownColumnRejected();
        testDeriveFunctionRejected();
        testDeriveConditionalPolicyStatus();
        testDeriveSelectAndOrderBy();
        testDeriveUnknownSelectRejected();
        testDeriveUnknownOrderByRejected();
        testAggregateSumDecimal();
        testAggregateCount();
        testAggregateSumInteger();
        testAggregateMin();
        testAggregateMax();
        testAggregateDuplicateGroupByRejected();
        testAggregateUnknownGroupByRejected();
        testAggregateUnknownMeasureColumnRejected();
        testAggregateEmptyFunctionRejected();
        testAggregateUnsupportedFunctionRejected();
        testAggregateSumTextRejected();
        testAggregateDuplicateOutputRejected();
        testJoinSuccess();
        testJoinInequalityOperatorAccepted();
        testJoinMissingLeftRejected();
        testJoinMissingRightRejected();
        testJoinUnknownLeftRejected();
        testJoinUnknownRightRejected();
        testJoinNoConditionsRejected();
        testJoinUnsupportedOperatorRejected();
        testJoinUnknownLeftColumnRejected();
        testJoinUnknownRightColumnRejected();
        testJoinIncompatibleTypesRejected();
        testJoinAmbiguousNonKeyRejected();
        testJoinMultipleConditions();
        testLeftJoinSuccess();
        testLeftJoinMissingLeftRejected();
        testLeftJoinMissingRightRejected();
        testLeftJoinUnknownLeftRejected();
        testLeftJoinUnknownRightRejected();
        testLeftJoinNoConditionsRejected();
        testLeftJoinUnsupportedOperatorRejected();
        testLeftJoinUnknownLeftColumnRejected();
        testLeftJoinUnknownRightColumnRejected();
        testLeftJoinIncompatibleTypesRejected();
        testLeftJoinAmbiguousNonKeyRejected();
        testCrossJoinSuccess();
        testCrossJoinMissingLeftRejected();
        testCrossJoinMissingRightRejected();
        testCrossJoinUnknownLeftRejected();
        testCrossJoinUnknownRightRejected();
        testCrossJoinOnConditionsRejected();
        testCrossJoinDuplicateColumnRejected();

        System.out.println("CapsuleNormalizerTest PASSED");
    }

    private static void testValidSchema() {
        RawSchema schema =
                new RawSchema(
                        "orders",
                        "1.0.0",
                        "orders",
                        List.of(
                                new RawSchema.Column(
                                        "order_id",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true)));

        /*
         * Exercise normalization through the public
         * normalizer contract using a minimal in-memory
         * package.
         */
        Map<String,RawSchema> schemas =
                Map.of(
                        "data/orders.schema.json", schema,
                        "expected/orders.schema.json", schema);

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "orders_relation",
                        "relations/orders_relation.json",
                        "json");

        RawModel.Output output =
                new RawModel.Output(
                        "orders_output",
                        "orders_relation",
                        "output/orders.csv",
                        "expected/orders.csv",
                        "expected/orders.schema.json",
                        "csv");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of(output));

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "orders_relation",
                        "test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(),
                        "orders");

        CapsuleNormalizer normalizer =
                new CapsuleNormalizer();

        Normalizer.NormalizationResult result =
                normalizer.normalize(
                        capsule,
                        Map.of(
                                "orders_relation",
                                document),
                        schemas);

        SemanticModel.Input normalized =
                result.capsule.inputs.get(0);

        require(
                normalized.schema.columns.size() == 2,
                "valid schema column count");

        require(
                normalized.schema.columns.get(0).type ==
                        SemanticModel.Type.TEXT,
                "order_id type");

        require(
                normalized.schema.columns.get(1).type ==
                        SemanticModel.Type.DECIMAL,
                "amount type");

        require(
                normalized.schema.columns.get(1).nullable,
                "amount nullable");

        require(
                result.verification.expectations.size() == 1,
                "verification expectation count");

        VerificationModel.Expectation expectation =
                result.verification.expectations.get(0);

        require(
                expectation.schema.columns.size() == 2,
                "verification schema column count");

        require(
                expectation.schema.columns.get(0).name.equals(
                        "order_id"),
                "verification schema first column");

        require(
                expectation.schema.columns.get(1).type ==
                        SemanticModel.Type.DECIMAL,
                "verification schema second type");

        require(
                expectation.schema.columns.get(1).nullable,
                "verification schema second nullability");
    }

    private static void testUnknownType() {
        RawSchema schema =
                new RawSchema(
                        "bad",
                        "1.0.0",
                        "bad",
                        List.of(
                                new RawSchema.Column(
                                        "x",
                                        "MONEY",
                                        false)));

        expectFailure(
                schema,
                "unsupported type");
    }

    private static void testDuplicateColumn() {
        RawSchema schema =
                new RawSchema(
                        "bad",
                        "1.0.0",
                        "bad",
                        List.of(
                                new RawSchema.Column(
                                        "x",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "x",
                                        "INTEGER",
                                        false)));

        expectFailure(
                schema,
                "duplicate column");
    }

    private static void testEmptyColumnName() {
        RawSchema schema =
                new RawSchema(
                        "bad",
                        "1.0.0",
                        "bad",
                        List.of(
                                new RawSchema.Column(
                                        "",
                                        "TEXT",
                                        false)));

        expectFailure(
                schema,
                "empty column name");
    }

    private static void testInputBindingByPath() {
        Normalizer.NormalizationResult result =
                normalizeWithBinding(
                        new RawModel.Binding(
                                "orders",
                                "data/orders.csv"));

        SemanticModel.Binding binding =
                result.capsule.relations.get(0)
                      .bindings.get(0);

        require(
                binding.sourceKind ==
                        SemanticModel.SourceKind.INPUT,
                "path binding source kind");

        require(
                binding.sourceId.equals("orders"),
                "path binding source id");

        require(
                binding.schema.columns.size() == 2,
                "path binding schema");
    }

    private static void testInputBindingById() {
        Normalizer.NormalizationResult result =
                normalizeWithBinding(
                        new RawModel.Binding(
                                "orders",
                                "orders"));

        SemanticModel.Binding binding =
                result.capsule.relations.get(0)
                      .bindings.get(0);

        require(
                binding.sourceKind ==
                        SemanticModel.SourceKind.INPUT,
                "id binding source kind");

        require(
                binding.sourceId.equals("orders"),
                "id binding source id");
    }

    private static void testExpectedBindingRejected() {
        expectBindingFailure(
                "expected/orders.csv",
                "expected evidence");
    }

    private static void testUnknownBindingRejected() {
        expectBindingFailure(
                "data/missing.csv",
                "unknown source");
    }

    private static void testRelationBindingResolved() {
        RawSchema schema = validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation upstream =
                new RawModel.Relation(
                        "upstream",
                        "relations/upstream.json",
                        "json");

        RawModel.Relation downstream =
                new RawModel.Relation(
                        "downstream",
                        "relations/downstream.json",
                        "json");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(upstream, downstream),
                        List.of());

        RawModel.RelationDocument upstreamDocument =
                new RawModel.RelationDocument(
                        "upstream",
                        "upstream",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(),
                        "");

        RawModel.RelationDocument downstreamDocument =
                new RawModel.RelationDocument(
                        "downstream",
                        "downstream",
                        List.of(
                                new RawModel.Binding(
                                        "upstream",
                                        "relations/upstream.json")),
                        List.of(),
                        "");

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "upstream",
                                upstreamDocument,
                                "downstream",
                                downstreamDocument),
                        Map.of(
                                "data/orders.schema.json",
                                schema));

        require(
                result.capsule.relations.size() == 2,
                "relation dependency count");

        SemanticModel.Relation normalizedUpstream =
                result.capsule.relations.get(0);

        SemanticModel.Relation normalizedDownstream =
                result.capsule.relations.get(1);

        require(
                normalizedUpstream.id.equals(
                        "upstream"),
                "upstream relation order");

        require(
                normalizedDownstream.id.equals(
                        "downstream"),
                "downstream relation order");

        require(
                normalizedDownstream.dependencies.equals(
                        List.of("upstream")),
                "downstream dependency");

        require(
                normalizedDownstream.bindings.size() == 1,
                "downstream binding count");

        SemanticModel.Binding binding =
                normalizedDownstream.bindings.get(0);

        require(
                binding.name.equals("upstream"),
                "relation binding name");

        require(
                binding.sourceKind ==
                        SemanticModel.SourceKind.RELATION,
                "relation binding source kind");

        require(
                binding.sourceId.equals("upstream"),
                "relation binding source id");

        require(
                binding.schema ==
                        normalizedUpstream.outputSchema,
                "relation binding schema");


    }
    private static void testForwardRelationDependency() {
        RawSchema schema = validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
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

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(downstream, upstream),
                        List.of());

        RawModel.RelationDocument downstreamDocument =
                new RawModel.RelationDocument(
                        "downstream",
                        "downstream",
                        List.of(
                                new RawModel.Binding(
                                        "upstream",
                                        "relations/upstream.json")),
                        List.of(),
                        "");

        RawModel.RelationDocument upstreamDocument =
                new RawModel.RelationDocument(
                        "upstream",
                        "upstream",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(),
                        "");

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "downstream",
                                downstreamDocument,
                                "upstream",
                                upstreamDocument),
                        Map.of(
                                "data/orders.schema.json",
                                schema));

        require(
                result.capsule.relations.get(0).id.equals(
                        "downstream"),
                "forward dependency preserves declaration order");

        require(
                result.capsule.relations.get(1).id.equals(
                        "upstream"),
                "forward dependency upstream order");

        require(
                result.capsule.relations
                        .get(0)
                        .bindings
                        .get(0)
                        .sourceKind ==
                        SemanticModel.SourceKind.RELATION,
                "forward dependency relation binding");

        require(
                result.capsule.relations
                        .get(0)
                        .bindings
                        .get(0)
                        .schema ==
                        result.capsule.relations
                        .get(1)
                        .outputSchema,
                "forward dependency resolved schema");
    }

    private static void testRelationDependencyChain() {
        RawSchema schema = validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relationA =
                new RawModel.Relation(
                        "A",
                        "relations/A.json",
                        "json");

        RawModel.Relation relationB =
                new RawModel.Relation(
                        "B",
                        "relations/B.json",
                        "json");

        RawModel.Relation relationC =
                new RawModel.Relation(
                        "C",
                        "relations/C.json",
                        "json");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relationC, relationA, relationB),
                        List.of());

        RawModel.RelationDocument documentA =
                new RawModel.RelationDocument(
                        "A",
                        "A",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(),
                        "");

        RawModel.RelationDocument documentB =
                new RawModel.RelationDocument(
                        "B",
                        "B",
                        List.of(
                                new RawModel.Binding(
                                        "A",
                                        "relations/A.json")),
                        List.of(),
                        "");

        RawModel.RelationDocument documentC =
                new RawModel.RelationDocument(
                        "C",
                        "C",
                        List.of(
                                new RawModel.Binding(
                                        "B",
                                        "relations/B.json")),
                        List.of(),
                        "");

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "A", documentA,
                                "B", documentB,
                                "C", documentC),
                        Map.of(
                                "data/orders.schema.json",
                                schema));

        require(
                result.capsule.relations.get(0).id.equals("C"),
                "chain declaration order C");

        require(
                result.capsule.relations.get(1).id.equals("A"),
                "chain declaration order A");

        require(
                result.capsule.relations.get(2).id.equals("B"),
                "chain declaration order B");

        require(
                result.capsule.relations
                        .get(0)
                        .dependencies
                        .equals(List.of("B")),
                "C dependency");

        require(
                result.capsule.relations
                        .get(2)
                        .dependencies
                        .equals(List.of("A")),
                "B dependency");

        require(
                result.capsule.relations
                        .get(0)
                        .bindings
                        .get(0)
                        .schema ==
                        result.capsule.relations
                        .get(2)
                        .outputSchema,
                "C resolves B schema");

        require(
                result.capsule.relations
                        .get(2)
                        .bindings
                        .get(0)
                        .schema ==
                        result.capsule.relations
                        .get(1)
                        .outputSchema,
                "B resolves A schema");
    }

    private static void testCyclicRelationDependencyRejected() {
        RawSchema schema = validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation a =
                new RawModel.Relation(
                        "A",
                        "relations/A.json",
                        "json");

        RawModel.Relation b =
                new RawModel.Relation(
                        "B",
                        "relations/B.json",
                        "json");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(a, b),
                        List.of());

        RawModel.RelationDocument aDocument =
                new RawModel.RelationDocument(
                        "A",
                        "A",
                        List.of(
                                new RawModel.Binding(
                                        "B",
                                        "relations/B.json")),
                        List.of(),
                        "");

        RawModel.RelationDocument bDocument =
                new RawModel.RelationDocument(
                        "B",
                        "B",
                        List.of(
                                new RawModel.Binding(
                                        "A",
                                        "relations/A.json")),
                        List.of(),
                        "");

        try {
            new CapsuleNormalizer().normalize(
                    capsule,
                    Map.of(
                            "A", aDocument,
                            "B", bDocument),
                    Map.of(
                            "data/orders.schema.json",
                            schema));

            throw new AssertionError(
                    "Expected cyclic relation dependency failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains("cyclic relation dependency"),
                    "cycle failure message: " +
                    ex.getMessage());
        }
    }
    private static void testDependencyAwareJoin() {
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

        /*
         * Deliberately declare downstream first. This proves
         * recursive dependency normalization and declaration-order
         * preservation at the same time.
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
                        List.of());

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

        Normalizer.NormalizationResult result =
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
                                targetsSchema));

        require(
                result.capsule.relations.size() == 2,
                "dependency join relation count");

        /*
         * Public semantic relation order remains declaration order,
         * even though upstream had to normalize first internally.
         */
        SemanticModel.Relation normalizedDownstream =
                result.capsule.relations.get(0);

        SemanticModel.Relation normalizedUpstream =
                result.capsule.relations.get(1);

        require(
                normalizedDownstream.id.equals(
                        "downstream"),
                "dependency join downstream order");

        require(
                normalizedUpstream.id.equals(
                        "upstream"),
                "dependency join upstream order");

        require(
                normalizedDownstream.dependencies.equals(
                        List.of("upstream")),
                "dependency join dependency list");

        SemanticModel.Binding upstreamBinding =
                normalizedDownstream.bindings.get(0);

        require(
                upstreamBinding.sourceKind ==
                        SemanticModel.SourceKind.RELATION,
                "dependency join relation source kind");

        require(
                upstreamBinding.sourceId.equals(
                        "upstream"),
                "dependency join relation source id");

        require(
                upstreamBinding.schema ==
                        normalizedUpstream.outputSchema,
                "dependency join consumes upstream output schema");

        /*
         * Strong proof that the transformed schema propagated:
         * upstream input had "category"; its output has "join_key".
         */
        require(
                upstreamBinding.schema.columns.get(0).name
                        .equals("join_key"),
                "dependency join transformed key");

        require(
                upstreamBinding.schema.columns.get(1).name
                        .equals("amount"),
                "dependency join transformed schema remainder");

        SemanticModel.Step normalizedJoin =
                normalizedDownstream.steps.get(0);

        require(
                normalizedJoin.operation ==
                        SemanticModel.Operation.JOIN,
                "dependency join operation");

        require(
                normalizedJoin.left.equals(
                        "upstream"),
                "dependency join left");

        require(
                normalizedJoin.right.equals(
                        "targets"),
                "dependency join right");

        require(
                normalizedJoin.conditions.size() == 1,
                "dependency join condition count");

        require(
                normalizedJoin.conditions.get(0)
                        .left.column.equals("join_key"),
                "dependency join left transformed key");

        require(
                normalizedJoin.conditions.get(0)
                        .right.column.equals("join_key"),
                "dependency join right key");

        /*
         * join_key is a same-named equality key and therefore
         * appears once. Left amount and right target follow.
         */
        require(
                normalizedJoin.outputSchema.columns.size() == 3,
                "dependency join output column count");

        require(
                normalizedJoin.outputSchema.columns.get(0)
                        .name.equals("join_key"),
                "dependency join output key");

        require(
                normalizedJoin.outputSchema.columns.get(1)
                        .name.equals("amount"),
                "dependency join output left column");

        require(
                normalizedJoin.outputSchema.columns.get(2)
                        .name.equals("target"),
                "dependency join output right column");

        require(
                normalizedDownstream.outputSchema ==
                        normalizedJoin.outputSchema,
                "dependency join relation output schema");
    }
    private static Normalizer.NormalizationResult normalizeWithBinding(
            RawModel.Binding binding) {

        RawSchema schema = validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "test_relation",
                        "relations/test_relation.json",
                        "json");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "test_relation",
                        "test",
                        List.of(binding),
                        List.of(),
                        "");

        return new CapsuleNormalizer().normalize(
                capsule,
                Map.of("test_relation", document),
                Map.of(
                        "data/orders.schema.json",
                        schema));
    }

    private static void expectBindingFailure(
            String reference,
            String expectedText) {

        try {
            normalizeWithBinding(
                    new RawModel.Binding(
                            "test",
                            reference));

            throw new AssertionError(
                    "Expected binding failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "binding failure message: " +
                    ex.getMessage());
        }
    }

    private static RawSchema validOrdersSchema() {
        return new RawSchema(
                "orders",
                "1.0.0",
                "orders",
                List.of(
                        new RawSchema.Column(
                                "order_id",
                                "TEXT",
                                false),
                        new RawSchema.Column(
                                "amount",
                                "DECIMAL",
                                true)));
    }
    private static void testRenameSuccess() {
        Normalizer.NormalizationResult result =
                normalizeWithRename(
                        orderedRename(
                                "amount",
                                "order_amount"));

        SemanticModel.Relation relation =
                result.capsule.relations.get(0);

        require(
                relation.steps.size() == 1,
                "rename step count");

        SemanticModel.Step step =
                relation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.RENAME,
                "rename operation");

        require(
                step.outputSchema.columns.size() == 2,
                "rename output column count");

        SemanticModel.Column first =
                step.outputSchema.columns.get(0);

        SemanticModel.Column second =
                step.outputSchema.columns.get(1);

        require(
                first.name.equals("order_id"),
                "rename preserves first column");

        require(
                first.type == SemanticModel.Type.TEXT,
                "rename preserves first type");

        require(
                !first.nullable,
                "rename preserves first nullability");

        require(
                second.name.equals("order_amount"),
                "rename changes target name");

        require(
                second.type == SemanticModel.Type.DECIMAL,
                "rename preserves renamed type");

        require(
                second.nullable,
                "rename preserves renamed nullability");

        require(
                relation.outputSchema == step.outputSchema,
                "relation output schema is rename output schema");

        require(
                step.rename.get("amount")
                    .equals("order_amount"),
                "semantic rename mapping");
    }

    private static void testRenameUnknownSourceRejected() {
        expectRenameFailure(
                orderedRename(
                        "missing",
                        "renamed"),
                "unknown source column");
    }

    private static void testRenameDuplicateTargetRejected() {
        LinkedHashMap<String,String> rename =
                new LinkedHashMap<>();

        rename.put(
                "order_id",
                "same");

        rename.put(
                "amount",
                "same");

        expectRenameFailure(
                rename,
                "multiple columns to target");
    }

    private static void testRenameUntouchedCollisionRejected() {
        expectRenameFailure(
                orderedRename(
                        "amount",
                        "order_id"),
                "conflicts with existing unrenamed column");
    }

    private static Normalizer.NormalizationResult normalizeWithRename(
            Map<String,String> rename) {

        RawSchema schema = validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "renamed_orders",
                        "relations/renamed_orders.json",
                        "json");

        RawModel.Step renameStep =
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
                        rename);

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "renamed_orders",
                        "rename test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(renameStep),
                        "renamed");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        return new CapsuleNormalizer().normalize(
                capsule,
                Map.of(
                        "renamed_orders",
                        document),
                Map.of(
                        "data/orders.schema.json",
                        schema));
    }

    private static void expectRenameFailure(
            Map<String,String> rename,
            String expectedText) {

        try {
            normalizeWithRename(rename);

            throw new AssertionError(
                    "Expected RENAME failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "RENAME failure message: " +
                    ex.getMessage());
        }
    }

    private static LinkedHashMap<String,String> orderedRename(
            String source,
            String target) {

        LinkedHashMap<String,String> result =
                new LinkedHashMap<>();

        result.put(source, target);

        return result;
    }
    private static void testFilterSuccess() {
        RawSchema schema =
                validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "filtered_orders",
                        "relations/filtered_orders.json",
                        "json");

        RawModel.RawExpression left =
                RawModel.RawExpression.column(
                        "amount");

        RawModel.RawExpression right =
                RawModel.RawExpression.literal(
                        100);

        RawModel.Condition where =
                new RawModel.Condition(
                        left,
                        ">=",
                        right);

        RawModel.Step filter =
                new RawModel.Step(
                        "filtered",
                        "filter",
                        "orders",
                        null,
                        null,
                        where,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "filtered_orders",
                        "filter test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(filter),
                        "filtered");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "filtered_orders",
                                document),
                        Map.of(
                                "data/orders.schema.json",
                                schema));

        SemanticModel.Input normalizedInput =
                result.capsule.inputs.get(0);

        SemanticModel.Relation normalizedRelation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                normalizedRelation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.FILTER,
                "filter operation");

        require(
                step.conditions.size() == 1,
                "filter condition count");

        SemanticModel.Condition condition =
                step.conditions.get(0);

        require(
                condition.left.kind ==
                        SemanticModel.Expression.Kind.COLUMN,
                "filter left expression kind");

        require(
                condition.right.kind ==
                        SemanticModel.Expression.Kind.LITERAL,
                "filter right expression kind");

        require(
                condition.right.literalType ==
                        SemanticModel.Type.DECIMAL,
                "filter literal contextual type");

        require(
                step.outputSchema ==
                        normalizedInput.schema,
                "filter preserves exact input schema");

        require(
                normalizedRelation.outputSchema ==
                        normalizedInput.schema,
                "filter relation output schema");
    }

    private static void testFilterUnknownColumnRejected() {
        expectFilterFailure(
                RawModel.RawExpression.column("missing"),
                RawModel.RawExpression.literal(100),
                ">=",
                "unknown column");
    }

    private static void testFilterMissingWhereRejected() {
        RawSchema schema =
                validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "filtered_orders",
                        "relations/filtered_orders.json",
                        "json");

        RawModel.Step filter =
                new RawModel.Step(
                        "filtered",
                        "filter",
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
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "filtered_orders",
                        "filter test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(filter),
                        "filtered");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        try {
            new CapsuleNormalizer().normalize(
                    capsule,
                    Map.of(
                            "filtered_orders",
                            document),
                    Map.of(
                            "data/orders.schema.json",
                            schema));

            throw new AssertionError(
                    "Expected missing FILTER where failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains("filter requires where"),
                    "missing where message");
        }
    }

    private static void testFilterUnsupportedOperatorRejected() {
        expectFilterFailure(
                RawModel.RawExpression.column("amount"),
                RawModel.RawExpression.literal(100),
                "LIKE",
                "unsupported comparison operator");
    }

    private static void testFilterTextNumericRejected() {
        expectFilterFailure(
                RawModel.RawExpression.column("amount"),
                RawModel.RawExpression.literal("100"),
                ">=",
                "must be numeric");
    }

    private static void expectFilterFailure(
            RawModel.RawExpression left,
            RawModel.RawExpression right,
            String operator,
            String expectedText) {

        RawSchema schema =
                validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "filtered_orders",
                        "relations/filtered_orders.json",
                        "json");

        RawModel.Condition where =
                new RawModel.Condition(
                        left,
                        operator,
                        right);

        RawModel.Step filter =
                new RawModel.Step(
                        "filtered",
                        "filter",
                        "orders",
                        null,
                        null,
                        where,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "filtered_orders",
                        "filter test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(filter),
                        "filtered");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        try {
            new CapsuleNormalizer().normalize(
                    capsule,
                    Map.of(
                            "filtered_orders",
                            document),
                    Map.of(
                            "data/orders.schema.json",
                            schema));

            throw new AssertionError(
                    "Expected FILTER failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "FILTER failure message: " +
                    ex.getMessage());
        }
    }
    private static void testProjectSuccess() {
        Normalizer.NormalizationResult result =
                normalizeWithProject(
                        List.of(
                                "amount",
                                "order_id"));

        SemanticModel.Input input =
                result.capsule.inputs.get(0);

        SemanticModel.Relation relation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                relation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.PROJECT,
                "project operation");

        require(
                step.outputSchema != input.schema,
                "project creates new schema");

        require(
                step.outputSchema.columns.size() == 2,
                "project output column count");

        require(
                step.outputSchema.columns.get(0).name
                        .equals("amount"),
                "project first column");

        require(
                step.outputSchema.columns.get(1).name
                        .equals("order_id"),
                "project second column");

        require(
                step.outputSchema.columns.get(0) ==
                        input.schema.columns.get(1),
                "project reuses immutable amount column");

        require(
                step.outputSchema.columns.get(1) ==
                        input.schema.columns.get(0),
                "project reuses immutable order_id column");

        require(
                step.outputSchema.columns.get(0).type ==
                        SemanticModel.Type.DECIMAL,
                "project preserves amount type");

        require(
                step.outputSchema.columns.get(0).nullable,
                "project preserves amount nullability");

        require(
                relation.outputSchema ==
                        step.outputSchema,
                "project relation output schema");
    }

    private static void testProjectUnknownColumnRejected() {
        expectProjectFailure(
                List.of("missing"),
                "unknown column");
    }

    private static void testProjectDuplicateColumnRejected() {
        expectProjectFailure(
                List.of(
                        "amount",
                        "amount"),
                "duplicate column");
    }

    private static void testProjectEmptyRejected() {
        expectProjectFailure(
                List.of(),
                "requires at least one column");
    }

    private static Normalizer.NormalizationResult normalizeWithProject(
            List<String> select) {

        RawSchema schema =
                validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "projected_orders",
                        "relations/projected_orders.json",
                        "json");

        RawModel.Step project =
                new RawModel.Step(
                        "projected",
                        "project",
                        "orders",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        select,
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "projected_orders",
                        "project test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(project),
                        "projected");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        return new CapsuleNormalizer().normalize(
                capsule,
                Map.of(
                        "projected_orders",
                        document),
                Map.of(
                        "data/orders.schema.json",
                        schema));
    }

    private static void expectProjectFailure(
            List<String> select,
            String expectedText) {

        try {
            normalizeWithProject(select);

            throw new AssertionError(
                    "Expected PROJECT failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "PROJECT failure message: " +
                    ex.getMessage());
        }
    }
    private static void testDeriveSuccess() {
        Normalizer.NormalizationResult result =
                normalizeWithDerive(
                        List.of(
                                new RawModel.DerivedColumn(
                                        "total_amount",
                                        RawModel.RawExpression.column(
                                                "amount"))));

        SemanticModel.Input input =
                result.capsule.inputs.get(0);

        SemanticModel.Relation relation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                relation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.DERIVE,
                "derive operation");

        require(
                step.outputSchema != input.schema,
                "derive creates new schema");

        require(
                step.outputSchema.columns.size() ==
                        input.schema.columns.size() + 1,
                "derive appends one column");

        SemanticModel.Column derived =
                step.outputSchema.columns.get(
                        step.outputSchema.columns.size() - 1);

        require(
                derived.name.equals("total_amount"),
                "derive output name");

        require(
                derived.type ==
                        SemanticModel.Type.DECIMAL,
                "derive output type");

        require(
                derived.nullable,
                "derive output nullability");

        require(
                step.derivedColumns.size() == 1,
                "derive semantic column count");

        require(
                step.derivedColumns.get(0).expression.kind ==
                        SemanticModel.Expression.Kind.COLUMN,
                "derive semantic expression");

        require(
                relation.outputSchema ==
                        step.outputSchema,
                "derive relation output schema");
    }

    private static void testDeriveEmptyRejected() {
        expectDeriveFailure(
                List.of(),
                "requires at least one column");
    }

    private static void testDeriveCollisionRejected() {
        expectDeriveFailure(
                List.of(
                        new RawModel.DerivedColumn(
                                "amount",
                                RawModel.RawExpression.column(
                                        "order_id"))),
                "conflicts with column");
    }

    private static void testDeriveDuplicateRejected() {
        expectDeriveFailure(
                List.of(
                        new RawModel.DerivedColumn(
                                "x",
                                RawModel.RawExpression.column(
                                        "amount")),
                        new RawModel.DerivedColumn(
                                "x",
                                RawModel.RawExpression.column(
                                        "order_id"))),
                "conflicts with column");
    }

    private static void testDeriveUnknownColumnRejected() {
        expectDeriveFailure(
                List.of(
                        new RawModel.DerivedColumn(
                                "x",
                                RawModel.RawExpression.column(
                                        "missing"))),
                "unknown column");
    }

    private static void testDeriveFunctionRejected() {
        RawModel.RawExpression function =
                new RawModel.RawExpression(
                        RawModel.RawExpression.Kind.FUNCTION,
                        null,
                        null,
                        "unsupported",
                        List.of(),
                        null,
                        null,
                        null);

        expectDeriveFailure(
                List.of(
                        new RawModel.DerivedColumn(
                                "x",
                                function)),
                "unsupported scalar function");
    }

    private static void testDeriveConditionalPolicyStatus() {
        RawSchema schema =
                new RawSchema(
                        "joined",
                        "1.0.0",
                        "joined",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "total_amount",
                                        "DECIMAL",
                                        false),
                                new RawSchema.Column(
                                        "target_amount",
                                        "DECIMAL",
                                        false)));

        RawModel.Input input =
                new RawModel.Input(
                        "joined",
                        "data/joined.csv",
                        "data/joined.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "decision_relation",
                        "relations/decision_relation.json",
                        "json");

        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "total_amount"),
                        ">=",
                        RawModel.RawExpression.column(
                                "target_amount"));

        RawModel.RawExpression expression =
                RawModel.RawExpression.conditional(
                        condition,
                        RawModel.RawExpression.literal(
                                "MEETS_TARGET"),
                        RawModel.RawExpression.literal(
                                "BELOW_TARGET"));

        RawModel.Step derive =
                new RawModel.Step(
                        "decision",
                        "derive",
                        "joined",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new RawModel.DerivedColumn(
                                        "policy_status",
                                        expression)),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "decision_relation",
                        "policy decision test",
                        List.of(
                                new RawModel.Binding(
                                        "joined",
                                        "data/joined.csv")),
                        List.of(derive),
                        "decision");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "decision_relation",
                                document),
                        Map.of(
                                "data/joined.schema.json",
                                schema));

        SemanticModel.Step step =
                result.capsule.relations
                        .get(0)
                        .steps
                        .get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.DERIVE,
                "conditional derive operation");

        require(
                step.derivedColumns.size() == 1,
                "conditional derive column count");

        SemanticModel.Expression normalized =
                step.derivedColumns
                        .get(0)
                        .expression;

        require(
                normalized.kind ==
                        SemanticModel.Expression.Kind.CONDITIONAL,
                "conditional derive expression kind");

        require(
                normalized.condition.operator.equals(">="),
                "conditional comparison operator");

        require(
                normalized.condition.left.column
                        .equals("total_amount"),
                "conditional left column");

        require(
                normalized.condition.right.column
                        .equals("target_amount"),
                "conditional right column");

        require(
                normalized.thenBranch.literal
                        .equals("MEETS_TARGET"),
                "conditional then value");

        require(
                normalized.thenBranch.literalType ==
                        SemanticModel.Type.TEXT,
                "conditional then type");

        require(
                normalized.elseBranch.literal
                        .equals("BELOW_TARGET"),
                "conditional else value");

        require(
                normalized.elseBranch.literalType ==
                        SemanticModel.Type.TEXT,
                "conditional else type");

        SemanticModel.Column policyStatus =
                step.outputSchema.columns.get(
                        step.outputSchema.columns.size() - 1);

        require(
                policyStatus.name.equals(
                        "policy_status"),
                "conditional derived column name");

        require(
                policyStatus.type ==
                        SemanticModel.Type.TEXT,
                "conditional derived column type");

        require(
                !policyStatus.nullable,
                "conditional derived column non-nullable");

        require(
                result.capsule.relations
                        .get(0)
                        .outputSchema ==
                        step.outputSchema,
                "conditional derive relation output schema");
    }

    private static void testDeriveSelectAndOrderBy() {
        Normalizer.NormalizationResult result =
                normalizeWithDerive(
                        List.of(
                                new RawModel.DerivedColumn(
                                        "total_amount",
                                        RawModel.RawExpression.column(
                                                "amount"))),
                        List.of("amount", "total_amount"),
                        List.of("amount"));

        SemanticModel.Step step =
                result.capsule.relations.get(0).steps.get(0);

        require(
                step.outputSchema.columns.size() == 2,
                "derive select output column count");

        require(
                step.outputSchema.columns.get(0).name.equals("amount") &&
                step.outputSchema.columns.get(1).name.equals("total_amount"),
                "derive select output columns");

        require(
                step.select.equals(List.of("amount", "total_amount")),
                "derive preserves select");

        require(
                step.orderBy.equals(List.of("amount")),
                "derive preserves order_by");
    }

    private static void testDeriveUnknownSelectRejected() {
        expectDeriveFailure(
                List.of(
                        new RawModel.DerivedColumn(
                                "total_amount",
                                RawModel.RawExpression.column("amount"))),
                List.of("missing"),
                List.of(),
                "unknown column");
    }

    private static void testDeriveUnknownOrderByRejected() {
        expectDeriveFailure(
                List.of(
                        new RawModel.DerivedColumn(
                                "total_amount",
                                RawModel.RawExpression.column("amount"))),
                List.of("amount"),
                List.of("missing"),
                "unknown column");
    }

    private static Normalizer.NormalizationResult normalizeWithDerive(
            List<RawModel.DerivedColumn> columns) {

        return normalizeWithDerive(
                columns,
                List.of(),
                List.of());
    }

    private static Normalizer.NormalizationResult normalizeWithDerive(
            List<RawModel.DerivedColumn> columns,
            List<String> select,
            List<String> orderBy) {

        RawSchema schema =
                validOrdersSchema();

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "derived_orders",
                        "relations/derived_orders.json",
                        "json");

        RawModel.Step derive =
                new RawModel.Step(
                        "derived",
                        "derive",
                        "orders",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        columns,
                        select,
                        orderBy,
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "derived_orders",
                        "derive test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(derive),
                        "derived");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        return new CapsuleNormalizer().normalize(
                capsule,
                Map.of(
                        "derived_orders",
                        document),
                Map.of(
                        "data/orders.schema.json",
                        schema));
    }

    private static void expectDeriveFailure(
            List<RawModel.DerivedColumn> columns,
            String expectedText) {

        expectDeriveFailure(
                columns,
                List.of(),
                List.of(),
                expectedText);
    }

    private static void expectDeriveFailure(
            List<RawModel.DerivedColumn> columns,
            List<String> select,
            List<String> orderBy,
            String expectedText) {

        try {
            normalizeWithDerive(columns, select, orderBy);

            throw new AssertionError(
                    "Expected DERIVE failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "DERIVE failure message: " +
                    ex.getMessage());
        }
    }
    private static void testAggregateSumDecimal() {
        RawSchema schema =
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

        RawModel.Input input =
                new RawModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "category_summary",
                        "relations/category_summary.json",
                        "json");

        RawModel.Step aggregate =
                new RawModel.Step(
                        "summary",
                        "aggregate",
                        "orders",
                        null,
                        null,
                        null,
                        List.of(),
                        List.of("category"),
                        List.of(
                                new RawModel.Measure(
                                        "total_amount",
                                        "sum",
                                        "amount")),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "category_summary",
                        "aggregate test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv")),
                        List.of(aggregate),
                        "summary");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "category_summary",
                                document),
                        Map.of(
                                "data/orders.schema.json",
                                schema));

        SemanticModel.Relation normalizedRelation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                normalizedRelation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.AGGREGATE,
                "aggregate operation");

        require(
                step.groupBy.equals(
                        List.of("category")),
                "aggregate group_by");

        require(
                step.measures.size() == 1,
                "aggregate measure count");

        SemanticModel.Measure measure =
                step.measures.get(0);

        require(
                measure.name.equals("total_amount"),
                "aggregate measure name");

        require(
                measure.function.equals("SUM"),
                "aggregate normalized function");

        require(
                measure.column.equals("amount"),
                "aggregate measure column");

        require(
                step.outputSchema.columns.size() == 2,
                "aggregate output column count");

        SemanticModel.Column category =
                step.outputSchema.columns.get(0);

        SemanticModel.Column total =
                step.outputSchema.columns.get(1);

        require(
                category.name.equals("category"),
                "aggregate group column order");

        require(
                category.type ==
                        SemanticModel.Type.TEXT,
                "aggregate group column type");

        require(
                !category.nullable,
                "aggregate group column nullability");

        require(
                total.name.equals("total_amount"),
                "aggregate measure output order");

        require(
                total.type ==
                        SemanticModel.Type.DECIMAL,
                "aggregate SUM decimal type");

        require(
                total.nullable,
                "aggregate SUM nullable");

        require(
                normalizedRelation.outputSchema ==
                        step.outputSchema,
                "aggregate relation output schema");
    }
    private static RawSchema aggregateSchema() {
        return new RawSchema(
                "aggregate_source",
                "1.0.0",
                "aggregate_source",
                List.of(
                        new RawSchema.Column(
                                "category",
                                "TEXT",
                                false),
                        new RawSchema.Column(
                                "amount",
                                "DECIMAL",
                                true),
                        new RawSchema.Column(
                                "quantity",
                                "INTEGER",
                                true)));
    }

    private static Normalizer.NormalizationResult normalizeAggregate(
            List<String> groupBy,
            List<RawModel.Measure> measures) {

        RawModel.Input input =
                new RawModel.Input(
                        "source",
                        "data/source.csv",
                        "data/source.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "aggregate_relation",
                        "relations/aggregate_relation.json",
                        "json");

        RawModel.Step aggregate =
                new RawModel.Step(
                        "aggregate_step",
                        "aggregate",
                        "source",
                        null,
                        null,
                        null,
                        List.of(),
                        groupBy,
                        measures,
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "aggregate_relation",
                        "aggregate normalization test",
                        List.of(
                                new RawModel.Binding(
                                        "source",
                                        "data/source.csv")),
                        List.of(aggregate),
                        "aggregate_step");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of());

        return new CapsuleNormalizer().normalize(
                capsule,
                Map.of(
                        "aggregate_relation",
                        document),
                Map.of(
                        "data/source.schema.json",
                        aggregateSchema()));
    }

    private static SemanticModel.Step aggregateStep(
            List<String> groupBy,
            List<RawModel.Measure> measures) {

        return normalizeAggregate(
                groupBy,
                measures)
                .capsule
                .relations
                .get(0)
                .steps
                .get(0);
    }

    private static void testAggregateCount() {
        SemanticModel.Step step =
                aggregateStep(
                        List.of("category"),
                        List.of(
                                new RawModel.Measure(
                                        "row_count",
                                        "count",
                                        "amount")));

        SemanticModel.Column result =
                step.outputSchema.columns.get(1);

        require(
                step.measures.get(0).function.equals("COUNT"),
                "aggregate COUNT normalized function");

        require(
                result.name.equals("row_count"),
                "aggregate COUNT output name");

        require(
                result.type ==
                        SemanticModel.Type.INTEGER,
                "aggregate COUNT type");

        require(
                !result.nullable,
                "aggregate COUNT non-nullable");
    }

    private static void testAggregateSumInteger() {
        SemanticModel.Step step =
                aggregateStep(
                        List.of("category"),
                        List.of(
                                new RawModel.Measure(
                                        "total_quantity",
                                        "sum",
                                        "quantity")));

        SemanticModel.Column result =
                step.outputSchema.columns.get(1);

        require(
                result.type ==
                        SemanticModel.Type.INTEGER,
                "aggregate SUM integer type");

        require(
                result.nullable,
                "aggregate SUM integer nullable");
    }

    private static void testAggregateMin() {
        SemanticModel.Step step =
                aggregateStep(
                        List.of("category"),
                        List.of(
                                new RawModel.Measure(
                                        "minimum_amount",
                                        "min",
                                        "amount")));

        SemanticModel.Column result =
                step.outputSchema.columns.get(1);

        require(
                step.measures.get(0).function.equals("MIN"),
                "aggregate MIN normalized function");

        require(
                result.type ==
                        SemanticModel.Type.DECIMAL,
                "aggregate MIN preserves source type");

        require(
                result.nullable,
                "aggregate MIN nullable");
    }

    private static void testAggregateMax() {
        SemanticModel.Step step =
                aggregateStep(
                        List.of("category"),
                        List.of(
                                new RawModel.Measure(
                                        "maximum_category",
                                        "max",
                                        "category")));

        SemanticModel.Column result =
                step.outputSchema.columns.get(1);

        require(
                step.measures.get(0).function.equals("MAX"),
                "aggregate MAX normalized function");

        require(
                result.type ==
                        SemanticModel.Type.TEXT,
                "aggregate MAX preserves source type");

        require(
                result.nullable,
                "aggregate MAX nullable");
    }

    private static void testAggregateDuplicateGroupByRejected() {
        expectAggregateFailure(
                List.of(
                        "category",
                        "category"),
                List.of(
                        new RawModel.Measure(
                                "total_amount",
                                "sum",
                                "amount")),
                "duplicate group_by");
    }

    private static void testAggregateUnknownGroupByRejected() {
        expectAggregateFailure(
                List.of("missing"),
                List.of(
                        new RawModel.Measure(
                                "total_amount",
                                "sum",
                                "amount")),
                "unknown group_by column");
    }

    private static void testAggregateUnknownMeasureColumnRejected() {
        expectAggregateFailure(
                List.of("category"),
                List.of(
                        new RawModel.Measure(
                                "total",
                                "sum",
                                "missing")),
                "unknown column");
    }

    private static void testAggregateEmptyFunctionRejected() {
        expectAggregateFailure(
                List.of("category"),
                List.of(
                        new RawModel.Measure(
                                "total_amount",
                                "",
                                "amount")),
                "empty function");
    }

    private static void testAggregateUnsupportedFunctionRejected() {
        expectAggregateFailure(
                List.of("category"),
                List.of(
                        new RawModel.Measure(
                                "average_amount",
                                "average",
                                "amount")),
                "unsupported measure function");
    }

    private static void testAggregateSumTextRejected() {
        expectAggregateFailure(
                List.of("category"),
                List.of(
                        new RawModel.Measure(
                                "bad_sum",
                                "sum",
                                "category")),
                "SUM requires numeric column");
    }

    private static void testAggregateDuplicateOutputRejected() {
        expectAggregateFailure(
                List.of("category"),
                List.of(
                        new RawModel.Measure(
                                "category",
                                "count",
                                "amount")),
                "duplicates output column");
    }

    private static void expectAggregateFailure(
            List<String> groupBy,
            List<RawModel.Measure> measures,
            String expectedText) {

        try {
            normalizeAggregate(
                    groupBy,
                    measures);

            throw new AssertionError(
                    "Expected AGGREGATE failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "AGGREGATE failure message: " +
                    ex.getMessage());
        }
    }
    private static RawSchema joinLeftSchema() {
        return new RawSchema(
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
    }

    private static RawSchema joinRightSchema() {
        return new RawSchema(
                "targets",
                "1.0.0",
                "targets",
                List.of(
                        new RawSchema.Column(
                                "category",
                                "TEXT",
                                false),
                        new RawSchema.Column(
                                "target",
                                "DECIMAL",
                                false)));
    }

    private static Normalizer.NormalizationResult normalizeJoin(
            String left,
            String right,
            List<RawModel.Condition> on,
            RawSchema leftSchema,
            RawSchema rightSchema) {

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

        RawModel.Relation relation =
                new RawModel.Relation(
                        "joined",
                        "relations/joined.json",
                        "json");

        RawModel.Step join =
                new RawModel.Step(
                        "joined_step",
                        "join",
                        null,
                        left,
                        right,
                        null,
                        on,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "joined",
                        "join normalization test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(join),
                        "joined_step");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(relation),
                        List.of());

        return new CapsuleNormalizer().normalize(
                capsule,
                Map.of(
                        "joined",
                        document),
                Map.of(
                        "data/orders.schema.json",
                        leftSchema,
                        "data/targets.schema.json",
                        rightSchema));
    }

    private static RawModel.Condition categoryJoinCondition() {
        return new RawModel.Condition(
                RawModel.RawExpression.column(
                        "category"),
                "=",
                RawModel.RawExpression.column(
                        "category"));
    }

    private static void testJoinSuccess() {
        Normalizer.NormalizationResult result =
                normalizeJoin(
                        "orders",
                        "targets",
                        List.of(
                                categoryJoinCondition()),
                        joinLeftSchema(),
                        joinRightSchema());

        SemanticModel.Relation relation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                relation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.JOIN,
                "join operation");

        require(
                step.left.equals("orders"),
                "join left");

        require(
                step.right.equals("targets"),
                "join right");

        require(
                step.conditions.size() == 1,
                "join condition count");

        SemanticModel.Condition condition =
                step.conditions.get(0);

        require(
                condition.operator.equals("="),
                "join condition operator");

        require(
                condition.left.column.equals(
                        "category"),
                "join left key");

        require(
                condition.right.column.equals(
                        "category"),
                "join right key");

        require(
                step.outputSchema.columns.size() == 3,
                "join output column count");

        require(
                step.outputSchema.columns.get(0).name
                        .equals("category"),
                "join key output order");

        require(
                step.outputSchema.columns.get(1).name
                        .equals("amount"),
                "join left output order");

        require(
                step.outputSchema.columns.get(2).name
                        .equals("target"),
                "join right output order");

        require(
                step.outputSchema.columns.get(0) ==
                        relation.bindings.get(0)
                                .schema.columns.get(0),
                "join retains left key definition");

        require(
                relation.outputSchema ==
                        step.outputSchema,
                "join relation output schema");
    }

    private static void testJoinMissingLeftRejected() {
        expectJoinFailure(
                null,
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "requires left");
    }

    private static void testJoinMissingRightRejected() {
        expectJoinFailure(
                "orders",
                null,
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "requires right");
    }

    private static void testJoinUnknownLeftRejected() {
        expectJoinFailure(
                "missing",
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown left source");
    }

    private static void testJoinUnknownRightRejected() {
        expectJoinFailure(
                "orders",
                "missing",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown right source");
    }

    private static void testJoinNoConditionsRejected() {
        expectJoinFailure(
                "orders",
                "targets",
                List.of(),
                joinLeftSchema(),
                joinRightSchema(),
                "requires at least one on condition");
    }

    private static void testJoinUnsupportedOperatorRejected() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "category"),
                        "LIKE",
                        RawModel.RawExpression.column(
                                "category"));

        expectJoinFailure(
                "orders",
                "targets",
                List.of(condition),
                joinLeftSchema(),
                joinRightSchema(),
                "unsupported comparison operator");
    }

    private static void testJoinInequalityOperatorAccepted() {
        RawSchema leftSchema =
                new RawSchema(
                        "aggregate_arr",
                        "1.0.0",
                        "aggregate_arr",
                        List.of(
                                new RawSchema.Column(
                                        "aggregate_arr_usd",
                                        "DECIMAL",
                                        false)));

        RawSchema rightSchema =
                new RawSchema(
                        "amd_thresholds",
                        "1.0.0",
                        "amd_thresholds",
                        List.of(
                                new RawSchema.Column(
                                        "minimum_aggregate_arr_usd",
                                        "DECIMAL",
                                        false)));

        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "aggregate_arr_usd"),
                        ">=",
                        RawModel.RawExpression.column(
                                "minimum_aggregate_arr_usd"));

        Normalizer.NormalizationResult result =
                normalizeJoin(
                        "orders",
                        "targets",
                        List.of(condition),
                        leftSchema,
                        rightSchema);

        require(
                result.capsule.relations.get(0).steps.get(0)
                        .conditions.get(0).operator.equals(">="),
                "JOIN retains inequality operator");
    }

    private static void testJoinUnknownLeftColumnRejected() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "missing"),
                        "=",
                        RawModel.RawExpression.column(
                                "category"));

        expectJoinFailure(
                "orders",
                "targets",
                List.of(condition),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown column");
    }

    private static void testJoinUnknownRightColumnRejected() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "category"),
                        "=",
                        RawModel.RawExpression.column(
                                "missing"));

        expectJoinFailure(
                "orders",
                "targets",
                List.of(condition),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown column");
    }

    private static void testJoinIncompatibleTypesRejected() {
        RawSchema right =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "BOOLEAN",
                                        false),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        expectJoinFailure(
                "orders",
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                right,
                "incompatible");
    }

    private static void testJoinAmbiguousNonKeyRejected() {
        RawSchema right =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true)));

        expectJoinFailure(
                "orders",
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                right,
                "ambiguous non-key output column");
    }

    private static void expectJoinFailure(
            String left,
            String right,
            List<RawModel.Condition> on,
            RawSchema leftSchema,
            RawSchema rightSchema,
            String expectedText) {

        try {
            normalizeJoin(
                    left,
                    right,
                    on,
                    leftSchema,
                    rightSchema);

            throw new AssertionError(
                    "Expected JOIN failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "JOIN failure message: " +
                    ex.getMessage());
        }
    }
    private static void testJoinMultipleConditions() {
        RawModel.Condition category =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "category"),
                        "=",
                        RawModel.RawExpression.column(
                                "category"));

        RawModel.Condition amount =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "amount"),
                        "=",
                        RawModel.RawExpression.column(
                                "target"));

        Normalizer.NormalizationResult result =
                normalizeJoin(
                        "orders",
                        "targets",
                        List.of(
                                category,
                                amount),
                        joinLeftSchema(),
                        joinRightSchema());

        SemanticModel.Step step =
                result.capsule.relations
                        .get(0)
                        .steps
                        .get(0);

        require(
                step.conditions.size() == 2,
                "join multiple condition count");

        SemanticModel.Condition first =
                step.conditions.get(0);

        SemanticModel.Condition second =
                step.conditions.get(1);

        require(
                first.left.column.equals(
                        "category"),
                "join first condition left");

        require(
                first.right.column.equals(
                        "category"),
                "join first condition right");

        require(
                first.operator.equals("="),
                "join first condition operator");

        require(
                second.left.column.equals(
                        "amount"),
                "join second condition left");

        require(
                second.right.column.equals(
                        "target"),
                "join second condition right");

        require(
                second.operator.equals("="),
                "join second condition operator");
    }
    private static void testLeftJoinSuccess() {
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

        RawModel.Relation relation =
                new RawModel.Relation(
                        "left_joined",
                        "relations/left_joined.json",
                        "json");

        RawModel.Step join =
                new RawModel.Step(
                        "left_joined_step",
                        "left_join",
                        null,
                        "orders",
                        "targets",
                        null,
                        List.of(
                                categoryJoinCondition()),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "left_joined",
                        "left join normalization test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(join),
                        "left_joined_step");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(relation),
                        List.of());

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "left_joined",
                                document),
                        Map.of(
                                "data/orders.schema.json",
                                joinLeftSchema(),
                                "data/targets.schema.json",
                                joinRightSchema()));

        SemanticModel.Relation normalizedRelation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                normalizedRelation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.LEFT_JOIN,
                "left join operation");

        require(
                step.left.equals("orders"),
                "left join left");

        require(
                step.right.equals("targets"),
                "left join right");

        require(
                step.conditions.size() == 1,
                "left join condition count");

        require(
                step.outputSchema.columns.size() == 3,
                "left join output column count");

        SemanticModel.Column key =
                step.outputSchema.columns.get(0);

        SemanticModel.Column amount =
                step.outputSchema.columns.get(1);

        SemanticModel.Column target =
                step.outputSchema.columns.get(2);

        require(
                key.name.equals("category"),
                "left join key name");

        require(
                !key.nullable,
                "left join preserves left key nullability");

        require(
                amount.name.equals("amount"),
                "left join left column name");

        require(
                amount.nullable,
                "left join preserves nullable left column");

        require(
                target.name.equals("target"),
                "left join right column name");

        require(
                target.type ==
                        SemanticModel.Type.DECIMAL,
                "left join right column type");

        require(
                target.nullable,
                "left join widens right column nullability");

        /*
         * The right source declares target NOT NULL.
         * LEFT_JOIN must not reuse that Column object because its
         * output form is nullable.
         */
        require(
                target !=
                        normalizedRelation.bindings
                                .get(1)
                                .schema.columns.get(1),
                "left join creates widened right column");

        require(
                !normalizedRelation.bindings
                        .get(1)
                        .schema.columns.get(1)
                        .nullable,
                "left join right source remains non-nullable");

        require(
                normalizedRelation.outputSchema ==
                        step.outputSchema,
                "left join relation output schema");
    }
    private static void expectLeftJoinFailure(
            String left,
            String right,
            List<RawModel.Condition> on,
            RawSchema leftSchema,
            RawSchema rightSchema,
            String expectedText) {

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

        RawModel.Relation relation =
                new RawModel.Relation(
                        "left_joined",
                        "relations/left_joined.json",
                        "json");

        RawModel.Step step =
                new RawModel.Step(
                        "left_joined_step",
                        "left_join",
                        null,
                        left,
                        right,
                        null,
                        on,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "left_joined",
                        "left join rejection test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(step),
                        "left_joined_step");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(relation),
                        List.of());

        try {
            new CapsuleNormalizer().normalize(
                    capsule,
                    Map.of(
                            "left_joined",
                            document),
                    Map.of(
                            "data/orders.schema.json",
                            leftSchema,
                            "data/targets.schema.json",
                            rightSchema));

            throw new AssertionError(
                    "Expected LEFT_JOIN failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "LEFT_JOIN failure message: " +
                    ex.getMessage());
        }
    }

    private static void testLeftJoinMissingLeftRejected() {
        expectLeftJoinFailure(
                null,
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "requires left");
    }

    private static void testLeftJoinMissingRightRejected() {
        expectLeftJoinFailure(
                "orders",
                null,
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "requires right");
    }

    private static void testLeftJoinUnknownLeftRejected() {
        expectLeftJoinFailure(
                "missing",
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown left source");
    }

    private static void testLeftJoinUnknownRightRejected() {
        expectLeftJoinFailure(
                "orders",
                "missing",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown right source");
    }

    private static void testLeftJoinNoConditionsRejected() {
        expectLeftJoinFailure(
                "orders",
                "targets",
                List.of(),
                joinLeftSchema(),
                joinRightSchema(),
                "requires at least one on condition");
    }

    private static void testLeftJoinUnsupportedOperatorRejected() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "category"),
                        "LIKE",
                        RawModel.RawExpression.column(
                                "category"));

        expectLeftJoinFailure(
                "orders",
                "targets",
                List.of(condition),
                joinLeftSchema(),
                joinRightSchema(),
                "unsupported comparison operator");
    }

    private static void testLeftJoinUnknownLeftColumnRejected() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "missing"),
                        "=",
                        RawModel.RawExpression.column(
                                "category"));

        expectLeftJoinFailure(
                "orders",
                "targets",
                List.of(condition),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown column");
    }

    private static void testLeftJoinUnknownRightColumnRejected() {
        RawModel.Condition condition =
                new RawModel.Condition(
                        RawModel.RawExpression.column(
                                "category"),
                        "=",
                        RawModel.RawExpression.column(
                                "missing"));

        expectLeftJoinFailure(
                "orders",
                "targets",
                List.of(condition),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown column");
    }

    private static void testLeftJoinIncompatibleTypesRejected() {
        RawSchema right =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "BOOLEAN",
                                        false),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        expectLeftJoinFailure(
                "orders",
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                right,
                "incompatible");
    }

    private static void testLeftJoinAmbiguousNonKeyRejected() {
        RawSchema right =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "amount",
                                        "DECIMAL",
                                        true)));

        expectLeftJoinFailure(
                "orders",
                "targets",
                List.of(categoryJoinCondition()),
                joinLeftSchema(),
                right,
                "ambiguous non-key output column");
    }
    private static void testCrossJoinSuccess() {
        RawSchema rightSchema =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false),
                                new RawSchema.Column(
                                        "priority",
                                        "INTEGER",
                                        true)));

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

        RawModel.Relation relation =
                new RawModel.Relation(
                        "cross_joined",
                        "relations/cross_joined.json",
                        "json");

        RawModel.Step join =
                new RawModel.Step(
                        "cross_joined_step",
                        "cross_join",
                        null,
                        "orders",
                        "targets",
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "cross_joined",
                        "cross join normalization test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(join),
                        "cross_joined_step");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(relation),
                        List.of());

        Normalizer.NormalizationResult result =
                new CapsuleNormalizer().normalize(
                        capsule,
                        Map.of(
                                "cross_joined",
                                document),
                        Map.of(
                                "data/orders.schema.json",
                                joinLeftSchema(),
                                "data/targets.schema.json",
                                rightSchema));

        SemanticModel.Relation normalizedRelation =
                result.capsule.relations.get(0);

        SemanticModel.Step step =
                normalizedRelation.steps.get(0);

        require(
                step.operation ==
                        SemanticModel.Operation.CROSS_JOIN,
                "cross join operation");

        require(
                step.left.equals("orders"),
                "cross join left");

        require(
                step.right.equals("targets"),
                "cross join right");

        require(
                step.conditions.isEmpty(),
                "cross join conditions empty");

        require(
                step.outputSchema.columns.size() == 4,
                "cross join output column count");

        SemanticModel.Column category =
                step.outputSchema.columns.get(0);

        SemanticModel.Column amount =
                step.outputSchema.columns.get(1);

        SemanticModel.Column target =
                step.outputSchema.columns.get(2);

        SemanticModel.Column priority =
                step.outputSchema.columns.get(3);

        require(
                category.name.equals("category"),
                "cross join first left column");

        require(
                amount.name.equals("amount"),
                "cross join second left column");

        require(
                target.name.equals("target"),
                "cross join first right column");

        require(
                priority.name.equals("priority"),
                "cross join second right column");

        require(
                category.type ==
                        SemanticModel.Type.TEXT,
                "cross join left type");

        require(
                amount.type ==
                        SemanticModel.Type.DECIMAL,
                "cross join second left type");

        require(
                target.type ==
                        SemanticModel.Type.DECIMAL,
                "cross join first right type");

        require(
                priority.type ==
                        SemanticModel.Type.INTEGER,
                "cross join second right type");

        require(
                !category.nullable,
                "cross join preserves left nullability");

        require(
                !target.nullable,
                "cross join preserves right nullability");

        require(
                priority.nullable,
                "cross join preserves nullable right column");

        require(
                normalizedRelation.outputSchema ==
                        step.outputSchema,
                "cross join relation output schema");
    }
    private static void expectCrossJoinFailure(
            String left,
            String right,
            List<RawModel.Condition> on,
            RawSchema leftSchema,
            RawSchema rightSchema,
            String expectedText) {

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

        RawModel.Relation relation =
                new RawModel.Relation(
                        "cross_joined",
                        "relations/cross_joined.json",
                        "json");

        RawModel.Step step =
                new RawModel.Step(
                        "cross_joined_step",
                        "cross_join",
                        null,
                        left,
                        right,
                        null,
                        on,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of());

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "cross_joined",
                        "cross join rejection test",
                        List.of(
                                new RawModel.Binding(
                                        "orders",
                                        "data/orders.csv"),
                                new RawModel.Binding(
                                        "targets",
                                        "data/targets.csv")),
                        List.of(step),
                        "cross_joined_step");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(
                                orders,
                                targets),
                        List.of(relation),
                        List.of());

        try {
            new CapsuleNormalizer().normalize(
                    capsule,
                    Map.of(
                            "cross_joined",
                            document),
                    Map.of(
                            "data/orders.schema.json",
                            leftSchema,
                            "data/targets.schema.json",
                            rightSchema));

            throw new AssertionError(
                    "Expected CROSS_JOIN failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage()
                      .toLowerCase(Locale.ROOT)
                      .contains(
                              expectedText.toLowerCase(
                                      Locale.ROOT)),
                    "CROSS_JOIN failure message: " +
                    ex.getMessage());
        }
    }

    private static void testCrossJoinMissingLeftRejected() {
        expectCrossJoinFailure(
                null,
                "targets",
                List.of(),
                joinLeftSchema(),
                joinRightSchema(),
                "requires left");
    }

    private static void testCrossJoinMissingRightRejected() {
        expectCrossJoinFailure(
                "orders",
                null,
                List.of(),
                joinLeftSchema(),
                joinRightSchema(),
                "requires right");
    }

    private static void testCrossJoinUnknownLeftRejected() {
        expectCrossJoinFailure(
                "missing",
                "targets",
                List.of(),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown left source");
    }

    private static void testCrossJoinUnknownRightRejected() {
        expectCrossJoinFailure(
                "orders",
                "missing",
                List.of(),
                joinLeftSchema(),
                joinRightSchema(),
                "unknown right source");
    }

    private static void testCrossJoinOnConditionsRejected() {
        expectCrossJoinFailure(
                "orders",
                "targets",
                List.of(
                        categoryJoinCondition()),
                joinLeftSchema(),
                joinRightSchema(),
                "does not accept on conditions");
    }

    private static void testCrossJoinDuplicateColumnRejected() {
        RawSchema right =
                new RawSchema(
                        "targets",
                        "1.0.0",
                        "targets",
                        List.of(
                                new RawSchema.Column(
                                        "category",
                                        "TEXT",
                                        false),
                                new RawSchema.Column(
                                        "target",
                                        "DECIMAL",
                                        false)));

        expectCrossJoinFailure(
                "orders",
                "targets",
                List.of(),
                joinLeftSchema(),
                right,
                "duplicate column name");
    }
    private static void expectFailure(
            RawSchema schema,
            String expectedText) {

        RawModel.Input input =
                new RawModel.Input(
                        "test",
                        "data/test.csv",
                        "data/test.schema.json",
                        "csv");

        RawModel.Relation relation =
                new RawModel.Relation(
                        "test_relation",
                        "relations/test_relation.json",
                        "json");

        RawModel.Output output =
                new RawModel.Output(
                        "test_output",
                        "test_relation",
                        "output/test.csv",
                        "expected/test.csv",
                        "expected/test.schema.json",
                        "csv");

        RawModel.Capsule capsule =
                new RawModel.Capsule(
                        "TEST",
                        "1.0.0",
                        List.of(input),
                        List.of(relation),
                        List.of(output));

        RawModel.RelationDocument document =
                new RawModel.RelationDocument(
                        "test_relation",
                        "test",
                        List.of(),
                        List.of(),
                        "");

        try {
            new CapsuleNormalizer().normalize(
                    capsule,
                    Map.of("test_relation", document),
                    Map.of(
                            "data/test.schema.json",
                            schema,
                            "expected/test.schema.json",
                            schema));

            throw new AssertionError(
                    "Expected failure containing: " +
                    expectedText);

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().toLowerCase(Locale.ROOT)
                       .contains(expectedText.toLowerCase(Locale.ROOT)),
                    "failure message: " + ex.getMessage());
        }
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition)
            throw new AssertionError(message);
    }
}
