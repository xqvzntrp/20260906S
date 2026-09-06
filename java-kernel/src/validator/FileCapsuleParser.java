import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class FileCapsuleParser
        implements CapsuleParser {

    @Override
    public RawModel.Capsule parse(
            Path capsuleRoot) {

        Objects.requireNonNull(capsuleRoot);

        Path root =
                capsuleRoot.toAbsolutePath()
                           .normalize();

        Map<String,Object> document =
                readObject(
                        root.resolve(
                                "capsule.json"));

        Map<String,Object> capsule =
                object(
                        document.get("capsule"),
                        "capsule");

        String id =
                requiredString(
                        capsule.get("id"),
                        "capsule.id");

        String version =
                requiredString(
                        capsule.get("version"),
                        "capsule.version");

        List<RawModel.Input> inputs =
                new ArrayList<>();

        for (Object value :
                array(
                        document.get("inputs"),
                        "inputs")) {

            Map<String,Object> input =
                    object(value, "input");

            inputs.add(
                    new RawModel.Input(
                            requiredString(
                                    input.get("id"),
                                    "input.id"),
                            requiredString(
                                    input.get("path"),
                                    "input.path"),
                            requiredString(
                                    input.get("schema_path"),
                                    "input.schema_path"),
                            requiredString(
                                    input.get("format"),
                                    "input.format")));
        }

        List<RawModel.Relation> relations =
                new ArrayList<>();

        for (Object value :
                array(
                        document.get("relations"),
                        "relations")) {

            Map<String,Object> relation =
                    object(value, "relation");

            relations.add(
                    new RawModel.Relation(
                            requiredString(
                                    relation.get("id"),
                                    "relation.id"),
                            requiredString(
                                    relation.get("path"),
                                    "relation.path"),
                            requiredString(
                                    relation.get("format"),
                                    "relation.format")));
        }

        List<RawModel.Output> outputs =
                new ArrayList<>();

        for (Object value :
                array(
                        document.get("outputs"),
                        "outputs")) {

            Map<String,Object> output =
                    object(value, "output");

            outputs.add(
                    new RawModel.Output(
                            requiredString(
                                    output.get("id"),
                                    "output.id"),
                            requiredString(
                                    output.get("relation"),
                                    "output.relation"),
                            requiredString(
                                    output.get("generated_path"),
                                    "output.generated_path"),
                            requiredString(
                                    output.get("expected_path"),
                                    "output.expected_path"),
                            requiredString(
                                    output.get("schema_path"),
                                    "output.schema_path"),
                            requiredString(
                                    output.get("format"),
                                    "output.format")));
        }

        return new RawModel.Capsule(
                id,
                version,
                inputs,
                relations,
                outputs);
    }

    @Override
    public RawModel.RelationDocument relation(
            RawModel.Relation declaration,
            Path capsuleRoot) {

        Objects.requireNonNull(declaration);
        Objects.requireNonNull(capsuleRoot);

        Path path =
                resolveInsideRoot(
                        capsuleRoot,
                        declaration.path);

        Map<String,Object> document =
                readObject(path);

        String relationId =
                requiredString(
                        document.get("relation_id"),
                        "relation_id");

        String description =
                optionalString(
                        document.get("description"));

        List<RawModel.Binding> bindings =
                new ArrayList<>();

        Object inputValue =
                document.get("inputs");

        if (inputValue != null) {
            Map<String,Object> inputs =
                    object(
                            inputValue,
                            "inputs");

            for (Map.Entry<String,Object> entry :
                    inputs.entrySet()) {

                bindings.add(
                        new RawModel.Binding(
                                entry.getKey(),
                                requiredString(
                                        entry.getValue(),
                                        "relation input " +
                                        entry.getKey())));
            }
        }

        List<RawModel.Step> steps =
                new ArrayList<>();

        for (Object value :
                array(
                        document.get("steps"),
                        "steps")) {

            steps.add(
                    parseStep(
                            object(value, "step")));
        }

        String output =
                requiredString(
                        document.get("output"),
                        "output");

        return new RawModel.RelationDocument(
                relationId,
                description,
                bindings,
                steps,
                output);
    }

    @Override
    public RawSchema schema(
            String relativePath,
            Path capsuleRoot) {

        Objects.requireNonNull(relativePath);
        Objects.requireNonNull(capsuleRoot);

        Path path =
                resolveInsideRoot(
                        capsuleRoot,
                        relativePath);

        Map<String,Object> document =
                readObject(path);

        String schemaId =
                requiredString(
                        document.get("schema_id"),
                        "schema_id");

        String version =
                requiredString(
                        document.get("version"),
                        "version");

        String relation =
                requiredString(
                        document.get("relation"),
                        "relation");

        List<RawSchema.Column> columns =
                new ArrayList<>();

        for (Object value :
                array(
                        document.get("columns"),
                        "columns")) {

            Map<String,Object> column =
                    object(
                            value,
                            "schema column");

            columns.add(
                    new RawSchema.Column(
                            requiredString(
                                    column.get("name"),
                                    "column.name"),
                            requiredString(
                                    column.get("type"),
                                    "column.type"),
                            requiredBoolean(
                                    column.get("nullable"),
                                    "column.nullable"),
                            optionalInteger(
                                    column.get("precision"),
                                    "column.precision"),
                            optionalInteger(
                                    column.get("scale"),
                                    "column.scale")));
        }

        return new RawSchema(
                schemaId,
                version,
                relation,
                columns);
    }

    private static RawModel.Step parseStep(
            Map<String,Object> step) {

        String id =
                requiredString(
                        step.get("id"),
                        "step.id");

        String op =
                requiredString(
                        step.get("op"),
                        "step.op");

        String from =
                optionalString(
                        step.get("from"));

        String left =
                optionalString(
                        step.get("left"));

        String right =
                optionalString(
                        step.get("right"));

        RawModel.Condition where =
                parseCondition(
                        step.get("where"));

        List<RawModel.Condition> on =
                new ArrayList<>();

        Object onValue =
                step.get("on");

        if (onValue != null) {
            for (Object value :
                    array(onValue, "step.on")) {

                on.add(
                        parseConditionObject(
                                value,
                                "step.on condition"));
            }
        }

        List<String> groupBy =
                stringList(
                        step.get("group_by"),
                        "step.group_by");

        List<RawModel.Measure> measures =
                new ArrayList<>();

        Object measuresValue =
                step.get("measures");

        if (measuresValue != null) {
            for (Object value :
                    array(
                            measuresValue,
                            "step.measures")) {

                Map<String,Object> measure =
                        object(
                                value,
                                "measure");

                measures.add(
                        new RawModel.Measure(
                                requiredString(
                                        measure.get("name"),
                                        "measure.name"),
                                requiredString(
                                        measure.get("function"),
                                        "measure.function"),
                                optionalString(
                                        measure.get("column"))));
            }
        }

        List<RawModel.DerivedColumn> columns =
                new ArrayList<>();

        Object columnsValue =
                step.get("columns");

        if (columnsValue != null) {
            for (Object value :
                    array(
                            columnsValue,
                            "step.columns")) {

                Map<String,Object> column =
                        object(
                                value,
                                "derived column");

                columns.add(
                        new RawModel.DerivedColumn(
                                requiredString(
                                        column.get("name"),
                                        "derived column.name"),
                                parseExpression(
                                        column.get("expression")),
                                optionalInteger(
                                        column.get("precision"),
                                        "derived column.precision"),
                                optionalInteger(
                                        column.get("scale"),
                                        "derived column.scale")));
            }
        }

        List<String> select =
                stringList(
                        step.get("select"),
                        "step.select");

        List<String> orderBy =
                stringList(
                        step.get("order_by"),
                        "step.order_by");

        Map<String,String> rename =
                stringMap(
                        step.get("rename"),
                        "step.rename");

        return new RawModel.Step(
                id,
                op,
                from,
                left,
                right,
                where,
                on,
                groupBy,
                measures,
                columns,
                select,
                orderBy,
                rename);
    }

    private static RawModel.Condition parseCondition(
            Object value) {

        if (value == null) {
            return null;
        }

        return parseConditionObject(
                value,
                "condition");
    }

    private static RawModel.Condition parseConditionObject(
            Object value,
            String context) {

        Map<String,Object> condition =
                object(value, context);

        Object leftValue =
                condition.get("left");

        if (leftValue == null) {
            leftValue =
                    condition.get("column");
        }

        Object rightValue =
                condition.get("right");

        if (rightValue == null &&
            condition.containsKey("value")) {

            rightValue =
                    Map.of(
                            "value",
                            condition.get("value"));
        }

        if (leftValue == null) {
            throw new IllegalArgumentException(
                    context +
                    " requires left or column");
        }

        return new RawModel.Condition(
                parseExpression(leftValue),
                requiredString(
                        condition.get("operator"),
                        context + ".operator"),
                rightValue == null
                        ? RawModel.RawExpression.literal(null)
                        : parseExpression(rightValue));
    }

    private static RawModel.RawExpression parseExpression(
            Object value) {

        if (value instanceof String) {
            return RawModel.RawExpression.column(
                    (String) value);
        }

        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(
                    "expression must be an object or column string; value=" +
                    String.valueOf(value));
        }

        Map<String,Object> expression =
                object(value, "expression");

        String column =
                optionalString(
                        expression.get("column"));

        if (column != null) {
            return RawModel.RawExpression.column(
                    column);
        }

        if (expression.containsKey("value")) {
            return RawModel.RawExpression.literal(
                    expression.get("value"));
        }

        Object condition =
                expression.get("condition");

        Object thenBranch =
                expression.get("then");

        Object elseBranch =
                expression.get("else");

        if (condition != null ||
            thenBranch != null ||
            elseBranch != null) {

            return RawModel.RawExpression.conditional(
                    parseConditionObject(
                            condition,
                            "expression.condition"),
                    parseExpression(
                            thenBranch),
                    parseExpression(
                            elseBranch));
        }

        String function =
                optionalString(
                        expression.get("function"));

        if (function != null) {
            List<RawModel.RawExpression> arguments =
                    new ArrayList<>();

            Object args =
                    expression.get("args");

            if (args != null) {
                for (Object argument :
                        array(
                                args,
                                "expression.args")) {

                    arguments.add(
                            parseExpression(argument));
                }
            }

            return new RawModel.RawExpression(
                    RawModel.RawExpression.Kind.FUNCTION,
                    null,
                    null,
                    function,
                    arguments,
                    null,
                    null,
                    null);
        }

        throw new IllegalArgumentException(
                "expression has no supported shape");
    }

    private static Integer optionalInteger(
            Object value,
            String context) {

        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Byte ||
                value instanceof Short ||
                value instanceof Integer ||
                value instanceof Long) {

                long number =
                        ((Number) value).longValue();

                if (number < Integer.MIN_VALUE ||
                    number > Integer.MAX_VALUE) {

                    throw new IllegalArgumentException(
                            context +
                            " is outside INTEGER range: " +
                            number);
                }

                return (int) number;
            }

            if (value instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) value)
                        .intValueExact();
            }

        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    context +
                    " must be an integer",
                    ex);
        }

        throw new IllegalArgumentException(
                context +
                " must be an integer");
    }

    private static Map<String,Object> readObject(
            Path path) {

        try {
            String text =
                    Files.readString(
                            path,
                            StandardCharsets.UTF_8);

            if (!text.isEmpty() &&
                text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }

            Object value =
                    new JsonParser(text).parse();

            return object(
                    value,
                    path.toString());

        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "cannot read JSON: " +
                    path,
                    ex);
        }
    }

    private static Path resolveInsideRoot(
            Path root,
            String relative) {

        Path normalizedRoot =
                root.toAbsolutePath()
                   .normalize();

        Path target =
                normalizedRoot.resolve(
                        relative)
                .normalize();

        if (!target.startsWith(
                normalizedRoot)) {

            throw new IllegalArgumentException(
                    "path escapes capsule root: " +
                    relative);
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> object(
            Object value,
            String context) {

        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(
                    context +
                    " must be an object");
        }

        return (Map<String,Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(
            Object value,
            String context) {

        if (!(value instanceof List)) {
            throw new IllegalArgumentException(
                    context +
                    " must be an array");
        }

        return (List<Object>) value;
    }

    private static String requiredString(
            Object value,
            String context) {

        if (!(value instanceof String) ||
            ((String) value).isEmpty()) {

            throw new IllegalArgumentException(
                    context +
                    " must be a non-empty string");
        }

        return (String) value;
    }

    private static String optionalString(
            Object value) {

        return value instanceof String
                ? (String) value
                : null;
    }

    private static boolean requiredBoolean(
            Object value,
            String context) {

        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    context +
                    " must be boolean");
        }

        return (Boolean) value;
    }

    private static List<String> stringList(
            Object value,
            String context) {

        if (value == null) {
            return List.of();
        }

        List<String> result =
                new ArrayList<>();

        for (Object item :
                array(value, context)) {

            result.add(
                    requiredString(
                            item,
                            context + " item"));
        }

        return result;
    }

    private static Map<String,String> stringMap(
            Object value,
            String context) {

        if (value == null) {
            return Map.of();
        }

        Map<String,Object> source =
                object(value, context);

        Map<String,String> result =
                new LinkedHashMap<>();

        for (Map.Entry<String,Object> entry :
                source.entrySet()) {

            result.put(
                    entry.getKey(),
                    requiredString(
                            entry.getValue(),
                            context +
                            " value for " +
                            entry.getKey()));
        }

        return result;
    }

    private static final class JsonParser {

        private final String text;
        private int index;

        JsonParser(String text) {
            this.text =
                    Objects.requireNonNull(text);
        }

        Object parse() {
            skipWhitespace();

            Object value =
                    parseValue();

            skipWhitespace();

            if (index != text.length()) {
                throw error(
                        "unexpected trailing content");
            }

            return value;
        }

        private Object parseValue() {
            skipWhitespace();

            if (index >= text.length()) {
                throw error("unexpected end of JSON");
            }

            char ch =
                    text.charAt(index);

            switch (ch) {
                case '{':
                    return parseObject();

                case '[':
                    return parseArray();

                case '"':
                    return parseString();

                case 't':
                    expect("true");
                    return Boolean.TRUE;

                case 'f':
                    expect("false");
                    return Boolean.FALSE;

                case 'n':
                    expect("null");
                    return null;

                default:
                    if (ch == '-' ||
                        Character.isDigit(ch)) {

                        return parseNumber();
                    }

                    throw error(
                            "unexpected character: " +
                            ch);
            }
        }

        private Map<String,Object> parseObject() {
            LinkedHashMap<String,Object> result =
                    new LinkedHashMap<>();

            expectChar('{');
            skipWhitespace();

            if (peek('}')) {
                index++;
                return result;
            }

            while (true) {
                skipWhitespace();

                String key =
                        parseString();

                skipWhitespace();
                expectChar(':');

                Object value =
                        parseValue();

                result.put(
                        key,
                        value);

                skipWhitespace();

                if (peek('}')) {
                    index++;
                    return result;
                }

                expectChar(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> result =
                    new ArrayList<>();

            expectChar('[');
            skipWhitespace();

            if (peek(']')) {
                index++;
                return result;
            }

            while (true) {
                result.add(
                        parseValue());

                skipWhitespace();

                if (peek(']')) {
                    index++;
                    return result;
                }

                expectChar(',');
            }
        }

        private String parseString() {
            expectChar('"');

            StringBuilder out =
                    new StringBuilder();

            while (index < text.length()) {
                char ch =
                        text.charAt(index++);

                if (ch == '"') {
                    return out.toString();
                }

                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw error(
                                "unterminated escape");
                    }

                    char escape =
                            text.charAt(index++);

                    switch (escape) {
                        case '"':
                        case '\\':
                        case '/':
                            out.append(escape);
                            break;

                        case 'b':
                            out.append('\b');
                            break;

                        case 'f':
                            out.append('\f');
                            break;

                        case 'n':
                            out.append('\n');
                            break;

                        case 'r':
                            out.append('\r');
                            break;

                        case 't':
                            out.append('\t');
                            break;

                        case 'u':
                            out.append(
                                    (char) Integer.parseInt(
                                            readHex(4),
                                            16));
                            break;

                        default:
                            throw error(
                                    "invalid escape: " +
                                    escape);
                    }

                } else {
                    out.append(ch);
                }
            }

            throw error(
                    "unterminated string");
        }

        private String readHex(
                int count) {

            if (index + count >
                text.length()) {

                throw error(
                        "incomplete unicode escape");
            }

            String value =
                    text.substring(
                            index,
                            index + count);

            index += count;
            return value;
        }

        private Number parseNumber() {
            int start = index;

            if (peek('-')) {
                index++;
            }

            while (index < text.length() &&
                   Character.isDigit(
                           text.charAt(index))) {
                index++;
            }

            boolean decimal =
                    false;

            if (peek('.')) {
                decimal = true;
                index++;

                while (index < text.length() &&
                       Character.isDigit(
                               text.charAt(index))) {
                    index++;
                }
            }

            if (peek('e') ||
                peek('E')) {

                decimal = true;
                index++;

                if (peek('+') ||
                    peek('-')) {
                    index++;
                }

                while (index < text.length() &&
                       Character.isDigit(
                               text.charAt(index))) {
                    index++;
                }
            }

            String token =
                    text.substring(
                            start,
                            index);

            try {
                return decimal
                        ? new java.math.BigDecimal(token)
                        : Long.valueOf(token);

            } catch (NumberFormatException ex) {
                throw error(
                        "invalid number: " +
                        token);
            }
        }

        private void expect(
                String token) {

            if (!text.startsWith(
                    token,
                    index)) {

                throw error(
                        "expected " +
                        token);
            }

            index += token.length();
        }

        private void expectChar(
                char expected) {

            if (index >= text.length() ||
                text.charAt(index) !=
                        expected) {

                throw error(
                        "expected '" +
                        expected +
                        "'");
            }

            index++;
        }

        private boolean peek(
                char expected) {

            return index < text.length() &&
                   text.charAt(index) ==
                           expected;
        }

        private void skipWhitespace() {
            while (index < text.length() &&
                   Character.isWhitespace(
                           text.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(
                String message) {

            return new IllegalArgumentException(
                    "malformed JSON at character " +
                    index +
                    ": " +
                    message);
        }
    }
}
