import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class ExpectedCsvReader {

    private ExpectedCsvReader() {
    }

    private static final class Field {
        final String text;
        final boolean quoted;

        Field(
                String text,
                boolean quoted) {

            this.text =
                    Objects.requireNonNull(text);

            this.quoted = quoted;
        }
    }

    public static ExecutionModel.RelationData read(
            byte[] bytes,
            SemanticModel.Schema schema) {

        Objects.requireNonNull(bytes);
        Objects.requireNonNull(schema);

        String text =
                decodeUtf8(bytes);

        validateDocumentEnvelope(text);

        List<List<Field>> records =
                parseRecords(text);

        if (records.isEmpty()) {
            throw new IllegalArgumentException(
                    "expected CSV requires header record");
        }

        validateHeader(
                records.get(0),
                schema);

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        for (int recordIndex = 1;
             recordIndex < records.size();
             recordIndex++) {

            List<Field> record =
                    records.get(recordIndex);

            validateWidth(
                    record,
                    schema,
                    recordIndex + 1);

            List<Object> values =
                    new ArrayList<>();

            for (int columnIndex = 0;
                 columnIndex < schema.columns.size();
                 columnIndex++) {

                values.add(
                        convertField(
                                record.get(columnIndex),
                                schema.columns.get(columnIndex),
                                recordIndex + 1));
            }

            rows.add(
                    new ExecutionModel.Row(
                            values));
        }

        return new ExecutionModel.RelationData(
                schema,
                rows);
    }

    private static String decodeUtf8(
            byte[] bytes) {

        if (bytes.length >= 3 &&
            (bytes[0] & 0xff) == 0xef &&
            (bytes[1] & 0xff) == 0xbb &&
            (bytes[2] & 0xff) == 0xbf) {

            throw new IllegalArgumentException(
                    "expected CSV must not contain UTF-8 BOM");
        }

        CharsetDecoder decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(
                                CodingErrorAction.REPORT)
                        .onUnmappableCharacter(
                                CodingErrorAction.REPORT);

        try {
            return decoder.decode(
                    ByteBuffer.wrap(bytes))
                    .toString();

        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException(
                    "expected CSV is not valid UTF-8",
                    ex);
        }
    }

    private static void validateDocumentEnvelope(
            String text) {

        if (text.isEmpty()) {
            throw new IllegalArgumentException(
                    "expected CSV is empty");
        }

        if (text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "expected CSV contains CR; canonical records require LF");
        }

        if (!text.endsWith("\n")) {
            throw new IllegalArgumentException(
                    "expected CSV must end with LF");
        }
    }

    private static List<List<Field>> parseRecords(
            String text) {

        List<List<Field>> records =
                new ArrayList<>();

        List<Field> record =
                new ArrayList<>();

        StringBuilder field =
                new StringBuilder();

        boolean quoted = false;

        enum State {
            START_FIELD,
            IN_UNQUOTED,
            IN_QUOTED,
            AFTER_QUOTE
        }

        State state =
                State.START_FIELD;

        for (int i = 0;
             i < text.length();
             i++) {

            char ch =
                    text.charAt(i);

            switch (state) {
                case START_FIELD:
                    if (ch == ',') {
                        record.add(
                                new Field(
                                        "",
                                        false));

                    } else if (ch == '"') {
                        quoted = true;
                        state =
                                State.IN_QUOTED;

                    } else if (ch == '\n') {
                        record.add(
                                new Field(
                                        "",
                                        false));

                        records.add(
                                List.copyOf(record));

                        record =
                                new ArrayList<>();

                    } else {
                        field.append(ch);
                        state =
                                State.IN_UNQUOTED;
                    }
                    break;

                case IN_UNQUOTED:
                    if (ch == ',') {
                        record.add(
                                new Field(
                                        field.toString(),
                                        false));

                        field.setLength(0);
                        state =
                                State.START_FIELD;

                    } else if (ch == '\n') {
                        record.add(
                                new Field(
                                        field.toString(),
                                        false));

                        field.setLength(0);

                        records.add(
                                List.copyOf(record));

                        record =
                                new ArrayList<>();

                        state =
                                State.START_FIELD;

                    } else if (ch == '"') {
                        throw malformed(
                                "double quote in unquoted field",
                                i);

                    } else {
                        field.append(ch);
                    }
                    break;

                case IN_QUOTED:
                    if (ch == '"') {
                        state =
                                State.AFTER_QUOTE;

                    } else {
                        field.append(ch);
                    }
                    break;

                case AFTER_QUOTE:
                    if (ch == '"') {
                        field.append('"');
                        state =
                                State.IN_QUOTED;

                    } else if (ch == ',') {
                        record.add(
                                new Field(
                                        field.toString(),
                                        quoted));

                        field.setLength(0);
                        quoted = false;
                        state =
                                State.START_FIELD;

                    } else if (ch == '\n') {
                        record.add(
                                new Field(
                                        field.toString(),
                                        quoted));

                        field.setLength(0);
                        quoted = false;

                        records.add(
                                List.copyOf(record));

                        record =
                                new ArrayList<>();

                        state =
                                State.START_FIELD;

                    } else {
                        throw malformed(
                                "unexpected character after closing quote",
                                i);
                    }
                    break;

                default:
                    throw new IllegalStateException(
                            "unknown CSV parser state");
            }
        }

        if (state == State.IN_QUOTED) {
            throw new IllegalArgumentException(
                    "expected CSV has unterminated quoted field");
        }

        /*
         * Canonical input ends in LF, so every record has
         * already been committed by the loop.
         */
        if (state != State.START_FIELD ||
            !record.isEmpty() ||
            field.length() != 0) {

            throw new IllegalArgumentException(
                    "expected CSV has malformed terminal record");
        }

        return records;
    }

    private static void validateHeader(
            List<Field> header,
            SemanticModel.Schema schema) {

        validateWidth(
                header,
                schema,
                1);

        for (int i = 0;
             i < schema.columns.size();
             i++) {

            String expected =
                    schema.columns.get(i).name;

            String actual =
                    header.get(i).text;

            if (!actual.equals(expected)) {
                throw new IllegalArgumentException(
                        "expected CSV header column " +
                        (i + 1) +
                        " is " + actual +
                        "; expected " + expected);
            }
        }
    }

    private static void validateWidth(
            List<Field> record,
            SemanticModel.Schema schema,
            int recordNumber) {

        if (record.size() !=
            schema.columns.size()) {

            throw new IllegalArgumentException(
                    "expected CSV record " +
                    recordNumber +
                    " has " +
                    record.size() +
                    " fields; expected " +
                    schema.columns.size());
        }
    }

    private static Object convertField(
            Field field,
            SemanticModel.Column column,
            int recordNumber) {

        if (!field.quoted &&
            "\\N".equals(field.text)) {

            if (!column.nullable) {
                throw new IllegalArgumentException(
                        "expected CSV record " +
                        recordNumber +
                        " column " +
                        column.name +
                        " is NULL but column is not nullable");
            }

            return null;
        }

        String text =
                field.text;

        try {
            switch (column.type) {
                case TEXT:
                    return text;

                case INTEGER:
                    return parseInteger(text);

                case DECIMAL:
                    return new BigDecimal(text);

                case BOOLEAN:
                    if ("true".equals(text)) {
                        return Boolean.TRUE;
                    }

                    if ("false".equals(text)) {
                        return Boolean.FALSE;
                    }

                    throw new IllegalArgumentException(
                            "BOOLEAN requires true or false");

                case DATE:
                    return LocalDate.parse(text);

                default:
                    throw new IllegalArgumentException(
                            "unsupported expected CSV type: " +
                            column.type);
            }

        } catch (NumberFormatException |
                 DateTimeParseException ex) {

            throw invalidValue(
                    recordNumber,
                    column,
                    text,
                    ex);

        } catch (IllegalArgumentException ex) {

            if (ex.getMessage() != null &&
                ex.getMessage().startsWith(
                        "expected CSV record ")) {

                throw ex;
            }

            throw invalidValue(
                    recordNumber,
                    column,
                    text,
                    ex);
        }
    }

    private static Long parseInteger(
            String text) {

        if (text.isEmpty()) {
            throw new NumberFormatException(
                    "empty INTEGER");
        }

        return Long.valueOf(text);
    }

    private static IllegalArgumentException invalidValue(
            int recordNumber,
            SemanticModel.Column column,
            String text,
            Exception cause) {

        return new IllegalArgumentException(
                "expected CSV record " +
                recordNumber +
                " column " +
                column.name +
                " cannot represent " +
                column.type +
                ": " +
                text,
                cause);
    }

    private static IllegalArgumentException malformed(
            String message,
            int offset) {

        return new IllegalArgumentException(
                "malformed expected CSV at character " +
                offset +
                ": " +
                message);
    }
}
