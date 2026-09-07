import java.util.*;

public final class CapsuleNormalizer implements Normalizer {

    @Override
    public NormalizationResult normalize(
            RawModel.Capsule capsule,
            Map<String,RawModel.RelationDocument> relations,
            Map<String,RawSchema> schemas) {

        Objects.requireNonNull(capsule);
        Objects.requireNonNull(relations);
        Objects.requireNonNull(schemas);

        validateUniqueInputs(capsule.inputs);
        validateUniqueRelations(capsule.relations);
        validateUniqueOutputs(capsule.outputs);

        Map<String,RawModel.Relation> relationById =
                new LinkedHashMap<>();

        for (RawModel.Relation relation : capsule.relations) {
            relationById.put(relation.id, relation);
        }

        List<SemanticModel.Input> inputs =
                new ArrayList<>();

        for (RawModel.Input input : capsule.inputs) {
            inputs.add(
                    new SemanticModel.Input(
                            input.id,
                            input.path,
                            input.schemaPath,
                            input.format,
                            requireSchema(
                                    input.id,
                                    input.schemaPath,
                                    schemas)));
        }

        Map<String,SemanticModel.Input> semanticInputById =
                new LinkedHashMap<>();

        Map<String,String> inputIdByPath =
                new LinkedHashMap<>();

        for (SemanticModel.Input input : inputs) {
            semanticInputById.put(
                    input.id,
                    input);

            inputIdByPath.put(
                    normalizeReference(input.path),
                    input.id);
        }

        Map<String,String> relationIdByPath =
                new LinkedHashMap<>();

        for (RawModel.Relation relation : capsule.relations) {
            relationIdByPath.put(
                    normalizeReference(relation.path),
                    relation.id);
        }

        Map<String,SemanticModel.Relation> normalizedRelations =
                new LinkedHashMap<>();

        Set<String> visitingRelations =
                new LinkedHashSet<>();

        for (RawModel.Relation relation : capsule.relations) {
            normalizeRelation(
                    relation.id,
                    relationById,
                    relations,
                    semanticInputById,
                    inputIdByPath,
                    relationIdByPath,
                    normalizedRelations,
                    visitingRelations);
        }

        List<SemanticModel.Relation> semanticRelations =
                new ArrayList<>();

        for (RawModel.Relation relation : capsule.relations) {
            semanticRelations.add(
                    normalizedRelations.get(
                            relation.id));
        }

        List<SemanticModel.Output> outputs =
                new ArrayList<>();

        List<VerificationModel.Expectation> expectations =
                new ArrayList<>();

        for (RawModel.Output output : capsule.outputs) {

            if (!relationById.containsKey(output.relation)) {
                throw new IllegalArgumentException(
                        "output " + output.id +
                        " references unknown relation: " +
                        output.relation);
            }

            outputs.add(
                    new SemanticModel.Output(
                            output.id,
                            output.relation,
                            output.generatedPath));

            SemanticModel.Relation outputRelation =
                    normalizedRelations.get(
                            output.relation);

            if (outputRelation == null) {
                throw new IllegalArgumentException(
                        "output " + output.id +
                        " has no normalized relation: " +
                        output.relation);
            }

            requireSchema(
                    "output " + output.id,
                    output.schemaPath,
                    schemas);

            expectations.add(
                    new VerificationModel.Expectation(
                            output.id,
                            output.expectedPath,
                            output.schemaPath,
                            outputRelation.outputSchema,
                            VerificationModel.Comparison.TYPED));
        }

        SemanticModel.Capsule semanticCapsule =
                new SemanticModel.Capsule(
                        capsule.id,
                        capsule.version,
                        inputs,
                        semanticRelations,
                        outputs);

        VerificationModel.Plan verification =
                new VerificationModel.Plan(expectations);

        return new NormalizationResult(
                semanticCapsule,
                verification);
    }

