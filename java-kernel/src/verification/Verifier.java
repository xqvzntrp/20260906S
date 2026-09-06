import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Verifier {

    private Verifier() {
    }

    public static void verifyTyped(
            String outputId,
            ExecutionModel.RelationData actual,
            ExecutionModel.RelationData expected) {

        Objects.requireNonNull(outputId);
        Objects.requireNonNull(actual);
        Objects.requireNonNull(expected);

        verifySchema(
                outputId,
                actual.schema,
                expected.schema);

        if (actual.rows.size() !=
            expected.rows.size()) {

            throw failure(
                    outputId,
                    "tuple count differs: actual " +
                    actual.rows.size() +
                    ", expected " +
                    expected.rows.size());
        }

        for (int rowIndex = 0;
             rowIndex < actual.rows.size();
             rowIndex++) {

            ExecutionModel.Row actualRow =
                    actual.rows.get(rowIndex);

            ExecutionModel.Row expectedRow =
                    expected.rows.get(rowIndex);

            for (int columnIndex = 0;
                 columnIndex < actual.schema.columns.size();
                 columnIndex++) {

                SemanticModel.Column column =
                        actual.schema.columns.get(
                                columnIndex);

                Object actualValue =
                        actualRow.get(columnIndex);

                Object expectedValue =
                        expectedRow.get(columnIndex);

                if (!equalValue(
                        column.type,
                        actualValue,
                        expectedValue)) {

                    throw failure(
                            outputId,
                            "tuple " +
                            (rowIndex + 1) +
                            " column " +
                            column.name +
                            " differs");
                }
            }
        }
    }

    private static void verifySchema(
            String outputId,
            SemanticModel.Schema actual,
            SemanticModel.Schema expected) {

        if (actual.columns.size() !=
            expected.columns.size()) {

            throw failure(
                    outputId,
                    "schema column count differs: actual " +
                    actual.columns.size() +
                    ", expected " +
                    expected.columns.size());
        }

        for (int i = 0;
             i < actual.columns.size();
             i++) {

            SemanticModel.Column a =
                    actual.columns.get(i);

            SemanticModel.Column e =
                    expected.columns.get(i);

            if (!a.name.equals(e.name)) {
                throw failure(
                        outputId,
                        "schema column " +
                        (i + 1) +
                        " name differs: actual " +
                        a.name +
                        ", expected " +
                        e.name);
            }

            if (a.type != e.type) {
                throw failure(
                        outputId,
                        "schema column " +
                        a.name +
                        " type differs: actual " +
                        a.type +
                        ", expected " +
                        e.type);
            }

            if (a.nullable != e.nullable) {
                throw failure(
                        outputId,
                        "schema column " +
                        a.name +
                        " nullability differs");
            }
        }
    }

    private static boolean equalValue(
            SemanticModel.Type type,
            Object actual,
            Object expected) {

        if (actual == null ||
            expected == null) {

            return actual == null &&
                   expected == null;
        }

        switch (type) {
            case TEXT:
                return ((String) actual)
                        .equals((String) expected);

            case INTEGER:
                return ((Number) actual)
                        .longValue() ==
                       ((Number) expected)
                        .longValue();

            case DECIMAL:
                return ((BigDecimal) actual)
                        .compareTo(
                                (BigDecimal) expected) == 0;

            case BOOLEAN:
                return ((Boolean) actual)
                        .equals((Boolean) expected);

            case DATE:
                return ((LocalDate) actual)
                        .equals((LocalDate) expected);

            default:
                throw new IllegalArgumentException(
                        "unsupported verification type: " +
                        type);
        }
    }

    private static IllegalArgumentException failure(
            String outputId,
            String detail) {

        return new IllegalArgumentException(
                "verification failed for output " +
                outputId +
                ": " +
                detail);
    }
}
