import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class CsvSerializer {

    private CsvSerializer() {
    }

    public static byte[] serialize(
            ExecutionModel.RelationData data) {

        Objects.requireNonNull(data);

        StringBuilder out =
                new StringBuilder();

        List<String> headers =
                new ArrayList<>();

        for (SemanticModel.Column column :
                data.schema.columns) {

            headers.add(
                    quoteIfRequired(
                            column.name));
        }

        appendRawRow(
                out,
                headers);

        for (ExecutionModel.Row row :
                data.rows) {

            List<String> fields =
                    new ArrayList<>();

            for (int i = 0;
                 i < data.schema.columns.size();
                 i++) {

                SemanticModel.Column column =
                        data.schema.columns.get(i);

                fields.add(
                        serializeField(
                                row.get(i),
                                column.type));
            }

            appendRawRow(
                    out,
                    fields);
        }

        return out.toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRawRow(
            StringBuilder out,
            List<String> fields) {

        for (int i = 0;
             i < fields.size();
             i++) {

            if (i > 0) {
                out.append(',');
            }

            out.append(fields.get(i));
        }

        out.append('\n');
    }

    private static String serializeField(
            Object value,
            SemanticModel.Type type) {

        if (value == null) {
            return "\\N";
        }

        String text;

        switch (type) {
            case TEXT:
                text = (String) value;
                break;

            case INTEGER:
                text =
                        Long.toString(
                                ((Number) value).longValue());
                break;

            case DECIMAL:
                text =
                        canonicalDecimal(
                                (BigDecimal) value);
                break;

            case BOOLEAN:
                text =
                        ((Boolean) value)
                                ? "true"
                                : "false";
                break;

            case DATE:
                text =
                        ((LocalDate) value)
                                .toString();
                break;

            default:
                throw new IllegalArgumentException(
                        "unsupported serialization type: " +
                        type);
        }

        if (type == SemanticModel.Type.TEXT) {
            return quoteIfRequired(text);
        }

        return text;
    }

    private static String canonicalDecimal(
            BigDecimal value) {

        if (value.signum() == 0) {
            return "0";
        }

        return value.stripTrailingZeros()
                .toPlainString();
    }

    private static String quoteIfRequired(
            String value) {

        if ("\\N".equals(value)) {
            return "\"\\N\"";
        }

        boolean needsQuote =
                value.indexOf(',') >= 0 ||
                value.indexOf('"') >= 0 ||
                value.indexOf('\r') >= 0 ||
                value.indexOf('\n') >= 0;

        if (!needsQuote) {
            return value;
        }

        return "\"" +
                value.replace("\"", "\"\"") +
                "\"";
    }
}