    private static SemanticModel.Relation normalizeRelation(
            String relationId,
            Map<String,RawModel.Relation> relationById,
            Map<String,RawModel.RelationDocument> relationDocuments,
            Map<String,SemanticModel.Input> inputById,
            Map<String,String> inputIdByPath,
            Map<String,String> relationIdByPath,
            Map<String,SemanticModel.Relation> normalizedRelations,
            Set<String> visitingRelations) {

        SemanticModel.Relation existing =
                normalizedRelations.get(relationId);

        if (existing != null)
            return existing;

        RawModel.Relation declaration =
                relationById.get(relationId);

        if (declaration == null) {
            throw new IllegalArgumentException(
                    "unknown relation dependency: " +
                    relationId);
        }

        if (!visitingRelations.add(relationId)) {
            throw new IllegalArgumentException(
                    "cyclic relation dependency involving: " +
                    relationId);
        }

        try {
            RawModel.RelationDocument document =
                    relationDocuments.get(relationId);

            if (document == null) {
                throw new IllegalArgumentException(
                        "missing relation document: " +
                        relationId);
            }

            List<String> dependencies =
                    resolveRelationDependencies(
                            relationId,
                            document.inputs,
                            relationById,
                            relationIdByPath);

            for (String dependencyId :
                    dependencies) {

                normalizeRelation(
                        dependencyId,
                        relationById,
                        relationDocuments,
                        inputById,
                        inputIdByPath,
                        relationIdByPath,
                        normalizedRelations,
                        visitingRelations);
            }

            List<SemanticModel.Binding> bindings =
                    normalizeBindings(
                            relationId,
                            document.inputs,
                            inputById,
                            inputIdByPath,
                            relationIdByPath,
                            normalizedRelations);

            List<SemanticModel.Step> steps =
                    normalizeLocalSteps(
                            relationId,
                            bindings,
                            document.steps);

            SemanticModel.Schema outputSchema =
                    resolveLocalOutputSchema(
                            relationId,
                            document.output,
                            bindings,
                            steps);

            SemanticModel.Relation normalized =
                    new SemanticModel.Relation(
                            relationId,
                            dependencies,
                            bindings,
                            steps,
                            document.output,
                            outputSchema);

            normalizedRelations.put(
                    relationId,
                    normalized);

            return normalized;

        } finally {
            visitingRelations.remove(relationId);
        }
    }

    private static List<String> resolveRelationDependencies(
            String relationId,
            List<RawModel.Binding> bindings,
            Map<String,RawModel.Relation> relationById,
            Map<String,String> relationIdByPath) {

        Set<String> dependencies =
                new LinkedHashSet<>();

        for (RawModel.Binding binding :
                bindings) {

            String reference =
                    normalizeReference(
                            binding.reference);

            String dependencyId =
                    relationIdByPath.get(reference);

            if (dependencyId == null &&
                relationById.containsKey(
                        binding.reference)) {

                dependencyId =
                        binding.reference;
            }

            if (dependencyId == null)
                continue;

            if (dependencyId.equals(relationId)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " depends on itself");
            }

            dependencies.add(
                    dependencyId);
        }

