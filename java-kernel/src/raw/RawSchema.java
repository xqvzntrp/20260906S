import java.util.*;

public final class RawSchema {

    public final String schemaId;
    public final String version;
    public final String relation;
    public final List<Column> columns;

    public RawSchema(
            String schemaId,
            String version,
            String relation,
            List<Column> columns) {

        this.schemaId = schemaId;
        this.version = version;
        this.relation = relation;

        this.columns = List.copyOf(
                Objects.requireNonNull(columns));
    }

    public static final class Column {
        public final String name;
        public final String type;
        public final boolean nullable;
        public final Integer precision;
        public final Integer scale;

        public Column(
                String name,
                String type,
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
                String type,
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
}