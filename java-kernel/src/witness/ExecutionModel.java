import java.util.*;

public final class ExecutionModel {

    private ExecutionModel() {
    }

    public static final class RelationData {
        public final SemanticModel.Schema schema;
        public final List<Row> rows;

        public RelationData(
                SemanticModel.Schema schema,
                List<Row> rows) {

            this.schema =
                    Objects.requireNonNull(schema);

            Objects.requireNonNull(rows);

            List<Row> copy =
                    new ArrayList<>();

            for (Row row : rows) {
                if (row == null) {
                    throw new IllegalArgumentException(
                            "relation data contains null row");
                }

                if (row.values.size() !=
                    schema.columns.size()) {

                    throw new IllegalArgumentException(
                            "row width " +
                            row.values.size() +
                            " does not match schema width " +
                            schema.columns.size());
                }

                copy.add(row);
            }

            this.rows =
                    List.copyOf(copy);
        }
    }

    public static final class Row {
        public final List<Object> values;

        public Row(List<Object> values) {
            Objects.requireNonNull(values);

            /*
             * List.copyOf rejects null elements, but semantic NULL
             * must be representable. Use an unmodifiable defensive
             * copy instead.
             */
            this.values =
                    Collections.unmodifiableList(
                            new ArrayList<>(values));
        }

        public Object get(int index) {
            return values.get(index);
        }

        public int size() {
            return values.size();
        }
    }
}
