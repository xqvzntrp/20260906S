import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class OutputWriter {

    private OutputWriter() {
    }

    public static void writeCsv(
            Path path,
            ExecutionModel.RelationData data) {

        Objects.requireNonNull(path);
        Objects.requireNonNull(data);

        byte[] bytes =
                CsvSerializer.serialize(data);

        writeBytes(
                path,
                bytes,
                "CSV");
    }

    public static void writeSchemaJson(
            Path path,
            SemanticModel.Schema schema) {

        Objects.requireNonNull(path);
        Objects.requireNonNull(schema);

        StringBuilder json =
                new StringBuilder();

        json.append("{\n");
        json.append("  \"columns\": [\n");

        for (int i = 0;
             i < schema.columns.size();
             i++) {

            SemanticModel.Column column =
                    schema.columns.get(i);

            json.append("    {\n");
            json.append("      \"name\": ")
                .append(jsonString(column.name))
                .append(",\n");
            json.append("      \"type\": ")
                .append(jsonString(column.type.name()))
                .append(",\n");
            json.append("      \"nullable\": ")
                .append(column.nullable)
                .append(",\n");
            json.append("      \"precision\": ")
                .append(
                        column.precision == null
                        ? "null"
                        : column.precision)
                .append(",\n");
            json.append("      \"scale\": ")
                .append(
                        column.scale == null
                        ? "null"
                        : column.scale)
                .append("\n");
            json.append("    }");

            if (i + 1 < schema.columns.size()) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        writeBytes(
                path,
                json.toString()
                    .getBytes(StandardCharsets.UTF_8),
                "schema JSON");
    }

    private static void writeBytes(
            Path path,
            byte[] bytes,
            String kind) {

        try {
            Path parent =
                    path.toAbsolutePath()
                        .getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(
                    path,
                    bytes);

        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "failed to write " +
                    kind +
                    " output: " +
                    path,
                    ex);
        }
    }

    private static String jsonString(String value) {

        StringBuilder result =
                new StringBuilder("\"");

        for (int i = 0;
             i < value.length();
             i++) {

            char c = value.charAt(i);

            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;

                case '"':
                    result.append("\\\"");
                    break;

                case '\n':
                    result.append("\\n");
                    break;

                case '\r':
                    result.append("\\r");
                    break;

                case '\t':
                    result.append("\\t");
                    break;

                default:
                    if (c < 0x20) {
                        result.append(
                                String.format(
                                        "\\u%04x",
                                        (int) c));
                    } else {
                        result.append(c);
                    }
            }
        }

        return result.append("\"").toString();
    }
}
