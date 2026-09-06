import java.util.*;

public final class SemanticModel {

    private SemanticModel() {
    }

    public static final class Capsule {
        public final String id;
        public final String version;
        public final List<Input> inputs;
        public final List<Relation> relations;
        public final List<Output> outputs;

        public Capsule(
                String id,
                String version,
                List<Input> inputs,
                List<Relation> relations,
                List<Output> outputs) {

            this.id = Objects.requireNonNull(id);
            this.version = Objects.requireNonNull(version);

            this.inputs = List.copyOf(
                    Objects.requireNonNull(inputs));

            this.relations = List.copyOf(
                    Objects.requireNonNull(relations));

            this.outputs = List.copyOf(
                    Objects.requireNonNull(outputs));
        }
    }

    public static final class Input {
        public final String id;
        public final String path;
        public final String schemaPath;
        public final String format;
        public final Schema schema;

        public Input(
                String id,
                String path,
                String schemaPath,
                String format,
                Schema schema) {

            this.id = Objects.requireNonNull(id);
            this.path = Objects.requireNonNull(path);
            this.schemaPath = Objects.requireNonNull(schemaPath);
            this.format = Objects.requireNonNull(format);
            this.schema = Objects.requireNonNull(schema);
        }
    }

    public static final class Binding {
        public final String name;
        public final SourceKind sourceKind;
        public final String sourceId;
        public final Schema schema;

        public Binding(
                String name,
                SourceKind sourceKind,
                String sourceId,
                Schema schema) {

            this.name = Objects.requireNonNull(name);
            this.sourceKind = Objects.requireNonNull(sourceKind);
            this.sourceId = Objects.requireNonNull(sourceId);
            this.schema = Objects.requireNonNull(schema);
        }
    }

    public enum SourceKind {
        INPUT,
        RELATION
    }
    public static final class Relation {
        public final String id;
        public final List<String> dependencies;
        public final List<Binding> bindings;
        public final List<Step> steps;
        public final String output;
        public final Schema outputSchema;

        public Relation(
                String id,
                List<String> dependencies,
                List<Binding> bindings,
                List<Step> steps,
                String output,
                Schema outputSchema) {

            this.id = Objects.requireNonNull(id);

            this.dependencies = List.copyOf(
                    Objects.requireNonNull(dependencies));

            this.bindings = List.copyOf(
                    Objects.requireNonNull(bindings));

            this.steps = List.copyOf(
                    Objects.requireNonNull(steps));

            this.output = Objects.requireNonNull(output);
            this.outputSchema = Objects.requireNonNull(outputSchema);
        }
    }
    public static final class Output {
        public final String id;
        public final String relation;
        public final String generatedPath;

        public Output(
                String id,
                String relation,
                String generatedPath) {

            this.id = Objects.requireNonNull(id);
            this.relation = Objects.requireNonNull(relation);
            this.generatedPath = Objects.requireNonNull(generatedPath);
        }
    }

    public static final class Schema {
        public final List<Column> columns;

        public Schema(List<Column> columns) {
            this.columns = List.copyOf(
                    Objects.requireNonNull(columns));
        }
    }

    public static final class Column {
        public final String name;
        public final Type type;
        public final boolean nullable;

        /*
         * Optional exact-decimal declaration metadata.
         *
         * NUMBER(p,s) is represented at runtime by DECIMAL/BigDecimal.
         * Existing DECIMAL and INTEGER columns normally leave these null.
         */
        public final Integer precision;
        public final Integer scale;

        public Column(
                String name,
                Type type,
                boolean nullable) {

            this(
                    name,
                    type,
                    nullable,
                    null,
                    null);
        }

        public Column(
                String name,
                Type type,
                boolean nullable,
                Integer precision,
                Integer scale) {

            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
            this.nullable = nullable;
            this.precision = precision;
            this.scale = scale;
        }
    }

    public enum Type {
        TEXT,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        DATE
    }

    public static final class Step {
        public final String id;
        public final Operation operation;
        public final Schema outputSchema;
        public final String from;
        public final String left;
        public final String right;
        public final List<Condition> conditions;
        public final List<String> groupBy;
        public final List<Measure> measures;
        public final List<DerivedColumn> derivedColumns;
        public final List<String> select;
        public final List<String> orderBy;
        public final Map<String,String> rename;

