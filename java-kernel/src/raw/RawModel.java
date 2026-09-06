import java.util.*;

public final class RawModel {

    private RawModel() {
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
            this.inputs = List.copyOf(Objects.requireNonNull(inputs));
            this.relations = List.copyOf(Objects.requireNonNull(relations));
            this.outputs = List.copyOf(Objects.requireNonNull(outputs));
        }
    }

    public static final class Input {
        public final String id;
        public final String path;
        public final String schemaPath;
        public final String format;

        public Input(
                String id,
                String path,
                String schemaPath,
                String format) {

            this.id = Objects.requireNonNull(id);
            this.path = Objects.requireNonNull(path);
            this.schemaPath = Objects.requireNonNull(schemaPath);
            this.format = Objects.requireNonNull(format);
        }
    }

    public static final class Relation {
        public final String id;
        public final String path;
        public final String format;

        public Relation(String id, String path, String format) {
            this.id = Objects.requireNonNull(id);
            this.path = Objects.requireNonNull(path);
            this.format = Objects.requireNonNull(format);
        }
    }

    public static final class Output {
        public final String id;
        public final String relation;
        public final String generatedPath;
        public final String expectedPath;
        public final String schemaPath;
        public final String format;

        public Output(
                String id,
                String relation,
                String generatedPath,
                String expectedPath,
                String schemaPath,
                String format) {

            this.id = Objects.requireNonNull(id);
            this.relation = Objects.requireNonNull(relation);
            this.generatedPath = Objects.requireNonNull(generatedPath);
            this.expectedPath = Objects.requireNonNull(expectedPath);
            this.schemaPath = Objects.requireNonNull(schemaPath);
            this.format = Objects.requireNonNull(format);
        }
    }

    public static final class RelationDocument {
        public final String relationId;
        public final String description;
        public final List<Binding> inputs;
        public final List<Step> steps;
        public final String output;

        public RelationDocument(
                String relationId,
                String description,
                List<Binding> inputs,
                List<Step> steps,
                String output) {

            this.relationId = Objects.requireNonNull(relationId);
            this.description = description;
            this.inputs = List.copyOf(Objects.requireNonNull(inputs));
            this.steps = List.copyOf(Objects.requireNonNull(steps));
            this.output = Objects.requireNonNull(output);
        }
    }

    public static final class Binding {
        public final String name;
        public final String reference;

        public Binding(String name, String reference) {
            this.name = Objects.requireNonNull(name);
            this.reference = Objects.requireNonNull(reference);
        }
    }

    public static final class Step {
        public final String id;
        public final String op;
        public final String from;
        public final String left;
        public final String right;
        public final Condition where;
        public final List<Condition> on;
        public final List<String> groupBy;
        public final List<Measure> measures;
        public final List<DerivedColumn> columns;
        public final List<String> select;
        public final List<String> orderBy;
        public final Map<String,String> rename;

        public Step(
                String id,
                String op,
                String from,
                String left,
                String right,
                Condition where,
                List<Condition> on,
                List<String> groupBy,
                List<Measure> measures,
                List<DerivedColumn> columns,
                List<String> select,
                List<String> orderBy,
                Map<String,String> rename) {

            this.id = Objects.requireNonNull(id);
            this.op = Objects.requireNonNull(op);
            this.from = from;
            this.left = left;
            this.right = right;
            this.where = where;
            this.on = List.copyOf(Objects.requireNonNull(on));
            this.groupBy = List.copyOf(Objects.requireNonNull(groupBy));
            this.measures = List.copyOf(Objects.requireNonNull(measures));
            this.columns = List.copyOf(Objects.requireNonNull(columns));
            this.select = List.copyOf(Objects.requireNonNull(select));
            this.orderBy = List.copyOf(Objects.requireNonNull(orderBy));
            this.rename = Map.copyOf(Objects.requireNonNull(rename));
        }
    }

    public static final class Condition {
        public final RawExpression left;
        public final String operator;
        public final RawExpression right;

        public Condition(
                RawExpression left,
                String operator,
                RawExpression right) {

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
        public final RawExpression expression;

        /*
         * Optional declared exact-decimal result contract.
         *
         * When present, the DERIVE result is enforced at the
         * materialization boundary rather than during intermediate
         * arithmetic.
         */
        public final Integer precision;
        public final Integer scale;

        public DerivedColumn(
                String name,
                RawExpression expression) {

            this(
                    name,
                    expression,
                    null,
                    null);
        }

        public DerivedColumn(
                String name,
                RawExpression expression,
                Integer precision,
                Integer scale) {

            this.name = Objects.requireNonNull(name);
            this.expression = Objects.requireNonNull(expression);
            this.precision = precision;
            this.scale = scale;
        }
    }

    public static final class RawExpression {
        public final Kind kind;
        public final String column;
        public final Object value;
        public final String function;
        public final List<RawExpression> arguments;
        public final Condition condition;
        public final RawExpression thenBranch;
        public final RawExpression elseBranch;

        public RawExpression(
                Kind kind,
                String column,
                Object value,
                String function,
                List<RawExpression> arguments,
                Condition condition,
                RawExpression thenBranch,
                RawExpression elseBranch) {

            this.kind = Objects.requireNonNull(kind);
            this.column = column;
            this.value = value;
            this.function = function;
            this.arguments = List.copyOf(Objects.requireNonNull(arguments));
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        public static RawExpression column(
                String name) {

            return new RawExpression(
                    Kind.COLUMN,
                    Objects.requireNonNull(name),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null);
        }

        public static RawExpression literal(
                Object value) {

            return new RawExpression(
                    Kind.LITERAL,
                    null,
                    value,
                    null,
                    List.of(),
                    null,
                    null,
                    null);
        }
        public static RawExpression conditional(
                Condition condition,
                RawExpression thenBranch,
                RawExpression elseBranch) {

            return new RawExpression(
                    Kind.CONDITIONAL,
                    null,
                    null,
                    null,
                    List.of(),
                    Objects.requireNonNull(condition),
                    Objects.requireNonNull(thenBranch),
                    Objects.requireNonNull(elseBranch));
        }
        public enum Kind {
            COLUMN,
            LITERAL,
            FUNCTION,
            CONDITIONAL
        }
    }
}