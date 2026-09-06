import java.io.IOException;
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
                    "failed to write CSV output: " +
                    path,
                    ex);
        }
    }
}