        return new ArrayList<>(
                dependencies);
    }

    private static List<SemanticModel.Step> normalizeLocalSteps(
            String relationId,
            List<SemanticModel.Binding> bindings,
            List<RawModel.Step> rawSteps) {

        Map<String,SemanticModel.Schema> available =
                new LinkedHashMap<>();

        for (SemanticModel.Binding binding : bindings) {
            available.put(
                    binding.name,
                    binding.schema);
        }

        List<SemanticModel.Step> result =
                new ArrayList<>();

        Set<String> stepIds =
                new LinkedHashSet<>();

        for (RawModel.Step raw : rawSteps) {

            if (!stepIds.add(raw.id)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " contains duplicate step id: " +
                        raw.id);
            }

            if (available.containsKey(raw.id)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step id conflicts with existing binding: " +
                        raw.id);
            }

            SemanticModel.Step step =
                    normalizeStep(
                            relationId,
                            raw,
                            available);

            result.add(step);

            available.put(
                    step.id,
                    step.outputSchema);
        }

        return result;
    }

    private static SemanticModel.Step normalizeStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        SemanticModel.Operation operation =
                normalizeOperation(
                        relationId,
                        raw);

        switch (operation) {
            case RENAME:
                return normalizeRenameStep(
                        relationId,
                        raw,
                        available);

            case FILTER:
                return normalizeFilterStep(
                        relationId,
                        raw,
                        available);

            case PROJECT:
                return normalizeProjectStep(
                        relationId,
                        raw,
                        available);

            case AGGREGATE:
                return normalizeAggregateStep(
                        relationId,
                        raw,
                        available);

            case JOIN:
                return normalizeJoinStep(
                        relationId,
                        raw,
                        available);

            case LEFT_JOIN:
                return normalizeLeftJoinStep(
                        relationId,
                        raw,
                        available);

            case CROSS_JOIN:
                return normalizeCrossJoinStep(
                        relationId,
                        raw,
                        available);

            case DERIVE:
                return normalizeDeriveStep(
                        relationId,
                        raw,
                        available);

            default:
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " uses unsupported operation: " +
                        raw.op);
        }
    }

    private static SemanticModel.Operation normalizeOperation(
            String relationId,
            RawModel.Step raw) {

        if (raw.op == null || raw.op.isBlank()) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " has empty operation");
        }

        try {
            return SemanticModel.Operation.valueOf(
                    raw.op.trim()
                          .toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " uses unsupported operation: " +
                    raw.op);
        }
    }

    private static SemanticModel.Step normalizeCrossJoinStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        if (raw.left == null ||
            raw.left.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " CROSS_JOIN requires left");
        }

        if (raw.right == null ||
            raw.right.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " CROSS_JOIN requires right");
        }

        SemanticModel.Schema leftSchema =
                available.get(raw.left);

        if (leftSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " CROSS_JOIN references unknown left source: " +
                    raw.left);
        }

        SemanticModel.Schema rightSchema =
                available.get(raw.right);

        if (rightSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " CROSS_JOIN references unknown right source: " +
                    raw.right);
        }

        if (raw.on != null &&
            !raw.on.isEmpty()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " CROSS_JOIN does not accept on conditions");
        }

        Set<String> names =
                new LinkedHashSet<>();

        List<SemanticModel.Column> output =
                new ArrayList<>();

        for (SemanticModel.Column column :
                leftSchema.columns) {

            names.add(column.name);
            output.add(column);
        }

        for (SemanticModel.Column column :
                rightSchema.columns) {

            if (!names.add(column.name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " CROSS_JOIN has duplicate column name: " +
                        column.name);
            }

            output.add(column);
        }

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(output);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.CROSS_JOIN,
                outputSchema,
                null,
                raw.left,
                raw.right,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static SemanticModel.Step normalizeLeftJoinStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        if (raw.left == null ||
            raw.left.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " LEFT_JOIN requires left");
        }

        if (raw.right == null ||
            raw.right.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " LEFT_JOIN requires right");
        }

        SemanticModel.Schema leftSchema =
                available.get(raw.left);

        if (leftSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " LEFT_JOIN references unknown left source: " +
                    raw.left);
        }

        SemanticModel.Schema rightSchema =
                available.get(raw.right);

        if (rightSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " LEFT_JOIN references unknown right source: " +
                    raw.right);
        }

        if (raw.on == null ||
            raw.on.isEmpty()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " LEFT_JOIN requires at least one on condition");
        }

        List<SemanticModel.Condition> conditions =
                new ArrayList<>();

        Set<String> equalityKeys =
                new LinkedHashSet<>();

        for (RawModel.Condition rawCondition :
                raw.on) {

            SemanticModel.Condition condition =
                    normalizeJoinCondition(
                            relationId,
                            raw.id,
                            leftSchema,
                            rightSchema,
                            rawCondition);

            conditions.add(condition);

            if ("=".equals(condition.operator) &&
                condition.left.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.right.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.left.column.equals(
                        condition.right.column)) {

                equalityKeys.add(
                        condition.left.column);
            }
        }

        Set<String> leftNames =
                new LinkedHashSet<>();

        List<SemanticModel.Column> output =
                new ArrayList<>();

        for (SemanticModel.Column column :
                leftSchema.columns) {

            leftNames.add(column.name);
            output.add(column);
        }

        for (SemanticModel.Column rightColumn :
                rightSchema.columns) {

            if (leftNames.contains(
                    rightColumn.name)) {

                if (!equalityKeys.contains(
                        rightColumn.name)) {

                    throw new IllegalArgumentException(
                            "relation " + relationId +
                            " step " + raw.id +
                            " LEFT_JOIN has ambiguous non-key output column: " +
                            rightColumn.name);
                }

                continue;
            }

            output.add(
                    new SemanticModel.Column(
                            rightColumn.name,
                            rightColumn.type,
                            true,
                            rightColumn.precision,
                            rightColumn.scale));
        }

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(output);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.LEFT_JOIN,
                outputSchema,
                null,
                raw.left,
                raw.right,
                conditions,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static SemanticModel.Step normalizeJoinStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        if (raw.left == null ||
            raw.left.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " JOIN requires left");
        }

        if (raw.right == null ||
            raw.right.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " JOIN requires right");
        }

        SemanticModel.Schema leftSchema =
                available.get(raw.left);

        if (leftSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " JOIN references unknown left source: " +
                    raw.left);
        }

        SemanticModel.Schema rightSchema =
                available.get(raw.right);

        if (rightSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " JOIN references unknown right source: " +
                    raw.right);
        }

        if (raw.on == null ||
            raw.on.isEmpty()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " JOIN requires at least one on condition");
        }

        List<SemanticModel.Condition> conditions =
                new ArrayList<>();

        List<String> equalityKeys =
                new ArrayList<>();

        for (RawModel.Condition rawCondition :
                raw.on) {

            SemanticModel.Condition condition =
                    normalizeJoinCondition(
                            relationId,
                            raw.id,
                            leftSchema,
                            rightSchema,
                            rawCondition);

            conditions.add(condition);

            if ("=".equals(condition.operator) &&
                condition.left.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.right.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.left.column.equals(
                        condition.right.column)) {

                equalityKeys.add(
                        condition.left.column);
            }
        }


        Set<String> leftNames =
                new LinkedHashSet<>();

        List<SemanticModel.Column> output =
                new ArrayList<>();

        for (SemanticModel.Column column :
                leftSchema.columns) {

            leftNames.add(column.name);
            output.add(column);
        }

        for (SemanticModel.Column rightColumn :
                rightSchema.columns) {

            if (!leftNames.contains(
                    rightColumn.name)) {

                output.add(rightColumn);
                continue;
            }

            if (!equalityKeys.contains(
                    rightColumn.name)) {

                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " JOIN has ambiguous non-key output column: " +
                        rightColumn.name);
            }
        }

        SemanticModel.Schema workingSchema =
                new SemanticModel.Schema(output);

        SemanticModel.Schema outputSchema =
                raw.select.isEmpty()
                ? workingSchema
                : normalizeProjectSchema(
                        relationId,
                        raw.id,
                        workingSchema,
                        raw.select);

        validateDeriveOrderBy(
                relationId,
                raw.id,
                outputSchema,
                raw.orderBy);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.JOIN,
                outputSchema,
                null,
                raw.left,
                raw.right,
                conditions,
                List.of(),
                List.of(),
                List.of(),
                raw.select,
                raw.orderBy,
                Map.of());
    }

    private static SemanticModel.Condition normalizeJoinCondition(
            String relationId,
            String stepId,
            SemanticModel.Schema leftSchema,
            SemanticModel.Schema rightSchema,
            RawModel.Condition raw) {

        if (raw == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " JOIN contains null on condition");
        }

        if (raw.operator == null ||
            raw.operator.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " JOIN condition has empty operator");
        }

        ExpressionNormalizer.TypedExpression left =
                ExpressionNormalizer.normalizeAgainst(
                        leftSchema,
                        raw.left,
                        null);

        ExpressionNormalizer.TypedExpression right =
                ExpressionNormalizer.normalizeAgainst(
                        rightSchema,
                        raw.right,
                        null);

        if (!ExpressionNormalizer.compatible(
                left.type,
                right.type)) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " JOIN compares incompatible types: " +
                    left.type + " and " + right.type);
        }

        ExpressionNormalizer.validateComparison(
                raw.operator,
                left.type,
                right.type);

        return new SemanticModel.Condition(
                left.expression,
                raw.operator,
                right.expression);
    }

    private static SemanticModel.Step normalizeAggregateStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        SemanticModel.Schema inputSchema =
                available.get(raw.from);

        if (inputSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " references unknown local source: " +
                    raw.from);
        }

        if (raw.groupBy == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " AGGREGATE has null group_by");
        }

        if (raw.measures == null ||
            raw.measures.isEmpty()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " AGGREGATE requires at least one measure");
        }

        Map<String,SemanticModel.Column> columns =
                new LinkedHashMap<>();

        for (SemanticModel.Column column :
                inputSchema.columns) {

            columns.put(column.name, column);
        }

        Set<String> outputNames =
                new LinkedHashSet<>();

        List<SemanticModel.Column> outputColumns =
                new ArrayList<>();

        List<String> normalizedGroupBy =
                new ArrayList<>();

        for (String name : raw.groupBy) {

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " AGGREGATE contains empty group_by column");
            }

            SemanticModel.Column column =
                    columns.get(name);

            if (column == null) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " AGGREGATE references unknown group_by column: " +
                        name);
            }

            if (!outputNames.add(name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " AGGREGATE contains duplicate group_by column: " +
                        name);
            }

            normalizedGroupBy.add(name);
            outputColumns.add(column);
        }

        List<SemanticModel.Measure> normalizedMeasures =
                new ArrayList<>();

        for (RawModel.Measure rawMeasure :
                raw.measures) {

            if (rawMeasure.name == null ||
                rawMeasure.name.isBlank()) {

                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " AGGREGATE contains empty measure name");
            }

            if (!outputNames.add(rawMeasure.name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " AGGREGATE duplicates output column: " +
                        rawMeasure.name);
            }

            SemanticModel.Measure measure =
                    normalizeMeasure(
                            relationId,
                            raw.id,
                            columns,
                            rawMeasure);

            normalizedMeasures.add(measure);

            outputColumns.add(
                    measureOutputColumn(
                            relationId,
                            raw.id,
                            columns,
                            rawMeasure));
        }

        SemanticModel.Schema outputSchema =
                new SemanticModel.Schema(
                        outputColumns);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.AGGREGATE,
                outputSchema,
                raw.from,
                null,
                null,
                List.of(),
                normalizedGroupBy,
                normalizedMeasures,
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static SemanticModel.Measure normalizeMeasure(
            String relationId,
            String stepId,
            Map<String,SemanticModel.Column> columns,
            RawModel.Measure raw) {

        if (raw.function == null ||
            raw.function.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " AGGREGATE measure " + raw.name +
                    " has empty function");
        }

        String function =
                raw.function.trim().toUpperCase(Locale.ROOT);

        if (!Set.of(
                "COUNT",
                "COUNT_DISTINCT",
                "SUM",
                "MIN",
                "MAX").contains(function)) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " AGGREGATE uses unsupported measure function: " +
                    raw.function);
        }

        if (raw.column == null ||
            raw.column.isBlank()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " AGGREGATE measure " + raw.name +
                    " requires column");
        }

        if (!columns.containsKey(raw.column)) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " AGGREGATE measure " + raw.name +
                    " references unknown column: " +
                    raw.column);
        }

        return new SemanticModel.Measure(
                raw.name,
                function,
                raw.column);
    }

    private static SemanticModel.Column measureOutputColumn(
            String relationId,
            String stepId,
            Map<String,SemanticModel.Column> columns,
            RawModel.Measure raw) {

        SemanticModel.Column source =
                columns.get(raw.column);

        String function =
                raw.function.trim().toUpperCase(Locale.ROOT);

        SemanticModel.Type type;
        boolean nullable;

        Integer precision = null;
        Integer scale = null;

        switch (function) {
            case "COUNT":
            case "COUNT_DISTINCT":
                type = SemanticModel.Type.INTEGER;
                nullable = false;
                break;

            case "SUM":
                if (!ExpressionNormalizer.compatible(
                        source.type,
                        SemanticModel.Type.DECIMAL)) {

                    throw new IllegalArgumentException(
                            "relation " + relationId +
                            " step " + stepId +
                            " SUM requires numeric column: " +
                            raw.column);
                }

                type =
                        source.type ==
                        SemanticModel.Type.DECIMAL
                        ? SemanticModel.Type.DECIMAL
                        : SemanticModel.Type.INTEGER;

                /*
                 * Deliberately do not retain source precision.
                 *
                 * SUM can exceed the precision of an individual
                 * source value. A future explicit aggregate result
                 * contract can constrain it when required.
                 */
                precision = null;
                scale = null;

                nullable = true;
                break;

            case "MIN":
            case "MAX":
                type = source.type;

                /*
                 * MIN/MAX return an existing value from the source
                 * domain, so preserving its declaration is safe.
                 */
                precision = source.precision;
                scale = source.scale;

                nullable = true;
                break;

            default:
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " unsupported measure function: " +
                        raw.function);
        }

        return new SemanticModel.Column(
                raw.name,
                type,
                nullable,
                precision,
                scale);
    }
    private static SemanticModel.Step normalizeDeriveStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        SemanticModel.Schema inputSchema =
                available.get(raw.from);

        if (inputSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " references unknown local source: " +
                    raw.from);
        }

        if (raw.columns == null ||
            raw.columns.isEmpty()) {

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " DERIVE requires at least one column");
        }

        List<SemanticModel.DerivedColumn> derived =
                new ArrayList<>();

        List<SemanticModel.Column> output =
                new ArrayList<>(inputSchema.columns);

        Set<String> names =
                new LinkedHashSet<>();

        for (SemanticModel.Column column :
                inputSchema.columns) {

            names.add(column.name);
        }

        for (RawModel.DerivedColumn rawColumn :
                raw.columns) {

            if (rawColumn.name == null ||
                rawColumn.name.isBlank()) {

                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " DERIVE contains empty column name");
            }

            if (!names.add(rawColumn.name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + raw.id +
                        " DERIVE duplicates or conflicts with column: " +
                        rawColumn.name);
            }

            ExpressionNormalizer.TypedExpression typed =
                    normalizeDerivedExpression(
                            relationId,
                            raw.id,
                            inputSchema,
                            rawColumn.expression);

            Integer precision =
                    rawColumn.precision;

            Integer scale =
                    rawColumn.scale;

            if (precision != null ||
                scale != null) {

                if (typed.type !=
                    SemanticModel.Type.DECIMAL) {

                    throw new IllegalArgumentException(
                            "relation " + relationId +
                            " step " + raw.id +
                            " DERIVE column " +
                            rawColumn.name +
                            " declares precision/scale but expression type is " +
                            typed.type);
                }

                try {
                    Numeric.validateDeclaration(
                            precision,
                            scale);

                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            "relation " + relationId +
                            " step " + raw.id +
                            " DERIVE column " +
                            rawColumn.name +
                            " has invalid NUMBER declaration: " +
                            ex.getMessage(),
                            ex);
                }
            }

            derived.add(
                    new SemanticModel.DerivedColumn(
                            rawColumn.name,
                            typed.expression,
                            precision,
                            scale));

            output.add(
                    new SemanticModel.Column(
                            rawColumn.name,
                            typed.type,
                            typed.nullable,
                            precision,
                            scale));
        }

        SemanticModel.Schema workingSchema =
                new SemanticModel.Schema(output);

        SemanticModel.Schema outputSchema =
                raw.select.isEmpty()
                ? workingSchema
                : normalizeProjectSchema(
                        relationId,
                        raw.id,
                        workingSchema,
                        raw.select);

        validateDeriveOrderBy(
                relationId,
                raw.id,
                outputSchema,
                raw.orderBy);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.DERIVE,
                outputSchema,
                raw.from,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                derived,
                raw.select,
                raw.orderBy,
                Map.of());
    }

    private static void validateDeriveOrderBy(
            String relationId,
            String stepId,
            SemanticModel.Schema schema,
            List<String> orderBy) {

        Set<String> names = new LinkedHashSet<>();

        for (SemanticModel.Column column : schema.columns) {
            names.add(column.name);
        }

        Set<String> seen = new LinkedHashSet<>();

        for (String name : orderBy) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " DERIVE order_by contains empty column name");
            }

            if (!seen.add(name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " DERIVE order_by contains duplicate column: " +
                        name);
            }

            if (!names.contains(name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " DERIVE order_by references unknown column: " +
                        name);
            }
        }
    }

    private static ExpressionNormalizer.TypedExpression
            normalizeDerivedExpression(
                    String relationId,
                    String stepId,
                    SemanticModel.Schema schema,
                    RawModel.RawExpression raw) {

        if (raw == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " DERIVE contains null expression");
        }

        try {
            return ExpressionNormalizer.normalizeAgainst(
                    schema,
                    raw,
                    null);

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " DERIVE " + ex.getMessage(),
                    ex);
        }
    }

    private static SemanticModel.Step normalizeProjectStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        SemanticModel.Schema inputSchema =
                available.get(raw.from);

        if (inputSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " references unknown local source: " +
                    raw.from);
        }

        SemanticModel.Schema outputSchema =
                normalizeProjectSchema(
                        relationId,
                        raw.id,
                        inputSchema,
                        raw.select);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.PROJECT,
                outputSchema,
                raw.from,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                raw.select,
                List.of(),
                Map.of());
    }

    private static SemanticModel.Schema normalizeProjectSchema(
            String relationId,
            String stepId,
            SemanticModel.Schema inputSchema,
            List<String> select) {

        if (select == null || select.isEmpty()) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " PROJECT requires at least one column");
        }

        Map<String,SemanticModel.Column> inputByName =
                new LinkedHashMap<>();

        for (SemanticModel.Column column :
                inputSchema.columns) {

            inputByName.put(
                    column.name,
                    column);
        }

        Set<String> seen =
                new LinkedHashSet<>();

        List<SemanticModel.Column> output =
                new ArrayList<>();

        for (String name : select) {

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " PROJECT contains empty column name");
            }

            if (!seen.add(name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " PROJECT contains duplicate column: " +
                        name);
            }

            SemanticModel.Column column =
                    inputByName.get(name);

            if (column == null) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " PROJECT references unknown column: " +
                        name);
            }

            /*
             * Column is immutable, so the normalized projection
             * can safely retain the authoritative column definition.
             */
            output.add(column);
        }

        return new SemanticModel.Schema(output);
    }

    private static SemanticModel.Step normalizeFilterStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        SemanticModel.Schema inputSchema =
                available.get(raw.from);

        if (inputSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " references unknown local source: " +
                    raw.from);
        }

        if (raw.where == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " FILTER requires where");
        }

        SemanticModel.Condition condition =
                normalizeCondition(
                        relationId,
                        raw.id,
                        inputSchema,
                        raw.where);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.FILTER,
                inputSchema,
                raw.from,
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
    private static SemanticModel.Condition normalizeCondition(
            String relationId,
            String stepId,
            SemanticModel.Schema schema,
            RawModel.Condition raw) {

        try {
            return ExpressionNormalizer.normalizeCondition(
                    schema,
                    raw);

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " " + ex.getMessage(),
                    ex);
        }
    }
    private static SemanticModel.Step normalizeRenameStep(
            String relationId,
            RawModel.Step raw,
            Map<String,SemanticModel.Schema> available) {

        SemanticModel.Schema inputSchema =
                available.get(raw.from);

        if (inputSchema == null) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + raw.id +
                    " references unknown local source: " +
                    raw.from);
        }

        SemanticModel.Schema outputSchema =
                normalizeRenameSchema(
                        relationId,
                        raw.id,
                        inputSchema,
                        raw.rename);

        Map<String,String> normalizedRename =
                new LinkedHashMap<>(raw.rename);

        return new SemanticModel.Step(
                raw.id,
                SemanticModel.Operation.RENAME,
                outputSchema,
                raw.from,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                normalizedRename);
    }

    private static SemanticModel.Schema normalizeRenameSchema(
            String relationId,
            String stepId,
            SemanticModel.Schema input,
            Map<String,String> rename) {

        if (rename == null || rename.isEmpty()) {
            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " step " + stepId +
                    " RENAME contains no mappings");
        }

        Set<String> inputNames =
                new LinkedHashSet<>();

        for (SemanticModel.Column column : input.columns) {
            inputNames.add(column.name);
        }

        Set<String> targetNames =
                new LinkedHashSet<>();

        for (Map.Entry<String,String> entry : rename.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();

            if (source == null ||
                source.isBlank() ||
                !inputNames.contains(source)) {

                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " RENAME references unknown source column: " +
                        source);
            }

            if (target == null || target.isBlank()) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " RENAME has empty target for column: " +
                        source);
            }

            if (!targetNames.add(target)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " RENAME maps multiple columns to target: " +
                        target);
            }
        }

        Set<String> untouched =
                new LinkedHashSet<>(inputNames);

        untouched.removeAll(rename.keySet());

        for (String target : targetNames) {
            if (untouched.contains(target)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " RENAME target conflicts with existing unrenamed column: " +
                        target);
            }
        }

        List<SemanticModel.Column> output =
                new ArrayList<>();

        Set<String> finalNames =
                new LinkedHashSet<>();

        for (SemanticModel.Column column : input.columns) {
            String name =
                    rename.getOrDefault(
                            column.name,
                            column.name);

            if (!finalNames.add(name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " step " + stepId +
                        " RENAME produces duplicate column: " +
                        name);
            }

            output.add(
                    new SemanticModel.Column(
                            name,
                            column.type,
                            column.nullable,
                            column.precision,
                            column.scale));
        }

        return new SemanticModel.Schema(output);
    }

    private static SemanticModel.Schema resolveLocalOutputSchema(
            String relationId,
            String outputName,
            List<SemanticModel.Binding> bindings,
            List<SemanticModel.Step> steps) {

        for (SemanticModel.Step step : steps) {
            if (step.id.equals(outputName))
                return step.outputSchema;
        }

        for (SemanticModel.Binding binding : bindings) {
            if (binding.name.equals(outputName))
                return binding.schema;
        }

        /*
         * Empty-step prototype relations still exist in the
         * current unit harness. Preserve that seam temporarily.
         */
        if (steps.isEmpty() &&
            (outputName == null || outputName.isEmpty())) {

            return placeholderSchema(relationId);
        }

        throw new IllegalArgumentException(
                "relation " + relationId +
                " declares unknown local output: " +
                outputName);
    }

    private static List<SemanticModel.Binding> normalizeBindings(
            String relationId,
            List<RawModel.Binding> rawBindings,
            Map<String,SemanticModel.Input> inputById,
            Map<String,String> inputIdByPath,
            Map<String,String> relationIdByPath,
            Map<String,SemanticModel.Relation> normalizedRelations) {

        List<SemanticModel.Binding> result =
                new ArrayList<>();

        Set<String> bindingNames =
                new LinkedHashSet<>();

        for (RawModel.Binding binding : rawBindings) {

            if (!bindingNames.add(binding.name)) {
                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " contains duplicate binding name: " +
                        binding.name);
            }

            String reference =
                    normalizeReference(binding.reference);

            if (reference.equals("expected") ||
                reference.startsWith("expected/")) {

                throw new IllegalArgumentException(
                        "relation " + relationId +
                        " uses expected evidence as execution input '" +
                        binding.reference + "'");
            }

            String inputId =
                    inputIdByPath.get(reference);

            if (inputId == null &&
                inputById.containsKey(binding.reference)) {

                inputId = binding.reference;
            }

            if (inputId != null) {
                SemanticModel.Input input =
                        inputById.get(inputId);

                result.add(
                        new SemanticModel.Binding(
                                binding.name,
                                SemanticModel.SourceKind.INPUT,
                                input.id,
                                input.schema));

                continue;
            }

            String dependencyId =
                    relationIdByPath.get(reference);

            if (dependencyId == null &&
                normalizedRelations.containsKey(
                        binding.reference)) {

                dependencyId =
                        binding.reference;
            }

            if (dependencyId != null) {
                SemanticModel.Relation dependency =
                        normalizedRelations.get(
                                dependencyId);

                if (dependency == null) {
                    throw new IllegalArgumentException(
                            "relation " + relationId +
                            " binding '" + binding.name +
                            "' references unresolved relation: " +
                            dependencyId);
                }

                result.add(
                        new SemanticModel.Binding(
                                binding.name,
                                SemanticModel.SourceKind.RELATION,
                                dependency.id,
                                dependency.outputSchema));

                continue;
            }

            throw new IllegalArgumentException(
                    "relation " + relationId +
                    " binding '" + binding.name +
                    "' references unknown source: " +
                    binding.reference);
        }

        return result;
    }

    private static String normalizeReference(
            String reference) {

        if (reference == null)
            return "";

        return reference
                .replace('\\', '/')
                .replaceAll("/+", "/");
    }

    private static SemanticModel.Schema requireSchema(
            String owner,
            String schemaPath,
            Map<String,RawSchema> schemas) {

        if (schemaPath == null || schemaPath.isBlank()) {
            throw new IllegalArgumentException(
                    "input " + owner +
                    " does not declare schemaPath");
        }

        RawSchema raw =
                schemas.get(schemaPath);

        if (raw == null) {
            throw new IllegalArgumentException(
                    "input " + owner +
                    " references missing schema: " +
                    schemaPath);
        }

        return normalizeSchema(
                owner,
                raw);
    }

    private static SemanticModel.Schema normalizeSchema(
            String owner,
            RawSchema raw) {

        Objects.requireNonNull(raw);

        List<SemanticModel.Column> columns =
                new ArrayList<>();

        Set<String> names =
                new LinkedHashSet<>();

        for (RawSchema.Column column : raw.columns) {

            if (column.name == null ||
                column.name.isBlank()) {

                throw new IllegalArgumentException(
                        "schema for " + owner +
                        " contains empty column name");
            }

            if (!names.add(column.name)) {
                throw new IllegalArgumentException(
                        "schema for " + owner +
                        " contains duplicate column: " +
                        column.name);
            }

            SemanticModel.Type type =
                    normalizeType(
                            owner,
                            column.name,
                            column.type);

            boolean declaredNumber =
                    "NUMBER".equalsIgnoreCase(
                            column.type.trim());

            if (declaredNumber) {
                Numeric.validateDeclaration(
                        column.precision,
                        column.scale);

            } else if (column.precision != null ||
                       column.scale != null) {

                throw new IllegalArgumentException(
                        "schema for " + owner +
                        " column " + column.name +
                        " declares precision/scale but type is " +
                        column.type +
                        "; precision/scale currently require NUMBER");
            }

            columns.add(
                    new SemanticModel.Column(
                            column.name,
                            type,
                            column.nullable,
                            declaredNumber
                                    ? column.precision
                                    : null,
                            declaredNumber
                                    ? column.scale
                                    : null));
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "schema for " + owner +
                    " contains no columns");
        }

        return new SemanticModel.Schema(columns);
    }

    private static SemanticModel.Type normalizeType(
            String owner,
            String column,
            String rawType) {

        if (rawType == null ||
            rawType.isBlank()) {

            throw new IllegalArgumentException(
                    "schema for " + owner +
                    " column " + column +
                    " has empty type");
        }

        String normalized =
                rawType.trim()
                       .toUpperCase(Locale.ROOT);

        /*
         * NUMBER is the schema-level exact decimal contract.
         * BigDecimal-backed DECIMAL remains the runtime carrier.
         */
        if ("NUMBER".equals(normalized)) {
            return SemanticModel.Type.DECIMAL;
        }

        try {
            return SemanticModel.Type.valueOf(
                    normalized);

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "schema for " + owner +
                    " column " + column +
                    " uses unsupported type: " +
                    rawType);
        }
    }

    /*
     * Temporary relation-output seam.
     *
     * Relation schemas will be derived from normalized
     * bindings and steps in the next normalization phase.
     */
    private static SemanticModel.Schema placeholderSchema(
            String owner) {

        return new SemanticModel.Schema(
                List.of());
    }
    private static void validateUniqueInputs(
            List<RawModel.Input> inputs) {

        Set<String> seen =
                new LinkedHashSet<>();

        for (RawModel.Input input : inputs) {
            if (!seen.add(input.id)) {
                throw new IllegalArgumentException(
                        "duplicate input id: " +
                        input.id);
            }
        }
    }

    private static void validateUniqueRelations(
            List<RawModel.Relation> relations) {

        Set<String> seen =
                new LinkedHashSet<>();

        for (RawModel.Relation relation : relations) {
            if (!seen.add(relation.id)) {
                throw new IllegalArgumentException(
                        "duplicate relation id: " +
                        relation.id);
            }
        }
    }

    private static void validateUniqueOutputs(
            List<RawModel.Output> outputs) {

        Set<String> seen =
                new LinkedHashSet<>();

        for (RawModel.Output output : outputs) {
            if (!seen.add(output.id)) {
                throw new IllegalArgumentException(
                        "duplicate output id: " +
                        output.id);
            }
        }
    }
}
