import java.util.*;

public final class WitnessExecutorTest {

    public static void main(String[] args) {
        testProjectReordersColumnsAndPreservesRows();
        testProjectUnavailableSourceRejected();
        testRenamePreservesRowsAndChangesSchema();
        testRenameUnavailableSourceRejected();
        testFilterRetainsOnlyTrue();
        testFilterUnknownIsDiscarded();
        testFilterPreservesTupleOrder();
        testFilterUnavailableSourceRejected();
        testCrossJoinCartesianProduct();
        testCrossJoinEmptyRightProducesEmpty();
        testCrossJoinUnavailableLeftRejected();
        testCrossJoinUnavailableRightRejected();
        testJoinMatchesAndCoalescesKey();
        testJoinGreaterThanOrEqualMatchesAndNullDoesNotMatch();
        testJoinUnknownDoesNotMatch();
        testJoinMultipleConditions();
        testJoinUnavailableLeftRejected();
        testJoinUnavailableRightRejected();
        testLeftJoinMatchesAndPreservesOrder();
        testLeftJoinUnmatchedProducesNulls();
        testLeftJoinUnknownProducesUnmatched();
        testLeftJoinEmptyRightPreservesLeftRows();
        testLeftJoinUnavailableLeftRejected();
        testLeftJoinUnavailableRightRejected();
        testAggregateCountGrouped();
        testAggregateCountIgnoresNull();
        testAggregateCountDistinctIgnoresDuplicatesAndNull();
        testAggregateCountPreservesFirstGroupOrder();
        testAggregateCountMultipleMeasures();
        testAggregateSumInteger();
        testAggregateSumDecimal();
        testAggregateSumIgnoresNull();
        testAggregateSumAllNullProducesNull();
        testAggregateSumIntegerOverflowRejected();
        testAggregateMinDecimal();
        testAggregateMaxInteger();
        testAggregateMinIgnoresNull();
        testAggregateMaxAllNullProducesNull();

        testAggregateUnavailableSourceRejected();
        testDeriveColumnAndLiteral();
        testDeriveConditional();
        testDerivePreservesInputOrder();
        testDeriveSelectProjectsColumns();
        testDeriveOrderBySortsProjectedRows();
        testDeriveUnavailableSourceRejected();
        testRelationExecutesStepsInDeclarationOrder();
        testRelationReturnsDeclaredOutput();
        testRelationUnavailableBindingRejected();
        testRelationBindingSchemaMismatchRejected();
        testCapsuleExecutesForwardDependency();
        testCapsuleResolvesBindingAliasBySourceId();
        testCapsuleReturnsDeclaredOutputIds();
        testCapsuleMissingInputRejected();
        testCapsuleInputSchemaMismatchRejected();

        System.out.println(
                "WitnessExecutorTest PASSED");
    }

