import java.util.*;
import java.nio.file.Path;

public final class WitnessExecutor {

    public Map<String,ExecutionModel.RelationData> executeCapsule(
            SemanticModel.Capsule capsule,
            Map<String,ExecutionModel.RelationData> inputs) {

        Objects.requireNonNull(capsule);
        Objects.requireNonNull(inputs);

        Map<String,SemanticModel.Input> inputById =
                new LinkedHashMap<>();

        for (SemanticModel.Input input :
                capsule.inputs) {

            inputById.put(
                    input.id,
                    input);
        }

        /*
         * Validate supplied runtime data for every declared package
         * input before executing relations.
         */
        for (SemanticModel.Input input :
                capsule.inputs) {

            ExecutionModel.RelationData data =
                    inputs.get(input.id);

            if (data == null) {
                throw new IllegalArgumentException(
                        "capsule " + capsule.id +
                        " requires unavailable input: " +
                        input.id);
            }

            if (!sameSchema(
                    input.schema,
                    data.schema)) {

                throw new IllegalArgumentException(
                        "capsule " + capsule.id +
                        " input " + input.id +
                        " runtime schema does not match normalized input schema");
            }
        }

        Map<String,SemanticModel.Relation> relationById =
                new LinkedHashMap<>();

        for (SemanticModel.Relation relation :
                capsule.relations) {

            relationById.put(
                    relation.id,
                    relation);
        }

        Map<String,ExecutionModel.RelationData> completed =
                new LinkedHashMap<>();

        Set<String> remaining =
                new LinkedHashSet<>();

        for (SemanticModel.Relation relation :
                capsule.relations) {

            remaining.add(
                    relation.id);
        }

        while (!remaining.isEmpty()) {
            boolean progressed = false;

            for (SemanticModel.Relation relation :
                    capsule.relations) {

                if (!remaining.contains(
                        relation.id)) {

                    continue;
                }

                boolean ready = true;

                for (String dependency :
                        relation.dependencies) {

                    if (!completed.containsKey(
                            dependency)) {

                        ready = false;
                        break;
                    }
                }

                if (!ready) {
                    continue;
                }

                Map<String,ExecutionModel.RelationData> localBindings =
                        new LinkedHashMap<>();

                for (SemanticModel.Binding binding :
                        relation.bindings) {

                    ExecutionModel.RelationData data;

                    switch (binding.sourceKind) {
                        case INPUT:
                            if (!inputById.containsKey(
                                    binding.sourceId)) {

                                throw new IllegalArgumentException(
                                        "relation " + relation.id +
                                        " binding " + binding.name +
                                        " references unknown input source: " +
                                        binding.sourceId);
                            }

                            data =
                                    inputs.get(
                                            binding.sourceId);
                            break;

                        case RELATION:
                            if (!relationById.containsKey(
                                    binding.sourceId)) {

                                throw new IllegalArgumentException(
                                        "relation " + relation.id +
                                        " binding " + binding.name +
                                        " references unknown relation source: " +
                                        binding.sourceId);
                            }

                            data =
                                    completed.get(
                                            binding.sourceId);
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    "relation " + relation.id +
                                    " binding " + binding.name +
                                    " has unsupported source kind: " +
                                    binding.sourceKind);
                    }

                    if (data == null) {
                        throw new IllegalArgumentException(
                                "relation " + relation.id +
                                " binding " + binding.name +
                                " source is unavailable: " +
                                binding.sourceId);
                    }

                    if (!sameSchema(
                            binding.schema,
                            data.schema)) {

                        throw new IllegalArgumentException(
                                "relation " + relation.id +
                                " binding " + binding.name +
                                " source schema does not match normalized binding schema");
                    }

                    localBindings.put(
                            binding.name,
                            data);
                }

                ExecutionModel.RelationData result =
                        executeRelation(
                                relation,
                                localBindings);

                completed.put(
                        relation.id,
                        result);

                remaining.remove(
                        relation.id);

                progressed = true;
            }

            if (!progressed) {
                throw new IllegalArgumentException(
                        "capsule " + capsule.id +
                        " contains unresolved relation dependencies: " +
                        remaining);
            }
        }

