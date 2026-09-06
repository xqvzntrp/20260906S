import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class ExpectedCsvReaderTest {

    public static void main(String[] args) {
        testReadsAllTypes();
        testNullVersusQuotedNullToken();
        testQuotedCommaQuoteAndLf();
        testHeaderMismatchRejected();
        testWrongWidthRejected();
        testMalformedQuoteRejected();
        testNonNullableNullRejected();
        testInvalidTypedValueRejected();
        testCrRejected();
        testMissingTerminalLfRejected();
        testBomRejected();

        System.out.println(
                "ExpectedCsvReaderTest PASSED");
    }

    private static void testReadsAllTypes() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("text", SemanticModel.Type.TEXT, false),
                                column("integer", SemanticModel.Type.INTEGER, false),
                                column("decimal", SemanticModel.Type.DECIMAL, false),
                                column("boolean", SemanticModel.Type.BOOLEAN, false),
                                column("date", SemanticModel.Type.DATE, false)));

        ExecutionModel.RelationData data =
                read(
                        "text,integer,decimal,boolean,date\n" +
                        "A,7,1.2300,true,2026-09-04\n",
                        schema);

        require(data.rows.size() == 1, "typed row count");

        ExecutionModel.Row row =
                data.rows.get(0);

        require(row.get(0).equals("A"), "TEXT");
        require(row.get(1).equals(7L), "INTEGER");
        require(
                ((BigDecimal) row.get(2))
                        .compareTo(new BigDecimal("1.23")) == 0,
                "DECIMAL");
        require(row.get(3).equals(Boolean.TRUE), "BOOLEAN");
        require(
                row.get(4).equals(LocalDate.of(2026, 9, 4)),
                "DATE");
    }

    private static void testNullVersusQuotedNullToken() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, true)));

        ExecutionModel.RelationData data =
                read(
                        "value\n" +
                        "\\N\n" +
                        "\"\\N\"\n" +
                        "\n",
                        schema);

        require(data.rows.size() == 3, "NULL row count");
        require(data.rows.get(0).get(0) == null, "semantic NULL");
        require(
                data.rows.get(1).get(0).equals("\\N"),
                "quoted NULL token TEXT");
        require(
                data.rows.get(2).get(0).equals(""),
                "empty TEXT");
    }

    private static void testQuotedCommaQuoteAndLf() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        ExecutionModel.RelationData data =
                read(
                        "value\n" +
                        "\"a,b\"\n" +
                        "\"a\"\"b\"\n" +
                        "\"a\nb\"\n",
                        schema);

        require(
                data.rows.get(0).get(0).equals("a,b"),
                "quoted comma");

        require(
                data.rows.get(1).get(0).equals("a\"b"),
                "escaped quote");

        require(
                data.rows.get(2).get(0).equals("a\nb"),
                "quoted LF");
    }

    private static void testHeaderMismatchRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        expectFailure(
                "wrong\nA\n",
                schema,
                "header column");
    }

    private static void testWrongWidthRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("a", SemanticModel.Type.TEXT, false),
                                column("b", SemanticModel.Type.TEXT, false)));

        expectFailure(
                "a,b\nA\n",
                schema,
                "fields; expected");
    }

    private static void testMalformedQuoteRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        expectFailure(
                "value\na\"b\n",
                schema,
                "double quote in unquoted field");
    }

    private static void testNonNullableNullRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        expectFailure(
                "value\n\\N\n",
                schema,
                "not nullable");
    }

    private static void testInvalidTypedValueRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column(
                                        "value",
                                        SemanticModel.Type.INTEGER,
                                        false)));

        expectFailure(
                "value\nabc\n",
                schema,
                "cannot represent INTEGER");
    }

    private static void testCrRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        expectFailure(
                "value\r\nA\r\n",
                schema,
                "contains CR");
    }

    private static void testMissingTerminalLfRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        expectFailure(
                "value\nA",
                schema,
                "must end with LF");
    }

    private static void testBomRejected() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                column("value", SemanticModel.Type.TEXT, false)));

        byte[] body =
                "value\nA\n"
                        .getBytes(
                                StandardCharsets.UTF_8);

        byte[] bytes =
                new byte[body.length + 3];

        bytes[0] = (byte) 0xef;
        bytes[1] = (byte) 0xbb;
        bytes[2] = (byte) 0xbf;

        System.arraycopy(
                body,
                0,
                bytes,
                3,
                body.length);

        try {
            ExpectedCsvReader.read(
                    bytes,
                    schema);

            throw new AssertionError(
                    "Expected BOM failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains("BOM"),
                    "BOM failure message");
        }
    }

    private static ExecutionModel.RelationData read(
            String csv,
            SemanticModel.Schema schema) {

        return ExpectedCsvReader.read(
                csv.getBytes(
                        StandardCharsets.UTF_8),
                schema);
    }

    private static void expectFailure(
            String csv,
            SemanticModel.Schema schema,
            String messageFragment) {

        try {
            read(csv, schema);

            throw new AssertionError(
                    "Expected reader failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            messageFragment),
                    "reader failure message: " +
                    ex.getMessage());
        }
    }

    private static SemanticModel.Column column(
            String name,
            SemanticModel.Type type,
            boolean nullable) {

        return new SemanticModel.Column(
                name,
                type,
                nullable);
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
