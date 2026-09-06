import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class OutputWriterTest {

    public static void main(String[] args)
            throws Exception {

        testWritesExactCanonicalBytes();
        testCreatesParentDirectories();

        System.out.println(
                "OutputWriterTest PASSED");
    }

    private static ExecutionModel.RelationData data() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "id",
                                        SemanticModel.Type.INTEGER,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true)));

        return new ExecutionModel.RelationData(
                schema,
                List.of(
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        7L,
                                        new BigDecimal("1.2300"))),
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        8L,
                                        null))));
    }

    private static void testWritesExactCanonicalBytes()
            throws Exception {

        Path directory =
                Files.createTempDirectory(
                        "v2-output-test-");

        Path path =
                directory.resolve(
                        "result.csv");

        OutputWriter.writeCsv(
                path,
                data());

        byte[] actual =
                Files.readAllBytes(path);

        byte[] expected =
                (
                        "id,amount\n" +
                        "7,1.23\n" +
                        "8,\\N\n")
                        .getBytes(
                                StandardCharsets.UTF_8);

        require(
                Arrays.equals(actual, expected),
                "exact canonical output bytes");
    }

    private static void testCreatesParentDirectories()
            throws Exception {

        Path directory =
                Files.createTempDirectory(
                        "v2-output-parent-");

        Path path =
                directory
                        .resolve("nested")
                        .resolve("deeper")
                        .resolve("result.csv");

        OutputWriter.writeCsv(
                path,
                data());

        require(
                Files.exists(path),
                "nested output file exists");
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