        Map<String,ExecutionModel.RelationData> outputs =
                new LinkedHashMap<>();

        for (SemanticModel.Output output :
                capsule.outputs) {

            ExecutionModel.RelationData data =
                    completed.get(
                            output.relation);

            if (data == null) {
                throw new IllegalArgumentException(
                        "output " + output.id +
                        " references unavailable relation: " +
                        output.relation);
            }

            outputs.put(
                    output.id,
                    data);
        }

        return Collections.unmodifiableMap(
                outputs);
    }
    public ExecutionModel.RelationData executeRelation(
            SemanticModel.Relation relation,
            Map<String,ExecutionModel.RelationData> bindings) {

        Objects.requireNonNull(relation);
        Objects.requireNonNull(bindings);

        Map<String,ExecutionModel.RelationData> available =
                new LinkedHashMap<>();

        for (SemanticModel.Binding binding :
                relation.bindings) {

            ExecutionModel.RelationData data =
                    bindings.get(binding.name);

            if (data == null) {
                throw new IllegalArgumentException(
                        "relation " + relation.id +
                        " requires unavailable binding: " +
                        binding.name);
            }

            if (!sameSchema(
                    binding.schema,
                    data.schema)) {

                throw new IllegalArgumentException(
                        "relation " + relation.id +
                        " binding " + binding.name +
                        " runtime schema does not match normalized binding schema");
            }

            available.put(
                    binding.name,
                    data);
        }

        for (SemanticModel.Step step :
                relation.steps) {

            ExecutionModel.RelationData result =
                    executeStep(
                            step,
                            available);

            available.put(
                    step.id,
                    result);
        }

        ExecutionModel.RelationData output =
                available.get(
                        relation.output);

        if (output == null) {
            throw new IllegalArgumentException(
                    "relation " + relation.id +
                    " output is unavailable after execution: " +
                    relation.output);
        }

        if (!sameSchema(
                relation.outputSchema,
                output.schema)) {

            throw new IllegalArgumentException(
                    "relation " + relation.id +
                    " runtime output schema does not match normalized output schema");
        }

        return output;
    }

    public void writeOutputs(
            SemanticModel.Capsule capsule,
            Map<String,ExecutionModel.RelationData> inputs) {

        Objects.requireNonNull(capsule);
        Objects.requireNonNull(inputs);

        Map<String,ExecutionModel.RelationData> outputs =
                executeCapsule(
                        capsule,
                        inputs);

        for (SemanticModel.Output output :
                capsule.outputs) {

            ExecutionModel.RelationData data =
                    outputs.get(output.id);

            if (data == null) {
                throw new IllegalArgumentException(
                        "output " + output.id +
                        " is unavailable after capsule execution");
            }

            OutputWriter.writeCsv(
                    Path.of(output.generatedPath),
                    data);
        }
    }
    private static boolean sameSchema(
            SemanticModel.Schema left,
            SemanticModel.Schema right) {

        if (left.columns.size() !=
            right.columns.size()) {

            return false;
        }

        for (int i = 0;
             i < left.columns.size();
             i++) {

            SemanticModel.Column a =
                    left.columns.get(i);

            SemanticModel.Column b =
                    right.columns.get(i);

            if (!a.name.equals(b.name) ||
                a.type != b.type ||
                a.nullable != b.nullable ||
                !Objects.equals(
                        a.precision,
                        b.precision) ||
                !Objects.equals(
                        a.scale,
                        b.scale)) {

                return false;
            }
        }

        return true;
    }
    public ExecutionModel.RelationData executeStep(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        Objects.requireNonNull(step);
        Objects.requireNonNull(available);

        switch (step.operation) {
            case PROJECT:
                return executeProject(
                        step,
                        available);

            case RENAME:
                return executeRename(
                        step,
                        available);

            case FILTER:
                return executeFilter(
                        step,
                        available);

            case CROSS_JOIN:
                return executeCrossJoin(
                        step,
                        available);

            case JOIN:
                return executeJoin(
                        step,
                        available);

            case LEFT_JOIN:
                return executeLeftJoin(
                        step,
                        available);

            case AGGREGATE:
                return executeAggregate(
                        step,
                        available);

            case DERIVE:
                return executeDerive(
                        step,
                        available);

            default:
                throw new IllegalArgumentException(
                        "witness operation not yet implemented: " +
                        step.operation);
        }
    }

    private static ExecutionModel.RelationData executeDerive(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData input =
                available.get(step.from);

        if (input == null) {
            throw new IllegalArgumentException(
                    "DERIVE step " + step.id +
                    " references unavailable source: " +
                    step.from);
        }

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        Map<String,Integer> workingIndex =
                new LinkedHashMap<>();

        for (int i = 0;
             i < input.schema.columns.size();
             i++) {

            workingIndex.put(
                    input.schema.columns.get(i).name,
                    i);
        }

        for (int i = 0;
             i < step.derivedColumns.size();
             i++) {

            workingIndex.put(
                    step.derivedColumns.get(i).name,
                    input.schema.columns.size() + i);
        }

        for (ExecutionModel.Row inputRow :
                input.rows) {

            List<Object> values =
                    new ArrayList<>(
                            inputRow.values);

            for (SemanticModel.DerivedColumn derived :
                    step.derivedColumns) {

                Object value =
                        ExpressionEvaluator.evaluate(
                                derived.expression,
                                input.schema,
                                inputRow);

                value =
                        enforceDerivedNumericContract(
                                derived,
                                value);

                values.add(value);
            }

            if (!step.select.isEmpty()) {
                List<Object> selected = new ArrayList<>();

                for (String name : step.select) {
                    selected.add(values.get(workingIndex.get(name)));
                }

                values = selected;
            }

            rows.add(new ExecutionModel.Row(values));
        }

        sortRows(step.outputSchema, rows, step.orderBy);

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }

    private static Object enforceDerivedNumericContract(
            SemanticModel.DerivedColumn derived,
            Object value) {

        if (derived.precision == null &&
            derived.scale == null) {

            return value;
        }

        if (value == null) {
            return null;
        }

        try {
            return Numeric.enforce(
                    Numeric.coerce(value),
                    derived.precision,
                    derived.scale);

        } catch (IllegalArgumentException |
                 ArithmeticException ex) {

            throw new IllegalArgumentException(
                    "DERIVE column " +
                    derived.name +
                    " violates declared NUMBER contract: " +
                    ex.getMessage(),
                    ex);
        }
    }

    private static void sortRows(
            SemanticModel.Schema schema,
            List<ExecutionModel.Row> rows,
            List<String> orderBy) {

        if (orderBy.isEmpty()) {
            return;
        }

        Map<String,Integer> indexes = new LinkedHashMap<>();

        for (int i = 0; i < schema.columns.size(); i++) {
            indexes.put(schema.columns.get(i).name, i);
        }

        rows.sort((left, right) -> {
            for (String name : orderBy) {
                int index = indexes.get(name);
                Object leftValue = left.get(index);
                Object rightValue = right.get(index);

                if (leftValue == null && rightValue == null) {
                    continue;
                }

                if (leftValue == null) {
                    return -1;
                }

                if (rightValue == null) {
                    return 1;
                }

                int comparison = PredicateEvaluator.compareValues(
                        leftValue,
                        rightValue,
                        schema.columns.get(index).type,
                        schema.columns.get(index).type);

                if (comparison != 0) {
                    return comparison;
                }
            }

            return 0;
        });
    }

    private static ExecutionModel.RelationData executeAggregate(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData input =
                available.get(step.from);

        if (input == null) {
            throw new IllegalArgumentException(
                    "AGGREGATE step " + step.id +
                    " references unavailable source: " +
                    step.from);
        }

        Map<String,Integer> inputIndex =
                new LinkedHashMap<>();

        for (int i = 0;
             i < input.schema.columns.size();
             i++) {

            inputIndex.put(
                    input.schema.columns.get(i).name,
                    i);
        }

        List<Integer> groupIndexes =
                new ArrayList<>();

        for (String name :
                step.groupBy) {

            Integer index =
                    inputIndex.get(name);

            if (index == null) {
                throw new IllegalArgumentException(
                        "AGGREGATE step " + step.id +
                        " references unavailable group column: " +
                        name);
            }

            groupIndexes.add(index);
        }

        List<Integer> measureIndexes =
                new ArrayList<>();

        List<SemanticModel.Type> measureTypes =
                new ArrayList<>();

        for (SemanticModel.Measure measure :
                step.measures) {

            if (!"COUNT".equals(measure.function) &&
                !"COUNT_DISTINCT".equals(measure.function) &&
                !"SUM".equals(measure.function) &&
                !"MIN".equals(measure.function) &&
                !"MAX".equals(measure.function)) {

                throw new IllegalArgumentException(
                        "AGGREGATE witness measure not yet implemented: " +
                        measure.function);
            }

            Integer index =
                    inputIndex.get(
                            measure.column);

            if (index == null) {
                throw new IllegalArgumentException(
                        "AGGREGATE step " + step.id +
                        " references unavailable measure column: " +
                        measure.column);
            }

            measureIndexes.add(index);

            measureTypes.add(
                    input.schema.columns
                            .get(index)
                            .type);
        }

        Map<List<Object>,Object[]> groups =
                new LinkedHashMap<>();

        for (ExecutionModel.Row row :
                input.rows) {

            List<Object> key =
                    new ArrayList<>();

            for (Integer index :
                    groupIndexes) {

                key.add(
                        row.get(index));
            }

            Object[] states =
                    groups.get(key);

            if (states == null) {
                states =
                        new Object[
                                step.measures.size()];

                for (int i = 0;
                     i < step.measures.size();
                     i++) {

                    if ("COUNT".equals(
                            step.measures.get(i).function)) {

                        states[i] = 0L;

                    } else if ("COUNT_DISTINCT".equals(
                            step.measures.get(i).function)) {

                        states[i] =
                                new LinkedHashSet<Object>();
                    }
                }

                groups.put(
                        Collections.unmodifiableList(
                                new ArrayList<>(key)),
                        states);
            }

            for (int i = 0;
                 i < step.measures.size();
                 i++) {

                SemanticModel.Measure measure =
                        step.measures.get(i);

                Object value =
                        row.get(
                                measureIndexes.get(i));

                if ("COUNT".equals(
                        measure.function)) {

                    if (value != null) {
                        states[i] =
                                Math.addExact(
                                        (Long) states[i],
                                        1L);
                    }

                    continue;
                }

                if ("COUNT_DISTINCT".equals(
                        measure.function)) {

                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        Set<Object> distinct =
                                (Set<Object>) states[i];

                        distinct.add(value);
                    }

                    continue;
                }

                if ("SUM".equals(
                        measure.function)) {

                    if (value == null) {
                        continue;
                    }

                    SemanticModel.Type type =
                            measureTypes.get(i);

                    if (type ==
                            SemanticModel.Type.INTEGER) {

                        long next =
                                ((Number) value)
                                        .longValue();

                        if (states[i] == null) {
                            states[i] = next;
                        } else {
                            states[i] =
                                    Math.addExact(
                                            (Long) states[i],
                                            next);
                        }

                        continue;
                    }

                    if (type ==
                            SemanticModel.Type.DECIMAL) {

                        java.math.BigDecimal next =
                                (java.math.BigDecimal) value;

                        if (states[i] == null) {
                            states[i] = next;
                        } else {
                            states[i] =
                                    ((java.math.BigDecimal)
                                            states[i])
                                            .add(next);
                        }

                        continue;
                    }

                    throw new IllegalArgumentException(
                            "AGGREGATE SUM has non-numeric runtime type: " +
                            type);
                }

                if ("MIN".equals(measure.function) ||
                    "MAX".equals(measure.function)) {

                    if (value == null) {
                        continue;
                    }

                    if (states[i] == null) {
                        states[i] = value;
                        continue;
                    }

                    int comparison =
                            PredicateEvaluator.compareValues(
                                    states[i],
                                    value,
                                    measureTypes.get(i),
                                    measureTypes.get(i));

                    if ("MIN".equals(
                            measure.function)) {

                        if (comparison > 0) {
                            states[i] = value;
                        }

                    } else {

                        if (comparison < 0) {
                            states[i] = value;
                        }
                    }
                }
            }
        }

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        for (Map.Entry<List<Object>,Object[]> entry :
                groups.entrySet()) {

            List<Object> values =
                    new ArrayList<>();

            values.addAll(
                    entry.getKey());

            Object[] states =
                    entry.getValue();

            for (int i = 0;
                 i < step.measures.size();
                 i++) {

                SemanticModel.Measure measure =
                        step.measures.get(i);

                if ("COUNT_DISTINCT".equals(
                        measure.function)) {

                    @SuppressWarnings("unchecked")
                    Set<Object> distinct =
                            (Set<Object>) states[i];

                    states[i] =
                            (long) distinct.size();
                }
            }

            values.addAll(
                    Arrays.asList(states));

            rows.add(
                    new ExecutionModel.Row(
                            values));
        }

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }
    private static ExecutionModel.RelationData executeLeftJoin(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData left =
                available.get(step.left);

        if (left == null) {
            throw new IllegalArgumentException(
                    "LEFT_JOIN step " + step.id +
                    " references unavailable left source: " +
                    step.left);
        }

        ExecutionModel.RelationData right =
                available.get(step.right);

        if (right == null) {
            throw new IllegalArgumentException(
                    "LEFT_JOIN step " + step.id +
                    " references unavailable right source: " +
                    step.right);
        }

        if (step.conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "LEFT_JOIN step " + step.id +
                    " requires at least one normalized condition");
        }

        Set<String> coalescedKeys =
                new LinkedHashSet<>();

        for (SemanticModel.Condition condition :
                step.conditions) {

            if ("=".equals(condition.operator) &&
                condition.left.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.right.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.left.column.equals(
                        condition.right.column)) {

                coalescedKeys.add(
                        condition.left.column);
            }
        }

        List<Integer> retainedRightIndexes =
                new ArrayList<>();

        List<SemanticModel.Column> workingColumns =
                new ArrayList<>(left.schema.columns);

        for (int i = 0;
             i < right.schema.columns.size();
             i++) {

            String name =
                    right.schema.columns.get(i).name;

            if (!coalescedKeys.contains(name)) {
                retainedRightIndexes.add(i);
                workingColumns.add(right.schema.columns.get(i));
            }
        }

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        for (ExecutionModel.Row leftRow :
                left.rows) {

            boolean matched = false;

            for (ExecutionModel.Row rightRow :
                    right.rows) {

                PredicateEvaluator.Truth truth =
                        PredicateEvaluator.Truth.TRUE;

                for (SemanticModel.Condition condition :
                        step.conditions) {

                    truth =
                            PredicateEvaluator.and(
                                    truth,
                                    ExpressionEvaluator
                                            .evaluateJoinCondition(
                                                    condition,
                                                    left.schema,
                                                    leftRow,
                                                    right.schema,
                                                    rightRow));

                    if (truth ==
                            PredicateEvaluator.Truth.FALSE) {

                        break;
                    }
                }

                if (truth !=
                        PredicateEvaluator.Truth.TRUE) {

                    continue;
                }

                matched = true;

                List<Object> values =
                        new ArrayList<>();

                values.addAll(
                        leftRow.values);

                for (Integer index :
                        retainedRightIndexes) {

                    values.add(
                            rightRow.get(index));
                }

                rows.add(
                        new ExecutionModel.Row(
                                values));
            }

            if (!matched) {
                List<Object> values =
                        new ArrayList<>();

                values.addAll(
                        leftRow.values);

                for (int i = 0;
                     i < retainedRightIndexes.size();
                     i++) {

                    values.add(null);
                }

                rows.add(
                        new ExecutionModel.Row(
                                values));
            }
        }

        if (!step.select.isEmpty()) {
            Map<String,Integer> workingIndexes =
                    new LinkedHashMap<>();

            for (int i = 0; i < workingColumns.size(); i++) {
                workingIndexes.put(workingColumns.get(i).name, i);
            }

            List<ExecutionModel.Row> projected =
                    new ArrayList<>();

            for (ExecutionModel.Row row : rows) {
                List<Object> values = new ArrayList<>();

                for (String name : step.select) {
                    values.add(row.get(workingIndexes.get(name)));
                }

                projected.add(new ExecutionModel.Row(values));
            }

            rows = projected;
        }

        sortRows(step.outputSchema, rows, step.orderBy);

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }

    private static ExecutionModel.RelationData executeJoin(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData left =
                available.get(step.left);

        if (left == null) {
            throw new IllegalArgumentException(
                    "JOIN step " + step.id +
                    " references unavailable left source: " +
                    step.left);
        }

        ExecutionModel.RelationData right =
                available.get(step.right);

        if (right == null) {
            throw new IllegalArgumentException(
                    "JOIN step " + step.id +
                    " references unavailable right source: " +
                    step.right);
        }

        if (step.conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "JOIN step " + step.id +
                    " requires at least one normalized condition");
        }

        Set<String> coalescedKeys =
                new LinkedHashSet<>();

        for (SemanticModel.Condition condition :
                step.conditions) {

            if ("=".equals(condition.operator) &&
                condition.left.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.right.kind ==
                        SemanticModel.Expression.Kind.COLUMN &&
                condition.left.column.equals(
                        condition.right.column)) {

                coalescedKeys.add(
                        condition.left.column);
            }
        }

        List<Integer> retainedRightIndexes =
                new ArrayList<>();

        List<SemanticModel.Column> workingColumns =
                new ArrayList<>(left.schema.columns);

        for (int i = 0;
             i < right.schema.columns.size();
             i++) {

            String name =
                    right.schema.columns.get(i).name;

            if (!coalescedKeys.contains(name)) {
                retainedRightIndexes.add(i);
                workingColumns.add(right.schema.columns.get(i));
            }
        }

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        for (ExecutionModel.Row leftRow :
                left.rows) {

            for (ExecutionModel.Row rightRow :
                    right.rows) {

                PredicateEvaluator.Truth truth =
                        PredicateEvaluator.Truth.TRUE;

                for (SemanticModel.Condition condition :
                        step.conditions) {

                    truth =
                            PredicateEvaluator.and(
                                    truth,
                                    ExpressionEvaluator
                                            .evaluateJoinCondition(
                                                    condition,
                                                    left.schema,
                                                    leftRow,
                                                    right.schema,
                                                    rightRow));

                    if (truth ==
                            PredicateEvaluator.Truth.FALSE) {

                        break;
                    }
                }

                if (truth !=
                        PredicateEvaluator.Truth.TRUE) {

                    continue;
                }

                List<Object> values =
                        new ArrayList<>();

                values.addAll(
                        leftRow.values);

                for (Integer index :
                        retainedRightIndexes) {

                    values.add(
                            rightRow.get(index));
                }

                rows.add(
                        new ExecutionModel.Row(
                                values));
            }
        }

        if (!step.select.isEmpty()) {
            Map<String,Integer> indexes = new LinkedHashMap<>();
            for (int i = 0; i < workingColumns.size(); i++) {
                indexes.put(workingColumns.get(i).name, i);
            }
            List<ExecutionModel.Row> projected = new ArrayList<>();
            for (ExecutionModel.Row row : rows) {
                List<Object> values = new ArrayList<>();
                for (String name : step.select) {
                    values.add(row.get(indexes.get(name)));
                }
                projected.add(new ExecutionModel.Row(values));
            }
            rows = projected;
        }

        sortRows(step.outputSchema, rows, step.orderBy);

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }

    private static ExecutionModel.RelationData executeCrossJoin(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData left =
                available.get(step.left);

        if (left == null) {
            throw new IllegalArgumentException(
                    "CROSS_JOIN step " + step.id +
                    " references unavailable left source: " +
                    step.left);
        }

        ExecutionModel.RelationData right =
                available.get(step.right);

        if (right == null) {
            throw new IllegalArgumentException(
                    "CROSS_JOIN step " + step.id +
                    " references unavailable right source: " +
                    step.right);
        }

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        /*
         * Deterministic nested-loop order:
         * left input order is outer order,
         * right input order is inner order.
         */
        for (ExecutionModel.Row leftRow :
                left.rows) {

            for (ExecutionModel.Row rightRow :
                    right.rows) {

                List<Object> values =
                        new ArrayList<>(
                                leftRow.values.size() +
                                rightRow.values.size());

                values.addAll(
                        leftRow.values);

                values.addAll(
                        rightRow.values);

                rows.add(
                        new ExecutionModel.Row(
                                values));
            }
        }

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }

    private static ExecutionModel.RelationData executeFilter(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData input =
                available.get(step.from);

        if (input == null) {
            throw new IllegalArgumentException(
                    "FILTER step " + step.id +
                    " references unavailable source: " +
                    step.from);
        }

        if (step.conditions.size() != 1) {
            throw new IllegalArgumentException(
                    "FILTER step " + step.id +
                    " requires exactly one normalized condition");
        }

        SemanticModel.Condition condition =
                step.conditions.get(0);

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        for (ExecutionModel.Row row :
                input.rows) {

            PredicateEvaluator.Truth truth =
                    ExpressionEvaluator.evaluateCondition(
                            condition,
                            input.schema,
                            row);

            if (truth ==
                    PredicateEvaluator.Truth.TRUE) {

                /*
                 * FILTER does not transform a retained tuple.
                 * Preserve the existing immutable row object.
                 */
                rows.add(row);
            }
        }

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }

    private static ExecutionModel.RelationData executeRename(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData input =
                available.get(step.from);

        if (input == null) {
            throw new IllegalArgumentException(
                    "RENAME step " + step.id +
                    " references unavailable source: " +
                    step.from);
        }

        if (input.schema.columns.size() !=
            step.outputSchema.columns.size()) {

            throw new IllegalArgumentException(
                    "RENAME step " + step.id +
                    " output width does not match input width");
        }

        /*
         * Normalization has already resolved the rename map,
         * collisions, and output column identities.
         *
         * RENAME changes schema identities only. Tuple values,
         * ordinal positions, NULLs, and tuple order are preserved.
         */
        return new ExecutionModel.RelationData(
                step.outputSchema,
                input.rows);
    }

    private static ExecutionModel.RelationData executeProject(
            SemanticModel.Step step,
            Map<String,ExecutionModel.RelationData> available) {

        ExecutionModel.RelationData input =
                available.get(step.from);

        if (input == null) {
            throw new IllegalArgumentException(
                    "PROJECT step " + step.id +
                    " references unavailable source: " +
                    step.from);
        }

        Map<String,Integer> inputIndex =
                new LinkedHashMap<>();

        for (int i = 0;
             i < input.schema.columns.size();
             i++) {

            inputIndex.put(
                    input.schema.columns.get(i).name,
                    i);
        }

        List<Integer> projection =
                new ArrayList<>();

        /*
         * The normalized output schema is authoritative for
         * execution order. The select list should describe the
         * same order, but execution does not reinterpret raw input.
         */
        for (SemanticModel.Column outputColumn :
                step.outputSchema.columns) {

            Integer index =
                    inputIndex.get(
                            outputColumn.name);

            if (index == null) {
                throw new IllegalArgumentException(
                        "PROJECT step " + step.id +
                        " output column is unavailable from source: " +
                        outputColumn.name);
            }

            projection.add(index);
        }

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        for (ExecutionModel.Row inputRow :
                input.rows) {

            List<Object> values =
                    new ArrayList<>();

            for (Integer index :
                    projection) {

                values.add(
                        inputRow.get(index));
            }

            rows.add(
                    new ExecutionModel.Row(
                            values));
        }

        return new ExecutionModel.RelationData(
                step.outputSchema,
                rows);
    }
}
