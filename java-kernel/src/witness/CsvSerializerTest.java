import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CsvSerializerTest {

    public static void main(String[] args) {
        testHeaderAndBasicTypes();
        testNullAndEmptyTextAreDistinct();
        testTextQuotingAndEscaping();
        testDecimalCanonicalization();
        testDateAndBoolean();
        testDeterministicUtf8AndLf();
        testEmptyRelationSerializesHeaderOnly();

        System.out.println(
                "CsvSerializerTest PASSED");
    }

    private static ExecutionModel.RelationData fixture() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "text_col",
                                        SemanticModel.Type.TEXT,
                                        true),
                                new SemanticModel.Column(
                                        "integer_col",
                                        SemanticModel.Type.INTEGER,
                                        false),
                                new SemanticModel.Column(
                                        "decimal_col",
                                        SemanticModel.Type.DECIMAL,
                                        true),
                                new SemanticModel.Column(
                                        "boolean_col",
                                        SemanticModel.Type.BOOLEAN,
                                        false),
                                new SemanticModel.Column(
                                        "date_col",
                                        SemanticModel.Type.DATE,
                                        false)));

        return new ExecutionModel.RelationData(
                schema,
                List.of(
                        new ExecutionModel.Row(
                                Arrays.asList(
                                        "hello",
                                        7L,
                                        new BigDecimal("1.2300"),
                                        true,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                4)))));
    }

    private static void testHeaderAndBasicTypes() {
        String csv =
                text(fixture());

        require(
                csv.startsWith(
                        "text_col,integer_col,decimal_col,boolean_col,date_col\n"),
                "header");

        require(
                csv.contains(
                        "hello,7,1.23,true,2026-09-04\n"),
                "basic typed row");
    }

    private static void testNullAndEmptyTextAreDistinct() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "value",
                                        SemanticModel.Type.TEXT,
                                        true)));

        ExecutionModel.RelationData data =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList((Object) null)),
                                new ExecutionModel.Row(
                                        Arrays.asList(""))));

        String csv = text(data);

        require(
                csv.equals(
                        "value\n\\N\n\n"),
                "NULL versus empty TEXT");
    }

    private static void testTextQuotingAndEscaping() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "value",
                                        SemanticModel.Type.TEXT,
                                        false)));

        ExecutionModel.RelationData data =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "a,b")),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "a\"b")),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "a\nb")),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "\\N"))));

        require(
                text(data).equals(
                        "value\n" +
                        "\"a,b\"\n" +
                        "\"a\"\"b\"\n" +
                        "\"a\n" +
                        "b\"\n" +
                        "\"\\N\"\n"),
                "TEXT quoting and escaping");
    }

    private static void testDecimalCanonicalization() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "value",
                                        SemanticModel.Type.DECIMAL,
                                        false)));

        ExecutionModel.RelationData data =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                new BigDecimal("1.2300"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                new BigDecimal("0.000"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                new BigDecimal("-0.000"))),
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                new BigDecimal("1000.00")))));

        require(
                text(data).equals(
                        "value\n" +
                        "1.23\n" +
                        "0\n" +
                        "0\n" +
                        "1000\n"),
                "DECIMAL canonicalization");
    }

    private static void testDateAndBoolean() {
        String csv =
                text(fixture());

        require(
                csv.contains(
                        ",true,2026-09-04\n"),
                "BOOLEAN and DATE serialization");
    }

    private static void testDeterministicUtf8AndLf() {
        byte[] first =
                CsvSerializer.serialize(
                        fixture());

        byte[] second =
                CsvSerializer.serialize(
                        fixture());

        require(
                Arrays.equals(first, second),
                "deterministic bytes");

        require(
                new String(
                        first,
                        StandardCharsets.UTF_8)
                        .indexOf('\r') < 0,
                "canonical LF only");
    }

    private static void testEmptyRelationSerializesHeaderOnly() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true)));

        ExecutionModel.RelationData data =
                new ExecutionModel.RelationData(
                        schema,
                        List.of());

        require(
                text(data).equals(
                        "category,amount\n"),
                "empty relation header-only serialization");
    }
    private static String text(
            ExecutionModel.RelationData data) {

        return new String(
                CsvSerializer.serialize(data),
                StandardCharsets.UTF_8);
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