    private static void testProjectReordersColumnsAndPreservesRows() {
        SemanticModel.Schema inputSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true),
                                new SemanticModel.Column(
                                        "count",
                                        SemanticModel.Type.INTEGER,
                                        false)));

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                inputSchema.columns.get(2),
                                inputSchema.columns.get(0),
                                inputSchema.columns.get(1)));

        SemanticModel.Step project =
                new SemanticModel.Step(
                        "projected",
                        SemanticModel.Operation.PROJECT,
                        outputSchema,
                        "orders",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                "count",
                                "category",
                                "amount"),
                        List.of(),
                        Map.of());

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        inputSchema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                null,
                                                2L)),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                "12.50",
                                                5L))));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        project,
                        Map.of(
                                "orders",
                                input));

        require(
                result.schema == outputSchema,
                "project output schema identity");

        require(
                result.rows.size() == 2,
                "project row count");

        require(
                result.rows.get(0).get(0).equals(2L),
                "project first row count");

        require(
                result.rows.get(0).get(1).equals("A"),
                "project first row category");

        require(
                result.rows.get(0).get(2) == null,
                "project preserves semantic null");

        require(
                result.rows.get(1).get(0).equals(5L),
                "project second row count");

        require(
                result.rows.get(1).get(1).equals("B"),
                "project preserves tuple order");

        require(
                result.rows.get(1).get(2).equals("12.50"),
                "project second row amount");
    }

    private static void testProjectUnavailableSourceRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false)));

        SemanticModel.Step project =
                new SemanticModel.Step(
                        "projected",
                        SemanticModel.Operation.PROJECT,
                        schema,
                        "missing",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("category"),
                        List.of(),
                        Map.of());

        try {
            new WitnessExecutor().executeStep(
                    project,
                    Map.of());

            throw new AssertionError(
                    "Expected unavailable PROJECT source failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable source"),
                    "project unavailable source message");
        }
    }

    private static void testRenamePreservesRowsAndChangesSchema() {
        SemanticModel.Schema inputSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true)));

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "segment",
                                        SemanticModel.Type.TEXT,
                                        false),
                                inputSchema.columns.get(1)));

        SemanticModel.Step rename =
                new SemanticModel.Step(
                        "renamed",
                        SemanticModel.Operation.RENAME,
                        outputSchema,
                        "orders",
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
                                "segment"));

        ExecutionModel.Row first =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                null));

        ExecutionModel.Row second =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "B",
                                "12.50"));

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        inputSchema,
                        List.of(
                                first,
                                second));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        rename,
                        Map.of(
                                "orders",
                                input));

        require(
                result.schema == outputSchema,
                "rename output schema identity");

        require(
                result.schema.columns.get(0).name
                        .equals("segment"),
                "rename output column identity");

        require(
                result.schema.columns.get(1).name
                        .equals("amount"),
                "rename preserves second column identity");

        require(
                result.rows.size() == 2,
                "rename row count");

        require(
                result.rows.get(0) == first,
                "rename preserves first row object");

        require(
                result.rows.get(1) == second,
                "rename preserves second row object");

        require(
                result.rows.get(0).get(0).equals("A"),
                "rename preserves first value");

        require(
                result.rows.get(0).get(1) == null,
                "rename preserves semantic null");

        require(
                result.rows.get(1).get(0).equals("B"),
                "rename preserves tuple order");

        require(
                result.rows.get(1).get(1).equals("12.50"),
                "rename preserves second value");
    }

    private static void testRenameUnavailableSourceRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "segment",
                                        SemanticModel.Type.TEXT,
                                        false)));

        SemanticModel.Step rename =
                new SemanticModel.Step(
                        "renamed",
                        SemanticModel.Operation.RENAME,
                        schema,
                        "missing",
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
                                "segment"));

        try {
            new WitnessExecutor().executeStep(
                    rename,
                    Map.of());

            throw new AssertionError(
                    "Expected unavailable RENAME source failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable source"),
                    "rename unavailable source message");
        }
    }

    private static SemanticModel.Schema filterSchema() {
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

    private static SemanticModel.Step amountFilter(
            SemanticModel.Schema schema) {

        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "amount"),
                        ">=",
                        SemanticModel.Expression.literal(
                                new java.math.BigDecimal("10"),
                                SemanticModel.Type.DECIMAL));

        return new SemanticModel.Step(
                "filtered",
                SemanticModel.Operation.FILTER,
                schema,
                "orders",
                null,
                null,
                List.of(condition),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static void testFilterRetainsOnlyTrue() {
        SemanticModel.Schema schema =
                filterSchema();

        ExecutionModel.Row below =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                new java.math.BigDecimal("5")));

        ExecutionModel.Row retained =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "B",
                                new java.math.BigDecimal("10")));

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                below,
                                retained));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        amountFilter(schema),
                        Map.of(
                                "orders",
                                input));

        require(
                result.rows.size() == 1,
                "filter retained row count");

        require(
                result.rows.get(0) == retained,
                "filter retains TRUE row");
    }

    private static void testFilterUnknownIsDiscarded() {
        SemanticModel.Schema schema =
                filterSchema();

        ExecutionModel.Row unknown =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                null));

        ExecutionModel.Row retained =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "B",
                                new java.math.BigDecimal("20")));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        amountFilter(schema),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                unknown,
                                                retained))));

        require(
                result.rows.size() == 1,
                "filter UNKNOWN discarded count");

        require(
                result.rows.get(0) == retained,
                "filter UNKNOWN is discarded");
    }

    private static void testFilterPreservesTupleOrder() {
        SemanticModel.Schema schema =
                filterSchema();

        ExecutionModel.Row first =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                new java.math.BigDecimal("30")));

        ExecutionModel.Row discarded =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "B",
                                new java.math.BigDecimal("1")));

        ExecutionModel.Row second =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "C",
                                new java.math.BigDecimal("20")));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        amountFilter(schema),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                first,
                                                discarded,
                                                second))));

        require(
                result.rows.size() == 2,
                "filter ordered row count");

        require(
                result.rows.get(0) == first,
                "filter preserves first retained row");

        require(
                result.rows.get(1) == second,
                "filter preserves second retained row");
    }

    private static void testFilterUnavailableSourceRejected() {
        SemanticModel.Schema schema =
                filterSchema();

        SemanticModel.Step filter =
                amountFilter(schema);

        try {
            new WitnessExecutor().executeStep(
                    filter,
                    Map.of());

            throw new AssertionError(
                    "Expected unavailable FILTER source failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable source"),
                    "filter unavailable source message");
        }
    }

    private static SemanticModel.Schema crossLeftSchema() {
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

    private static SemanticModel.Schema crossRightSchema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "target",
                                SemanticModel.Type.DECIMAL,
                                false),
                        new SemanticModel.Column(
                                "enabled",
                                SemanticModel.Type.BOOLEAN,
                                false)));
    }

    private static SemanticModel.Step crossJoinStep(
            SemanticModel.Schema leftSchema,
            SemanticModel.Schema rightSchema) {

        List<SemanticModel.Column> columns =
                new ArrayList<>();

        columns.addAll(leftSchema.columns);
        columns.addAll(rightSchema.columns);

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(columns);

        return new SemanticModel.Step(
                "crossed",
                SemanticModel.Operation.CROSS_JOIN,
                outputSchema,
                null,
                "left",
                "right",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static void testCrossJoinCartesianProduct() {
        SemanticModel.Schema leftSchema =
                crossLeftSchema();

        SemanticModel.Schema rightSchema =
                crossRightSchema();

        ExecutionModel.Row leftFirst =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                new java.math.BigDecimal("10")));

        ExecutionModel.Row leftSecond =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "B",
                                null));

        ExecutionModel.Row rightFirst =
                new ExecutionModel.Row(
                        Arrays.asList(
                                new java.math.BigDecimal("5"),
                                true));

        ExecutionModel.Row rightSecond =
                new ExecutionModel.Row(
                        Arrays.asList(
                                new java.math.BigDecimal("7"),
                                false));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        crossJoinStep(
                                leftSchema,
                                rightSchema),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                leftFirst,
                                                leftSecond)),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                rightFirst,
                                                rightSecond))));

        require(
                result.rows.size() == 4,
                "cross join Cartesian product count");

        require(
                result.rows.get(0).get(0).equals("A"),
                "cross join first left value");

        require(
                result.rows.get(0).get(2)
                        .equals(new java.math.BigDecimal("5")),
                "cross join first right value");

        require(
                result.rows.get(1).get(2)
                        .equals(new java.math.BigDecimal("7")),
                "cross join inner right ordering");

        require(
                result.rows.get(2).get(0).equals("B"),
                "cross join outer left ordering");

        require(
                result.rows.get(2).get(1) == null,
                "cross join preserves left NULL");

        require(
                result.rows.get(3).get(3).equals(false),
                "cross join final right value");

        require(
                result.schema.columns.size() == 4,
                "cross join output schema width");

        require(
                result.schema.columns.get(0).name
                        .equals("category"),
                "cross join left schema order");

        require(
                result.schema.columns.get(1).name
                        .equals("amount"),
                "cross join left second column");

        require(
                result.schema.columns.get(2).name
                        .equals("target"),
                "cross join right schema order");

        require(
                result.schema.columns.get(3).name
                        .equals("enabled"),
                "cross join right second column");
    }

    private static void testCrossJoinEmptyRightProducesEmpty() {
        SemanticModel.Schema leftSchema =
                crossLeftSchema();

        SemanticModel.Schema rightSchema =
                crossRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        crossJoinStep(
                                leftSchema,
                                rightSchema),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null)))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of())));

        require(
                result.rows.isEmpty(),
                "cross join empty right");
    }

    private static void testCrossJoinUnavailableLeftRejected() {
        SemanticModel.Schema leftSchema =
                crossLeftSchema();

        SemanticModel.Schema rightSchema =
                crossRightSchema();

        try {
            new WitnessExecutor().executeStep(
                    crossJoinStep(
                            leftSchema,
                            rightSchema),
                    Map.of(
                            "right",
                            new ExecutionModel.RelationData(
                                    rightSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected unavailable CROSS_JOIN left failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable left source"),
                    "cross join unavailable left message");
        }
    }

    private static void testCrossJoinUnavailableRightRejected() {
        SemanticModel.Schema leftSchema =
                crossLeftSchema();

        SemanticModel.Schema rightSchema =
                crossRightSchema();

        try {
            new WitnessExecutor().executeStep(
                    crossJoinStep(
                            leftSchema,
                            rightSchema),
                    Map.of(
                            "left",
                            new ExecutionModel.RelationData(
                                    leftSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected unavailable CROSS_JOIN right failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable right source"),
                    "cross join unavailable right message");
        }
    }

    private static SemanticModel.Schema joinExecLeftSchema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "category",
                                SemanticModel.Type.TEXT,
                                true),
                        new SemanticModel.Column(
                                "amount",
                                SemanticModel.Type.DECIMAL,
                                false)));
    }

    private static SemanticModel.Schema joinExecRightSchema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "category",
                                SemanticModel.Type.TEXT,
                                true),
                        new SemanticModel.Column(
                                "target",
                                SemanticModel.Type.DECIMAL,
                                false)));
    }

    private static SemanticModel.Step joinExecStep(
            List<SemanticModel.Condition> conditions) {

        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                leftSchema.columns.get(0),
                                leftSchema.columns.get(1),
                                rightSchema.columns.get(1)));

        return new SemanticModel.Step(
                "joined",
                SemanticModel.Operation.JOIN,
                outputSchema,
                null,
                "left",
                "right",
                conditions,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static SemanticModel.Condition joinCategoryCondition() {
        return new SemanticModel.Condition(
                SemanticModel.Expression.column(
                        "category"),
                "=",
                SemanticModel.Expression.column(
                        "category"));
    }

    private static void testJoinMatchesAndCoalescesKey() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        joinExecStep(
                                List.of(
                                        joinCategoryCondition())),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("20"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("100"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("200"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("300")))))));

        require(
                result.rows.size() == 3,
                "join match count");

        require(
                result.rows.get(0).size() == 3,
                "join coalesced width");

        require(
                result.rows.get(0).get(0).equals("A"),
                "join retained left key");

        require(
                result.rows.get(0).get(2)
                        .equals(new java.math.BigDecimal("100")),
                "join first right encounter");

        require(
                result.rows.get(1).get(2)
                        .equals(new java.math.BigDecimal("200")),
                "join second right encounter");

        require(
                result.rows.get(2).get(0).equals("B"),
                "join left outer ordering");

        require(
                result.rows.get(2).get(2)
                        .equals(new java.math.BigDecimal("300")),
                "join B match");
    }

    private static void testJoinUnknownDoesNotMatch() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        joinExecStep(
                                List.of(
                                        joinCategoryCondition())),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                null,
                                                                new java.math.BigDecimal("10"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                null,
                                                                new java.math.BigDecimal("100")))))));

        require(
                result.rows.isEmpty(),
                "join UNKNOWN does not match");
    }

    private static void testJoinGreaterThanOrEqualMatchesAndNullDoesNotMatch() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        SemanticModel.Condition amountAtLeastTarget =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column("amount"),
                        ">=",
                        SemanticModel.Expression.column("target"));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        joinExecStep(
                                List.of(
                                        joinCategoryCondition(),
                                        amountAtLeastTarget)),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("20"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("5"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("15"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("20")))))));

        require(
                result.rows.size() == 2,
                "JOIN >= match count excludes FALSE and UNKNOWN");

        require(
                result.rows.get(0).get(0).equals("A") &&
                result.rows.get(0).get(2).equals(
                        new java.math.BigDecimal("5")),
                "JOIN >= retains first matching pair");

        require(
                result.rows.get(1).get(0).equals("B") &&
                result.rows.get(1).get(2).equals(
                        new java.math.BigDecimal("20")),
                "JOIN >= preserves traversal order");
    }

    private static void testJoinMultipleConditions() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        SemanticModel.Condition amountTarget =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "amount"),
                        "=",
                        SemanticModel.Expression.column(
                                "target"));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        joinExecStep(
                                List.of(
                                        joinCategoryCondition(),
                                        amountTarget)),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("5"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10")))))));

        require(
                result.rows.size() == 1,
                "join multiple conditions count");

        require(
                result.rows.get(0).get(2)
                        .equals(new java.math.BigDecimal("10")),
                "join all conditions TRUE");
    }

    private static void testJoinUnavailableLeftRejected() {
        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        try {
            new WitnessExecutor().executeStep(
                    joinExecStep(
                            List.of(
                                    joinCategoryCondition())),
                    Map.of(
                            "right",
                            new ExecutionModel.RelationData(
                                    rightSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected unavailable JOIN left failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable left source"),
                    "join unavailable left message");
        }
    }

    private static void testJoinUnavailableRightRejected() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        try {
            new WitnessExecutor().executeStep(
                    joinExecStep(
                            List.of(
                                    joinCategoryCondition())),
                    Map.of(
                            "left",
                            new ExecutionModel.RelationData(
                                    leftSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected unavailable JOIN right failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable right source"),
                    "join unavailable right message");
        }
    }

    private static SemanticModel.Step leftJoinExecStep() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                leftSchema.columns.get(0),
                                leftSchema.columns.get(1),
                                new SemanticModel.Column(
                                        "target",
                                        SemanticModel.Type.DECIMAL,
                                        true)));

        return new SemanticModel.Step(
                "left_joined",
                SemanticModel.Operation.LEFT_JOIN,
                outputSchema,
                null,
                "left",
                "right",
                List.of(
                        joinCategoryCondition()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static void testLeftJoinMatchesAndPreservesOrder() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        leftJoinExecStep(),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("20"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("100"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("200"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("300")))))));

        require(
                result.rows.size() == 3,
                "left join match count");

        require(
                result.rows.get(0).get(0).equals("A"),
                "left join first left row");

        require(
                result.rows.get(0).get(2)
                        .equals(new java.math.BigDecimal("100")),
                "left join first right encounter");

        require(
                result.rows.get(1).get(2)
                        .equals(new java.math.BigDecimal("200")),
                "left join second right encounter");

        require(
                result.rows.get(2).get(0).equals("B"),
                "left join outer left order");

        require(
                result.rows.get(2).get(2)
                        .equals(new java.math.BigDecimal("300")),
                "left join B match");
    }

    private static void testLeftJoinUnmatchedProducesNulls() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        leftJoinExecStep(),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "Z",
                                                                new java.math.BigDecimal("40"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("100")))))));

        require(
                result.rows.size() == 1,
                "left join unmatched count");

        require(
                result.rows.get(0).get(0).equals("Z"),
                "left join unmatched left key");

        require(
                result.rows.get(0).get(1)
                        .equals(new java.math.BigDecimal("40")),
                "left join unmatched left value");

        require(
                result.rows.get(0).get(2) == null,
                "left join unmatched right NULL");

        require(
                result.schema.columns.get(2).nullable,
                "left join right output nullable");
    }

    private static void testLeftJoinUnknownProducesUnmatched() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        leftJoinExecStep(),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                null,
                                                                new java.math.BigDecimal("10"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                null,
                                                                new java.math.BigDecimal("100")))))));

        require(
                result.rows.size() == 1,
                "left join UNKNOWN unmatched count");

        require(
                result.rows.get(0).get(0) == null,
                "left join preserves NULL left key");

        require(
                result.rows.get(0).get(2) == null,
                "left join UNKNOWN right NULL");
    }

    private static void testLeftJoinEmptyRightPreservesLeftRows() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        leftJoinExecStep(),
                        Map.of(
                                "left",
                                new ExecutionModel.RelationData(
                                        leftSchema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("20"))))),
                                "right",
                                new ExecutionModel.RelationData(
                                        rightSchema,
                                        List.of())));

        require(
                result.rows.size() == 2,
                "left join empty right count");

        require(
                result.rows.get(0).get(0).equals("A"),
                "left join empty right first left");

        require(
                result.rows.get(0).get(2) == null,
                "left join empty right first NULL");

        require(
                result.rows.get(1).get(0).equals("B"),
                "left join empty right second left");

        require(
                result.rows.get(1).get(2) == null,
                "left join empty right second NULL");
    }

    private static void testLeftJoinUnavailableLeftRejected() {
        SemanticModel.Schema rightSchema =
                joinExecRightSchema();

        try {
            new WitnessExecutor().executeStep(
                    leftJoinExecStep(),
                    Map.of(
                            "right",
                            new ExecutionModel.RelationData(
                                    rightSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected unavailable LEFT_JOIN left failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable left source"),
                    "left join unavailable left message");
        }
    }

    private static void testLeftJoinUnavailableRightRejected() {
        SemanticModel.Schema leftSchema =
                joinExecLeftSchema();

        try {
            new WitnessExecutor().executeStep(
                    leftJoinExecStep(),
                    Map.of(
                            "left",
                            new ExecutionModel.RelationData(
                                    leftSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected unavailable LEFT_JOIN right failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable right source"),
                    "left join unavailable right message");
        }
    }

    private static SemanticModel.Schema aggregateCountSchema() {
        return new SemanticModel.Schema(
                List.of(
                        new SemanticModel.Column(
                                "category",
                                SemanticModel.Type.TEXT,
                                false),
                        new SemanticModel.Column(
                                "amount",
                                SemanticModel.Type.DECIMAL,
                                true),
                        new SemanticModel.Column(
                                "note",
                                SemanticModel.Type.TEXT,
                                true)));
    }

    private static SemanticModel.Step aggregateCountStep(
            List<SemanticModel.Measure> measures) {

        SemanticModel.Schema inputSchema =
                aggregateCountSchema();

        List<SemanticModel.Column> output =
                new ArrayList<>();

        output.add(
                inputSchema.columns.get(0));

        for (SemanticModel.Measure measure :
                measures) {

            output.add(
                    new SemanticModel.Column(
                            measure.name,
                            SemanticModel.Type.INTEGER,
                            false));
        }

        return new SemanticModel.Step(
                "summary",
                SemanticModel.Operation.AGGREGATE,
                new SemanticModel.Schema(output),
                "orders",
                null,
                null,
                List.of(),
                List.of("category"),
                measures,
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static SemanticModel.Measure countAmount() {
        return new SemanticModel.Measure(
                "amount_count",
                "COUNT",
                "amount");
    }

    private static void testAggregateCountGrouped() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateCountStep(
                                List.of(
                                        countAmount())),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"),
                                                                "x")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("20"),
                                                                "y")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("30"),
                                                                "z"))))));

        require(
                result.rows.size() == 2,
                "aggregate group count");

        require(
                result.rows.get(0).get(0).equals("A"),
                "aggregate first group");

        require(
                result.rows.get(0).get(1).equals(2L),
                "aggregate A count");

        require(
                result.rows.get(1).get(0).equals("B"),
                "aggregate second group");

        require(
                result.rows.get(1).get(1).equals(1L),
                "aggregate B count");
    }

    private static void testAggregateCountIgnoresNull() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateCountStep(
                                List.of(
                                        countAmount())),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "x")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"),
                                                                "y")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "z"))))));

        require(
                result.rows.size() == 1,
                "aggregate NULL count group");

        require(
                result.rows.get(0).get(1).equals(1L),
                "COUNT ignores NULL");
    }

    private static void testAggregateCountDistinctIgnoresDuplicatesAndNull() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure distinctNotes =
                new SemanticModel.Measure(
                        "distinct_note_count",
                        "COUNT_DISTINCT",
                        "note");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateCountStep(
                                List.of(
                                        distinctNotes)),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "PASS")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "PASS")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "FAIL")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "FAIL"))))));

        require(
                result.rows.size() == 1,
                "COUNT_DISTINCT group count");

        require(
                result.rows.get(0).get(0).equals(
                        "A"),
                "COUNT_DISTINCT group key");

        require(
                result.rows.get(0).get(1).equals(
                        2L),
                "COUNT_DISTINCT ignores duplicates and NULL");
    }
    private static void testAggregateCountPreservesFirstGroupOrder() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateCountStep(
                                List.of(
                                        countAmount())),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("1"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("2"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("3"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "C",
                                                                new java.math.BigDecimal("4"),
                                                                null))))));

        require(
                result.rows.get(0).get(0).equals("B"),
                "aggregate first-occurrence B");

        require(
                result.rows.get(1).get(0).equals("A"),
                "aggregate first-occurrence A");

        require(
                result.rows.get(2).get(0).equals("C"),
                "aggregate first-occurrence C");
    }

    private static void testAggregateCountMultipleMeasures() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure noteCount =
                new SemanticModel.Measure(
                        "note_count",
                        "COUNT",
                        "note");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateCountStep(
                                List.of(
                                        countAmount(),
                                        noteCount)),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("1"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "x")),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("2"),
                                                                "y"))))));

        require(
                result.rows.get(0).get(1).equals(2L),
                "aggregate first measure declaration order");

        require(
                result.rows.get(0).get(2).equals(2L),
                "aggregate second measure declaration order");
    }

    private static SemanticModel.Step aggregateSumStep(
            SemanticModel.Schema inputSchema,
            SemanticModel.Measure measure,
            SemanticModel.Type outputType) {

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                inputSchema.columns.get(0),
                                new SemanticModel.Column(
                                        measure.name,
                                        outputType,
                                        true)));

        return new SemanticModel.Step(
                "summary",
                SemanticModel.Operation.AGGREGATE,
                outputSchema,
                "orders",
                null,
                null,
                List.of(),
                List.of("category"),
                List.of(measure),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static void testAggregateSumInteger() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "quantity",
                                        SemanticModel.Type.INTEGER,
                                        true)));

        SemanticModel.Measure sum =
                new SemanticModel.Measure(
                        "total_quantity",
                        "SUM",
                        "quantity");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                sum,
                                SemanticModel.Type.INTEGER),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                10L)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                20L))))));

        require(
                result.rows.get(0).get(1).equals(30L),
                "aggregate INTEGER SUM");
    }

    private static void testAggregateSumDecimal() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure sum =
                new SemanticModel.Measure(
                        "total_amount",
                        "SUM",
                        "amount");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                sum,
                                SemanticModel.Type.DECIMAL),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("1.25"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("2.750"),
                                                                null))))));

        java.math.BigDecimal total =
                (java.math.BigDecimal)
                        result.rows.get(0).get(1);

        require(
                total.compareTo(
                        new java.math.BigDecimal("4.000")) == 0,
                "aggregate DECIMAL SUM exact value");
    }

    private static void testAggregateSumIgnoresNull() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure sum =
                new SemanticModel.Measure(
                        "total_amount",
                        "SUM",
                        "amount");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                sum,
                                SemanticModel.Type.DECIMAL),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("2.5"),
                                                                null))))));

        require(
                ((java.math.BigDecimal)
                        result.rows.get(0).get(1))
                        .compareTo(
                                new java.math.BigDecimal("2.5")) == 0,
                "aggregate SUM ignores NULL");
    }

    private static void testAggregateSumAllNullProducesNull() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure sum =
                new SemanticModel.Measure(
                        "total_amount",
                        "SUM",
                        "amount");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                sum,
                                SemanticModel.Type.DECIMAL),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "x"))))));

        require(
                result.rows.get(0).get(1) == null,
                "aggregate all-NULL SUM");
    }

    private static void testAggregateSumIntegerOverflowRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "quantity",
                                        SemanticModel.Type.INTEGER,
                                        false)));

        SemanticModel.Measure sum =
                new SemanticModel.Measure(
                        "total_quantity",
                        "SUM",
                        "quantity");

        try {
            new WitnessExecutor().executeStep(
                    aggregateSumStep(
                            schema,
                            sum,
                            SemanticModel.Type.INTEGER),
                    Map.of(
                            "orders",
                            new ExecutionModel.RelationData(
                                    schema,
                                    List.of(
                                            new ExecutionModel.Row(
                                                    Arrays.asList(
                                                            "A",
                                                            Long.MAX_VALUE)),
                                            new ExecutionModel.Row(
                                                    Arrays.asList(
                                                            "A",
                                                            1L))))));

            throw new AssertionError(
                    "Expected INTEGER SUM overflow");

        } catch (ArithmeticException ex) {
            require(
                    ex.getMessage() != null,
                    "aggregate INTEGER overflow exception");
        }
    }
    private static void testAggregateMinDecimal() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure min =
                new SemanticModel.Measure(
                        "minimum_amount",
                        "MIN",
                        "amount");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                min,
                                SemanticModel.Type.DECIMAL),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("9.50"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("2.25"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("7.75"),
                                                                null))))));

        java.math.BigDecimal value =
                (java.math.BigDecimal)
                        result.rows.get(0).get(1);

        require(
                value.compareTo(
                        new java.math.BigDecimal("2.25")) == 0,
                "aggregate DECIMAL MIN");
    }

    private static void testAggregateMaxInteger() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "quantity",
                                        SemanticModel.Type.INTEGER,
                                        true)));

        SemanticModel.Measure max =
                new SemanticModel.Measure(
                        "maximum_quantity",
                        "MAX",
                        "quantity");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                max,
                                SemanticModel.Type.INTEGER),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                10L)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                30L)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                20L))))));

        require(
                result.rows.get(0).get(1).equals(30L),
                "aggregate INTEGER MAX");
    }

    private static void testAggregateMinIgnoresNull() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure min =
                new SemanticModel.Measure(
                        "minimum_amount",
                        "MIN",
                        "amount");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                min,
                                SemanticModel.Type.DECIMAL),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("4.5"),
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("8.5"),
                                                                null))))));

        require(
                ((java.math.BigDecimal)
                        result.rows.get(0).get(1))
                        .compareTo(
                                new java.math.BigDecimal("4.5")) == 0,
                "aggregate MIN ignores NULL");
    }

    private static void testAggregateMaxAllNullProducesNull() {
        SemanticModel.Schema schema =
                aggregateCountSchema();

        SemanticModel.Measure max =
                new SemanticModel.Measure(
                        "maximum_amount",
                        "MAX",
                        "amount");

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        aggregateSumStep(
                                schema,
                                max,
                                SemanticModel.Type.DECIMAL),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                null)),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                null,
                                                                "x"))))));

        require(
                result.rows.get(0).get(1) == null,
                "aggregate all-NULL MAX");
    }
    private static void testAggregateUnavailableSourceRejected() {
        try {
            new WitnessExecutor().executeStep(
                    aggregateCountStep(
                            List.of(
                                    countAmount())),
                    Map.of());

            throw new AssertionError(
                    "Expected unavailable AGGREGATE source failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable source"),
                    "aggregate unavailable source message");
        }
    }

    private static SemanticModel.Schema deriveSchema() {
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

    private static SemanticModel.Step deriveColumnLiteralStep() {
        return deriveColumnLiteralStep(
                List.of(),
                List.of());
    }

    private static SemanticModel.Step deriveColumnLiteralStep(
            List<String> select,
            List<String> orderBy) {

        SemanticModel.Schema schema =
                deriveSchema();

        List<SemanticModel.Column> output =
                new ArrayList<>(
                        schema.columns);

        output.add(
                new SemanticModel.Column(
                        "same_category",
                        SemanticModel.Type.TEXT,
                        false));

        output.add(
                new SemanticModel.Column(
                        "constant",
                        SemanticModel.Type.INTEGER,
                        false));

        List<SemanticModel.DerivedColumn> derived =
                List.of(
                        new SemanticModel.DerivedColumn(
                                "same_category",
                                SemanticModel.Expression.column(
                                        "category")),
                        new SemanticModel.DerivedColumn(
                                "constant",
                                SemanticModel.Expression.literal(
                                        42L,
                                        SemanticModel.Type.INTEGER)));

        List<SemanticModel.Column> selected =
                new ArrayList<>();

        if (select.isEmpty()) {
            selected.addAll(output);
        } else {
            for (String name : select) {
                for (SemanticModel.Column column : output) {
                    if (column.name.equals(name)) {
                        selected.add(column);
                        break;
                    }
                }
            }
        }

        return new SemanticModel.Step(
                "derived",
                SemanticModel.Operation.DERIVE,
                new SemanticModel.Schema(selected),
                "orders",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                derived,
                select,
                orderBy,
                Map.of());
    }
    private static SemanticModel.Step deriveConditionalStep() {
        SemanticModel.Schema schema =
                deriveSchema();

        SemanticModel.Condition condition =
                new SemanticModel.Condition(
                        SemanticModel.Expression.column(
                                "amount"),
                        ">=",
                        SemanticModel.Expression.literal(
                                new java.math.BigDecimal("10"),
                                SemanticModel.Type.DECIMAL));

        SemanticModel.Expression expression =
                SemanticModel.Expression.conditional(
                        condition,
                        SemanticModel.Expression.literal(
                                "HIGH",
                                SemanticModel.Type.TEXT),
                        SemanticModel.Expression.literal(
                                "LOW",
                                SemanticModel.Type.TEXT));

        List<SemanticModel.Column> output =
                new ArrayList<>(
                        schema.columns);

        output.add(
                new SemanticModel.Column(
                        "status",
                        SemanticModel.Type.TEXT,
                        false));

        List<SemanticModel.DerivedColumn> derived =
                List.of(
                        new SemanticModel.DerivedColumn(
                                "status",
                                expression));

        return new SemanticModel.Step(
                "derived",
                SemanticModel.Operation.DERIVE,
                new SemanticModel.Schema(output),
                "orders",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                derived,
                List.of(),
                List.of(),
                Map.of());
    }
    private static void testDeriveColumnAndLiteral() {
        SemanticModel.Schema schema =
                deriveSchema();

        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                new java.math.BigDecimal("12.5")));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        deriveColumnLiteralStep(),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(row))));

        require(
                result.rows.size() == 1,
                "derive row count");

        require(
                result.rows.get(0).size() == 4,
                "derive output width");

        require(
                result.rows.get(0).get(0).equals("A"),
                "derive preserves source category");

        require(
                result.rows.get(0).get(1)
                        .equals(new java.math.BigDecimal("12.5")),
                "derive preserves source amount");

        require(
                result.rows.get(0).get(2).equals("A"),
                "derive column expression");

        require(
                result.rows.get(0).get(3).equals(42L),
                "derive literal expression");
    }

    private static void testDeriveConditional() {
        SemanticModel.Schema schema =
                deriveSchema();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        deriveConditionalStep(),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "A",
                                                                new java.math.BigDecimal("10"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "B",
                                                                new java.math.BigDecimal("5"))),
                                                new ExecutionModel.Row(
                                                        Arrays.asList(
                                                                "C",
                                                                null))))));

        require(
                result.rows.get(0).get(2).equals("HIGH"),
                "derive conditional TRUE");

        require(
                result.rows.get(1).get(2).equals("LOW"),
                "derive conditional FALSE");

        require(
                result.rows.get(2).get(2).equals("LOW"),
                "derive conditional UNKNOWN");
    }

    private static void testDerivePreservesInputOrder() {
        SemanticModel.Schema schema =
                deriveSchema();

        ExecutionModel.Row first =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                new java.math.BigDecimal("1")));

        ExecutionModel.Row second =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "B",
                                new java.math.BigDecimal("2")));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        deriveColumnLiteralStep(),
                        Map.of(
                                "orders",
                                new ExecutionModel.RelationData(
                                        schema,
                                        List.of(
                                                first,
                                                second))));

        require(
                result.rows.get(0).get(0).equals("A"),
                "derive first row order");

        require(
                result.rows.get(1).get(0).equals("B"),
                "derive second row order");
    }

    private static void testDeriveSelectProjectsColumns() {
        SemanticModel.Schema schema = deriveSchema();

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new java.math.BigDecimal("12.5")))));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        deriveColumnLiteralStep(
                                List.of("amount", "same_category"),
                                List.of()),
                        Map.of("orders", input));

        require(
                result.schema.columns.size() == 2,
                "derive select schema width");

        require(
                result.rows.get(0).size() == 2,
                "derive select row width");

        require(
                result.rows.get(0).get(0)
                        .equals(new java.math.BigDecimal("12.5")) &&
                result.rows.get(0).get(1).equals("A"),
                "derive select values");
    }

    private static void testDeriveOrderBySortsProjectedRows() {
        SemanticModel.Schema schema = deriveSchema();

        ExecutionModel.RelationData input =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "B",
                                                new java.math.BigDecimal("1"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                new java.math.BigDecimal("2")))));

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeStep(
                        deriveColumnLiteralStep(
                                List.of("category", "constant"),
                                List.of("category")),
                        Map.of("orders", input));

        require(
                result.rows.get(0).get(0).equals("A") &&
                result.rows.get(1).get(0).equals("B"),
                "derive order_by ascending");
    }

    private static void testDeriveUnavailableSourceRejected() {
        try {
            new WitnessExecutor().executeStep(
                    deriveColumnLiteralStep(),
                    Map.of());

            throw new AssertionError(
                    "Expected unavailable DERIVE source failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable source"),
                    "derive unavailable source message");
        }
    }
    private static SemanticModel.Relation relationExecutionFixture() {
        SemanticModel.Schema sourceSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true),
                                new SemanticModel.Column(
                                        "ignored",
                                        SemanticModel.Type.TEXT,
                                        true)));

        SemanticModel.Schema projectedSchema =
                new SemanticModel.Schema(
                        List.of(
                                sourceSchema.columns.get(0),
                                sourceSchema.columns.get(1)));

        SemanticModel.Step project =
                new SemanticModel.Step(
                        "projected",
                        SemanticModel.Operation.PROJECT,
                        projectedSchema,
                        "orders",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                "category",
                                "amount"),
                        List.of(),
                        Map.of());

        SemanticModel.Expression status =
                SemanticModel.Expression.conditional(
                        new SemanticModel.Condition(
                                SemanticModel.Expression.column(
                                        "amount"),
                                ">=",
                                SemanticModel.Expression.literal(
                                        new java.math.BigDecimal("10"),
                                        SemanticModel.Type.DECIMAL)),
                        SemanticModel.Expression.literal(
                                "HIGH",
                                SemanticModel.Type.TEXT),
                        SemanticModel.Expression.literal(
                                "LOW",
                                SemanticModel.Type.TEXT));

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        List.of(
                                projectedSchema.columns.get(0),
                                projectedSchema.columns.get(1),
                                new SemanticModel.Column(
                                        "status",
                                        SemanticModel.Type.TEXT,
                                        false)));

        SemanticModel.Step derive =
                new SemanticModel.Step(
                        "classified",
                        SemanticModel.Operation.DERIVE,
                        outputSchema,
                        "projected",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new SemanticModel.DerivedColumn(
                                        "status",
                                        status)),
                        List.of(),
                        List.of(),
                        Map.of());

        SemanticModel.Binding binding =
                new SemanticModel.Binding(
                        "orders",
                        SemanticModel.SourceKind.INPUT,
                        "orders",
                        sourceSchema);

        return new SemanticModel.Relation(
                "classified_orders",
                List.of(),
                List.of(binding),
                List.of(
                        project,
                        derive),
                "classified",
                outputSchema);
    }

    private static ExecutionModel.RelationData relationExecutionInput() {
        SemanticModel.Schema schema =
                relationExecutionFixture()
                        .bindings
                        .get(0)
                        .schema;

        return new ExecutionModel.RelationData(
                schema,
                List.of(
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        new java.math.BigDecimal("12"),
                                        "drop-a")),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "B",
                                        new java.math.BigDecimal("5"),
                                        "drop-b")),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "C",
                                        null,
                                        "drop-c"))));
    }

    private static void testRelationExecutesStepsInDeclarationOrder() {
        ExecutionModel.RelationData result =
                new WitnessExecutor().executeRelation(
                        relationExecutionFixture(),
                        Map.of(
                                "orders",
                                relationExecutionInput()));

        require(
                result.rows.size() == 3,
                "relation execution row count");

        require(
                result.rows.get(0).size() == 3,
                "relation execution final width");

        require(
                result.rows.get(0).get(0).equals("A"),
                "relation execution first row");

        require(
                result.rows.get(0).get(2).equals("HIGH"),
                "relation execution PROJECT then DERIVE");

        require(
                result.rows.get(1).get(2).equals("LOW"),
                "relation execution second derived result");

        require(
                result.rows.get(2).get(2).equals("LOW"),
                "relation execution UNKNOWN conditional result");
    }

    private static void testRelationReturnsDeclaredOutput() {
        SemanticModel.Relation relation =
                relationExecutionFixture();

        ExecutionModel.RelationData result =
                new WitnessExecutor().executeRelation(
                        relation,
                        Map.of(
                                "orders",
                                relationExecutionInput()));

        require(
                result.schema == relation.outputSchema,
                "relation declared output schema identity");
    }

    private static void testRelationUnavailableBindingRejected() {
        try {
            new WitnessExecutor().executeRelation(
                    relationExecutionFixture(),
                    Map.of());

            throw new AssertionError(
                    "Expected unavailable relation binding failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable binding"),
                    "relation unavailable binding message");
        }
    }

    private static void testRelationBindingSchemaMismatchRejected() {
        SemanticModel.Schema wrongSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false)));

        try {
            new WitnessExecutor().executeRelation(
                    relationExecutionFixture(),
                    Map.of(
                            "orders",
                            new ExecutionModel.RelationData(
                                    wrongSchema,
                                    List.of())));

            throw new AssertionError(
                    "Expected relation binding schema mismatch");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "runtime schema does not match"),
                    "relation binding schema mismatch message");
        }
    }
    private static SemanticModel.Capsule capsuleExecutionFixture() {
        SemanticModel.Schema sourceSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true)));

        SemanticModel.Input input =
                new SemanticModel.Input(
                        "orders",
                        "data/orders.csv",
                        "data/orders.schema.json",
                        "csv",
                        sourceSchema);

        SemanticModel.Schema upstreamSchema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "join_key",
                                        SemanticModel.Type.TEXT,
                                        false),
                                sourceSchema.columns.get(1)));

        SemanticModel.Step rename =
                new SemanticModel.Step(
                        "renamed",
                        SemanticModel.Operation.RENAME,
                        upstreamSchema,
                        "source",
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

        SemanticModel.Relation upstream =
                new SemanticModel.Relation(
                        "upstream",
                        List.of(),
                        List.of(
                                new SemanticModel.Binding(
                                        "source",
                                        SemanticModel.SourceKind.INPUT,
                                        "orders",
                                        sourceSchema)),
                        List.of(rename),
                        "renamed",
                        upstreamSchema);

        SemanticModel.Schema downstreamSchema =
                new SemanticModel.Schema(
                        List.of(
                                upstreamSchema.columns.get(0)));

        SemanticModel.Step project =
                new SemanticModel.Step(
                        "final",
                        SemanticModel.Operation.PROJECT,
                        downstreamSchema,
                        "prior",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                "join_key"),
                        List.of(),
                        Map.of());

        SemanticModel.Relation downstream =
                new SemanticModel.Relation(
                        "downstream",
                        List.of(
                                "upstream"),
                        List.of(
                                new SemanticModel.Binding(
                                        "prior",
                                        SemanticModel.SourceKind.RELATION,
                                        "upstream",
                                        upstreamSchema)),
                        List.of(project),
                        "final",
                        downstreamSchema);

        SemanticModel.Output output =
                new SemanticModel.Output(
                        "result",
                        "downstream",
                        "generated/result.csv");

        /*
         * Downstream intentionally precedes upstream.
         */
        return new SemanticModel.Capsule(
                "TEST",
                "1.0.0",
                List.of(input),
                List.of(
                        downstream,
                        upstream),
                List.of(output));
    }

    private static ExecutionModel.RelationData capsuleExecutionInput() {
        SemanticModel.Schema schema =
                capsuleExecutionFixture()
                        .inputs
                        .get(0)
                        .schema;

        return new ExecutionModel.RelationData(
                schema,
                List.of(
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "A",
                                        new java.math.BigDecimal("10"))),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "B",
                                        new java.math.BigDecimal("20")))));
    }

    private static void testCapsuleExecutesForwardDependency() {
        Map<String,ExecutionModel.RelationData> outputs =
                new WitnessExecutor().executeCapsule(
                        capsuleExecutionFixture(),
                        Map.of(
                                "orders",
                                capsuleExecutionInput()));

        ExecutionModel.RelationData result =
                outputs.get("result");

        require(
                result != null,
                "capsule forward dependency output");

        require(
                result.rows.size() == 2,
                "capsule forward dependency row count");

        require(
                result.rows.get(0).get(0).equals("A"),
                "capsule forward dependency first row");

        require(
                result.rows.get(1).get(0).equals("B"),
                "capsule forward dependency second row");
    }

    private static void testCapsuleResolvesBindingAliasBySourceId() {
        ExecutionModel.RelationData result =
                new WitnessExecutor().executeCapsule(
                        capsuleExecutionFixture(),
                        Map.of(
                                "orders",
                                capsuleExecutionInput()))
                        .get("result");

        require(
                result != null &&
                result.schema.columns.get(0)
                        .name.equals("join_key"),
                "capsule binding alias resolves sourceId");
    }

    private static void testCapsuleReturnsDeclaredOutputIds() {
        Map<String,ExecutionModel.RelationData> outputs =
                new WitnessExecutor().executeCapsule(
                        capsuleExecutionFixture(),
                        Map.of(
                                "orders",
                                capsuleExecutionInput()));

        require(
                outputs.size() == 1,
                "capsule declared output count");

        require(
                outputs.containsKey("result"),
                "capsule declared output id");

        require(
                !outputs.containsKey("downstream"),
                "capsule does not expose relation id as output id");
    }

    private static void testCapsuleMissingInputRejected() {
        try {
            new WitnessExecutor().executeCapsule(
                    capsuleExecutionFixture(),
                    Map.of());

            throw new AssertionError(
                    "Expected missing capsule input failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "unavailable input"),
                    "capsule missing input message");
        }
    }

    private static void testCapsuleInputSchemaMismatchRejected() {
        SemanticModel.Schema wrong =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "wrong",
                                        SemanticModel.Type.TEXT,
                                        false)));

        try {
            new WitnessExecutor().executeCapsule(
                    capsuleExecutionFixture(),
                    Map.of(
                            "orders",
                            new ExecutionModel.RelationData(
                                    wrong,
                                    List.of())));

            throw new AssertionError(
                    "Expected capsule input schema mismatch");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "runtime schema does not match"),
                    "capsule input schema mismatch message");
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