        public Step(
                String id,
                Operation operation,
                Schema outputSchema,
                String from,
                String left,
                String right,
                List<Condition> conditions,
                List<String> groupBy,
                List<Measure> measures,
                List<DerivedColumn> derivedColumns,
                List<String> select,
                List<String> orderBy,
                Map<String,String> rename) {

            this.id = Objects.requireNonNull(id);
            this.operation = Objects.requireNonNull(operation);
            this.outputSchema = Objects.requireNonNull(outputSchema);
            this.from = from;
            this.left = left;
            this.right = right;

            this.conditions = List.copyOf(
                    Objects.requireNonNull(conditions));

            this.groupBy = List.copyOf(
                    Objects.requireNonNull(groupBy));

            this.measures = List.copyOf(
                    Objects.requireNonNull(measures));

            this.derivedColumns = List.copyOf(
                    Objects.requireNonNull(derivedColumns));

            this.select = List.copyOf(
                    Objects.requireNonNull(select));

            this.orderBy = List.copyOf(
                    Objects.requireNonNull(orderBy));

            this.rename = Collections.unmodifiableMap(
                    new LinkedHashMap<>(
                            Objects.requireNonNull(rename)));
        }
    }

    public enum Operation {
        FILTER,
        PROJECT,
        RENAME,
        AGGREGATE,
        JOIN,
        LEFT_JOIN,
        CROSS_JOIN,
        DERIVE
    }

    public static final class Condition {
        public final Expression left;
        public final String operator;
        public final Expression right;

        public Condition(
                Expression left,
                String operator,
                Expression right) {

            this.left = Objects.requireNonNull(left);
            this.operator = Objects.requireNonNull(operator);
            this.right = Objects.requireNonNull(right);
        }
    }

    public static final class Measure {
        public final String name;
        public final String function;
        public final String column;

        public Measure(
                String name,
                String function,
                String column) {

            this.name = Objects.requireNonNull(name);
            this.function = Objects.requireNonNull(function);
            this.column = column;
        }
    }

    public static final class DerivedColumn {
        public final String name;
        public final Expression expression;

        /*
         * Optional exact-decimal result contract.
         *
         * Intermediate arithmetic remains full-precision BigDecimal.
         * These constraints are enforced only when the derived result
         * is materialized.
         */
        public final Integer precision;
        public final Integer scale;

        public DerivedColumn(
                String name,
                Expression expression) {

            this(
                    name,
                    expression,
                    null,
                    null);
        }

        public DerivedColumn(
                String name,
                Expression expression,
                Integer precision,
                Integer scale) {

            this.name = Objects.requireNonNull(name);
            this.expression = Objects.requireNonNull(expression);
            this.precision = precision;
            this.scale = scale;
        }
    }

    public static final class Expression {
        public final Kind kind;
        public final String column;
        public final Object literal;
        public final Type literalType;
        public final String function;
        public final Condition condition;
        public final Expression thenBranch;
        public final Expression elseBranch;
        public final List<Expression> arguments;

        public Expression(
                Kind kind,
                String column,
                Object literal,
                Type literalType,
                String function,
                Condition condition,
                Expression thenBranch,
                Expression elseBranch,
                List<Expression> arguments) {

            this.kind = Objects.requireNonNull(kind);
            this.column = column;
            this.literal = literal;
            this.literalType = literalType;
            this.function = function;
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;

            this.arguments = List.copyOf(
                    Objects.requireNonNull(arguments));

            if (kind == Kind.LITERAL &&
                literalType == null) {

                throw new IllegalArgumentException(
                        "literal expression requires literalType");
            }

            if (kind != Kind.LITERAL &&
                literalType != null) {

                throw new IllegalArgumentException(
                        "non-literal expression cannot have literalType");
            }
        }

        public static Expression column(
                String name) {

            return new Expression(
                    Kind.COLUMN,
                    Objects.requireNonNull(name),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of());
        }

        public static Expression literal(
                Object value,
                Type type) {

            return new Expression(
                    Kind.LITERAL,
                    null,
                    value,
                    Objects.requireNonNull(type),
                    null,
                    null,
                    null,
                    null,
                    List.of());
        }

        public static Expression function(
                String function,
                List<Expression> arguments) {

            return new Expression(
                    Kind.FUNCTION,
                    null,
                    null,
                    null,
                    Objects.requireNonNull(function),
                    null,
                    null,
                    null,
                    arguments);
        }

        public static Expression conditional(
                Condition condition,
                Expression thenBranch,
                Expression elseBranch) {

            return new Expression(
                    Kind.CONDITIONAL,
                    null,
                    null,
                    null,
                    null,
                    Objects.requireNonNull(condition),
                    Objects.requireNonNull(thenBranch),
                    Objects.requireNonNull(elseBranch),
                    List.of());
        }

        public enum Kind {
            COLUMN,
            LITERAL,
            FUNCTION,
            CONDITIONAL
        }
    }
}